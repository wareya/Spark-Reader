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

public class Splitter
{
    @Test
    public void testSplitter() throws IOException
    {
        Segmenter.extended = false;
        Segmenter.instance = new HeavySegmenter();
        Main.options = new Options();

        Main.known = new Known(Main.options.getFile("knownWordsPath"));
        Main.prefDef = new PrefDef(Main.options.getFile("preferredDefsPath"));
        Main.blacklistDef = new BlacklistDef();

        Main.options.setOption("splitterMode", "full");
        Main.options.setOption("deconMode", "recursive");
        Main.options.setOption("kuromojiSupportLevel", "disabled");

        Dictionary dict = new Dictionary(new File("../dictionaries"));
        WordSplitter splitter = new WordSplitter(dict);

        List<FoundWord> words;
        
        // make sure "initial segment" optimization works right
        words = splitter.split("沙夜",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("沙夜"), true);
        }
        words = splitter.split("そんな事言うお兄ちゃん、きらい！",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("お"), false);
        }
        
        // まだ疑問が残っている倉科さんの手を取ると
        
        // test かえって得体の知れないものを想像させる
    }
}
