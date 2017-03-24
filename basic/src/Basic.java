
import main.Main;
import language.Segmenter;

import java.util.ArrayList;

class BasicSegmenter extends Segmenter
{
    public ArrayList<Piece> Segment(String text)
    {
        ArrayList<Piece> r = new ArrayList<>();
        for(char c : text.toCharArray())
        {
            r.add(new Piece(c+"", false));
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
