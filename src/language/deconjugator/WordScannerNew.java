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

import java.util.ArrayList;
import java.util.LinkedList;

public class WordScannerNew extends WordScanner implements WordScanner.SubScanner
{
    public void subInit()
    {
        if(ruleList != null)return;
        ruleList = new ArrayList<>();

        //Decensor
        ruleList.add(new DecensorRule('ん'));
        ruleList.add(new DecensorRule('っ'));

        /*
        // handle "must" in a single block because it's dumb and long
        // todo: add a type of rule that allows A/B matches in conjugated ending
        // todo: or allow conjugation display to substitute sequences "process" text for others
        // todo: (so that this isn't needed to make "must" look like "must"
        ruleList.add(new StdRule("いけない", "", "must", DefTag.stem_must_first_half, DefTag.adj_i));
        ruleList.add(new StdRule("いけぬ", "", "must", DefTag.stem_must_first_half, DefTag.adj_i));
        ruleList.add(new StdRule("ならない", "", "must", DefTag.stem_must_first_half, DefTag.adj_i));
        ruleList.add(new StdRule("ならぬ", "", "must", DefTag.stem_must_first_half, DefTag.adj_i));
        ruleList.add(new StdRule("ねば", "", "(negative condition)", DefTag.stem_a, DefTag.stem_must_first_half));
        ruleList.add(new StdRule("ねば", "", "(negative condition)", DefTag.stem_ku, DefTag.stem_must_first_half));
        ruleList.add(new StdRule("なければ", "", "(negative condition)", DefTag.stem_a, DefTag.stem_must_first_half));
        ruleList.add(new StdRule("なければ", "", "(negative condition)", DefTag.stem_ku, DefTag.stem_must_first_half));
        ruleList.add(new StdRule("なくては", "", "(negative condition)", DefTag.stem_a, DefTag.stem_must_first_half));
        ruleList.add(new StdRule("なくては", "", "(negative condition)", DefTag.stem_ku, DefTag.stem_must_first_half));
        // fixme: can this use たら instead of ば as well? are certain combinations forbidden?
        // fixme: なかったら? ないと?
        // fixme: なりません? いけません?
        */

        ruleList.add(new ContextRule("たい", "", "want", DefTag.stem_ren, DefTag.adj_i, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_adj_base)
                return false;
            return true;
        }));

        ruleList.add(new StdRule("ください", "", "polite request", DefTag.stem_te, DefTag.adj_i));

        // te form
        // verbs
        ruleList.add(new StdRule("で", "", "(te form)", DefTag.stem_ren_less_v, DefTag.stem_te));
        ruleList.add(new StdRule("て", "", "(te form)", DefTag.stem_ren_less, DefTag.stem_te));
        ruleList.add(new StdRule("て", "", "(te form)", DefTag.stem_ren, DefTag.stem_te)); // formal but real
        // i-adjectives
        // i-adjectives have two te forms. One works well with auxiliary verbs (and thus deconjugation), and the other does not.
        ruleList.add(new StdRule("で", "", "(te form)", DefTag.adj_i, DefTag.stem_te));
        ruleList.add(new StdRule("て", "", "(te form)", DefTag.stem_ku, DefTag.stem_te_defective));

        // te-form auxiliaries that sometimes require rewrites after they eat て
        ruleList.add(new StdRule("しまう", "", "completely", DefTag.stem_te, DefTag.v5u));
        ruleList.add(new StdRule("ちゃう", "てしまう", "(reduced)", DefTag.v5u, DefTag.v5u));
        ruleList.add(new StdRule("じゃう", "でしまう", "(reduced)", DefTag.v5u, DefTag.v5u));

        // improves parsing. can be rewritten by ちゃ
        ruleList.add(new StdRule("は", "", "(topic)", DefTag.stem_te, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ちゃ", "ては", "(reduced)", DefTag.stem_te, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("じゃ", "では", "(reduced)", DefTag.stem_te, DefTag.uninflectable));

        ruleList.add(new StdRule("は", "", "(topic)", DefTag.stem_te_defective, DefTag.uninflectable));

        // todo: add better names for these later
        // Should be restricted to verbs
        ruleList.add(new StdRule("いる", "", "teiru", DefTag.stem_te, DefTag.v1));
        // -- common colloquial form drops the い entirely));
        ruleList.add(new ContextRule("る", "", "teru", DefTag.stem_te, DefTag.v1, (rule, word) -> {
            if(word.getConjugationTags().size() < 1) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(0);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_ren
            || tagOfInterest == DefTag.stem_ren_less)
                return false;
            return true;
        }));
        // Not sure if these should be restricted to verbs but probably
        ruleList.add(new StdRule("いく", "", "teiku", DefTag.stem_te, DefTag.v5k_s));
        ruleList.add(new StdRule("くる", "", "tekuru", DefTag.stem_te, DefTag.vk));
        // Should form differently on adjectives than verbs
        ruleList.add(new StdRule("ある", "", "tearu", DefTag.stem_te, DefTag.v5aru));

        // たら, the generic conditional
        // verbs
        ruleList.add(new OnlyFinalRule("たら", "", "conditional", DefTag.stem_ren_less, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("たらば", "", "formal conditional", DefTag.stem_ren_less, DefTag.uninflectable));
        // (voiced)
        ruleList.add(new OnlyFinalRule("だら", "", "conditional", DefTag.stem_ren_less_v, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("だらば", "", "formal conditional", DefTag.stem_ren_less_v, DefTag.uninflectable));
        // i-adjectives
        ruleList.add(new OnlyFinalRule("ったら", "", "conditional", DefTag.stem_ka, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ったらば", "", "formal conditional", DefTag.stem_ka, DefTag.uninflectable));

        // ば, the provisional conditional
        // verbs
        ruleList.add(new StdRule("ば", "", "provisional conditional", DefTag.stem_e, DefTag.uninflectable));
        // i-adjectives
        ruleList.add(new StdRule("れば", "", "provisional conditional", DefTag.stem_ke, DefTag.uninflectable));

        ruleList.add(new OnlyFinalRule("きゃ", "ければ", "(reduced)", DefTag.uninflectable, DefTag.uninflectable));

        // past
        // verbs
        ruleList.add(new OnlyFinalRule("だ", "", "past", DefTag.stem_ren_less_v, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("た", "", "past", DefTag.stem_ren_less, DefTag.uninflectable));
        // i-adjectives
        ruleList.add(new OnlyFinalRule("った", "", "past", DefTag.stem_ka, DefTag.uninflectable));

        // たり is its own morpheme, not た+り, and semgmenters (like kuromoji) should make たり an entire segment, so we have to deconjugate たり (it's also the right thing to do)
        // * etymology: てあり; as in てある
        ruleList.add(new OnlyFinalRule("だり", "", "~tari", DefTag.stem_ren_less_v, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("たり", "", "~tari", DefTag.stem_ren_less, DefTag.uninflectable));
        // i-adjectives
        ruleList.add(new OnlyFinalRule("ったり", "", "~tari", DefTag.stem_ka, DefTag.uninflectable));


        // passive (godan)
        ruleList.add(new StdRule("れる", "", "passive", DefTag.stem_a, DefTag.v1)); // ichidan cannot conjugate to "stem_a"

        // passive-potential (ichidan)
        ruleList.add(new StdRule("られる", "る", "potential/passive nexus", DefTag.v1, DefTag.v1));

        // potential
        // pattern is the same for ichidan and godan verbs; the ichidan one is PROscribed, but still real.
        ruleList.add(new StdRule("る", "", "potential", DefTag.stem_e, DefTag.v1));

        // causative
        ruleList.add(new StdRule("させる", "る", "causative", DefTag.v1, DefTag.v1));
        ruleList.add(new StdRule("せる", "", "causative", DefTag.stem_a, DefTag.v1)); // ichidan cannot conjugate to "stem_a"
        // spoken language -- this also covers the "short causative passive" indirectly
        // Only allowed on non-す godan verbs.
        ruleList.add(new ContextRule("す", "", "short causative", DefTag.stem_a, DefTag.v5s, (rule, word) -> {
            if(word.getWord().equals("")) return false;
            if(!word.getWord().endsWith(rule.ending)) return false;
            String base = word.getWord().substring(0, word.getWord().length() - rule.ending.length());
            //noinspection RedundantIfStatement
            if(base.endsWith("さ")) return false;
            else return true;
        }));
        // nasai
        // technically an i-adjective, but again, letting the deconjugator use it like that would cause more problems than it's worth
        ruleList.add(new OnlyFinalRule("なさい", "", "kind request", DefTag.stem_ren, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("な", "", "casual kind request", DefTag.stem_ren, DefTag.uninflectable));

        // nagara, a "while" term
        ruleList.add(new OnlyFinalRule("ながら", "", "while", DefTag.stem_ren, DefTag.uninflectable));

        // ます inflects, but does so entirely irregularly.
        ruleList.add(new OnlyFinalRule("ます", "", "polite", DefTag.stem_ren, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ません", "", "negative polite", DefTag.stem_ren, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ました", "", "past polite", DefTag.stem_ren, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ませんでした", "", "past negative polite", DefTag.stem_ren, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ましょう", "", "polite volitional", DefTag.stem_ren, DefTag.uninflectable));

        // part-of-speech roles
        ruleList.add(new StdRule("に", "", "adverb", DefTag.adj_na));
        ruleList.add(new StdRule("な", "", "attributive", DefTag.adj_na));
        ruleList.add(new StdRule("の", "", "attributive", DefTag.adj_no));
        ruleList.add(new StdRule("と", "", "adverb", DefTag.adv_to));

        // i-adjective stems
        ruleList.add(new StdRule("く", "い", "(adverb)", DefTag.adj_i, DefTag.stem_ku));
        ruleList.add(new StdRule("か", "い", "(ka stem)", DefTag.adj_i, DefTag.stem_ka));
        ruleList.add(new StdRule("け", "い", "(ke stem)", DefTag.adj_i, DefTag.stem_ke));
        ruleList.add(new StdRule("さ", "い", "noun form", DefTag.adj_i, DefTag.n));
        // also applies to verbs
        ruleList.add(new StdRule("すぎる", "い", "excess", DefTag.adj_i, DefTag.v1));
        ruleList.add(new StdRule("そう", "い", "seemingness", DefTag.adj_i, DefTag.adj_na));
        ruleList.add(new StdRule("がる", "い", "~garu", DefTag.adj_i, DefTag.v5r));
        ruleList.add(new StdRule("", "い", "(stem)", DefTag.adj_i, DefTag.stem_adj_base));

        // negative
        // verbs
        ruleList.add(new ContextRule("ない", "", "negative", DefTag.stem_mizenkei, DefTag.adj_i, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_adj_base)
                return false;
            return true;
        }));
        ruleList.add(new OnlyFinalRule("ず", "", "adverbial negative", DefTag.stem_mizenkei, DefTag.uninflectable)); // archaically, not adverbiall, but in modern japanese, almost always adverbial
        ruleList.add(new OnlyFinalRule("ずに", "", "without doing so", DefTag.stem_mizenkei, DefTag.uninflectable)); // exactly the same meaning, despite the difference in label
        // i-adjectives
        ruleList.add(new ContextRule("ない", "", "negative", DefTag.stem_ku, DefTag.adj_i, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_adj_base)
                return false;
            return true;
        }));

        ruleList.add(new NeverFinalRule("", "", "(mizenkei)", DefTag.stem_a, DefTag.stem_mizenkei));
        ruleList.add(new NeverFinalRule("", "る", "(mizenkei)", DefTag.v1, DefTag.stem_mizenkei));

        // potential stem (and stem of some conjunctions)
        ruleList.add(new NeverFinalRule("け", "く", "(izenkei)", DefTag.v5k, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("せ", "す", "(izenkei)", DefTag.v5s, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("て", "つ", "(izenkei)", DefTag.v5t, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("え", "う", "(izenkei)", DefTag.v5u, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("れ", "る", "(izenkei)", DefTag.v5r, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("げ", "ぐ", "(izenkei)", DefTag.v5g, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("べ", "ぶ", "(izenkei)", DefTag.v5b, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("ね", "ぬ", "(izenkei)", DefTag.v5n, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("め", "む", "(izenkei)", DefTag.v5m, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("れ", "る", "(izenkei)", DefTag.v1,  DefTag.stem_e)); // not a copy/paste mistake
        // marginal categories
        ruleList.add(new NeverFinalRule("え", "う", "(izenkei)", DefTag.v5u_s, DefTag.stem_e));
        ruleList.add(new NeverFinalRule("け", "く", "(izenkei)", DefTag.v5k_s, DefTag.stem_e));

        // imperatives
        ruleList.add(new OnlyFinalRule("け", "く", "imperative", DefTag.v5k, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("せ", "す", "imperative", DefTag.v5s, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("て", "つ", "imperative", DefTag.v5t, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("え", "う", "imperative", DefTag.v5u, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("れ", "る", "imperative", DefTag.v5r, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("げ", "ぐ", "imperative", DefTag.v5g, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("べ", "ぶ", "imperative", DefTag.v5b, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ね", "ぬ", "imperative", DefTag.v5n, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("め", "む", "imperative", DefTag.v5m, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("ろ", "る", "imperative", DefTag.v1, DefTag.uninflectable));
        // marginal categories
        ruleList.add(new OnlyFinalRule("え", "う", "imperative", DefTag.v5u_s, DefTag.uninflectable));
        ruleList.add(new OnlyFinalRule("け", "く", "imperative", DefTag.v5k_s, DefTag.uninflectable));

        // "a" stem used by godan verbs
        ruleList.add(new NeverFinalRule("か", "く", "('a' stem)", DefTag.v5k, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("さ", "す", "('a' stem)", DefTag.v5s, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("た", "つ", "('a' stem)", DefTag.v5t, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("わ", "う", "('a' stem)", DefTag.v5u, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("ら", "る", "('a' stem)", DefTag.v5r, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("が", "ぐ", "('a' stem)", DefTag.v5g, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("ば", "ぶ", "('a' stem)", DefTag.v5b, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("な", "ぬ", "('a' stem)", DefTag.v5n, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("ま", "む", "('a' stem)", DefTag.v5m, DefTag.stem_a));
        // marginal categories
        ruleList.add(new NeverFinalRule("わ", "う", "('a' stem)", DefTag.v5u_s, DefTag.stem_a));
        ruleList.add(new NeverFinalRule("か", "く", "('a' stem)", DefTag.v5k_s, DefTag.stem_a));

        // past stem
        ruleList.add(new NeverFinalRule("い", "く", "(unstressed infinitive)", DefTag.v5k, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("し", "す", "(unstressed infinitive)", DefTag.v5s, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("っ", "つ", "(unstressed infinitive)", DefTag.v5t, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("っ", "う", "(unstressed infinitive)", DefTag.v5u, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("っ", "る", "(unstressed infinitive)", DefTag.v5r, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("い", "ぐ", "(unstressed infinitive)", DefTag.v5g, DefTag.stem_ren_less_v));
        ruleList.add(new NeverFinalRule("ん", "ぶ", "(unstressed infinitive)", DefTag.v5b, DefTag.stem_ren_less_v));
        ruleList.add(new NeverFinalRule("ん", "ぬ", "(unstressed infinitive)", DefTag.v5n, DefTag.stem_ren_less_v));
        ruleList.add(new NeverFinalRule("ん", "む", "(unstressed infinitive)", DefTag.v5m, DefTag.stem_ren_less_v));
        ruleList.add(new NeverFinalRule(""  , "る", "(unstressed infinitive)", DefTag.v1,  DefTag.stem_ren_less));
        // marginal categories
        ruleList.add(new NeverFinalRule("う", "う", "(unstressed infinitive)", DefTag.v5u_s, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("っ", "く", "(unstressed infinitive)", DefTag.v5k_s, DefTag.stem_ren_less));

        // masu stem
        ruleList.add(new StdRule("き", "く", "(infinitive)", DefTag.v5k, DefTag.stem_ren));
        ruleList.add(new StdRule("ち", "つ", "(infinitive)", DefTag.v5t, DefTag.stem_ren));
        ruleList.add(new StdRule("い", "う", "(infinitive)", DefTag.v5u, DefTag.stem_ren));
        ruleList.add(new StdRule("り", "る", "(infinitive)", DefTag.v5r, DefTag.stem_ren));
        ruleList.add(new StdRule("ぎ", "ぐ", "(infinitive)", DefTag.v5g, DefTag.stem_ren));
        ruleList.add(new StdRule("び", "ぶ", "(infinitive)", DefTag.v5b, DefTag.stem_ren));
        ruleList.add(new StdRule("に", "ぬ", "(infinitive)", DefTag.v5n, DefTag.stem_ren));
        ruleList.add(new StdRule("み", "む", "(infinitive)", DefTag.v5m, DefTag.stem_ren));
        // marginal categories
        ruleList.add(new StdRule("い", "う", "(infinitive)", DefTag.v5u_s, DefTag.stem_ren));
        ruleList.add(new StdRule("き", "く", "(infinitive)", DefTag.v5k_s, DefTag.stem_ren));
        // prevent bogus deconjugations
        ruleList.add(new ContextRule("し", "す", "(infinitive)", DefTag.v5s, DefTag.stem_ren, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_te)
                return false;
            return true;
        }));
        ruleList.add(new ContextRule(""  , "る", "(infinitive)", DefTag.v1,  DefTag.stem_ren, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_te)
                return false;
            return true;
        }));

        // volitional stem
        ruleList.add(new StdRule("こう", "く", "volitional", DefTag.v5k, DefTag.stem_ren_less));
        ruleList.add(new StdRule("そう", "す", "volitional", DefTag.v5s, DefTag.stem_ren_less));
        ruleList.add(new StdRule("とう", "つ", "volitional", DefTag.v5t, DefTag.stem_ren_less));
        ruleList.add(new StdRule("おう", "う", "volitional", DefTag.v5u, DefTag.stem_ren_less));
        ruleList.add(new StdRule("ろう", "る", "volitional", DefTag.v5r, DefTag.stem_ren_less));
        ruleList.add(new StdRule("ごう", "ぐ", "volitional", DefTag.v5g, DefTag.stem_ren_less_v));
        ruleList.add(new StdRule("ぼう", "ぶ", "volitional", DefTag.v5b, DefTag.stem_ren_less_v));
        ruleList.add(new StdRule("のう", "ぬ", "volitional", DefTag.v5n, DefTag.stem_ren_less_v));
        ruleList.add(new StdRule("もう", "む", "volitional", DefTag.v5m, DefTag.stem_ren_less_v));
        ruleList.add(new StdRule("よう"  , "る", "volitional", DefTag.v1,  DefTag.stem_ren_less));
        // marginal categories
        ruleList.add(new StdRule("おう", "う", "volitional", DefTag.v5u_s, DefTag.stem_ren_less));
        ruleList.add(new StdRule("こう", "く", "volitional", DefTag.v5k_s, DefTag.stem_ren_less));

        // irregulars
        ruleList.add(new ContextRule("し", "する", "(infinitive)", DefTag.vs_i,  DefTag.stem_ren, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_te)
                return false;
            return true;
        }));
        ruleList.add(new NeverFinalRule("し", "する", "(unstressed infinitive)", DefTag.vs_i, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("し", "する", "(mizenkei)", DefTag.vs_i, DefTag.stem_mizenkei)); // actually irregular itself but this will do for now
        ruleList.add(new NeverFinalRule("すれ", "する", "(izenkei)", DefTag.vs_i, DefTag.stem_e));
        ruleList.add(new StdRule("しろ", "する", "imperative", DefTag.vs_i, DefTag.uninflectable));
        ruleList.add(new StdRule("せよ", "する", "imperative", DefTag.vs_i, DefTag.uninflectable));

        ruleList.add(new ContextRule("き", "くる", "(infinitive)", DefTag.vk, DefTag.stem_ren, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_te)
                return false;
            return true;
        }));
        ruleList.add(new NeverFinalRule("き", "くる", "(unstressed infinitive)", DefTag.vk, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("こ", "くる", "(mizenkei)", DefTag.vk, DefTag.stem_mizenkei));
        ruleList.add(new NeverFinalRule("くれ", "くる", "(izenkei)", DefTag.vk, DefTag.stem_e));
        ruleList.add(new StdRule("こい", "くる", "imperative", DefTag.vk, DefTag.uninflectable));

        ruleList.add(new ContextRule("来", "来る", "(infinitive)", DefTag.vk, DefTag.stem_ren, (rule, word) -> {
            if(word.getConjugationTags().size() < 2) return true;
            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
            //noinspection RedundantIfStatement
            if(tagOfInterest == DefTag.stem_te)
                return false;
            return true;
        }));
        ruleList.add(new NeverFinalRule("来", "来る", "(unstressed infinitive)", DefTag.vk, DefTag.stem_ren_less));
        ruleList.add(new NeverFinalRule("来", "来る", "(mizenkei)", DefTag.vk, DefTag.stem_mizenkei));
        ruleList.add(new NeverFinalRule("来れ", "来る", "(izenkei)", DefTag.vk, DefTag.stem_e));
        ruleList.add(new StdRule("来い", "来る", "imperative", DefTag.vk, DefTag.uninflectable));

        ruleList.add(new NeverFinalRule("あり", "ある", "(infinitive)", DefTag.v5r_i, DefTag.stem_ren));
        ruleList.add(new StdRule("あっ", "ある", "(unstressed infinitive)", DefTag.v5r_i, DefTag.stem_ren_less));
        //ruleList.add(new StdRule("", "ある", "(mizenkei)", DefTag.v5r_i, DefTag.stem_mizenkei)); // not used
        ruleList.add(new NeverFinalRule("あれ", "ある", "(izenkei)", DefTag.v5r_i, DefTag.stem_e));
        // ruleList.add(new StdRule("あれ", "ある", "imperative", DefTag.v5r_i, DefTag.uninflectable)); // rare and conflicts with あれ "that"

        // rewrite rules
        ruleList.add(new RewriteRule("でした", "です", "past", DefTag.aux, DefTag.aux));

        // enable deconjugation of bound noun+suru dictionary entries that might have archaic equivalents, like 冠する
        // only has an effect on the kuromoji backend, on the normal one the archaic equivalent doesn't cause problems
        ruleList.add(new StdRule("する", "する", "", DefTag.vs_s, DefTag.vs_i));
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
