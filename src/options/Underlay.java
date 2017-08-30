package options;

import language.FixupOCR;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by wareya on 2017/08/29.
 * 
 * Arbitrary logical "facts" that get used by all sorts of random language processing stuff across Spark Reader.
 * So that people can change them if necessary without recompiling Spark Reader completely.
 * For example, conjugation rules, confusable characters for OCR fixup, etc.
 */
public class Underlay
{
    public static ArrayList<String> fixupRelationsList = new ArrayList<>();
    
    public static void load(File file) throws IOException
    {
        if(!file.exists())
        {
            System.out.println("WARN: no underlay file");
            return;
        }
        
        FileInputStream is = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);
        String mode = "";
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            if(line.equals("")) continue;
            if(line.equals("fix up ocr:"))
                mode = line;
            else
            {
                if(mode.equals("fix up ocr:"))
                {
                    fixupRelationsList.add(line);
                }
            }
        }
        br.close();
        
        FixupOCR.fixupInitRelations();
    }
}
