package language;

import language.splitter.FoundWord;
import main.Main;
import options.Underlay;
import ui.Line;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static main.Main.splitter;

/**
 * Created by wareya on 2017/08/29.
 */
public class FixupOCR
{
    private static Map<Character, List<Character>> fixupRelations;
    private static void addRelations(String dump)
    {
        for(int i = 0; i < dump.length(); i++)
        {
            List<Character> tochars = new ArrayList<>();
            for(int j = 0; j < dump.length(); j++)
            {
                if(j == i) continue;
                tochars.add(dump.charAt(j));
            }
            fixupRelations.put(dump.charAt(i), tochars);
        }
    }
    public static void fixupInitRelations()
    {
        fixupRelations = new HashMap<>();
        for(String s : Underlay.fixupRelationsList)
            addRelations(s);
    } 
    
    public static void fixupOCR(Line line)
    {
        int currline = -1;
        
        for(int i = 0; i < Main.currPage.getLineCount(); i++)
        {
            if(line == Main.currPage.getLine(i))
            {
                currline = i;
                break;
            }
        }

        int length = 0;
        for(FoundWord word : line.getWords())
        {
            // pretend undefined segments are two words long so that we can just use a "length" check to see if test strings are better
            if(word.getDefinitionCount() == 0)
                length += 2;
            else
                length += 1;
        }
        
        System.out.println("length " + length);
        
        // FIXME: move the actual algorithm to src/language/ somewhere?
        
        String text = new String(line.toString());
        for(int i = 0; i < text.length(); i++)
        {
            String newstring = null;
            List<FoundWord> listB = null;
            Boolean shorten = false;
            if(fixupRelations.containsKey(text.charAt(i)))
            {
                char[] newtext = new String(text).toCharArray();
                List<Character> replacements = fixupRelations.get(text.charAt(i));
                
                for(Character rep : replacements)
                {
                    if(rep != ' ')
                    {
                        newtext[i] = rep;
                        newstring = new String(newtext);
                    }
                    else
                    {
                        newstring = text.substring(0, i);
                        if(i+1 < text.length())
                            newstring += text.substring(i+1, text.length());
                        shorten = true;
                    }
                    
                    listB = splitter.split(newstring, line.getMarkers());
                    
                    if(newstring != null && listB != null)
                    {
                        int newlength = 0;
                        for(FoundWord word : listB)
                        {
                            if(word.getDefinitionCount() == 0)
                                newlength += 2;
                            else
                                newlength += 1;
                        }
                        System.out.println("test string " + newstring);
                        System.out.println("test length " + newlength);
                        if(newlength < length)
                        {
                            length = newlength;
                            text = newstring;
                            if(shorten) i--;
                        }
                    }
                }
            }
        }
        
        String finaltext = "";
        
        for(int i = 0; i < currline && i < Main.currPage.getLineCount(); i++)
            finaltext += Main.currPage.getLine(i).toString() + "\n";
        
        finaltext += text + "\n";
        
        for(int i = currline+1; i < Main.currPage.getLineCount(); i++)
            finaltext += Main.currPage.getLine(i).toString() + (i+1 == Main.currPage.getLineCount()?"":"\n");
        
        Main.currPage.setText(finaltext);
    }
}
