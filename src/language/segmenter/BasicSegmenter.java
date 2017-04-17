package language.segmenter;

import main.Main;

import java.util.ArrayList;

public class BasicSegmenter extends Segmenter
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
