import language.segmenter.Segmenter;
import language.dictionary.Dictionary;
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
public class Heuristics
{
    @Test
    public void testHeuristics() throws IOException
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

        words = splitter.split("俺はそう、一言だけつぶやいた。",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("はそう"), false); // false split
        }

        words = splitter.split("男を入れて三人しかいなかった",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("しかい"), false); // false split
        }
        
        words = splitter.split("別段なにがいいってわけでもない",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("ない"), false); // false forced split
        }

        words = splitter.split("そんな難しく考えないほうがいいよな",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("ほうが"), false); // false split
        }

        words = splitter.split("原因なのかも知れない",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("かも"), false); // false forced split
        }

        words = splitter.split("気温は亜熱帯かと思うほど高いのに",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("かと"), false); // false split
        }

        words = splitter.split("その不自然さは、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("さは"), false); // false split
        }

        words = splitter.split("武装してはいるが不安は消えない、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("はいる"), false); // false split
        }

        words = splitter.split("あれが大人になると、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("なると"), false); // false split
        }

        words = splitter.split("俺がしっかりしていれば、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("し"), false);
        }

        words = splitter.split("連れて帰ろう、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("て"), false);
        }

        words = splitter.split("やっぱり覚えてやがったな",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("や"), false);
        }

        words = splitter.split("謙遜なんかしないでください。",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("し"), false);
        }

        words = splitter.split("お役に立てることなら手伝おうかな、と思って",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("か"), false);
        }

        words = splitter.split("そういうのはナシだって",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("はナシ"), false);
        }

        words = splitter.split("そんなことしたら落ちっ",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("ことし"), false);
        }

        words = splitter.split("見納めにするかのように、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("か"), false);
        }

        words = splitter.split("「どうしました？」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("どうし"), false);
        }

        words = splitter.split("この学校にいないから",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("にい"), false);
        }

        words = splitter.split("「君が噂の転校生くんで間違いないようだね」",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("くんで"), false);
        }
        
        
        
        
        words = splitter.split("とても個人的なことで申し訳ないのですが",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("のです"), false); // misleading split
        }
        words = splitter.split("いや、なんでもない。",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("なん"), false); // misleading split
        }
    }
}
