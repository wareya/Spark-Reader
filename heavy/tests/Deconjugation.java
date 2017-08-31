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

/**
 * Test for the heuristics used to prevent the word splitter from inventing words
 */
public class Deconjugation
{
    @Test
    public void testDeconjugation() throws IOException
    {
        Segmenter.extended = true;
        Segmenter.instance = new HeavySegmenter();
        Segmenter.basicInstance = new BasicSegmenter();
        Main.options = new Options();
        
        Underlay.load(Main.options.getFile("underlayPath"));

        Main.known = new Known(Main.options.getFile("knownWordsPath"));
        Main.prefDef = new PrefDef(Main.options.getFile("preferredDefsPath"));
        Main.blacklistDef = new BlacklistDef();
        
        Main.options.setOption("splitterMode", "full");
        Main.options.setOption("deconMode", "recursive");
        Main.options.setOption("kuromojiSupportLevel", "heuristics");

        Dictionary dict = new Dictionary(Main.options.getFile("dictionaryPath"));
        WordSplitter splitter = new WordSplitter(dict);

        List<FoundWord> words;

        words = splitter.split("「これ……いいの？」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("いいの"), false); // incorrectly deconjugated ii as a noun
        }

        words = splitter.split("「これ……いいの？」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("いいの"), false); // incorrectly deconjugated ii as an adjective
        }

        words = splitter.split("それじゃ最近してたことって何？",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("た"), false); // failed to deconjugate to た
        }
        
        words = splitter.split("「ありがとうございましたっ！」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("ました"), false); // failed to handle gozaimasu
        }
        
        words = splitter.split("「さっさと終わらせよ……」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("終"), false); // failed to do the よ imperative
        }
        
    }
}
