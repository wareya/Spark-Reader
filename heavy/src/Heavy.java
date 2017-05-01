
import com.atilika.kuromoji.TokenizerBase;
import jdk.nashorn.internal.runtime.options.Options;
import language.segmenter.BasicSegmenter;
import main.Main;
import language.segmenter.Segmenter;

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

    Segmenter basic = new BasicSegmenter();

    private void ensureInitialized()
    {
        if(kuro == null)
            kuro = new Tokenizer.Builder()
                .mode(TokenizerBase.Mode.SEARCH) // punish long terms
                .kanjiPenalty(options.getOptionInt("kuromojiKanjiPenaltyLength"), options.getOptionInt("kuromojiKanjiPenalty"))
                .otherPenalty(options.getOptionInt("kuromojiOtherPenaltyLength"), options.getOptionInt("kuromojiOtherPenalty"))
                .build();
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
        "てよ"
    ));
    private boolean shouldForceUnigram(String s)
    {
        return (badSegments.contains(s));
    }

    // unigram unknown tokens before adding them
    private void addWithUnigramCheck(ArrayList<Piece> r, Token t, boolean strong)
    {
        if(!t.isKnown() || shouldForceUnigram(t.getSurface()))
        {
            for(char c : t.getSurface().toCharArray())
                r.add(new Piece(c+"", false));
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

                // "Strong" means that a segment forces the word splitter to make a segment after it IF it is the first segment in the current attempt at finding a word.
                
                // ～しかいなかった and others
                boolean strong = (t.getPartOfSpeechLevel1().equals("助詞") 
                                && (t.getPartOfSpeechLevel2().equals("格助詞") || t.getPartOfSpeechLevel2().equals("係助詞") || n.getPartOfSpeechLevel1().equals("副詞"))
                                && !n.getPartOfSpeechLevel1().contains("助詞")) && !n.getPartOfSpeechLevel2().equals("接尾");
                // ～かと思う
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("終助詞")
                       && n.getPartOfSpeechLevel1().contains("助詞")
                       && !n.getPartOfSpeechLevel2().contains("終助詞"));
                // ～さは (is the blacklist better for this?)
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("接尾")
                       && n.getPartOfSpeechLevel2().contains("係助詞"));
                // になると
                strong = strong
                      || (t.getPartOfSpeechLevel1().equals("動詞")
                       && t.getConjugationForm().equals("基本形")
                       && n.getPartOfSpeechLevel1().equals("助詞"));
                
                if(i+2 < tokens.size())
                {
                    //のですが
                    Token m = tokens.get(i+2);
                    strong = strong
                        || ((t.getSurface().length() == 1 || m.getSurface().length() == 1) 
                         && m.getPartOfSpeechLevel2().equals("接続助詞")
                         && n.getPartOfSpeechLevel1().equals("助動詞"));
                }
                
                // これですっ！
                if(t.getSurface().length() == 1 && shouldForceUnigram(n.getSurface()))
                    strong = false;
                
                // Force non-split on one-character-surface independent verbs followed by auxiliaries
                // TODO: Figure out what this was needed for and add a test for it.
                if(false)//t.getSurface().length() == 1 && t.getPartOfSpeechLevel2().equals("自立") && n.getPartOfSpeechLevel1().contains("助動")
                //&& t.isKnown() && n.isKnown())
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