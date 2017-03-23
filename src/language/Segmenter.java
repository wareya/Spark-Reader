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
    static public ArrayList<String> subList(ArrayList<String> array, Integer start, Integer end)
    {
        ArrayList<String> r = new ArrayList<>();
        for(; start < end; start++)
        {
            r.add(array.get(start));
        }
        return r;
    }

    abstract public ArrayList<String> Segment(String input);
}
