
import language.dictionary.DefSource;
import language.dictionary.Definition;
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
                //けいどろ,5145,5145,8735,名詞,普通名詞,一般,*,*,*,ケイドロ,警泥,けいどろ,ケードロ,けいどろ,ケードロ,混,*,*,*,*
                //A,5145,5145,C,名詞,普通名詞,一般,*,*,*,B,A,A,B,A,B,混,*,*,*,*
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
                kuromojiUserdict += String.format("%s,5145,5145,%d,名詞,普通名詞,一般,*,*,*,%s,%s,%s,%s,%s,%s,混,*,*,*,*\n",
                    word, options.getOptionInt("kuromojiUserdictWeight"), reading, word, word, reading, word, reading);
                
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
    
    private void addAsUnigram(ArrayList<Piece> r, String s)
    {
        for(char c : s.toCharArray())
            r.add(new Piece(c+"", false));
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
            r.add(new Piece("っ", false));
        }
        else
            r.add(p);
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
                // にいない
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("格助詞")
                       && n.getPartOfSpeechLevel1().equals("動詞")
                       && n.getPartOfSpeechLevel2().equals("自立")
                       && !n.getSurface().equals("すっ"));
                // ～くんで
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("接尾辞")
                       && t.getPartOfSpeechLevel2().equals("名詞的")
                       && n.getPartOfSpeechLevel1().equals("助詞")
                       && n.getPartOfSpeechLevel2().equals("格助詞"));
                // ここでしたい
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("助詞")
                       && t.getPartOfSpeechLevel2().equals("格助詞")
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
                
                if(weak)
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