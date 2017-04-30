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
public class Hacks
{
    @Test
    public void testHacks() throws IOException
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

        //ArrayList<Piece> pieces = Segmenter.instance.Segment();
        List<FoundWord> words;

        words = splitter.split("「絡んだっつうか…」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("だっ"), false); // failed to split segment that should be forced to unigramize
        }

        words = splitter.split("さっき言ったろ、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("たろ"), false); // failed to split segment that should be forced to unigramize
        }

        words = splitter.split("飛び掛ってきても、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("飛び掛っ"), false); // failed to split segment that wasn't in a dictionary
        }
        
        words = splitter.split("ベッドに寝っ転がると、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("寝っ転がる"), false); // failed to split segment that wasn't in a dictionary
        }
        
        words = splitter.split("戦ってようと",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("てよ"), false); // failed to split segment that wasn't in a dictionary
        }
    }
}
