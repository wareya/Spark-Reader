import com.atilika.kuromoji.unidic.Token;
import language.segmenter.BasicSegmenter;
import language.segmenter.Segmenter;
import language.splitter.FoundWord;
import main.Main;
import options.Options;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by wareya on 2017/03/25.
 */
public class LongTokens
{
    @Test
    public void testLongTokens() throws IOException
    {
        Segmenter.extended = true;
        Segmenter.instance = new HeavySegmenter();
        Segmenter.basicInstance = new BasicSegmenter();
        Main.options = new Options();

        List<Token> tokens;
        
        boolean foundRightToken = false; 
        
        //tokens = ((HeavySegmenter)(Segmenter.instance)).DebugSegment("それで、魔法について教わった？");
        //System.out.println(tokens);
        //foundRightToken = false;
        //for(Token token : tokens)
        //    if(token.getSurface().equals("について")) foundRightToken = true;
        //assertEquals(foundRightToken, true);
        
        //tokens = ((HeavySegmenter)(Segmenter.instance)).DebugSegment("というわけで");
        //System.out.println(tokens);
        //foundRightToken = false;
        //for(Token token : tokens)
        //    if(token.getSurface().equals("という")) foundRightToken = true;
        //assertEquals(foundRightToken, true);
        
        tokens = ((HeavySegmenter)(Segmenter.instance)).DebugSegment("謙遜なんかしないでください。");
        System.out.println(tokens);
        foundRightToken = false;
        for(Token token : tokens)
            if(token.getSurface().equals("なんか")) foundRightToken = true;
        assertEquals(foundRightToken, true);

        //words = splitter.split("それで、魔法について教わった？",  new HashSet<>());
        //for(FoundWord word : words)
        //{
        //    assertEquals(word.getText().equals("について"), true); // failed to split segment that wasn't in a dictionary
        //}
    }
}
