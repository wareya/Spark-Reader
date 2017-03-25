import com.atilika.kuromoji.ipadic.Token;
import language.Segmenter;
import language.dictionary.Dictionary;
import language.splitter.FoundWord;
import language.splitter.WordSplitter;
import main.Main;
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
 * Created by wareya on 2017/03/25.
 */
public class Penalties
{
    @Test
    public void testPenalties() throws IOException
    {
        Segmenter.extended = true;
        Segmenter.instance = new HeavySegmenter();
        Main.options = new Options();

        List<Token> tokens;

        tokens = ((HeavySegmenter)(Segmenter.instance)).DebugSegment("何も悪いことしてなきゃいい人なんだな");
        System.out.println(tokens);
        boolean foundRightToken = false;
        for(Token token : tokens)
            if(token.getSurface().equals("こと")) foundRightToken = true;
        assertEquals(foundRightToken, true);
    }
}
