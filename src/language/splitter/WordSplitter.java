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

import language.segmenter.BasicSegmenter;
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
    private boolean mightBeDeconjugatable(String text)
    {
        boolean goodMatch = false;
        for(String ending:WordScanner.possibleEndings())
        {
            String attempt = text + ending; // "ending" is the deconjugated ending which the conjugation replaces 
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
        if(options.getOption("splitterMode").equals("none"))
        {
            ArrayList<FoundWord> list = new ArrayList<>();
            WordScanner word = new WordScanner(text);//deconjugate
            FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
            attachDefinitions(matchedWord, word);//add cached definitions
            list.add(matchedWord);
            return list;
        }
        else if(!options.getOption("splitterMode").equals("full"))
        {
            ArrayList<FoundWord> list = new ArrayList<>();
            if(Segmenter.extended)
            {
                String scratch = "";
                boolean wasAllKanji = false;
                for(Piece s : Segmenter.instance.Segment(text))
                {
                    boolean isAllKana = Japanese.hasOnlyKana(s.txt);
                    boolean isAllKanji = Japanese.hasKanji(s.txt) && !Japanese.hasKana(s.txt);
                    
                    if(!isAllKana && !wasAllKanji && !scratch.equals(""))
                    {
                        WordScanner word = new WordScanner(scratch);//deconjugate
                        FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                        attachDefinitions(matchedWord, word);//add cached definitions
                        list.add(matchedWord);
                        scratch = "";
                    }
                    scratch += s.txt;
                    
                    wasAllKanji = isAllKanji;
                }
                if(!scratch.equals(""))
                {
                    WordScanner word = new WordScanner(scratch);//deconjugate
                    FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                    attachDefinitions(matchedWord, word);//add cached definitions
                    list.add(matchedWord);
                }
            }
            else
            {
                String scratch = "";
                boolean wasKana = false;
                for(char s : text.toCharArray())
                {
                    boolean isKanji = Japanese.isKanji(s);
                    
                    if(isKanji && wasKana && !scratch.equals(""))
                    {
                        WordScanner word = new WordScanner(scratch);//deconjugate
                        FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                        attachDefinitions(matchedWord, word);//add cached definitions
                        list.add(matchedWord);
                        scratch = "";
                    }
                    
                    wasKana = Japanese.isKana(s);
                }
                if(!scratch.equals(""))
                {
                    WordScanner word = new WordScanner(scratch);//deconjugate
                    FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                    attachDefinitions(matchedWord, word);//add cached definitions
                    list.add(matchedWord);
                }
            }
            return list;
        }
        else
            return splitSectionInternal(instance.Segment(text), firstSection);
    }
    
    private FoundWord splitSectionSingleWord(List<Piece> segments, boolean firstSection, boolean isExtended)
    {

        // Concepts:
        // - Segment: A string of text at least one character long which can be marked as "strong".
        // - Word: A string of text that has associated dictionary entries.
        // - Deconjugation: Taking a string of text and finding the desired "head word" or "lemma" with which we can find its definitions in a dictionary.
        // - Full Lookup: Full deconjugation and dictionary checking for a piece of text.
        // - Trivial Lookup: A lookup that needed no deconjugation. Refers to the result, not the process.
        // - Strong segments: Can only be the first segment of the word if they are the only segment of the word.
        // The strength of the following segments does not matter and the only number restriction is "not exactly one", and the rule does not apply if the strong segment is not the first segment.
        
        // - Deconjugation works by finding plausible original words based on recursive pattern replacement.
        // These plausible original words are then all looked up in the dictionaries.
        
        // - Long words are preferred to short words, otherwise parsing would be very hard.
        // Because of this, word splitting tries treating long sections of text as words before it tries treating short sections of text as words.
        // This would be extremely slow on long lines if it were not limited, but real words are relatively short, conjugated or otherwise.
        // As such, we limit the starting length to 100 characters.
        
        // - Because rule-based recursive deconjugation has relatively high computational complexity, 100 characters is still too long if you end up with 50 words in that 100 character string.
        // Because of this, we sort of want to avoid deconjugating long text. However, conjugations can be arbitrarily long, theoretically, so there's no limit.
        // The easiest way to deal with this is to find the longest string that consists of a dictionary word plus *possible* *single* conjugations, and use this as our "real" baseline for where we're starting.
        // However, segmentation can disagree with the deconjugator as to where conujgation segments start and end, so we have to do this in text, not segments.
        // This necessarily only covers a single step of recursive deconjugation, but is sufficient for turning strings into dictionary words with no care for accuracy.
        
        // - Sometimes segmentation makes it so that finding the right word is impossible. This means that we may need to do the process a second time, ignoring segments, to make sure it's not responsible.
        // We have to do this regardless of whether the single first segment gives dictionary results, to fix things like 知りあった being 知|りあった instead of 知り|あった (edict does not have the spelling 知りあう)
        // We only need to do this if it's possible that there is a word longer than one segment. Otherwise it would not find a new word.
        
        // - Sometimes the only available word is shorter than a single segment. In this case we need to split the segment up into single pieces and search words on that.  
        
        // Algorithm overview:
        // - Start with an insane length (100 characters)
        // - Shrink it down to the shortest *possible* word, based on partial deconjugation of text
        // - Start doing Full Lookups starting here, shrinking by segments each time; Full Lookups are constrained to segments
        // - Check strength rules when doing Full Lookups if the Full Lookup is also a Trivial Lookup
        // 
        int end = segments.size();
        int limit = end;
        // Parsing is faster if we start with a relatively short segment instead of the entire string.
        // Otherwise, deconjugation can be unnecessarily expensive, since it'll be passing around tons of memory unnecessarily
        // Luckily, even for words that can be deconjugated, we can make a pretty good guess at the longest string that could possibly be valid
        boolean mayBeLong = false;
        
        // We only need to start with a length as the longest dictionary term, because it can be extended to pick up conjugation characters
        // 100 is a good conservative "no way there's a dictionary term this long" limit, and prevents long text from taking several seconds to parse
        int char_length = 0;
        int segment_length = 0;
        for(Piece s : Segmenter.subList(segments, 0, segments.size()))
        {
            char_length += s.txt.length();
            segment_length += 1;
            if(char_length >= 100) break;
        }
        if(end > segment_length)
            end = segment_length;
        
        // look for the longest section covered as-is in the dictionary
        while(end > 1)
        {
            String textHere = Segmenter.Unsegment(segments, 0, end);
            //System.out.println(textHere);
            // only check the epwing dictionary if this is the first segment in the section (for speed reasons)
            if(dict.findText(textHere) != null || (firstSection && dict.hasEpwingDef(textHere)) || mightBeDeconjugatable(textHere))
            {
                if(end > 1) mayBeLong = true;
                break;
            }
            end--;
        }

        // extend it to include any contiguous segments that might be conjugation text or aren't kana when the previous was kana
        String lastSegment = "";
        while(end < limit)
        {
            String nextSegment = segments.get(end).txt;
            if(Japanese.isJapaneseWriting(nextSegment) && (mightBeConjugation(nextSegment) || !(Japanese.hasKanji(nextSegment) && !Japanese.hasOnlyKana(lastSegment))))
            {
                end++;
                lastSegment = nextSegment;
            }
            else
                break;
        }
        
        // Now check if our string is a word, conjugated or not. If it isn't, shorten the string and try again until we only have one segment left.
        // suppress bogus java compiler complaint when otherwise using while(end > 0)
        assert(end > 0);
        while(true)
        {
            if(end > 1 && !segments.get(0).strong)
            {
                String str = Segmenter.Unsegment(segments, 0, end);
                System.out.println(str);
    
                WordScanner word = new WordScanner(str);//deconjugate
                FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                attachDefinitions(matchedWord, word);//add cached definitions
    
                // Use current string if it's a good word or if we do not have full parsing enabled or if it's only one segment long
                if(matchedWord.getDefinitionCount() > 0 || (firstSection && dict.hasEpwingDef(word.getWord())))
                {
                    System.out.println("Match type 1");
                    System.out.println("Strength: " + segments.get(0).strong);
                    System.out.println("End: " + end);
                    System.out.println("Extended: " + isExtended);
                    for(int i = 0; i < end; i++)
                        segments.remove(0);
                    return matchedWord;
                }
            }
            else if(end == 1)
            {
                // If we went all the way down to the first segment without matching anything yet, but it might be because of segment positions, retry with characters as segments instead of substrings as segments
                if(mayBeLong && isExtended && !segments.get(0).strong)
                {
                    FoundWord word = splitSectionSingleWord(Segmenter.basicInstance.Segment(Segmenter.Unsegment(segments, 0, segments.size())), firstSection, false);
                    int targetLen = word.getText().length();
                    
                    // Need to rebuild the portion of the segments list that word consumed
                    int removedLen = 0;
                    while(removedLen < targetLen)
                    {
                        int segmentLen = segments.get(0).txt.length();
                        if(removedLen + segmentLen <= targetLen)
                        {
                            segments.remove(0);
                            removedLen += segmentLen;
                        }
                        else
                        {
                            segments.get(0).txt = segments.get(0).txt.substring(targetLen-removedLen);
                            segments.get(0).strong = false;
                            removedLen = targetLen;
                        }
                    }
                    System.out.println("Match type 2");
                    return word;
                }
                else
                {
                    String str = Segmenter.Unsegment(segments, 0, end);
                    System.out.println(str);
        
                    WordScanner word = new WordScanner(str);//deconjugate
                    FoundWord matchedWord = new FoundWord(word.getWord());//prototype definition
                    attachDefinitions(matchedWord, word);//add cached definitions
                    
                    boolean defined = matchedWord.getDefinitionCount() > 0 || (firstSection && dict.hasEpwingDef(word.getWord()));
                    
                    if(!defined && (segments.get(0).txt.length() > 1 && isExtended))
                    {
                        FoundWord foundword = splitSectionSingleWord(Segmenter.basicInstance.Segment(Segmenter.Unsegment(segments, 0, 1)), firstSection, false);
                        segments.get(0).txt = segments.get(0).txt.substring(foundword.getText().length());
                        segments.get(0).strong = false;
                        assert(segments.get(0).txt.length() > 0);
                        System.out.println("Match type 3");
                        return foundword;
                    }
                    else
                    {
                        // Use current string if it's a good word or if we do not have full parsing enabled or if it's only one segment long
                        for(int i = 0; i < end; i++)
                            segments.remove(0);
                        System.out.println("Match type 4");
                        return matchedWord;
                    }
                }
            }
            end--;
        }
    }
    
    private List<FoundWord> splitSectionInternal(List<Piece> segments, boolean firstSection)
    {
        List<FoundWord> words = new ArrayList<>();
        
        System.out.println("Splitter output:");
        for(Piece p : segments)
            System.out.println(p.txt);
        
        Integer start = 0;
        while(segments.size() > 0)
        {
            words.add(splitSectionSingleWord(segments, firstSection, Segmenter.extended));
            firstSection = false;
            System.out.println("Full word cycle");
        }
        
        System.out.println("Done splitting");
        
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
