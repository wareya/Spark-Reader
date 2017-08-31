
import language.dictionary.DefSource;
import language.dictionary.Definition;
import language.dictionary.Japanese;
import language.dictionary.UserDefinition;
import language.segmenter.BasicSegmenter;
import main.Main;
import language.segmenter.Segmenter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.atilika.kuromoji.unidic.Token;
import com.atilika.kuromoji.unidic.Tokenizer;
import options.Underlay;

import javax.swing.*;

import static main.Main.options;

class HeavySegmenter extends Segmenter
{
    Tokenizer kuro;
    String kuromojiUserdict = null;
    static HashSet<String> badSegments = null;

    private void ensureInitialized()
    {
        if(badSegments == null)
            badSegments = new HashSet<>(Underlay.badSegments);
        if(kuro == null)
        {
            if(kuromojiUserdict == null)
            {
                kuromojiUserdict = "";
                for(Definition definition : DefSource.getSource("Custom").getDefinitions())
                {
                    UserDefinition def = (UserDefinition)definition;
                    String word = "";
                    if(def.getWord().length > 0)
                        word = def.getWord()[0];
                    String reading = "";
                    if(def.getReadings().length > 0)
                        reading = Japanese.toKatakana(def.getReadings()[0], true);
                    
                    /*
                    // Incorrect user dictionary format
                    //警泥,5145,5145,7380,名詞,普通名詞,一般,*,*,*,ケイドロ,警泥,警泥,ケードロ,警泥,ケードロ,混,*,*,*,*
                    //A,   5145,5145,C,   名詞,普通名詞,一般,*,*,*,B,       A,   A,   B        ,A,  B,混,*,*,*,*
                    kuromojiUserdict += String.format("%s,5145,5145,%d,名詞,普通名詞,一般,*,*,*,%s,%s,%s,%s,%s,%s,混,*,*,*,*\n",
                        word, options.getOptionInt("kuromojiUserdictWeight"), reading, word, word, reading, word, reading); 
                     */
                    // Real one. Note that kuromoji 1.0 is going to add support for better custom dictionary stuff 
                    // 東京スカイツリー,東京 スカイツリー,トウキョウ スカイツリー,カスタム名詞
                    // A,A,B,カスタム名詞
                    kuromojiUserdict += String.format("%s,%s,%s,カスタム名詞\n",
                        word, word, reading);
                }
            }
            System.out.println("Userdict string:");
            System.out.println(kuromojiUserdict);
            try
            {
                kuro = new Tokenizer.Builder()
                // kuromoji-unidic doesn't support this, unfortunately.
                //.mode(TokenizerBase.Mode.SEARCH) // punish long terms
                //.kanjiPenalty(options.getOptionInt("kuromojiKanjiPenaltyLength"), options.getOptionInt("kuromojiKanjiPenalty"))
                //.otherPenalty(options.getOptionInt("kuromojiOtherPenaltyLength"), options.getOptionInt("kuromojiOtherPenalty"))
                .userDictionary(new ByteArrayInputStream(kuromojiUserdict.getBytes("UTF-8")))
                .build();
            }
            catch (IOException e)
            { /* not thrown in our use case */ }
        }
    }

    public List<Token> DebugSegment(String text)
    {
        ensureInitialized();

        return kuro.tokenize(text);
    }
    
    // Considered a hack
    private boolean shouldForceUnigram(String s)
    {
        return (badSegments.contains(s));
    }
    
    private void add(ArrayList<Piece> r, Piece piece)
    {
        r.add(piece);
        System.out.println(piece.txt + (piece.strong?" (strong)":""));
    }
    
    private void addAsUnigram(ArrayList<Piece> r, String s)
    {
        for(char c : s.toCharArray())
            add(r, new Piece(c+"", false));
    }

    // unigram unknown tokens before adding them
    private void addWithUnigramCheck(ArrayList<Piece> r, Token t, boolean strong)
    {
        if(!t.isKnown())
            addAsUnigram(r, t.getSurface());
        else
            addWithUnigramCheck(r, new Piece(t.getSurface(), strong));
    }
    private void addWithUnigramCheck(ArrayList<Piece> r, Piece p)
    {
        if(shouldForceUnigram(p.txt))
        {
            addAsUnigram(r, p.txt);
        }
        else if(p.txt.endsWith("っ") && p.txt.length() > 1)
        {
            addWithUnigramCheck(r, new Piece(p.txt.substring(0, p.txt.length()-1), false));
            add(r, new Piece("っ", false));
        }
        else
            add(r, p);
    }

