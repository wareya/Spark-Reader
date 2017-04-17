package language.segmenter;

import java.util.List;
import java.util.ArrayList;

abstract public class Segmenter
{
    static public Segmenter instance;
    static public boolean extended = false;
    public class Piece
    {
        public String txt;
        public boolean strong; // means that this segment cannot be combined alone with the next segment, used mainly for particles followed by non-particles
        public Piece(String txt, boolean strong)
        {
            this.txt = txt;
            this.strong = strong;
        }
    }
    static public String Unsegment(List<Piece> array, Integer start, Integer end)
    {
        String r = "";
        for(Integer i = start; i < end; i++)
        {
            r += array.get(i).txt;
        }
        return r;
    }
    static public List<Piece> subList(List<Piece> array, Integer start, Integer end)
    {
        List<Piece> r = new ArrayList<>();
        for(; start < end; start++)
        {
            r.add(array.get(start));
        }
        return r;
    }

    abstract public List<Piece> Segment(String input);
}
