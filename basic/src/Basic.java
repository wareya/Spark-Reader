
import main.Main;
import language.Segmenter;

import java.util.ArrayList;

class BasicSegmenter extends Segmenter
{
    public ArrayList<String> Segment(String text)
    {
        ArrayList<String> r = new ArrayList<>();
        for(char c : text.toCharArray())
        {
            r.add(c+"");
        }
        return r;
    }
}

public class Basic
{
    public static void main(String[] args) throws Exception
    {
        Segmenter.instance = new BasicSegmenter();
        Main.main(args);
    }
}
