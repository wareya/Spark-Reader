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
import language.dictionary.Definition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Holds a possible valid conjugation if a word exists with the valid tags
 * @author Laurens Weyn
 */
public class ValidWord extends AbstractWord
{
    protected HashSet<String> seenForms;
    protected ArrayList<DefTag> conjugationTags;
    protected Integer conjugations;

    public ValidWord(Integer conjugations, String originalWord, String word, HashSet<String> seenForms, Set<DefTag> neededTags, ArrayList<DefTag> conjugationTags, String process)
    {
        super(originalWord, word, neededTags, process);
        this.conjugations = conjugations;
        this.seenForms = seenForms;
        this.conjugationTags = conjugationTags;
    }
    public ValidWord(String word, String process)
    {
        super(word, process);
        this.conjugations = 0;
        this.neededTags = new HashSet<>();
        this.seenForms = new HashSet<>();
        this.conjugationTags = new ArrayList<>();
    }
    public Integer getNumConjugations()
    {
        return conjugations;
    }
    public HashSet<String> getSeenForms()
    {
        return seenForms;
    }
    public ArrayList<DefTag> getConjugationTags()
    {
        return conjugationTags;
    }
    boolean hasSeenForm(String test)
    {
        return seenForms.contains(test);
    }

    /**
     * Checks if a definition is valid for this word, assuming this matches one of the spellings
     * @param def the definition to check
     * @return true if this is a valid definition, false otherwise
     */
    public boolean defMatches(Definition def)
    {

        if(def.getTags() == null && getNeededTags().isEmpty()) return true;//still accept if no tags needed
        else if (def.getTags() == null) return false;//does not have needed tags

        for(DefTag needed:getNeededTags())
        {
            if(needed.toString().equals(""))
                return false;
            if(!def.getTags().contains(needed))
                return false;
        }
        return true;
    }

    public String getProcess()
    {
        return process;
    }

    @Override
    public String toString()
    {
        // hide non-freestanding weak forms and remove parens from freestanding ones
        String temp = process;
        if(temp.startsWith("("))
        {
            temp = temp.replaceFirst("[(]", "");
            temp = temp.replaceFirst("[)]", "");
        }
        temp = temp.replaceAll("[(].*?[)]", "");
        return word + "―" + temp;
    }
}
