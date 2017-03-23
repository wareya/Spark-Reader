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
public abstract class AbstractWord
{
    protected String word, originalWord;
    protected Set<DefTag> neededTags;
    protected String process;

    public AbstractWord(String originalWord, String word, Set<DefTag> neededTags, String process)
    {
        this.originalWord = originalWord;
        this.word = word;
        this.neededTags = neededTags;
        this.process = process.trim();
    }
    public AbstractWord(String word, String process)
    {
        this.originalWord = word;
        this.word = word;
        this.neededTags = new HashSet<>();
        this.process = process;
    }
    public String getOriginalWord()
    {
        return originalWord;
    }
    public String getWord()
    {
        return word;
    }

    public Set<DefTag> getNeededTags()
    {
        return neededTags;
    }

    abstract public boolean defMatches(Definition def);

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
