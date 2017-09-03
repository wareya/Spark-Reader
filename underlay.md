# Underlay formatting

In order to give sensible parsing, Spark Reader has to do non-trivial language processing. Up until now, most of that language processing has used hardcoded rules. With the "underlay" (the data that lies under the program's language processing logic) now part of a file, you can modify this without knowing how to modify Spark Reader itself. However, it still has a syntax of its own, and Spark Reader will behave badly if the underlay is not formatted correctly.

The underlay is split into sections. Each section has its own special syntax. These are the sections that are implemented right now:

    fix up ocr:
    deconjugation:
    badsegments:
    heuristics:

To start a section, the line must consist of exactly the section name, including the colon at the end, with no leading/trailing whitespace or tabs.

Here's a stripped down example underlay:
    
    fix up ocr:
    やゃ
    ゆゅ
    よょ
    ー―一|]-
    間聞問閤
      
    deconjugation:
    //Decensor
    DecensorRule	ん
    DecensorRule	っ
    
    ContextRuleAdjSpecial	たい		want	stem_ren	adj_i
    
    StdRule	すぎる		too much	stem_ren	v1
    
    StdRule	ください		polite request	stem_te	adj_i
    badsegments:
    すっ
    たろ
    てん
    heuristics:
    strong 0:pos2:has:副助詞 1:pos1:has:動詞 1:pos2:has:非自立可能
    strong 0:pos2:has:終助詞 1:pos1:has:助詞 1:pos2:has:格助詞
    #strong 0:surface:is:は 0:pos2:is:係助詞 1:surface:is:いら 1:pos1:is:動詞
    weak 0:pos1:is:動詞 0:surface:is:し 1:pos1:is:助動詞
    bipolar 0:surface:is:と 0:pos2:is:格助詞 1:lemma:is:か 1:pos2:is:副助詞
    
Explanations of each section and its syntax follow.

##fix up ocr:

Each line is a list of confusable characters. OCR correction will attempt to replace each character in one of these lines with another character from that same line and see if the replacement causes a better parse. You can guide this process by adding manual splits by middle clicking before correcting OCR.

This is a naiive approach and limits itself to replacing one character at a time, so, for example, びつたり will never correct to ぴったり.

There is a special case rule consisting of two spaces on a line. This is the last line in the example above. Having this rule in the underlay file enables deleting spaces when fixing up OCR. Some OCR systems introduce bogus spaces to the text, this "fixes" that by removing spaces entirely. If you don't want spaces removed, remove this rule.

##deconjugation:

Spark Reader uses a recursive rule-based deconjugator, starting at the right edge of a word, and trying to shorten the word into a dictionary word. It does this through brute force, seeing if it hits a dictionary word while shortening the word.

It does this with awareness of the rules of the deconjugations it's stringing together, so it won't try to deconjugate a word into an adverb, then try to deconjugate that adverb as if it were a verb. 

There are several deconjugation rule types. Every rule type is used in the default underlay.

Badly constructed rules can cause Spark Reader to give bad parses, so be careful.

##badsegments:

The Kuromoji segmentation backend automatically splits up segments kuromoji spits out into single characters if they're known to be problematic for further parsing. This is a list of such known segments.

##heuristics:

The Kuromoji segmentation backend tags and filters the token list before feeding it to the word splitter. This tagging and filtering helps the word splitter avoid misinterpreting short sections of kana, like treating 猫がいる as 猫　がい　る instead of 猫　が　いる.

"Strong" segments have to be treated as a single unit if they're the start of a word, unless the word splitter can't find a definition for them at all.

"Weak" segments are automatically combined with the next one and marked as non-strong, no matter what.

"Bipolar" segments are combined with the next one and marked as strong.

The heuristics syntax is almost self-explanatory, but you need to know how Kuromoji is outputting the particular sequence you need heuristics for. For this, use [this mecab demo](http://www.edrdg.org/~jwb/mecabdemo.html), which uses the same parsing dictionary as the Kuromoji version that Spark Reader uses.

You also need to know how the heuristics operations work.

A heuristic is a list of rules. A rule is a colon-separated list of fields. A heuristic is triggered when all rules test as true.

The first field selects the index of the segment to test, where 0 is the current segment, 1 is the next later segment, etc.

The second field is the basis of the heuristic. Every basis, except a certain one, is just "check this field".

[This is a link to a list of basis names](https://github.com/wareya/Spark-Reader/blob/ef050cd540ac3853a9e1c297f11179a3b537f7aa/heavy/src/Heavy.java#L194)

The third field is the operation to run on "check this field" basises. The four operations are "is", "not", "has", and "starts". These are equality, inequality, finding anywhere ("contains"), and finding at the start ("starts with"). 

The fourth field is the second argument of that operation.


##But why?

Parsing is fundamentally bad for language learning, but it allows Spark Reader to do several useful things that can't be done without parsing, like:

* Tracking known words, marking known words so you don't look them up absentmindedly
* Making it more reasonable for absolute beginners to understand material that is very structurally/grammatically complex
* Language processing like fixing up OCR
* Making typos in the original Japanese text more obvious

Normally, parsing is very dangerous because it's generally inaccurate and is an unnecessary crutch at a certain point, but the parsing in Spark Reader aims to be as useful and flexible as possible.

* Spark Reader allows you to correct the automatic segmentation when it's wrong (by middle clicking)
* Spark Reader uses as many tricks as possible to make the automatic segmentation more accurate than any naiive parser (like pure mecab/kuromoji or jparser alone), without being too slow to use in real time
* Spark Reader allows those tricks to be modified, disabled, or improved, through the options or through the underlay file

If this isn't enough, Spark Reader might get a way to detect problematic phrases as a whole and fix them, in ways that the current tricks, which don't care about the phrase, can't.