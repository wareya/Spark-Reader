package ui.input;

import language.splitter.FoundWord;
import main.Main;
import ui.Line;
import ui.UI;
import ui.menubar.Menubar;
import ui.popup.DefPopup;
import ui.popup.WordPopup;

import java.awt.*;
import java.util.Date;
import java.util.Set;

import static main.Main.*;
import static ui.UI.*;

/**
 * Handles keyboard based control of Spark Reader. <br>
 * This class is abstract and requires an implementation to get key events, since there are a few ways of doing this,
 * some of which are platform dependent.
 */
public abstract class MouseHandler
{
    protected UI ui;
    protected Point mousePos;
    protected boolean mouseClear = false;
    protected int mouseLine = -1;
    protected FoundWord mousedWord;

    protected int resizeEdgeSize = 5;
    protected boolean resizeState = false;//true if cursor is <-> icon

    protected double stopTime = 0;
    protected Point stopPoint = null;
    protected FoundWord stopWord = null;

    public MouseHandler(UI ui)
    {
        this.ui= ui;
    }

    public abstract void addListeners();

    public void leftClick()
    {
        if(mousePos != null)leftClick(mousePos);
    }
    public void rightClick()
    {
        if(mousePos != null)rightClick(mousePos);
    }
    public void middleClick()
    {
        if(mousePos != null)middleClick(mousePos);
    }
    
    private void setSelectedWord(Point pos, boolean toggle, boolean unknownonly)
    {
        if(pos.y >= textStartY && pos.y <= textEndY)
        {
            //int charIndex = toCharPos(pos.x);
            int lineIndex = ui.getLineIndex(pos);
            ui.selectedWord = null;//to recalculate
            //TODO move this over to Page functions?

            //reset selection on all unselected lines:
            int i = 0;
            for(Line line:currPage)
            {
                if(i != lineIndex)line.resetSelection();
                i++;
            }
            //toggle on selected line:
            FoundWord word = currPage.getLine(lineIndex).getWordAt(pos.x);
            for(FoundWord word2:currPage.getLine(lineIndex).getWords())
            {
                if(word2 == word)
                {
                    if(toggle)
                        word.toggleWindow();
                    else if(!word.isShowingDef() && !(unknownonly && word.isKnown()))
                        word.toggleWindow();
                    if(word.isShowingDef() && word.getDefinitionCount() > 0)ui.selectedWord = word;
                }
                else
                    word2.showDef(false);
            }
            
            ui.render();
        }
    
    }

    public void leftClick(Point pos)
    {
        stopWord = null;
        //minimise button
        if(pos.y < textStartY && pos.x > minimiseStartX)
        {
            ui.minimise();
            return;
        }

        if(UI.showMenubar)//on menubar
        {
            //TODO avoid triggering this when the user intends to move the window, not click a Menubar item
            ui.menubar.processClick(pos);
            return;
        }
        
        setSelectedWord(pos, true, false);
    }
    public void rightClick(Point pos)
    {
        stopWord = null;
        //settings button
        /*if(pos.y > furiganaStartY && pos.y < textStartY)
        {
            new MenuPopup(ui).display(pos);//no longer requires button; right click anywhere on bar works
        }*/

        //word
        if(pos.y >= textStartY && pos.y <= textEndY)
        {
            WordPopup popup = null;
            int lineIndex = ui.getLineIndex(pos);
            Line line = currPage.getLine(lineIndex);
            FoundWord word = line.getWordAt(pos.x);
            
            if(word != null)
                popup = new WordPopup(line, word, ui);
            
            if(popup != null)
                popup.show(pos.x, pos.y);
        }
        //definition
        else if(options.getOptionBool("defsShowUpwards") ? (pos.y < defStartY):(pos.y > defStartY))
        {
            DefPopup popup = new DefPopup(ui.selectedWord, ui, pos.y);
            popup.show(pos.x, pos.y);
        }
    }
    public void middleClick(Point pos)
    {
        stopWord = null;
        if(pos.y > textStartY && pos.y < textEndY)//place marker
        {
            //int point = toCharPos(pos.x + mainFontSize/2);
            int lineIndex = ui.getLineIndex(pos);
            int point = currPage.getLine(lineIndex).getCharAt(pos.x);
            Set<Integer> markers = currPage.getLine(lineIndex).getMarkers();
            //toggle markers
            if(markers.contains(point))markers.remove(point);
            else
            {
                Main.persist.manualSpacesPlaced++;
                markers.add(point);
            }

            ui.updateText(currPage.getText());//reflow (TODO don't pass text to itself)
            ui.render();//redraw
        }
    }


