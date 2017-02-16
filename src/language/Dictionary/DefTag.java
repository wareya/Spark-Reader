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
package language.dictionary;

/**
 *
 * @author laure
 */
public enum DefTag
{
    //codes:
    //0=part of speech
    //1=term type
    //2=dialect
    //////////////////
    //part of speech//
    //////////////////
    adj_i(0, "adjective (keiyoushi)"),
    adj_na(0, "adjectival nouns or quasi_adjectives (keiyodoshi)"),
    adj_no(0, "nouns which may take the genitive case particle `no'"),
    adj_pn(0, "pre_noun adjectival (rentaishi)"),
    adj_t(0, "`taru' adjective"),
    adj_f(0, "noun or verb acting prenominally (other than the above)"),
    
    adj(0, "former adjective classification (being removed)"),
    adv(0, "adverb (fukushi)"),
    adv_n(0, "adverbial noun"),
    adv_to(0, "adverb taking the `to' particle"),
    aux(0, "auxiliary"),
    aux_v(0, "auxiliary verb"),
    aux_adj(0, "auxiliary adjective"),
    conj(0, "conjunction"),
    ctr(0, "counter"),
    exp(0, "Expressions (phrases, clauses, etc.)"),
    INT(0, "interjection (kandoushi)"),
    iv(0, "irregular verb"),
    n(0, "noun (common) (futsuumeishi)"),
    n_adv(0, "adverbial noun (fukushitekimeishi)"),
    n_pref(0, "noun, used as a prefix"),
    n_suf(0, "noun, used as a suffix"),
    n_t(0, "noun (temporal) (jisoumeishi)"),
    num(0, "numeric"),
    pn(0, "pronoun"),
    pref(0, "prefix"),
    prt(0, "particle"),
    suf(0, "suffix"),
    
    v1(0, "Ichidan verb"),
    v2a_s(0, "Nidan verb with 'u' ending (archaic)"),
    v4h(0, "Yodan verb with `hu/fu' ending (archaic)"),
    v4r(0, "Yodan verb with `ru' ending (archaic)"),
    v5(0, "Godan verb (not completely classified)"),
    v5aru(0, "Godan verb - -aru special class"),
    v5b(0, "Godan verb with `bu' ending"),
    v5g(0, "Godan verb with `gu' ending"),
    v5k(0, "Godan verb with `ku' ending"),
    v5k_s(0, "Godan verb - iku/yuku special class"),
    v5m(0, "Godan verb with `mu' ending"),
    v5n(0, "Godan verb with `nu' ending"),
    v5r(0, "Godan verb with `ru' ending"),
    v5r_i(0, "Godan verb with `ru' ending (irregular verb)"),
    v5s(0, "Godan verb with `su' ending"),
    v5t(0, "Godan verb with `tsu' ending"),
    v5u(0, "Godan verb with `u' ending"),
    v5u_s(0, "Godan verb with `u' ending (special class)"),
    v5uru(0, "Godan verb - uru old class verb (old form of Eru)"),
    v5z(0, "Godan verb with `zu' ending"),
    vz(0, "Ichidan verb - zuru verb - (alternative form of -jiru verbs)"),
    
    vi(0, "intransitive verb"),
    vk(0, "kuru verb - special class"),
    vn(0, "irregular nu verb"),
    vs(0, "noun or participle which takes the aux. verb suru"),
    vs_c(0, "su verb - precursor to the modern suru"),
    vs_i(0, "suru verb - irregular"),
    vs_s(0, "suru verb - special class"),
    vt(0, "transitive verb"),
    
    //////////////
    //term types//
    //////////////
    Buddh(1, "Buddhist term"),
    MA(1, "martial arts term"),
    comp(1, "computer terminology"),
    food(1, "food term"),
    geom(1, "geometry term"),
    gram(1, "grammatical term"),
    ling(1, "linguistics terminology"),
    math(1, "mathematics"),
    mil(1, "military"),
    physics(1, "physics terminology"),
    X(2, "rude or X_rated term"),
    abbr(2, "abbreviation"),
    arch(2, "archaism"),
    ateji(2, "ateji (phonetic) reading"),
    chn(2, "children's language"),
    col(2, "colloquialism"),
    derog(2, "derogatory term"),
    eK(2, "exclusively kanji"),
    ek(2, "exclusively kana"),
    fam(2, "familiar language"),
    fem(2, "female term or language"),
    gikun(2, "gikun (meaning) reading"),
    hon(2, "honorific or respectful (sonkeigo) language"),
    hum(2, "humble (kenjougo) language"),
    ik(2, "word containing irregular kana usage"),
    iK(2, "word containing irregular kanji usage"),
    id(2, "idiomatic expression"),
    io(2, "irregular okurigana usage"),
    m_sl(2, "manga slang"),
    male(2, "male term or language"),
    male_sl(2, "male slang"),
    oK(2, "word containing out_dated kanji"),
    obs(2, "obsolete term"),
    obsc(2, "obscure term"),
    ok(2, "out_dated or obsolete kana usage"),
    on_mim(2, "onomatopoeic or mimetic word"),
    poet(2, "poetical term"),
    pol(2, "polite (teineigo) language"),
    rare(2, "rare"),
    sens(2, "sensitive word"),
    sl(2, "slang"),
    uK(2, "word usually written using kanji alone"),
    uk(2, "word usually written using kana alone"),
    vulg(2, "vulgar expression or word"),
    kyb(3, "Kyoto-ben"),
    osb(3, "Osaka-ben"),
    ksb(3, "Kansai-ben"),
    ktb(3, "Kantou-ben"),
    tsb(3, "Tosa-ben"),
    thb(3, "Touhoku-ben"),
    tsug(3, "Tsugaru-ben"),
    kyu(3, "Kyuushuu-ben"),
    rkb(3, "Ryuukyuu-ben"),
    
    p(4, ""), P(4, "");//TODO what are these, actually?
    private int group;
    private String name;
    DefTag(int group, String name)
    {
        this.group = group;
        this.name = name;
    }
    public String toString()
    {
        return name;
    }
    public static DefTag toTag(String text)
    {
        //System.out.println("tagging " + text);
        try
        {
            return valueOf(text.toLowerCase().replace('-', '_'));
        }catch(IllegalArgumentException e)
        {
            return null;
        }
    }
    
}
