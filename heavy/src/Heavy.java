
import main.Main;
import language.Segmenter;

import java.util.ArrayList;
import java.util.List;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import static main.Main.options;

class HeavySegmenter extends Segmenter
{
    Tokenizer kuro;
    public ArrayList<Piece> Segment(String text)
    {
        List<Token> tokens = kuro.tokenize(text);
        ArrayList<Piece> r = new ArrayList<>();

        for(int i = 0; i < tokens.size(); i++)
        {
            Token t = tokens.get(i);
            if(i+1 == tokens.size() || !options.getOptionBool("kuromojiExtendedUse"))
            {
                r.add(new Piece(t.getSurface(), false));
            }
            // Reduce segmentation errors caused by the word splitter binding the segment list back together into words
            // Method 1: directly combining single-character verb stems with their auxiliaries, so they don't get bound to an earlier word (しかい, ことし)
            // Method 2: make strong segmentations between token sequences that often cause problems (particles followed by non-particles) (はそう, がいい)
            // Wouldn't be necessary if the segmenter exposed more information to the word splitter, but that's just the way that it goes.
            else
            {
                Token n = tokens.get(i+1);

                // Force split
                boolean strong = (t.getPartOfSpeechLevel1().equals("助詞")
                                && (t.getPartOfSpeechLevel2().equals("格助詞") || n.getPartOfSpeechLevel1().equals("副詞")) // most unsegmentation errors
                                && !n.getPartOfSpeechLevel1().contains("助詞"));
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("終助詞")
                       && n.getPartOfSpeechLevel1().contains("助詞"));
                strong = strong
                      || (t.getPartOfSpeechLevel2().contains("接尾")
                       && n.getPartOfSpeechLevel2().contains("係助詞"));

                // Force non-split on one-character-surface independent verbs followed by auxiliaries
                if(t.getSurface().length() == 1 && t.getPartOfSpeechLevel2().equals("自立") && n.getPartOfSpeechLevel1().contains("助動"))
                {
                    r.add(new Piece(t.getSurface()+n.getSurface(), false));
                    i++;
                }
                else
                {
                    r.add(new Piece(t.getSurface(), strong));
                }
            }
        }
        System.out.println("Segmenter output: " + Unsegment(r,0,r.size()));
        return r;
    }
    HeavySegmenter()
    {
        kuro = new Tokenizer();
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