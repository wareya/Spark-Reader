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
public class Hacks
{
    @Test
    public void testHacks() throws IOException
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

        //ArrayList<Piece> pieces = Segmenter.instance.Segment();
        List<FoundWord> words;

        words = splitter.split("「絡んだっつうか…」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("だっ"), false);
        }

        words = splitter.split("さっき言ったろ、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("たろ"), false);
        }

        words = splitter.split("飛び掛ってきても、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("飛び掛っ"), false);
        }
        
        words = splitter.split("ベッドに寝っ転がると、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("寝っ転がる"), false);
        }
        
        words = splitter.split("戦ってようと",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("てよ"), false);
        }
        
        words = splitter.split("「な、なんでしょうかっ？」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("かっ"), false);
        }
        
        words = splitter.split("これですっ！",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("すっ"), false);
            assertEquals(word.getText().equals("す"), false);
        }
        
        words = splitter.split("鍵がかかってんのか",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("鍵"), false);
        }
        
        words = splitter.split("えへへ答えは千波でしたーって、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("はいらない"), false);
        }
        
        boolean foundWantedWord = false;
        words = splitter.split("働いている鈴木を見物しに",  new HashSet<>());
        for(FoundWord word : words)
        {
            if(word.getText().equals("木") ||word.getText().equals("鈴木")) foundWantedWord = true;
        }
        assertEquals(foundWantedWord, true);
        
        foundWantedWord = false;
        words = splitter.split("すりーぱーほーるど決まったぁーーッ！",  new HashSet<>());
        for(FoundWord word : words)
        {
            if(word.getText().equals("決まった")) foundWantedWord = true;
        }
        assertEquals(foundWantedWord, true);
        
        Main.options.setOption("kuromojiSupportLevel", "basic");
        words = splitter.split("「紫央ちゃんは常人離れした力『気』を使うことができるの」",  new HashSet<>());
        for(FoundWord word : words)
        {
            if(word.getText().equals("常人")) foundWantedWord = true;
        }
        assertEquals(foundWantedWord, true);
        
        
        // hybrid test: both default penalties and deconjugation
        words = splitter.split("噛みしめるようにして返事をした",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("噛"), false); // failed to deconjugate word that gets split from its kana segmentally in kuromoji mode with penalties
            assertEquals(word.getText().equals("みし"), false); // failed to rebuild segment list correctly
        }
    }
}
