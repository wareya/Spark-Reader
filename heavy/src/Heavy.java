
import main.Main;
import language.Segmenter;

import java.util.ArrayList;
import java.util.List;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

class HeavySegmenter extends Segmenter
{
    Tokenizer kuro;
    public ArrayList<String> Segment(String text)
    {
        List<Token> tokens = kuro.tokenize(text);
        ArrayList<String> r = new ArrayList<>();

        for(int i = 0; i < tokens.size(); i++)
        {
            Token t = tokens.get(i);
            if(i+1 == tokens.size() || t.getSurface().length() > 1)
                r.add(t.getSurface());
            // Reduce segmentation errors caused by single-character verb stems (e.g. しかい|なかった vs しか|いなかった)
            // Wouldn't be necessary if the segmenter exposed more information to the word splitter, but that's just the way that it goes.
            else
            {
                Token n = tokens.get(i+1);
                if(t.getPartOfSpeechLevel2().contains("自立") && n.getPartOfSpeechLevel1().contains("助"))
                {
                    r.add(t.getSurface()+n.getSurface());
                    i++;
                }
                else
                    r.add(t.getSurface());
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
        Segmenter.instance = new HeavySegmenter();
        Main.main(args);
    }
}