package language;

import java.util.ArrayList;

abstract public class Segmenter
{
    static public Segmenter instance;
    static public String Unsegment(ArrayList<String> array, Integer start, Integer end)
    {
        String r = "";
        for(Integer i = start; i < end; i++)
        {
            r += array.get(i);
        }
        return r;
    }

    abstract public ArrayList<String> Segment(String input);
}
