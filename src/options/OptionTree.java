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

import options.page.Page;
import options.page.PageGroup;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;

/**
 * The TreeModel used for the options window's option page selection
 * @author Laurens Weyn
 */
public class OptionTree implements TreeModel
{
    private final Page root;

    public OptionTree(Page root)
    {
        this.root = root;
    }
    
    @Override
    public Object getRoot()
    {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        if(parent instanceof PageGroup)
        {
            PageGroup pg = (PageGroup)parent;
            return pg.getPages().get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent)
    {
        if(parent instanceof PageGroup)
        {
            PageGroup pg = (PageGroup)parent;
            return pg.getPages().size();
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node)
    {
        return !(node instanceof PageGroup);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        if(parent instanceof PageGroup)
        {
            PageGroup pg = (PageGroup)parent;
            return pg.getPages().indexOf(child);
        }
        return 0;
    }
    
    private ArrayList<TreeModelListener> listeners = new ArrayList<>();

    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
        listeners.remove(l);
    }

    
}
