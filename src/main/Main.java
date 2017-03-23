package main;

import hooker.ClipboardHook;
import hooker.Hook;
import hooker.Log;
import language.Segmenter;
import language.dictionary.Dictionary;
import language.dictionary.EPWINGDefinition;
import language.splitter.WordSplitter;
import multiplayer.MPController;
import options.Known;
import options.Options;
import options.PrefDef;
import ui.UI;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by Laurens on 2/19/2017.
 */
public class Main
{
    public static final String VERSION = "Beta 0.6";

    public static UI ui;
    /**
     * Used for multiplayer. Null if not connected/hosting
     */
    public static MPController mpManager;
    public static Thread mpThread;
    /**
     * The currently displayed line of text
     */
    public static String text = "";
    /**
     * Text line history
     */
    public static Log log;
    /**
     * Source used for new lines of text
     */
    public static Hook hook;
    public static Dictionary dict;
    public static Known known;
    public static PrefDef prefDef;
    /**
     * The currently active configuration
     */
    public static Options options;
    public static WordSplitter splitter;

    public static void main(String[] args)throws Exception
    {
        if(Segmenter.instance == null)
        {
            System.out.println("Segmenter was not initialized. Rebuild Spark Reader with a segmenter as the artefact.");
            return;
        }
        System.out.println(VERSION);
        initLoadingScreen();
        try
        {
            //load in configuration
            options = new Options(Options.SETTINGS_FILE);
            known = new Known(options.getFile("knownWordsPath"));
            prefDef = new PrefDef(options.getFile("preferredDefsPath"));

            hook = new ClipboardHook();//default hook
            log = new Log(50);//new log

            loadDictionaries();
            splitter = new WordSplitter(dict);

        }catch(Exception err)
        {
            JOptionPane.showMessageDialog(null, "Error starting Spark Reader:\n" + err, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        System.out.println("init done");
        UI.runUI();
        System.out.println("UI done");
    }
    private static void loadDictionaries()throws IOException
    {
        EPWINGDefinition.loadBlacklist();

        //TODO display some sort of progress bar during this operation
        dict = new Dictionary(new File(Main.options.getOption("dictionaryPath")));
        System.out.println("loaded " + Dictionary.getLoadedWordCount() + " in total");

    }

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
        //loadScreen.set
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
            }
        });
        loadUpdater.setRepeats(true);
        loadUpdater.start();
    }
}
