package main;

import hooker.ClipboardHook;
import hooker.Hook;
import hooker.Log;
import language.segmenter.Segmenter;
import language.deconjugator.WordScanner;
import language.dictionary.Dictionary;
import language.dictionary.EPWINGDefinition;
import language.splitter.WordSplitter;
import multiplayer.MPController;
import options.BlacklistDef;
import options.Known;
import options.Options;
import options.PrefDef;
import options.WantToLearn;
import ui.Page;
import ui.UI;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

/**
 * Main entry point and holds global program objects and methods.
 */
public class Main
{
    public static final String VERSION = "Beta 0.8";
    public static final String ABOUT = "Spark Reader " + VERSION + "\n\n" +
            "Lead developer: Laurens Weyn\n" +
            "Contributions: Alexander Nadeau\n\n";//TODO mention EDICT, libraries, links

    public static UI ui;
    /**
     * Used for multiplayer. Null if not connected/hosting
     */
    public static MPController mpManager;
    public static Thread mpThread;
    /**
     * Text line history
     */
    public static Log log;
    /**
     * The current line of text
     */
    public static Page currPage;
    /**
     * Source used for new lines of text
     */
    public static Hook hook;
    public static Dictionary dict;
    public static Known known;
    public static WantToLearn wantToLearn;
    public static PrefDef prefDef;
    public static BlacklistDef blacklistDef;
    /**
     * The currently active configuration
     */
    public static Options options;
    public static WordSplitter splitter;

    /**
     * Machine readable only persistence data (stats, last window hooked to etc.)
     */
    public static Persist persist;


    public static void main(String[] args)throws Exception
    {
        // If segmenter was not initialized, then this is /actually/ the main class function of the executable, and we need to make our own segmenter instance.
        // (the "heavy" module links in the core module and calls core main directly after making its own instance)
        if(Segmenter.instance == null)
        {
            Segmenter.extended = false;
            Segmenter.instance = new language.segmenter.BasicSegmenter();
        }
        System.out.println(VERSION);
        initLoadingScreen();
        //try
        {
            //load in configuration
            options = new Options(Options.SETTINGS_FILE);
            persist = Persist.load(options.getFile("persistPath"));
            known = new Known(options.getOptionBool("enableKnown")? options.getFile("knownWordsPath"):null);
            wantToLearn = new WantToLearn(known);
            prefDef = new PrefDef(options.getFile("preferredDefsPath"));
            blacklistDef = new BlacklistDef(options.getFile("blacklistDefsPath"));

            hook = new ClipboardHook();//default hook
            log = new Log(50);//new log

            loadDictionaries();
            splitter = new WordSplitter(dict);

        }
        //catch(Exception err)
        //{
        //    JOptionPane.showMessageDialog(null, "Error starting Spark Reader:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
        //    System.exit(1);
        //}
        System.out.println("init done");
        persist.startupCount++;
        UI.runUI();
    }
    private static void loadDictionaries()throws IOException
    {
        EPWINGDefinition.loadBlacklist();

        dict = new Dictionary(new File(Main.options.getOption("dictionaryPath")));
        System.out.println("loaded " + Dictionary.getLoadedWordCount() + " in total");
        WordScanner.init();
    }

    /**
     * A call to this method cleanly exits the program, saving changes if possible.
     */
    public static void exit()
    {
        JFrame frame = getParentFrame();
        try
        {
            if(known != null)Main.known.save();
            if(prefDef != null)Main.prefDef.save();
            if(blacklistDef != null)Main.blacklistDef.save();
            persist.save();
        }catch(IOException err)
        {
            JOptionPane.showMessageDialog(getParentFrame(), "Error while saving changes:\n" + err);
            err.printStackTrace();
        }
        if(frame != null)frame.setVisible(false);
        System.exit(0);
    }

    /**
     * Gets the GUI frame if available.
     * For use in functions that need a parent frame, like {@link JOptionPane}
     */
    public static JFrame getParentFrame()
    {
        //TODO change usages of ui.disp.getFrame() to use this one instead

        if(ui != null && ui.disp != null)return ui.disp.getFrame();
        return null;
    }

    //loading screen-specific variables
    private static JDialog loadScreen;
    private static JProgressBar loadProgress;
    private static JLabel loadStatus;
    private static boolean doneLoading = false;
    private static volatile Timer loadUpdater;

    public static void doneLoading()
    {
        doneLoading = true;
    }

    private static void initLoadingScreen()throws IOException
    {
        loadScreen = new JDialog((JFrame) null, "Starting Spark Reader");
        loadProgress = new JProgressBar(0, 377089);//TODO don't hardcode this value, use last boot as estimate
        loadStatus = new JLabel("Loading dictionaries...");
        JPanel mainPanel = new JPanel(new BorderLayout());
        loadScreen.setContentPane(mainPanel);
        mainPanel.add(loadStatus, BorderLayout.WEST);
        mainPanel.add(new JLabel(VERSION), BorderLayout.EAST);
        mainPanel.add(loadProgress, BorderLayout.SOUTH);
        loadScreen.setSize(300,100);
        Utils.centerWindow(loadScreen);
        loadScreen.setIconImage(ImageIO.read(loadScreen.getClass().getResourceAsStream("/ui/icon.gif")));
        loadScreen.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if(!doneLoading)
                {
                    System.out.println("Startup aborted");
                    exit();
                }
            }
        });
        loadScreen.setVisible(true);

        loadUpdater = new Timer(50, e ->
        {
            if(dict == null)
            {
                //still loading dictionaries
                loadProgress.setValue(Dictionary.getLoadedWordCount());
            }
            else
            {
                loadProgress.setValue(loadProgress.getMaximum());
                loadStatus.setText("Loading main UI...");
            }
            if(doneLoading)
            {
                loadUpdater.stop();
                loadScreen.setVisible(false);
                loadScreen.dispose();
            }
        });
        loadUpdater.setRepeats(true);
        loadUpdater.start();
    }
}
