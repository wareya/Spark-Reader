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

    private class splitWordResult
    {
        protected FoundWord term;
        protected List<Piece> segments;
        splitWordResult(FoundWord term, List<Piece> segments)
        {
            this.term = term;
            this.segments = segments;
        }
    }

    private boolean mightBeDeconjugatable(String text)
    {
        boolean goodMatch = false;

        for(String ending:WordScanner.possibleEndings())
        {
            String attempt = text+ending;
            if(dict.find(attempt) != null || dict.hasEpwingDef(attempt))
                goodMatch = true;
        }
        return goodMatch;
    }

    private boolean mightBeConjugation(String text)
    {
        boolean goodMatch = true;
        for(char c : text.toCharArray())
        {
            if(!WordScanner.isAcceptableCharacter(c))
            {
                goodMatch = false;
                break;
            }
        }
        return goodMatch;
    }

    private splitWordResult splitWord(List<Piece> segments)
    {
        // select the initial "overly long and certainly bogus" segment list to test for validity
        int length = segments.size();

        // fixme: reintroduce "strong piece" logic

        // reduce length of plausible segment list unless parsing is completely disabled
        if(!options.getOption("splitterMode").equals("none"))
        {
            // look for the longest segment covered as-is in the dictionary
            while(length > 0)
            {
                String textHere = Segmenter.Unsegment(segments, 0, length);
                if(dict.find(textHere) != null || dict.hasEpwingDef(textHere) || mightBeDeconjugatable(textHere))
                    break;
                length--;
            }
            // no good, try splitting the first segment
            if(length == 0)
            {
                List<Piece> workingList;

                String workingText = segments.get(0).txt;
                int position = workingText.length();
                while(position > 1)
                {
                    String textHere = workingText.substring(0, position);
                    if(dict.find(textHere) != null || dict.hasEpwingDef(textHere) || mightBeDeconjugatable(textHere))
                        break;
                    position--;
                }

                workingList = new ArrayList<>();
                workingList.add(instance.new Piece(workingText.substring(0, position), false));
                workingList.add(instance.new Piece(workingText.substring(position, workingText.length()), false));
                for(int i = 1; i < segments.size(); i++)
                    workingList.add(segments.get(i));

                segments = workingList;
                length = 1;
            }

            // extend it to include any contiguous segments that might be conjugation
            while(length < segments.size())
            {
                String nextSegment = segments.get(length).txt;
                if(mightBeConjugation(nextSegment))
                    length++;
                else
                    break;
            }
        }

        //until we've tried all lengths and failed
        while(length > 0)
        {
            String str = Segmenter.Unsegment(segments, 0, length);

            WordScanner word = new WordScanner(str);//deconjugate
            FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
            attachDefinitions(matchedWord, word);//add cached definitions

            // Return current string if it's a good word or if we do not have full parsing enabled
            if(matchedWord.getDefinitionCount() > 0 || dict.hasEpwingDef(word.getWord()) || !options.getOption("splitterMode").equals("full"))
                return new splitWordResult(matchedWord, segments.subList(length, segments.size()));
            else
                length--;//try shorter word
        }
        // fall through = no valid word here, return first segment alone
        return new splitWordResult(new FoundWord(segments.get(0).txt + ""), segments.subList(1, segments.size()));
    }

    private List<FoundWord> splitSection(String text, boolean firstSection)
    {
        List<Piece> segments = instance.Segment(text);
        List<FoundWord> words = new ArrayList<>();

        //until we've covered all words
        while(segments.size() > 0)
        {
            splitWordResult result = splitWord(segments);
            words.add(result.term);
            segments = result.segments;
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
