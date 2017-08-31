
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
            if(i+1 == tokens.size() || !options.getOption("kuromojiSupportLevel").equals("heuristics"))
            {
                addWithUnigramCheck(r, t, false);
            }
            // Reduce segmentation errors caused by the word splitter binding the segment list back together into words
            // Method 1: directly combining single-character verb stems with their auxiliaries, so they don't get bound to an earlier word (しかい, ことし)
            // Method 2: make strong segmentations between token sequences that often cause problems (particles followed by non-particles) (はそう, がいい)
            // Wouldn't be necessary if the segmenter exposed more information to the word splitter, but that's just the way that it goes.
            else
            {
                Token n = tokens.get(i+1);
                
                // "Strong" means that a segment forces the word splitter to make a segment after it iff it is the first segment in the current attempt at finding a word.
                boolean strong = false;
                // "Weak" means that a segment is forced to merge with the next one, with higher priority than strongness
                boolean weak = false;
                // しかいなかった
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("副助詞")
                       && n.getPartOfSpeechLevel1().contains("動詞")
                       && n.getPartOfSpeechLevel2().contains("非自立可能"));
                // ～かと思う
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("終助詞")
                       && n.getPartOfSpeechLevel1().contains("助詞")
                       && n.getPartOfSpeechLevel2().contains("格助詞"));
                // ～さは (is the blacklist better for this?)
                strong = strong
                      || (t.getPartOfSpeechLevel1().contains("接尾辞")
                       && n.getPartOfSpeechLevel2().contains("係助詞"));
                // になると
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("動詞")
                       && t.getConjugationForm().equals("終止形-一般")
                       && n.getPartOfSpeechLevel1().equals("助詞"));
                // これですっ！
                strong = strong
                      || (t.getPartOfSpeechLevel2().equals("代名詞")
                       && t.getPartOfSpeechLevel3().equals("一般")
                       && !t.getSurface().equals("なん")// なんで・なんでもない
                       && n.getPartOfSpeechLevel2().equals("格助詞")
                       && n.getPartOfSpeechLevel3().equals("一般"));
                // はナシだって
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getPartOfSpeechLevel1().equals("名詞")
                       && n.getPartOfSpeechLevel2().equals("普通名詞"));
                // ことしたら
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("名詞")
                       && t.getPartOfSpeechLevel2().equals("普通名詞")
                       && t.getPartOfSpeechLevel3().equals("一般")
                       && n.getPartOfSpeechLevel1().equals("動詞")
                       && n.getPartOfSpeechLevel2().equals("非自立可能"));
                // はそう
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getPartOfSpeechLevel1().equals("副詞"));
                // はいつ
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getPartOfSpeechLevel1().equals("代名詞"));
                // ～くんで
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("接尾辞")
                       && t.getPartOfSpeechLevel2().equals("名詞的")
                       && n.getPartOfSpeechLevel1().equals("助詞")
                       && n.getPartOfSpeechLevel2().equals("格助詞"));
                // ここでしたい
                // にいない
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("格助詞")
                       && (t.getSurface().equals("で") || t.getSurface().equals("に"))
                       && n.getPartOfSpeechLevel1().equals("動詞"));
                // 俺がちゃんと作りますから
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && n.getPartOfSpeechLevel1().equals("副詞"));
                // 二冊目もお亡くなりに
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && n.getPartOfSpeechLevel1().equals("接頭辞")
                       && n.getSurface().equals("お"));
                // 都会に暮らしていた頃となんら変わらぬ環境
                strong = strong
                      || (t.getPartOfSpeechLevel2().equals("格助詞")
                       && n.getPartOfSpeechLevel1().equals("代名詞"));
                // なんだよそれ
                strong = strong
                      || (t.getPartOfSpeechLevel2().equals("終助詞")
                       && n.getPartOfSpeechLevel1().equals("代名詞"));
                // 僕はといえば
                strong = strong
                      || (t.getSurface().equals("は")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getSurface().equals("と")
                       && n.getPartOfSpeechLevel2().equals("格助詞"));
                // 安定はしている
                strong = strong
                      || (t.getSurface().equals("は")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getSurface().equals("し")
                       && n.getPartOfSpeechLevel1().equals("動詞"));
                // はあった
                strong = strong
                      || (t.getSurface().equals("は")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getSurface().equals("あっ")
                       && n.getPartOfSpeechLevel1().equals("動詞"));
                // はいらない
                strong = strong
                      || (t.getSurface().equals("は")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getSurface().equals("いら")
                       && n.getPartOfSpeechLevel1().equals("動詞"));
                // がい
                strong = strong
                      || (t.getSurface().equals("が")
                       && t.getPartOfSpeechLevel2().equals("格助詞")
                       && n.getSurface().startsWith("い"));
                       
                // はいけません
                strong = strong
                      || (t.getSurface().equals("は")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getSurface().startsWith("い"));
                
                if(i+2 < tokens.size())
                {
                    //のですが
                    Token m = tokens.get(i+2);
                    strong = strong
                        || ((t.getSurface().length() == 1 || m.getSurface().length() == 1) 
                         && t.getPartOfSpeechLevel2().equals("非自立")// しないでください
                         && m.getPartOfSpeechLevel2().equals("接続助詞")
                         && n.getPartOfSpeechLevel1().equals("助動詞"));
                }
                
                // どう説明したものか
                weak = weak
                    || (t.getPartOfSpeechLevel1().equals("動詞")
                     && t.getSurface().equals("し")
                     && n.getPartOfSpeechLevel1().equals("助動詞"));
                
                // 詰め込みすぎなんだよ
                weak = weak
                    || (t.getSurface().equals("な")
                     && t.getPartOfSpeechLevel1().equals("助動詞")
                     && n.getPartOfSpeechLevel1().equals("助詞")
                     && n.getLemma().equals("の"));
                
                // なのに彼女のその身にまとう衣装は奇抜であり。
                weak = weak
                    || (t.getSurface().equals("で")
                     && t.getConjugationType().equals("助動詞-ダ")
                     && n.getLemma().equals("有る"));
                
                // hardcoded heuristic because this one is complicated 
                // 強盗とかしてたのかな
                if (t.getSurface().equals("と")
                    && t.getPartOfSpeechLevel2().equals("格助詞")
                    && n.getLemma().equals("か")
                    && n.getPartOfSpeechLevel2().equals("副助詞"))
                {
                    addWithUnigramCheck(r, new Piece(t.getSurface()+n.getSurface(), true));
                    i++;
                }
                else if(weak)
                {
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