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

package language.deconjugator;

import language.dictionary.DefTag;
import language.dictionary.Japanese;

import java.util.ArrayList;

public class WordScannerOld extends WordScanner implements SubScanner
{
    protected static ArrayList<ValidWordOld> matches;
    public void subinit()
    {
        if(ruleList != null)return;
        ruleList = new ArrayList<>();


        //Hiragana->Katakana: words are often written in katakana for emphasis but won't be found in EDICT in that form
        ruleList.add(word ->
        {
            String hiragana = Japanese.toHiragana(word.getWord(), false);
            if(!word.getWord().equals(hiragana))
            {
                return new ValidWord(hiragana, "hiragana");
            }
            else return null;
        });


        //Decensor: simple, but actually works well enough with a lot of 'censored' words
        ruleList.add(l_word ->
        {
            if(l_word.getWord().contains("○"))
            {
                return new ValidWord(l_word.getWord().replace('○', 'っ'), (l_word.getProcess() + " " + "censored").trim());
            }
            else return null;
        });
        ruleList.add(l_word ->
        {
            if(l_word.getWord().contains("○"))
            {
                return new ValidWord(l_word.getWord().replace('○', 'ん'), (l_word.getProcess() + " " + "censored").trim());
            }
            else return null;
        });

        //entire Japanese deconjugation lookup table
        //see http://www.wikiwand.com/en/Japanese_verb_conjugation

        //conditional/past conditional (ra) (+ba for formal) (adds on to past, must come before)
        ruleList.add(new StdRuleOld("ったら", "った", "conditional", DefTag.v5u));
        ruleList.add(new StdRuleOld("ったらば", "った", "conditional-formal", DefTag.v5u));
        ruleList.add(new StdRuleOld("いたら", "いた", "conditional", DefTag.v5k));
        ruleList.add(new StdRuleOld("いたらば", "いた", "conditional-formal", DefTag.v5k));
        ruleList.add(new StdRuleOld("いだら", "いだ", "conditional", DefTag.v5g));
        ruleList.add(new StdRuleOld("いだらば", "いだ", "conditional-formal", DefTag.v5g));
        ruleList.add(new StdRuleOld("したら", "した", "conditional", DefTag.v5s));
        ruleList.add(new StdRuleOld("したらば", "した", "conditional-formal", DefTag.v5s));
        ruleList.add(new StdRuleOld("ったら", "った", "conditional", DefTag.v5t));
        ruleList.add(new StdRuleOld("ったらば", "った", "conditional-formal", DefTag.v5t));
        ruleList.add(new StdRuleOld("んだら", "んだ", "conditional", DefTag.v5b));
        ruleList.add(new StdRuleOld("んだらば", "んだ", "conditional-formal", DefTag.v5b));
        ruleList.add(new StdRuleOld("んだら", "んだ", "conditional", DefTag.v5n));
        ruleList.add(new StdRuleOld("んだらば", "んだ", "conditional-formal", DefTag.v5n));
        ruleList.add(new StdRuleOld("んだら", "んだ", "conditional", DefTag.v5m));
        ruleList.add(new StdRuleOld("んだらば", "んだ", "conditional-formal", DefTag.v5m));
        ruleList.add(new StdRuleOld("ったら", "った", "conditional", DefTag.v5r));
        ruleList.add(new StdRuleOld("ったらば", "った", "conditional-formal", DefTag.v5r));

        ruleList.add(new StdRuleOld("かったら", "かった", "conditional", DefTag.adj_i));//TODO does this work with i adjectives?
        ruleList.add(new StdRuleOld("かったらば", "かった", "conditional-formal", DefTag.adj_i));//TODO does this work with i adjectives?
        ruleList.add(new StdRuleOld("たら", "た", "conditional", DefTag.v1));
        ruleList.add(new StdRuleOld("たらば", "た", "conditional-formal", DefTag.v1));

        //potential (can do verb, can combine with past)
        //further conjugates like v1
        ruleList.add(new StdRuleOld("える", "う", "potential", DefTag.v5u, DefTag.v1));
        ruleList.add(new StdRuleOld("ける", "く", "potential", DefTag.v5k, DefTag.v1));
        ruleList.add(new StdRuleOld("げる", "ぐ", "potential", DefTag.v5g, DefTag.v1));
        ruleList.add(new StdRuleOld("せる", "す", "potential", DefTag.v5s, DefTag.v1));
        ruleList.add(new StdRuleOld("てる", "つ", "potential", DefTag.v5t, DefTag.v1));
        ruleList.add(new StdRuleOld("べる", "ぶ", "potential", DefTag.v5b, DefTag.v1));
        ruleList.add(new StdRuleOld("ねる", "ぬ", "potential", DefTag.v5n, DefTag.v1));
        ruleList.add(new StdRuleOld("める", "む", "potential", DefTag.v5m, DefTag.v1));
        ruleList.add(new StdRuleOld("れる", "る", "potential", DefTag.v5r, DefTag.v1));

        //past->dict
        ruleList.add(new StdRuleOld("った", "う", "past", DefTag.v5u));
        ruleList.add(new StdRuleOld("いた", "く", "past", DefTag.v5k));
        ruleList.add(new StdRuleOld("いだ", "ぐ", "past", DefTag.v5g));
        ruleList.add(new StdRuleOld("した", "す", "past", DefTag.v5s));
        ruleList.add(new StdRuleOld("った", "つ", "past", DefTag.v5t));
        ruleList.add(new StdRuleOld("んだ", "ぶ", "past", DefTag.v5b));
        ruleList.add(new StdRuleOld("んだ", "ぬ", "past", DefTag.v5n));
        ruleList.add(new StdRuleOld("んだ", "む", "past", DefTag.v5m));
        ruleList.add(new StdRuleOld("った", "る", "past", DefTag.v5r));

        ruleList.add(new StdRuleOld("かった", "い", "past", DefTag.adj_i));
        ruleList.add(new StdRuleOld("た", "る", "past", DefTag.v1));

        //te->dict
        ruleList.add(new StdRuleOld("って", "う", "て form", DefTag.v5u));
        ruleList.add(new StdRuleOld("いて", "く", "て form", DefTag.v5k));
        ruleList.add(new StdRuleOld("いで", "ぐ", "て form", DefTag.v5g));
        ruleList.add(new StdRuleOld("して", "す", "て form", DefTag.v5s));
        ruleList.add(new StdRuleOld("って", "つ", "て form", DefTag.v5t));
        ruleList.add(new StdRuleOld("んで", "ぶ", "て form", DefTag.v5b));
        ruleList.add(new StdRuleOld("んで", "ぬ", "て form", DefTag.v5n));
        ruleList.add(new StdRuleOld("んで", "む", "て form", DefTag.v5m));
        ruleList.add(new StdRuleOld("って", "る", "て form", DefTag.v5r));

        ruleList.add(new StdRuleOld("くて", "い", "て form", DefTag.adj_i));
        ruleList.add(new StdRuleOld("て", "る", "て form", DefTag.v1));

        //neg->dict (te form must be done first)
        //further conjugates like i adjective
        ruleList.add(new StdRuleOld("わない", "う", "negative", DefTag.v5u, DefTag.adj_i));
        ruleList.add(new StdRuleOld("かない", "く", "negative", DefTag.v5k, DefTag.adj_i));
        ruleList.add(new StdRuleOld("がない", "ぐ", "negative", DefTag.v5g, DefTag.adj_i));
        ruleList.add(new StdRuleOld("さない", "す", "negative", DefTag.v5s, DefTag.adj_i));
        ruleList.add(new StdRuleOld("たない", "つ", "negative", DefTag.v5t, DefTag.adj_i));
        ruleList.add(new StdRuleOld("ばない", "ぶ", "negative", DefTag.v5b, DefTag.adj_i));
        ruleList.add(new StdRuleOld("なない", "ぬ", "negative", DefTag.v5n, DefTag.adj_i));
        ruleList.add(new StdRuleOld("まない", "む", "negative", DefTag.v5m, DefTag.adj_i));
        ruleList.add(new StdRuleOld("らない", "る", "negative", DefTag.v5r, DefTag.adj_i));

        ruleList.add(new StdRuleOld("くない", "い", "negative", DefTag.adj_i));
        ruleList.add(new StdRuleOld("ない", "る", "negative", DefTag.v1, DefTag.adj_i));

        //masu/tai/etc removal (handled after past so that still conjugates, before i stem)
        //TODO make these only work with verbs (not with gatai!)
        ruleList.add(new StdRuleOld("ます", "", "polite"));
        ruleList.add(new StdRuleOld("ません", "", "negative polite"));
        ruleList.add(new StdRuleOld("たい", "", "want"));
        ruleList.add(new StdRuleOld("なさい", "", "command"));

        //i stem (polite/tai/etc)
        ruleList.add(new StdRuleOld("い", "う", "i stem", DefTag.v5u));
        ruleList.add(new StdRuleOld("き", "く", "i stem", DefTag.v5k));
        ruleList.add(new StdRuleOld("ぎ", "ぐ", "i stem", DefTag.v5g));
        ruleList.add(new StdRuleOld("し", "す", "I stem", DefTag.v5s));//note: capital I to stop removal of v5s tag
        ruleList.add(new StdRuleOld("ち", "つ", "i stem", DefTag.v5t));
        ruleList.add(new StdRuleOld("に", "ぬ", "i stem", DefTag.v5n));
        ruleList.add(new StdRuleOld("び", "ぶ", "i stem", DefTag.v5b));
        ruleList.add(new StdRuleOld("み", "む", "i stem", DefTag.v5m));
        ruleList.add(new StdRuleOld("り", "る", "i stem", DefTag.v5r));

        ruleList.add(new StdRuleOld("", "る", "i stem", DefTag.v1));
        //adjective stems moved to bottom to avoid conflict with provisional-conditional

        //adjective conjugations
        ruleList.add(new StdRuleOld("く", "い", "adverb", DefTag.adj_i));
        ruleList.add(new StdRuleOld("な", "", "adjective", DefTag.adj_na));
        ruleList.add(new StdRuleOld("の", "", "adjective", DefTag.adj_no));

        //potential was here

        //not for adjectives
        ruleList.add(new StdRuleOld("られる", "る", "potential", DefTag.v1));//normal
        ruleList.add(new StdRuleOld("らる", "る", "potential", DefTag.v1));//coloquial

        //passive
        //further conjugates like v1
        ruleList.add(new StdRuleOld("われる", "う", "passive", DefTag.v5u, DefTag.v1));
        ruleList.add(new StdRuleOld("かれる", "く", "passive", DefTag.v5k, DefTag.v1));
        ruleList.add(new StdRuleOld("がれる", "ぐ", "passive", DefTag.v5g, DefTag.v1));
        ruleList.add(new StdRuleOld("される", "す", "passive", DefTag.v5s, DefTag.v1));
        ruleList.add(new StdRuleOld("たてる", "つ", "passive", DefTag.v5t, DefTag.v1));
        ruleList.add(new StdRuleOld("ばれる", "ぶ", "passive", DefTag.v5b, DefTag.v1));
        ruleList.add(new StdRuleOld("なれる", "ぬ", "passive", DefTag.v5n, DefTag.v1));
        ruleList.add(new StdRuleOld("まれる", "む", "passive", DefTag.v5m, DefTag.v1));
        ruleList.add(new StdRuleOld("られる", "る", "passive", DefTag.v5r, DefTag.v1));

        //not for adjectives
        ruleList.add(new StdRuleOld("られる", "る", "passive", DefTag.v1));


        //causative

        //causative passive (colloquial version, add polite later?)


        //-eba form (provisional conditional)
        ruleList.add(new StdRuleOld("えば", "う", "provisional-conditional", DefTag.v5u));
        ruleList.add(new StdRuleOld("けば", "く", "provisional-conditional", DefTag.v5k));
        ruleList.add(new StdRuleOld("げば", "ぐ", "provisional-conditional", DefTag.v5g));
        ruleList.add(new StdRuleOld("せば", "す", "provisional-conditional", DefTag.v5s));
        ruleList.add(new StdRuleOld("てば", "つ", "provisional-conditional", DefTag.v5t));
        ruleList.add(new StdRuleOld("べば", "ぶ", "provisional-conditional", DefTag.v5b));
        ruleList.add(new StdRuleOld("ねば", "ぬ", "provisional-conditional", DefTag.v5n));
        ruleList.add(new StdRuleOld("めば", "む", "provisional-conditional", DefTag.v5m));
        ruleList.add(new StdRuleOld("れば", "る", "provisional-conditional", DefTag.v5r));
        ruleList.add(new StdRuleOld("れば", "る", "provisional-conditional", DefTag.v5r_i));

        ruleList.add(new StdRuleOld("くない", "い", "provisional-conditional", DefTag.adj_i));
        ruleList.add(new StdRuleOld("なければ", "ない", "provisional-conditional", DefTag.aux_adj));
        ruleList.add(new StdRuleOld("れば", "る", "provisional-conditional", DefTag.v1));


        //imperative (for orders)
        ruleList.add(new StdRuleOld("え", "う", "imperative", DefTag.v5u));
        ruleList.add(new StdRuleOld("け", "く", "imperative", DefTag.v5k));
        ruleList.add(new StdRuleOld("げ", "ぐ", "imperative", DefTag.v5g));
        ruleList.add(new StdRuleOld("せ", "す", "imperative", DefTag.v5s));
        ruleList.add(new StdRuleOld("て", "つ", "imperative", DefTag.v5t));
        ruleList.add(new StdRuleOld("べ", "ぶ", "imperative", DefTag.v5b));
        ruleList.add(new StdRuleOld("ね", "ぬ", "imperative", DefTag.v5n));
        ruleList.add(new StdRuleOld("め", "む", "imperative", DefTag.v5m));
        ruleList.add(new StdRuleOld("れ", "る", "imperative", DefTag.v5r));
        ruleList.add(new StdRuleOld("れ", "る", "imperative", DefTag.v5r_i));

        //not for i-adj, 4 exist for v1
        ruleList.add(new StdRuleOld("いろ", "いる", "imperative", DefTag.v1));
        ruleList.add(new StdRuleOld("いよ", "いる", "imperative", DefTag.v1));
        ruleList.add(new StdRuleOld("えろ", "える", "imperative", DefTag.v1));
        ruleList.add(new StdRuleOld("えよ", "える", "imperative", DefTag.v1));

        //volitional (let's)
        ruleList.add(new StdRuleOld("おう", "う", "volitional", DefTag.v5u));
        ruleList.add(new StdRuleOld("こう", "く", "volitional", DefTag.v5k));
        ruleList.add(new StdRuleOld("ごう", "ぐ", "volitional", DefTag.v5g));
        ruleList.add(new StdRuleOld("そう", "す", "volitional", DefTag.v5s));
        ruleList.add(new StdRuleOld("とう", "つ", "volitional", DefTag.v5t));
        ruleList.add(new StdRuleOld("ぼう", "ぶ", "volitional", DefTag.v5b));
        ruleList.add(new StdRuleOld("のう", "ぬ", "volitional", DefTag.v5n));
        ruleList.add(new StdRuleOld("もう", "む", "volitional", DefTag.v5m));
        ruleList.add(new StdRuleOld("ろう", "る", "volitional", DefTag.v5r));

        ruleList.add(new StdRuleOld("かろう", "い", "volitional", DefTag.adj_i));
        ruleList.add(new StdRuleOld("よう", "る", "volitional", DefTag.v1));


        ruleList.add(new StdRuleOld("", "い", "adj 'stem'", DefTag.adj_i));//Try match stem anyways, needed for things like '頼もしげに'
        //no stem for adjectives, but -sou sort-of uses a stem
        ruleList.add(new StdRuleOld("そう", "い", "-sou", DefTag.adj_i));
    }
    // nasty subroutine: make functional? how much overhead does passing data structures have in java?
    public void ScanWord(String word)
    {
        matches = new ArrayList<>();
        matches.add(new ValidWordOld(word, ""));//add initial unmodified word

        for(DeconRule rule:ruleList)
        {
            int size = matches.size();//don't scan ones added by this rule
            for(int i = 0; i < size; i++)
            {
                ValidWordOld gotten = (ValidWordOld)rule.process(matches.get(i));
                if(gotten != null)
                    matches.add(gotten);
            }
        }
    }

    public ArrayList<? extends AbstractWord> getInnerMatches()
    {
        return matches;
    }
}
