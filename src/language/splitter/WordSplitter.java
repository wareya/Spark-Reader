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

import language.segmenter.Segmenter;
import language.segmenter.Segmenter.*;
import language.deconjugator.ValidWord;
import language.deconjugator.WordScanner;
import language.dictionary.Definition;
import language.dictionary.Dictionary;
import language.dictionary.Japanese;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static language.segmenter.Segmenter.instance;
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

    // not in dictionary, see if adding possible deconjugation match endings to it gives us a dictionary entry (fixes 振り返ります etc)
    private boolean mightBeDeconjugatable(String text, boolean firstSection)
    {
        boolean goodMatch = false;
        for(String ending:WordScanner.possibleEndings())
        {
            String attempt = text + ending;
            //only check Edict here; Epwing does cannot handle conjugations
            if(dict.findText(attempt) != null)
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

    private List<FoundWord> splitSection(String text, boolean firstSection)
    {
        List<Piece> segments = instance.Segment(text);
        List<FoundWord> words = new ArrayList<>();
        
        System.out.println("Splitter output:");
        for(Piece p : segments)
            System.out.println(p.txt);

        //until we've covered all words
        int start = 0;
        while(segments.size()-start > 0)
        {
            // select the initial "overly long and certainly bogus" segment list to test for validity
            int end = segments.size();

            // if the first segment is strong, use it in isolation
            if(options.getOption("splitterMode").equals("full"))
            {
                if(segments.get(start).strong)
                    end = start+1;
            }
            int limit = end;

            // Parsing is faster if we start with a relatively short segment instead of the entire string.
            // Otherwise, deconjugation can be unnecessarily expensive, since it'll be passing around tons of memory unnecessarily
            // Luckily, even for words that can be deconjugated, we can make a pretty good guess at the longest string that could possibly be valid
            if(!options.getOption("splitterMode").equals("none"))
            {
                // We only need to start with a length as the longest dictionary term, because it can be extended to pick up conjugation characters
                // 100 is a good conservative "no way there's a dictionary term this long" limit, and prevents long text from taking several seconds to parse
                // todo: make this be an option?
                if(!Segmenter.extended)
                {
                    if(end-start > 100)
                        end = start+100;
                }
                else // If the segmenter is "smart" then each segment is larger than a single character and we should be more aggressive
                {
                    int scratch_length = 0;
                    for(Piece s : Segmenter.subList(segments, start, segments.size()))
                    {
                        scratch_length += s.txt.length();
                        if(scratch_length >= 100) break;
                    }
                    if(end-start > scratch_length)
                        end = start+scratch_length;
                }
                
                // look for the longest section covered as-is in the dictionary
                while(end > start)
                {
                    String textHere = Segmenter.Unsegment(segments, start, end);
                    // only check the epwing dictionary if this is the first segment in the section (for speed reasons)
                    if(dict.findText(textHere) != null || (firstSection && dict.hasEpwingDef(textHere)) || mightBeDeconjugatable(textHere, firstSection))
                        break;
                    end--;
                }
                // Fell through: no good, try splitting the first segment (uncommon, performance impact isn't big even though we rewrite the segment list)
                if(end <= start)
                {
                    // only bother for weak non-unigrams or long things
                    //if((segments.get(start).txt.length() > 1 && !segments.get(start).strong) || segments.get(start).txt.length() >= 3)
                    if(segments.get(start).txt.length() > 1)
                    {
                        String workingText = segments.get(start).txt;
                        int position = workingText.length();
                        while(position > 1)
                        {
                            String textHere = workingText.substring(0, position);
                            
                            // Annoying change: Using the dictionary isn't good enough and causes segmentation errors like 知りあっ|た -> 知|りあ|った. We have to deconjugate it.
                            WordScanner word = new WordScanner(textHere);//deconjugate
                            FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                            attachDefinitions(matchedWord, word);//add cached definitions
            
                            if(matchedWord.getDefinitionCount() > 0 || (firstSection && dict.hasEpwingDef(word.getWord())))
                                break;
                            position--;
                        }

                        List<Piece> workingList;
                        workingList = new ArrayList<>();
                        workingList.add(instance.new Piece(workingText.substring(0, position), false));
                        workingList.add(instance.new Piece(workingText.substring(position, workingText.length()), false));
                        for(int i = start+1; i < segments.size(); i++)
                            workingList.add(segments.get(i));

                        segments = workingList;
                        limit = segments.size();
                        start = 0;
                    }
                    end = start+1; // only pick up exactly one segment
                }

                // extend it to include any contiguous segments that might be conjugation text
                while(end < limit)
                {
                    String nextSegment = segments.get(end).txt;
                    if(mightBeConjugation(nextSegment))
                        end++;
                    else
                        break;
                }
            }
            
            int safe_end = end;

            // Now check if our string is a word, conjugated or not. If it isn't, shorten the string and try again until we only have one segment left.
            while(end > start)
            {
                String str = Segmenter.Unsegment(segments, start, end);

                WordScanner word = new WordScanner(str);//deconjugate
                FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                attachDefinitions(matchedWord, word);//add cached definitions

                System.out.println(matchedWord.getText());
                // Use current string if it's a good word or if we do not have full parsing enabled or if it's only one segment long
                if(matchedWord.getDefinitionCount() > 0 || (firstSection && dict.hasEpwingDef(word.getWord())) || !options.getOption("splitterMode").equals("full"))
                {
                    words.add(matchedWord);
                    start = end;
                    break;
                }
                // We failed to match a word and only have one segment left
                else if(end == start+1)
                {
                    // Hack: try it with substrings of the next segment (todo: make this encompass more failure cases without making it nasty)
                    if(end < segments.size() && segments.get(end).txt.length() > 1)
                    {
                        String segment1 = segments.get(start).txt;
                        String segment2 = segments.get(end).txt;
                        int n;
                        for (n = segment2.length(); n > 0; n--)
                        {
                            String tempString = segment1 + segment2.substring(0, n);
                            
                            System.out.println("wow");
                            System.out.println(tempString);
                            
                            WordScanner tempWord = new WordScanner(tempString);//deconjugate
                            FoundWord tempMatchedWord = new FoundWord(tempWord.getWord());//prototype definition
                            attachDefinitions(tempMatchedWord, word);//add cached definitions
                            if(tempMatchedWord.getDefinitionCount() > 0 || (firstSection && dict.hasEpwingDef(tempWord.getWord())) || !options.getOption("splitterMode").equals("full"))
                            {
                                words.add(tempMatchedWord);
                                
                                // rebuild segment list
                                List<Piece> workingList;
                                workingList = new ArrayList<>();
                                workingList.add(instance.new Piece(segment2.substring(n, segment2.length()), false));
                                for(int i = end+1; i < segments.size(); i++)
                                    workingList.add(segments.get(i));
                                segments = workingList;
                                start = 0;
                                
                                System.out.println("Rebuilt segment list.");
                                break;
                            }
                        }
                        if(n != 0)
                        {
                            break;
                        }
                        else
                        {
                            System.out.println("Boo");
                            words.add(matchedWord);
                            start = end;
                            break;
                        }
                    }
                    else
                    {
                        words.add(matchedWord);
                        start = end;
                        break;
                    }
                }
                else
                    end--;//try shorter word
            }
            firstSection = false;
        }
        return words;

    }

    private void attachDefinitions(FoundWord word, WordScanner conjugations)
    {
        for(ValidWord match:conjugations.getMatches())//for each possible conjugation...
        {
            List<Definition> defs = dict.findWord(match);
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

        boolean wasJapanese;
        boolean isJapanese = true;//Japanese.isJapaneseWriting(text.charAt(0));
        while(pos < text.length())
        {
            wasJapanese = isJapanese;
            isJapanese = true;//Japanese.isJapaneseWriting(text.charAt(pos));
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
