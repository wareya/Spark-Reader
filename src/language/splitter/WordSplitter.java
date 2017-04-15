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
import language.Segmenter.*;
import language.deconjugator.ValidWord;
import language.deconjugator.WordScanner;
import language.dictionary.Definition;
import language.dictionary.Dictionary;
import language.dictionary.Japanese;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static language.Segmenter.instance;
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
        ArrayList<Piece> segments = instance.Segment(text);
        ArrayList<FoundWord> words = new ArrayList<>();
        int start = 0;
        //until we've covered all words
        while(start < segments.size())
        {
            // select the initial "overly long and certainly bogus" segment for deconjugation
            int pos = segments.size();

            // don't segment after strong segments
            if(options.getOption("splitterMode").equals("full"))
            {
                for(int i = segments.size(); i > start; i--)
                {
                    if(segments.get(i-1).strong && i-1 == start)
                    {
                        pos = i;
                    }
                }
            }
            int pos_max = pos;

            System.out.println("before segment optimization: start: " + start + " pos: " + pos);

            if(!options.getOption("splitterMode").equals("none")) // (unless parsing is disabled)
            {
                // look for the longest segment covered as-is in the dictionary
                while(pos > start)
                {
                    String textHere = Segmenter.Unsegment(segments, start, pos);
                    if(dict.find(textHere) == null && !dict.hasEpwingDef(textHere))
                    {
                        // not in dictionary, see if adding possible deconjugation match endings to it gives us a dictionary entry (fixes 振り返ります etc)
                        boolean goodMatch = false;

                        for(String ending:WordScanner.possibleEndings())
                        {
                            String attempt = textHere+ending;
                            if(dict.find(attempt) != null || dict.hasEpwingDef(attempt))
                                goodMatch = true;
                        }
                        if(!goodMatch)
                        {
                            pos--;
                            continue; // don't fall through to "break;"
                        }
                    }
                    break;
                }
                if(pos == start) pos = start+1;
                // extend it until it's about to pick up characters that aren't acceptable in conjugations
                while(pos < pos_max)
                {
                    String nextSegment = segments.get(pos).txt;
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

            System.out.println("after segment optimization: start: " + start + " pos: " + pos);

            FoundWord matchedWord = null;
            //until we've tried all lengths and failed
            while(pos > start)
            {
                String str = Segmenter.Unsegment(segments, start, pos);
                System.out.println("start: " + start + " pos: " + pos + " str: " + str);
                WordScanner word = new WordScanner(str);//deconjugate
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

                if(matchedWord.getDefinitionCount() == 0 && options.getOption("splitterMode").equals("full")) // (only if full parsing is enabled)
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
                words.add(new FoundWord(segments.get(start).txt + ""));//add the segment as an 'unknown word'
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
        // fixme: not segmenting non-japanese text into single characters makes the renderer's assumptions on segment width break, horribly.
        boolean wasJapanese;
        boolean isJapanese = Japanese.isJapaneseWriting(text.charAt(0));
        while(pos < text.length())
        {
            wasJapanese = isJapanese;
            isJapanese = Japanese.isJapaneseWriting(text.charAt(pos));
            if(breaks.contains(pos) || isJapanese != wasJapanese)
            {
                // cause wasJapanese to be equal to isJapanese on the next iteration
                if(breaks.contains(pos) && pos+1 < text.length())
                    isJapanese = Japanese.isJapaneseWriting(text.charAt(pos+1));

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
