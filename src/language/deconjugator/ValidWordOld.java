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

/**
 * Holds a possible valid conjugation if a word exists with the valid tags
 * @author Laurens Weyn
 */
public class ValidWordOld extends AbstractWord
{
    private Set<DefTag> impliedTags;

    public ValidWordOld(String word, Set<DefTag> neededTags, Set<DefTag> impliedTags, String process)
    {
        super(word, word, neededTags, process);
        this.impliedTags = impliedTags;
    }
    public ValidWordOld(String word, String process)
    {
        super(word, process);
        this.impliedTags = new HashSet<>();
    }

    public Set<DefTag> getImpliedTags()
    {
        return impliedTags;
    }

    public boolean defMatches(Definition def)
    {
        if(def.getTags() == null && getNeededTags().isEmpty())return true;//still accept if no tags needed
        else if (def.getTags() == null)return false;//does not have needed tags

        for(DefTag needed:getNeededTags())
        {
            if(!def.getTags().contains(needed) &&!getImpliedTags().contains(needed))
            {
                return false;
            }
        }
        return true;
    }
}
