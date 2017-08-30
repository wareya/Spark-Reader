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

// With contributions Copyright 2017 Alexander Nadeau

package language.deconjugator;

import language.dictionary.DefTag;
import language.dictionary.Japanese;
import options.Underlay;

import java.util.ArrayList;
import java.util.LinkedList;

public class WordScannerNew extends WordScanner implements WordScanner.SubScanner
{
    public void subInit()
    {
        if(ruleList != null)return;
        ruleList = new ArrayList<>(Underlay.underlayDeconRules);
    }
    private int test_rules(int start)
    {
        int new_matches = 0;

        //attempt all deconjugation rules in order
        int size = matches.size();//don't scan matches added during iteration
        if(start == size) return 0;

        // Iterate matches outside of rules to reduce memory use (prevents making the same conjugation multiple ways)
        for(int i = start; i < size; i++)
        {
            for(DeconRule rule:ruleList)
            {
                //check if any of our possible matches can be deconjugated by this rule
                ValidWord gotten = rule.process(matches.get(i));
                if(gotten != null)
                {
                    matches.add(gotten);
                    new_matches++;
                }
            }
        }
        return new_matches;
    }
    public void scanWord(String word)
    {
        matches.add(new ValidWord(word));//add initial unmodified word
        // convert to kana and add that too if it's not already in hiragana
        String hiragana = Japanese.toHiragana(word, false);
        if(!word.equals(hiragana))
            matches.add(new ValidWord(hiragana));

        // start from the top of the list when we have a successful deconjugation
        int fully_covered_matches = 0;
        int iters = 0;
        while(true)
        {
            int matches_before_testing = matches.size();
            int number_of_new_matches = test_rules(fully_covered_matches);

            // the safeguards in process() should be enough, but in case they're not, or they break...
            if(iters > 24)
                break;

            iters++;

            if(number_of_new_matches > 0)
                fully_covered_matches = matches_before_testing;
            else
                break;
        }
    }
}
