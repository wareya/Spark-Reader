import language.dictionary.Dictionary;
import language.segmenter.BasicSegmenter;
import language.segmenter.Segmenter;
import language.splitter.FoundWord;
import language.splitter.WordSplitter;
import main.Main;
import options.BlacklistDef;
import options.Known;
import options.Options;
import options.PrefDef;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test to make sure user dictionary is understood by kuromoji
 */
public class UserDictionary
{
    @Test
    public void testUserDictionary() throws IOException
    {
        Segmenter.extended = false;
        Segmenter.instance = new HeavySegmenter();
        Segmenter.basicInstance = new BasicSegmenter();
        Main.options = new Options();

        Main.known = new Known(Main.options.getFile("knownWordsPath"));
        Main.prefDef = new PrefDef(Main.options.getFile("preferredDefsPath"));
        Main.blacklistDef = new BlacklistDef();

        Main.options.setOption("splitterMode", "full");
        Main.options.setOption("deconMode", "recursive");
        Main.options.setOption("kuromojiSupportLevel", "heuristics");
        Main.options.setOption("kuromojiUserdictWeight", "20000");

        Dictionary dict = new Dictionary(new File("../dictionaries"));
        WordSplitter splitter = new WordSplitter(dict);

        List<FoundWord> words;
        
        // make sure "initial segment" optimization works right
        words = splitter.split("沙夜の目",  new HashSet<>());
        System.out.println("Words:");
        System.out.println(words);
        boolean foundRightWord = false;
        for(FoundWord word : words)
            if(word.getText().equals("沙夜")) foundRightWord = true;
        assertEquals(foundRightWord, true);
        
        // まだ疑問が残っている倉科さんの手を取ると
        
        // test かえって得体の知れないものを想像させる
    }
}