    public void mouseMove(Point pos)
    {
        if(pos == null) return;
        boolean moved = false;
        if(mousePos != null)
            moved = !mousePos.equals(pos);
        mousePos = pos;//keep track of where the mouse is
        boolean reRender = false;//true if re-render needed

        int lineIndex = ui.getLineIndex(pos);

        if(!UI.tempIgnoreMouseExit)
        {
            if(pos.getY() < UI.textStartY)//over furigana bar
            {
                if(!UI.showMenubar)
                {
                    UI.showMenubar = true;
                    Menubar.ignoreNextClick = null;
                    reRender = true;//render menu instead
                }
            }else//not over furigana bar
            {
                if(UI.showMenubar)
                {
                    UI.showMenubar = false;
                    reRender = true;//render furigana instead
                }
            }
        }
        
        if(lineIndex >= currPage.getLineCount() || lineIndex < 0)//over definition text
        {
            reRender |= clearWordMouseover();//disable any mouseover effects
        }
        else
        {
            FoundWord word = currPage.getLine(lineIndex).getWordAt(mousePos.x);
            if(word != null) mouseClear = false;
            if(lineIndex != mouseLine || (mousedWord != null && mousedWord != word))
            {
                if(mousedWord != null)
                {
                    mousedWord.setMouseover(false);
                    if(mousedWord.updateOnMouse()) reRender = true;
                }
                mousedWord = null;//to recalculate
                
                mousedWord = word;
                mouseLine = lineIndex;
    
                if(mousedWord != null)
                {
                    //System.out.println("mouseover'd word changed to " + mousedWord.getText());
                    mousedWord.setMouseover(true);
                    if(mousedWord.updateOnMouse())reRender = true;
                }

                if(reRender)ui.render();
            }
            
            if(options.getOptionInt("rikaiEmulation") > 0 && moved)
            {
                stopPoint = pos;
                stopTime = new Date().getTime();
                stopWord = mousedWord;
            }
        }
        //TODO could be more efficient, revisit when width is consistent
        boolean newResizeState = pos.getY() >= UI.textStartY && pos.getX() >= options.getOptionInt("windowWidth") - resizeEdgeSize;
        if(newResizeState != resizeState)
        {
            resizeState = newResizeState;
            if(resizeState)
            {
                //show that we can resize
                //uncomment to display resize cursor
                //ui.disp.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            }
            else
            {
                //return to normal
                ui.disp.getFrame().setCursor(Cursor.getDefaultCursor());
            }
        }
        if(reRender)ui.render();
    }
    
    public void think()
    {
        double currtime = new Date().getTime();
        if(options.getOptionInt("rikaiEmulation") > 0 && currtime-stopTime > 200 && mousePos == stopPoint && stopPoint != null && stopWord == mousedWord && stopWord != null)
            setSelectedWord(stopPoint, false, options.getOptionInt("rikaiEmulation") > 1);
    }

    public void mouseExit()
    {
        stopWord = null;
        boolean rerender = false;
        if(UI.showMenubar && !UI.tempIgnoreMouseExit)
        {
            UI.showMenubar = false;
            rerender = true;
        }
        //temporary ignore loose focus
        if(tempIgnoreMouseExit)return;

        //collapse definitions
        if(ui.selectedWord != null && options.getOptionBool("hideDefOnMouseLeave"))
        {
            ui.selectedWord.showDef(false);
            ui.selectedWord = null;
            rerender = true;
        }
        rerender |= clearWordMouseover();

        if(rerender)ui.render();
    }

    public void mouseScroll(int scrollDir)
    {
        stopWord = null;
        if(mousePos == null)return;
        mouseScroll(scrollDir, mousePos);
    }

    public void mouseScroll(int scrollDir, Point pos)
    {
        boolean onTextRange = (pos.y < textEndY && pos.y >= textStartY);

        //scroll up/down definition
        if((options.getOptionBool("defsShowUpwards") ? (pos.y < defStartY):
                (pos.y > defStartY)) && ui.selectedWord != null)
        {
            if(mouseClear) return;
            if(scrollDir > 0)ui.selectedWord.getCurrentDef().scrollDown();
            if(scrollDir < 0)ui.selectedWord.getCurrentDef().scrollUp();
            ui.render();
        }

        //scroll through definitions
        else if(onTextRange && ui.selectedWord != null)
        {
            System.out.println("Trying to scroll through definitions");
            if(mouseClear) return;
            if(ui.selectedWord == currPage.getLine(ui.getLineIndex(pos)).getWordAt(pos.x))
            {
                if(scrollDir > 0)ui.selectedWord.scrollDown();
                if(scrollDir < 0)ui.selectedWord.scrollUp();
            }
            else//not over this word: close definition and scroll text instead
            {
                ui.selectedWord.showDef(false);
                ui.xOffset += scrollDir * -mainFontSize;
                ui.boundXOff();
                ui.selectedWord = null;
            }
            ui.render();
        }
        else if(onTextRange && ui.selectedWord == null)//scroll text
        {
            ui.xOffset += scrollDir * -mainFontSize;
            ui.boundXOff();
            ui.render();
            mouseMove(mousePos);//update highlighted word since text moved (kind of a hack right now)
        }
        else if(pos.y <= textStartY && pos.y > furiganaStartY)//scroll history
        {
            String historyLine;
            if(scrollDir < 0)//scroll up
            {
                historyLine = log.back();
            }
            else
            {
                historyLine = log.forward();
            }
            if(!options.getOptionBool("splitLines"))historyLine = historyLine.replace("\n", "");//all on one line if not splitting
            System.out.println("loading line " + historyLine);
            currPage.clearMarkers();//markers not relevant for this text
            ui.updateText(historyLine);//flow new text
            ui.xOffset = 0;//scroll back to front
            ui.render();//update
        }
    }
    protected boolean clearWordMouseover()
    {
        boolean rerender = false;
        if(mousedWord != null)
        {
            mousedWord.setMouseover(false);
            rerender = mousedWord.updateOnMouse();
            mousedWord = null;
            if(rerender)ui.render();
        }
        mouseLine = -1;
        //mousePos = null;
        mouseClear = true;
        return rerender;
    }
    protected int toCharPos(int x)
    {
        x -= ui.xOffset;
        x /= mainFontSize;
        return x;
    }
}
