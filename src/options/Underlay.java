package options;

import language.FixupOCR;
import language.deconjugator.*;
import language.dictionary.DefTag;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static language.dictionary.DefTag.toTag;

/**
 * Created by wareya on 2017/08/29.
 * 
 * Arbitrary logical "facts" that get used by all sorts of random language processing stuff across Spark Reader.
 * So that people can change them if necessary without recompiling Spark Reader completely.
 * For example, conjugation rules, confusable characters for OCR fixup, etc.
 */
public class Underlay
{
    public static ArrayList<String> fixupRelationsList = new ArrayList<>();
    public static ArrayList<DeconRule> underlayDeconRules = new ArrayList<>();
    public static ArrayList<DeconRule> underlayOldDeconRules = new ArrayList<>();
    public static HashSet<String> badSegments = new HashSet<>();
    
    static public class HeuristicRule
    {
        public int index;
        public String trait;
        public String operation;
        public String argument; 
    }
    static public class Heuristic
    {
        public String type;
        public ArrayList<HeuristicRule> rules;
    }
    
    public static ArrayList<Heuristic> heuristics = new ArrayList<>();
    
    public static void load(File file) throws IOException
    {
        if(!file.exists())
        {
            System.out.println("WARN: no underlay file " + file.getAbsolutePath());
            return;
        }
        
        FileInputStream is = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);
        String mode = "";
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            if(line.equals("")) continue;
            if(line.equals("fix up ocr:")
            || line.equals("badsegments:")
            || line.equals("heuristics:")
            || line.equals("deconjugation:")
            || line.equals("olddeconjugation:"))
                mode = line;
            else
            {
                if(mode.equals("fix up ocr:"))
                {
                    fixupRelationsList.add(line);
                }
                else if(mode.equals("badsegments:"))
                {
                    badSegments.add(line);
                }
                else if(mode.equals("heuristics:"))
                {
                    String[] parts = line.split(" ", 2);
                    Heuristic heuristic = new Heuristic();
                    heuristic.type = parts[0];
                    heuristic.rules = new ArrayList<>();
                    parts = parts[1].split(" ");
                    for(String part : parts)
                    {
                        String[] fields = part.split(":");
                        HeuristicRule rule = new HeuristicRule();
                        rule.index = Integer.parseInt(fields[0]);
                        rule.trait = fields[1];
                        if(fields.length > 2)
                            rule.operation = fields[2];
                        if(fields.length > 3)
                            rule.argument = fields[3];
                        heuristic.rules.add(rule);
                    }
                    heuristics.add(heuristic);
                }
                else if(mode.equals("deconjugation:") || mode.equals("olddeconjugation:"))
                {
                    ArrayList<DeconRule> targetRuleList = mode.equals("olddeconjugation:")?underlayOldDeconRules:underlayDeconRules;
                    line = line.replaceFirst("//.*", "");
                    
                    String[] parts = line.split("\t");
                    if(parts.length <= 1) continue;
                    
                    if(parts[0].equals("DecensorRule"))
                    {
                        targetRuleList.add(new DecensorRule(parts[1].charAt(0)));
                        continue;
                    }
                    
                    if(parts.length <= 3)
                    {
                        System.out.println("WARN: Weird skipped entry in deconjugation underlay.");
                        System.out.println(line);
                        continue;
                    }
                    
                    if(parts[0].equals("RewriteRule"))
                        targetRuleList.add(new RewriteRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5])));
                        
                    if(parts[0].equals("StdRule"))
                    {
                        if(parts.length >= 6)
                            targetRuleList.add(new StdRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5])));
                        else if(parts.length == 5)
                            targetRuleList.add(new StdRule(parts[1], parts[2], parts[3], toTag(parts[4])));
                    }
                    if(parts[0].equals("OnlyFinalRule"))
                        targetRuleList.add(new OnlyFinalRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5])));
                    if(parts[0].equals("NeverFinalRule"))
                        targetRuleList.add(new NeverFinalRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5])));
                        
                    if(parts[0].equals("FuriStdRule"))
                        targetRuleList.add(new FuriStdRule(parts[1], parts[2], parts[3], parts[4], parts[5], toTag(parts[6]), toTag(parts[7])));
                    if(parts[0].equals("FuriNeverFinalRule"))
                        targetRuleList.add(new FuriNeverFinalRule(parts[1], parts[2], parts[3], parts[4], parts[5], toTag(parts[6]), toTag(parts[7])));
                    
                    if(parts[0].equals("ContextRuleAdjSpecial"))
                        targetRuleList.add(new ContextRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5]), (rule, word) -> {
                            if(word.getConjugationTags().size() < 2) return true;
                            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
                            //noinspection RedundantIfStatement
                            if(tagOfInterest == DefTag.stem_adj_base)
                                return false;
                            return true;
                        }));
                    
                    if(parts[0].equals("ContextRuleTeTrapSpecial"))
                        targetRuleList.add(new ContextRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5]), (rule, word) -> {
                            if(word.getConjugationTags().size() < 1) return true;
                            DefTag tagOfInterest = word.getConjugationTags().get(0);
                            //noinspection RedundantIfStatement
                            if(tagOfInterest == DefTag.stem_ren
                            || tagOfInterest == DefTag.stem_ren_less)
                                return false;
                            return true;
                        }));
                    
                    if(parts[0].equals("ContextRuleSaSpecial"))
                        targetRuleList.add(new ContextRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5]), (rule, word) -> {
                            if(word.getWord().equals("")) return false;
                            if(!word.getWord().endsWith(rule.ending)) return false;
                            String base = word.getWord().substring(0, word.getWord().length() - rule.ending.length());
                            //noinspection RedundantIfStatement
                            if(base.endsWith("さ")) return false;
                            else return true;
                        }));
                    
                    if(parts[0].equals("ContextRuleTeTrap"))
                        targetRuleList.add(new ContextRule(parts[1], parts[2], parts[3], toTag(parts[4]), toTag(parts[5]), (rule, word) -> {
                            if(word.getConjugationTags().size() < 2) return true;
                            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
                            //noinspection RedundantIfStatement
                            if(tagOfInterest == DefTag.stem_te)
                                return false;
                            return true;
                        }));
                    
                    if(parts[0].equals("FuriContextRuleTeTrap"))
                        targetRuleList.add(new FuriContextRule(parts[1], parts[2], parts[3], parts[4], parts[5], toTag(parts[6]), toTag(parts[7]), (rule, word) -> {
                            if(word.getConjugationTags().size() < 2) return true;
                            DefTag tagOfInterest = word.getConjugationTags().get(word.getConjugationTags().size()-2);
                            //noinspection RedundantIfStatement
                            if(tagOfInterest == DefTag.stem_te)
                                return false;
                            return true;
                        }));
                }
            }
        }
        br.close();
        
        FixupOCR.fixupInitRelations();
    }
}
