
import com.atilika.kuromoji.TokenizerBase;
import jdk.nashorn.internal.runtime.options.Options;
import language.dictionary.DefSource;
import language.dictionary.Definition;
import language.dictionary.UserDefinition;
import language.segmenter.BasicSegmenter;
import main.Main;
import language.segmenter.Segmenter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import static main.Main.options;

class HeavySegmenter extends Segmenter
{
    Tokenizer kuro;
    String kuromojiUserdict;

    Segmenter basic = new BasicSegmenter();

    private void ensureInitialized()
    {
        if(kuro == null)
        {
            kuromojiUserdict = "";
            for(Definition definition : DefSource.getSource("Custom").getDefinitions())
            {
                UserDefinition def = (UserDefinition)definition;
                //1 その他,間投,*,*,*,*,* (etc)
                //酔,1285,1285,4879,名詞,一般,*,*,*,*,酔,ヨイ,ヨイ
                //よ,1,1,6514,その他,間投,*,*,*,*,よ,ヨ,ヨ
                String word = "";
                if(def.getWord().length > 0)
                    word = def.getWord()[0];
                String reading = "*";
                if(def.getReadings().length > 0)
                    reading = def.getReadings()[0];
                else
                    reading = word;
                
                //kuromojiUserdict += String.format("%s,1,1,%d,その他,間投,*,*,*,*,%s,%s,%s\n",
                //using the "others" category makes kuromoji basically refuse to use the term, proving that it cares about morphological category
                // todo: map some word categories to the appropriate formats instead of loading everything as a noun
                kuromojiUserdict += String.format("%s,1285,1285,%d,名詞,一般,*,*,*,*,%s,%s,%s\n",
                    word, options.getOptionInt("kuromojiUserdictWeight"), word, reading, reading);
                
            }
            System.out.println("Userdict string:");
            System.out.println(kuromojiUserdict);
            try
            {
                kuro = new Tokenizer.Builder()
                .mode(TokenizerBase.Mode.SEARCH) // punish long terms
                .kanjiPenalty(options.getOptionInt("kuromojiKanjiPenaltyLength"), options.getOptionInt("kuromojiKanjiPenalty"))
                .otherPenalty(options.getOptionInt("kuromojiOtherPenaltyLength"), options.getOptionInt("kuromojiOtherPenalty"))
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
    static HashSet<String> badSegments = new HashSet<>(Arrays.asList(
        "すっ",
        "たろ",
        "てん",
        "てぇ",
        "てよ"
    ));
    private boolean shouldForceUnigram(String s)
    {
        return (badSegments.contains(s));
    }
    
    private void addAsUnigram(ArrayList<Piece> r, Token t)
    {
        for(char c : t.getSurface().toCharArray())
            r.add(new Piece(c+"", false));
    }

    // unigram unknown tokens before adding them
    private void addWithUnigramCheck(ArrayList<Piece> r, Token t, boolean strong)
    {
        if(!t.isKnown() || shouldForceUnigram(t.getSurface()))
        {
            addAsUnigram(r, t);
        }
        else if(t.getSurface().endsWith("っ") && t.getSurface().length() > 1)
        {
            r.add(new Piece(t.getSurface().substring(0, t.getSurface().length()-1), false));
            r.add(new Piece("っ", false));
        }
        else
            r.add(new Piece(t.getSurface(), strong));
    }

    public List<Piece> Segment(String text)
    {
        if(options.getOption("kuromojiSupportLevel").equals("disabled"))
            return basic.Segment(text);

        ensureInitialized();

        List<Token> tokens = kuro.tokenize(text);
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
                
                if(i-1 > 0)
                {
                    Token o = tokens.get(i-1);
                    
                    // Hack: kuromoji-unidic's internal weights for こ and として are bogus
                    if(o.getSurface().equals("こ") && t.getSurface().equals("として"))
                    {
                        addAsUnigram(r, t);
                        continue;
                    }
                    // same but with でし たい
                    if(o.getSurface().equals("でし") && t.getSurface().equals("たい"))
                    {
                        addAsUnigram(r, t);
                        continue;
                    }
                }

                // "Strong" means that a segment forces the word splitter to make a segment after it IF it is the first segment in the current attempt at finding a word.
                
                boolean strong = false;
                // しかいなかった
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("係助詞")
                       && n.getPartOfSpeechLevel1().contains("動詞")
                       && n.getPartOfSpeechLevel2().contains("自立"));
                // ～かと思う
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("終助詞")
                       && n.getPartOfSpeechLevel1().contains("助詞")
                       && n.getPartOfSpeechLevel2().contains("格助詞"));
                // ～さは (is the blacklist better for this?)
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("接尾")
                       && n.getPartOfSpeechLevel2().contains("係助詞"));
                // になると
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("動詞")
                       && t.getConjugationForm().equals("基本形")
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
                       && n.getPartOfSpeechLevel2().equals("一般"));
                // ことしたら
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("名詞")
                       && t.getPartOfSpeechLevel2().equals("非自立")
                       // && t.getPartOfSpeechLevel3().equals("一般")
                       && n.getPartOfSpeechLevel1().equals("動詞")
                       && n.getPartOfSpeechLevel2().equals("自立"));
                // はそう
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("係助詞")
                       && n.getPartOfSpeechLevel1().equals("副詞"));
                // どうしました
                strong = strong
                      || (t.getPartOfSpeechLevel2().equals("助詞類接続")
                       && n.getPartOfSpeechLevel1().equals("動詞")
                       && n.getPartOfSpeechLevel2().equals("自立"));
                // にいない
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("格助詞")
                       && n.getPartOfSpeechLevel1().equals("動詞")
                       && n.getPartOfSpeechLevel2().equals("自立")
                       && !n.getSurface().equals("すっ")); // で|すっ, bad lexeme in kuromoji-ipadic
                // ～くんで (kuromoji interpreting で one way)
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("名詞")
                       && t.getPartOfSpeechLevel2().equals("接尾")
                       && n.getPartOfSpeechLevel1().equals("助詞")
                       && n.getPartOfSpeechLevel2().equals("格助詞"));
                // ～くんで (kuromoji interpreting で another)
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("名詞")
                       && t.getPartOfSpeechLevel2().equals("接尾")
                       && n.getPartOfSpeechLevel1().equals("助動詞"));
                
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
                
                // Force non-split on one-character-surface independent verbs followed by auxiliaries
                // TODO: Figure out what this was needed for and add a test for it.
                if(false)//t.getSurface().length() == 1 && t.getPartOfSpeechLevel2().equals("自立") && n.getPartOfSpeechLevel1().contains("助動")
                //    && t.isKnown() && n.isKnown())
                {
                    r.add(new Piece(t.getSurface()+n.getSurface(), false));
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
        Main.main(args);
    }
}