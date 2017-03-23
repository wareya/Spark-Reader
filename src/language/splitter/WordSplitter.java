/* 
 * Copyright (C) 2017 Laurens Weyn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package language.splitter;

import language.Segmenter;
import language.deconjugator.ValidWord;
import language.deconjugator.WordScanner;
import language.dictionary.*;
import language.dictionary.Dictionary;
import java.util.*;

import static main.Main.options;

/**
 * Takes in text and splits it into individual words
 * @author Laurens Weyn
 */
public class WordSplitter
{
    private final Dictionary dict;

    public WordSplitter(Dictionary dict)
    {
        this.dict = dict;
    }
    public static void recalcPositions(List<FoundWord> words)
    {
        int x = 0;
        for(FoundWord word:words)
        {
            word.setStartX(x);
            x = word.endX();
        }
    }
    private List<FoundWord> splitSection(String text, boolean firstSection)
    {
        ArrayList<String> segments = Segmenter.instance.Segment(text);
        ArrayList<FoundWord> words = new ArrayList<>();
        int start = 0;
        //until we've covered all words
        while(start < segments.size())
        {
            // select the initial "overly long and certainly bogus" segment for deconjugation

            int pos = segments.size();

            if(!options.getOption("automaticallyParse").equals("none")) // (unless parsing is disabled)
            {
                // look for the longest segment covered as-is in the dictionary
                while(pos > start)
                {
                    String string_at = Segmenter.Unsegment(segments, start, pos);
                    if(dict.find(string_at) == null && !dict.hasEpwingDef(string_at))
                    {
                        // not in dictionary, see if adding possible deconjugation match endings to it gives us a dictionary entry (fixes 振り返ります etc)
                        //string_at = string_at.substring(0, string_at.length()-1);
                        boolean good_match = false;
                        for(String ending:WordScanner.possibleEndings())
                        {
                            String attempt = string_at+ending;
                            if(dict.find(attempt) != null || dict.hasEpwingDef(attempt))
                                good_match = true;
                        }
                        if(!good_match)
                        {
                            pos--;
                            continue; // don't fall through to "break;"
                        }
                    }
                    break;
                }
                // extend it until it's about to pick up characters that aren't acceptable in conjugations
                while(pos < segments.size())
                {
                    String nextSegment = segments.get(pos);
                    boolean good_segment = true;
                    for(char c : nextSegment.toCharArray())
                    {
                        if(!WordScanner.isAcceptableCharacter(c))
                        {
                            good_segment = false;
                            break;
                        }
                    }
                    if(good_segment)
                        pos++;
                    else
                        break;
                }
            }
            FoundWord matchedWord = null;
            //until we've tried all lengths and failed
            while(pos > start)
            {
                WordScanner word = new WordScanner(Segmenter.Unsegment(segments, start, pos));//deconjugate
                matchedWord = new FoundWord(word.getWord());//prototype definition
                attachDefinitions(matchedWord, word);//add cached definitions
                //override: match more words than usual
                if(matchedWord.getDefinitionCount() == 0 && firstSection)
                {
                    //if found in an EPWING dictionary
                    if(dict.hasEpwingDef(word.getWord()))
                    {
                        start = pos;//start next definition from here
                        break;//stop searching and add this word
                    }
                }

                if(matchedWord.getDefinitionCount() == 0 && options.getOption("automaticallyParse").equals("full")) // (only if full parsing is enabled)
                {
                    matchedWord = null;
                    pos--;//try shorter word
                }
                else//found a word
                {
                    start = pos;//start next definition from here
                    break;//stop searching and add this word
                }
            }
            if(matchedWord == null)//if we failed
            {
                words.add(new FoundWord(segments.get(start) + ""));//add the segment as an 'unknown word'
                start++;
            }
            else words.add(matchedWord);

            firstSection = false;
        }
        return words;
    }

    private void attachDefinitions(FoundWord word, WordScanner conjugations)
    {
        for(ValidWord match:conjugations.getMatches())//for each possible conjugation...
        {
            List<Definition> defs = dict.find(match.getWord());
            if(defs != null)for(Definition def:defs)//for each possible definition...
            {
                //check if it meets the tag requirements of this conjugation
                if(match.defMatches(def))
                {
                    word.addDefinition(new FoundDef(match, def));//add the definition and how we got the form for it
                }
            }
        }
        word.sortDefs();//sort these new definitions
    }


    public List<FoundWord> split(String text, Set<Integer> breaks)
    {
        ArrayList<FoundWord> words = new ArrayList<>();
        if(text.equals("")) return words;
        int pos = 0;
        int start = 0;
        breaks.add(0);
        
        // todo: make segmenting on writing system changes optional? (when normal segmentation disabled only; dropdown menu?)
        // fixme: not segmenting non-japanese text into single characters makes the renderer's assumtions on segment width break, horribly.
        boolean was_japanese;
        boolean is_japanese = Japanese.isJapaneseWriting(text.charAt(0));
        while(pos < text.length())
        {
            was_japanese = is_japanese;
            is_japanese = Japanese.isJapaneseWriting(text.charAt(pos));
            if(breaks.contains(pos) || is_japanese != was_japanese)
            {
                // cause was_japanese to be equal to is_japanese on the next iteration
                if(breaks.contains(pos) && pos+1 < text.length())
                    is_japanese = Japanese.isJapaneseWriting(text.charAt(pos+1));
                
                String section = text.substring(start, pos);
                words.addAll(splitSection(section, breaks.contains(start)));
                start = pos;
            }
            pos++;
        }
        if(pos > start && pos <= text.length())
        {
            String section = text.substring(start, pos);
            words.addAll(splitSection(section, breaks.contains(start)));
        }
        recalcPositions(words);
        breaks.remove(0);
        return words;
    }

    public static void main(String[] args)
    {
        new WordSplitter(null).split("こんにちは、世界！", new HashSet<>());
    }
}
