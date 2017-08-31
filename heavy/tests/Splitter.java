import language.dictionary.Dictionary;
import language.segmenter.BasicSegmenter;
import language.segmenter.Segmenter;
import language.splitter.FoundWord;
import language.splitter.WordSplitter;
import main.Main;
import options.*;
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
        Segmenter.basicInstance = new BasicSegmenter();
        Main.options = new Options();
        
        Underlay.load(Main.options.getFile("underlayPath"));

        Main.known = new Known(Main.options.getFile("knownWordsPath"));
        Main.prefDef = new PrefDef(Main.options.getFile("preferredDefsPath"));
        Main.blacklistDef = new BlacklistDef();

        Main.options.setOption("splitterMode", "full");
        Main.options.setOption("deconMode", "recursive");
        Main.options.setOption("kuromojiSupportLevel", "disabled");

        Dictionary dict = new Dictionary(Main.options.getFile("dictionaryPath"));
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
        
        Main.options.setOption("kuromojiSupportLevel", "basic");
        
        words = splitter.split("彼女とどのように知りあったのかも覚えていない。",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("知"), false);
        }
        
        words = splitter.split("「なんで腹黒いんだよっ、ちゃっかりさんって言ってよっ」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("言"), false);
        }
        
        words = splitter.split("「浄水器の押し売りとか新聞の勧誘とかと間違われてるのかな？」。",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("間"), false);
        }
        
        // まだ疑問が残っている倉科さんの手を取ると
        
        // test かえって得体の知れないものを想像させる
    }
}