    public List<Piece> Segment(String text)
    {
        if(options.getOption("kuromojiSupportLevel").equals("disabled"))
            return Segmenter.basicInstance.Segment(text);

        ensureInitialized();
        List<Token> tokens;
        try
        {
            tokens = kuro.tokenize(text);
        }
        catch(StringIndexOutOfBoundsException e)
        {
            JOptionPane.showMessageDialog(null, "Kuromoji threw an exception interally.\nThis probably means the user dictionary is formatted funny.\nSpark Reader has not crashed.");
            kuro = null;
            
            return Segmenter.basicInstance.Segment(text);
        }
        ArrayList<Piece> r = new ArrayList<>();
        
        System.out.println("Kuromoji output:");
        for(Token t : tokens)
            System.out.println(t.getSurface());

        for(int i = 0; i < tokens.size(); i++)
        {
            Token t = tokens.get(i);
            if(!options.getOption("kuromojiSupportLevel").equals("heuristics"))
            {
                addWithUnigramCheck(r, t, false);
            }
            // Reduce segmentation errors caused by the word splitter binding the segment list back together into words
            // Method 1: directly combining single-character verb stems with their auxiliaries, so they don't get bound to an earlier word (しかい, ことし)
            // Method 2: make strong segmentations between token sequences that often cause problems (particles followed by non-particles) (はそう, がいい)
            // Wouldn't be necessary if the segmenter exposed more information to the word splitter, but that's just the way that it goes.
            else
            {
                
                // "Strong" means that a segment forces the word splitter to make a segment after it iff it is the first segment in the current attempt at finding a word.
                boolean strong = false;
                // "Weak" means that a segment is forced to merge with the next one and the merged segment is not strong, with higher priority than strongness.
                boolean weak = false;
                // "Bipolar" means that a segment is forced to merge with the next one *and* the merged segment is strong, with maximum priority.
                boolean bipolar = false;
                
                for(Underlay.Heuristic heuristic : Underlay.heuristics)
                {
                    boolean state = true;
                    
                    for(Underlay.HeuristicRule rule : heuristic.rules)
                    {
                        int index = i+rule.index;
                        if(index < 0 || index >= tokens.size())
                        {
                            System.out.println("breaking a heuristic rule");
                            state = false;
                            break;
                        }
                        Token token = tokens.get(index);
                        
                        if(rule.trait.equals("unigram"))
                        {
                            state = state && (token.getSurface().length() == 1);
                            continue;
                        }
                        
                        String data = "";
                        if(rule.trait.equals("pos1"))
                            data = token.getPartOfSpeechLevel1();
                        else if(rule.trait.equals("pos2"))
                            data = token.getPartOfSpeechLevel2();
                        else if(rule.trait.equals("pos3"))
                            data = token.getPartOfSpeechLevel3();
                        else if(rule.trait.equals("pos4"))
                            data = token.getPartOfSpeechLevel4();
                        else if(rule.trait.equals("conjf"))
                            data = token.getConjugationForm();
                        else if(rule.trait.equals("conjt"))
                            data = token.getConjugationType();
                        else if(rule.trait.equals("fsoundf"))
                            data = token.getFinalSoundAlterationForm();
                        else if(rule.trait.equals("fsoundt"))
                            data = token.getFinalSoundAlterationType();
                        else if(rule.trait.equals("isoundf"))
                            data = token.getInitialSoundAlterationForm();
                        else if(rule.trait.equals("isoundt"))
                            data = token.getInitialSoundAlterationType();
                        else if(rule.trait.equals("lang"))
                            data = token.getLanguageType();
                        else if(rule.trait.equals("lemma"))
                            data = token.getLemma();
                        else if(rule.trait.equals("lemmareading"))
                            data = token.getLemmaReadingForm();
                        else if(rule.trait.equals("pron"))
                            data = token.getPronunciation();
                        else if(rule.trait.equals("pronbase"))
                            data = token.getPronunciationBaseForm();
                        else if(rule.trait.equals("written"))
                            data = token.getWrittenForm();
                        else if(rule.trait.equals("writtenbase"))
                            data = token.getWrittenBaseForm();
                        else if(rule.trait.equals("surface"))
                            data = token.getSurface();
                        
                        if(rule.operation.equals("is"))
                            state = state && data.equals(rule.argument);
                        else if(rule.operation.equals("not"))
                            state = state && !data.equals(rule.argument);
                        else if(rule.operation.equals("has"))
                            state = state && data.contains(rule.argument);
                        else if(rule.operation.equals("starts"))
                            state = state && data.startsWith(rule.argument);
                            
                    }
                    if(heuristic.type.equals("strong"))
                        strong = strong || state;
                    else if(heuristic.type.equals("weak"))
                        weak = weak || state;
                    else if(heuristic.type.equals("bipolar"))
                        bipolar = bipolar || state;
                }
                if(bipolar)
                {
                    Token n = tokens.get(i+1);
                    addWithUnigramCheck(r, new Piece(t.getSurface()+n.getSurface(), true));
                    i++;
                }
                else if(weak)
                {
                    Token n = tokens.get(i+1);
                    addWithUnigramCheck(r, new Piece(t.getSurface()+n.getSurface(), false));
                    i++;
                }
                else
                {
                    addWithUnigramCheck(r, t, strong);
                }
                
            }
        }
        return r;
    }
    HeavySegmenter()
    {
    }
}

public class Heavy
{
    public static void main(String[] args) throws Exception
    {
        Segmenter.extended = true;
        Segmenter.instance = new HeavySegmenter();
        Segmenter.basicInstance = new BasicSegmenter();
        Main.main(args);
    }
}