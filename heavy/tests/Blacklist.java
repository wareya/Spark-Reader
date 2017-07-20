import language.dictionary.Dictionary;
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
 * Test for the heuristics used to prevent the word splitter from inventing words
 */
public class Blacklist
{
    @Test
    public void testBlacklist() throws IOException
    {
        Segmenter.extended = true;
        Segmenter.instance = new HeavySegmenter();
        Main.options = new Options();

        Main.known = new Known(Main.options.getFile("knownWordsPath"));
        Main.prefDef = new PrefDef(Main.options.getFile("preferredDefsPath"));
        Main.blacklistDef = new BlacklistDef();

        Main.options.setOption("splitterMode", "full");
        Main.options.setOption("deconMode", "recursive");
        Main.options.setOption("kuromojiSupportLevel", "heuristics");

        Dictionary dict = new Dictionary(new File("../dictionaries"));
        WordSplitter splitter = new WordSplitter(dict);

        List<FoundWord> words;
        
        Main.blacklistDef.debugForceBlacklist((long)1203350, "がいい");
        Main.blacklistDef.debugForceBlacklist((long)1757620, "がいい");
        Main.blacklistDef.debugForceBlacklist((long)1868030, "がいい");

        words = splitter.split("次からはちゃんと姫様と話すがいいです",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("がいい"), false); // false split
        }

        Main.blacklistDef.debugForceBlacklist((long)1367880	, "他人事");
        words = splitter.split("他人事だからって言ってくれるわ。",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("他人事"), false); // false split
        }
        
        // まだ疑問が残っている倉科さんの手を取ると
        
        // test かえって得体の知れないものを想像させる
    }
}
