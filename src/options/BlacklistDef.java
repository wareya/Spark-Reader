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
package options;

import language.dictionary.Definition;
import language.dictionary.JMDict.Spelling;
import language.splitter.FoundDef;
import main.Main;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

public class BlacklistDef
{
    private TreeMap<Long, ArrayList<String>> table;
    private int dueChanges = 0;
    private int saveThreshold = 10;
    private final File file;
    private HashMap<Long, Definition> blacklistedDefinitions;
    public BlacklistDef(File file)throws IOException
    {
        table = new TreeMap<>();
        blacklistedDefinitions = new HashMap<>();
        this.file = file;
        if(!file.exists())
        {
            System.out.println("WARN: no blacklisted definitions file");
            return;
        }
        
        FileInputStream is = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);
        
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            if(line.split("\t",2).length != 2) continue;
            if(!line.split("\t",2)[1].contains(",")) continue;
            
            Long key = Long.parseLong(line.split("\t", 2)[0]);
            ArrayList<String> defs = new ArrayList<>(Arrays.asList(line.split("\t", 2)[1].split(",")));
            if(defs.size() == 0) continue;
            
            ArrayList<String> real_defs = new ArrayList<>();
            for(String s : defs) if (!s.isEmpty()) real_defs.add(s);
            //for(String s : defs) removeFromDict(key, s);
            
            table.put(key, real_defs);
        }
        br.close();
    }
    public BlacklistDef()
    {
        table = new TreeMap<>();
        file = null;
        System.out.println("Note: using debug constructor for BlacklistDef. This should only happen in regression tests.");
    }
    public void save()throws IOException
    {
        if(file == null) return;
        if(dueChanges == 0)return;//don't bother writing if nothing changed
        Writer fr = new OutputStreamWriter(new FileOutputStream(file, false), Charset.forName("UTF-8"));
        for(Entry<Long, ArrayList<String>> entry : table.entrySet())
        {
            fr.append(entry.getKey().toString()).append("\t");
            for(String s : entry.getValue())
                fr.append(s).append(",");
            fr.append("\n");
        }
        fr.close();
        dueChanges = 0;
    }

    public void setSaveThreshold(int saveThreshold)
    {
        this.saveThreshold = saveThreshold;
    }
    public void debugForceBlacklist(Long id, String spelling)
    {
        System.out.println("Adding " + spelling + " to debug blacklist");
        if(table.containsKey(id))
        {
            System.out.println("BL: adding to additional");
            ArrayList<String> forms = table.get(id);
            forms.add(spelling);
        }
        // adding
        else
        {
            System.out.println("BL: adding new");
            ArrayList<String> forms = new ArrayList<>();
            forms.add(spelling);
            table.put(id, forms);
        }
    }
    public void toggleBlacklist(FoundDef def)
    {
        Long id = def.getDefinition().getID();
        String spelling = def.getFoundForm().getOriginalWord();
        if(table.containsKey(id))
        {
            // removing
            if(table.get(id).contains(spelling))
            {
                //FIXME these print statements seem to contradict the comments
                System.out.println("BL: removing entry");
                ArrayList<String> forms = table.get(id);
                forms.remove(spelling);
            }
            // adding
            else
            {
                System.out.println("BL: adding entry to existing key");
                ArrayList<String> forms = table.get(id);
                forms.add(spelling);
            }
        }
        // adding
        else
        {
            System.out.println("BL: adding new");
            ArrayList<String> forms = new ArrayList<>();
            forms.add(spelling);
            table.put(id, forms);
        }
        System.out.println("BL: size: " + table.size());
        dueChanges++;
        
        if(dueChanges > saveThreshold || !Main.options.getOptionBool("reduceSave"))
        {
            try
            {
                save();
            }catch(IOException e)
            {
                System.out.println("Failed to write changes: " + e);
                //if this fails, we'll try again on the next change
            }
        }
    }

    public boolean isBlacklisted(Long id, String spelling)
    {
        return (table.containsKey(id) && table.get(id).contains(spelling));
    }
}
