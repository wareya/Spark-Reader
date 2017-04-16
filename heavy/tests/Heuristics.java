import com.atilika.kuromoji.ipadic.Token;
import language.Segmenter;
import language.Segmenter.*;
import language.dictionary.Dictionary;
import language.splitter.FoundWord;
import language.splitter.WordSplitter;
import main.Main;
import options.Known;
import options.Options;
import options.PrefDef;
import org.junit.Test;
import sun.misc.JavaIOAccess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;
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

        Main.options.setOption("automaticallyParse", "full");
        Main.options.setOption("useOldParser", "false");
        Main.options.setOption("kuromojiExtendedUse", "true");

        Dictionary dict = new Dictionary(new File("../dictionaries"));
        WordSplitter splitter = new WordSplitter(dict);

        //ArrayList<Piece> pieces = Segmenter.instance.Segment();
        List<FoundWord> words;

        words = splitter.split("男を入れて三人しかいなかった",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("しかい"), false); // false split
        }

        words = splitter.split("それはそうね",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("はそう"), false); // false split
        }

        words = splitter.split("別段なにがいいってわけでもない",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("がいい"), false); // false split
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

        /*
        List<Token> tokenlist;

        tokenlist = ((HeavySegmenter)(Segmenter.instance)).DebugSegment("あれが大人になると");
        for(Token token : tokenlist)
            System.out.println(token.getConjugationForm());
        */
        words = splitter.split("あれが大人になると、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("なると"), false); // false split
        }

        words = splitter.split("俺がしっかりしていれば、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("し"), false); // false split
        }

        words = splitter.split("連れて帰ろう、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("て"), false); // false split
        }

        words = splitter.split("飛び掛ってきても、",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("飛び掛っ"), false); // false split
        }

        words = splitter.split("やっぱり覚えてやがったな",  new HashSet<>());
        for(FoundWord word : words)
        {
            assertEquals(word.getText().equals("や"), false); // false split
        }

        // TODO: Blacklist ものを somehow and test かえって得体の知れないものを想像させる
    }
}
