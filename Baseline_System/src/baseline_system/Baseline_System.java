/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baseline_system;

/**
 *
 * @author Praveen
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


//import com.sun.org.apache.xalan.internal.xsltc.compiler.Pattern;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.util.CoreMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import org.tartarus.martin.*;
/**
 *
 * @author PRAVEEN
 */

/*

 Porter stemmer in Java. The original paper is in

 Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
 no. 3, pp 130-137,

 See also http://www.tartarus.org/~martin/PorterStemmer

 History:

 Release 1

 Bug 1 (reported by Gonzalo Parra 16/10/99) fixed as marked below.
 The words 'aed', 'eed', 'oed' leave k at 'a' for step 3, and b[k-1]
 is then out outside the bounds of b.

 Release 2

 Similarly,

 Bug 2 (reported by Steve Dyrdahl 22/2/00) fixed as marked below.
 'ion' by itself leaves j = -1 in the test for 'ion' in step 5, and
 b[j] is then outside the bounds of b.

 Release 3

 Considerably revised 4/9/00 in the light of many helpful suggestions
 from Brian Goetz of Quiotix Corporation (brian@quiotix.com).

 Release 4

 */
import java.io.*;
//import static postagger.POSTagger.producerWords;

/**
 * Stemmer, implementing the Porter Stemming Algorithm
 *
 * The Stemmer class transforms a word into its root form. The input word can be
 * provided a character at time (by calling add()), or at once by calling one of
 * the various stem(something) methods.
 */
class Stemmer {

    private char[] b;
    private int i, /* offset into b */
            i_end, /* offset to end of stemmed word */
            j, k;
    private static final int INC = 50;
    /* unit of size whereby b is increased */

    public Stemmer() {
        b = new char[INC];
        i = 0;
        i_end = 0;
    }

    /**
     * Add a character to the word being stemmed. When you are finished adding
     * characters, you can call stem(void) to stem the word.
     */
    public void add(char ch) {
        if (i == b.length) {
            char[] new_b = new char[i + INC];
            for (int c = 0; c < i; c++) {
                new_b[c] = b[c];
            }
            b = new_b;
        }
        b[i++] = ch;
    }

    /**
     * Adds wLen characters to the word being stemmed contained in a portion of
     * a char[] array. This is like repeated calls of add(char ch), but faster.
     */
    public void add(char[] w, int wLen) {
        if (i + wLen >= b.length) {
            char[] new_b = new char[i + wLen + INC];
            for (int c = 0; c < i; c++) {
                new_b[c] = b[c];
            }
            b = new_b;
        }
        for (int c = 0; c < wLen; c++) {
            b[i++] = w[c];
        }
    }

    /**
     * After a word has been stemmed, it can be retrieved by toString(), or a
     * reference to the internal buffer can be retrieved by getResultBuffer and
     * getResultLength (which is generally more efficient.)
     */
    public String toString() {
        return new String(b, 0, i_end);
    }

    /**
     * Returns the length of the word resulting from the stemming process.
     */
    public int getResultLength() {
        return i_end;
    }

    /**
     * Returns a reference to a character buffer containing the results of the
     * stemming process. You also need to consult getResultLength() to determine
     * the length of the result.
     */
    public char[] getResultBuffer() {
        return b;
    }

    /* cons(i) is true <=> b[i] is a consonant. */
    private final boolean cons(int i) {
        switch (b[i]) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return false;
            case 'y':
                return (i == 0) ? true : !cons(i - 1);
            default:
                return true;
        }
    }

    /* m() measures the number of consonant sequences between 0 and j. if c is
     a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
     presence,

     <c><v>       gives 0
     <c>vc<v>     gives 1
     <c>vcvc<v>   gives 2
     <c>vcvcvc<v> gives 3
     ....
     */
    private final int m() {
        int n = 0;
        int i = 0;
        while (true) {
            if (i > j) {
                return n;
            }
            if (!cons(i)) {
                break;
            }
            i++;
        }
        i++;
        while (true) {
            while (true) {
                if (i > j) {
                    return n;
                }
                if (cons(i)) {
                    break;
                }
                i++;
            }
            i++;
            n++;
            while (true) {
                if (i > j) {
                    return n;
                }
                if (!cons(i)) {
                    break;
                }
                i++;
            }
            i++;
        }
    }

    /* vowelinstem() is true <=> 0,...j contains a vowel */
    private final boolean vowelinstem() {
        int i;
        for (i = 0; i <= j; i++) {
            if (!cons(i)) {
                return true;
            }
        }
        return false;
    }

    /* doublec(j) is true <=> j,(j-1) contain a double consonant. */
    private final boolean doublec(int j) {
        if (j < 1) {
            return false;
        }
        if (b[j] != b[j - 1]) {
            return false;
        }
        return cons(j);
    }

    /* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
     and also if the second c is not w,x or y. this is used when trying to
     restore an e at the end of a short word. e.g.

     cav(e), lov(e), hop(e), crim(e), but
     snow, box, tray.

     */
    private final boolean cvc(int i) {
        if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) {
            return false;
        }
        {
            int ch = b[i];
            if (ch == 'w' || ch == 'x' || ch == 'y') {
                return false;
            }
        }
        return true;
    }

    private final boolean ends(String s) {
        int l = s.length();
        int o = k - l + 1;
        if (o < 0) {
            return false;
        }
        for (int i = 0; i < l; i++) {
            if (b[o + i] != s.charAt(i)) {
                return false;
            }
        }
        j = k - l;
        return true;
    }

    /* setto(s) sets (j+1),...k to the characters in the string s, readjusting
     k. */
    private final void setto(String s) {
        int l = s.length();
        int o = j + 1;
        for (int i = 0; i < l; i++) {
            b[o + i] = s.charAt(i);
        }
        k = j + l;
    }

    /* r(s) is used further down. */
    private final void r(String s) {
        if (m() > 0) {
            setto(s);
        }
    }

    /* step1() gets rid of plurals and -ed or -ing. e.g.

     caresses  ->  caress
     ponies    ->  poni
     ties      ->  ti
     caress    ->  caress
     cats      ->  cat

     feed      ->  feed
     agreed    ->  agree
     disabled  ->  disable

     matting   ->  mat
     mating    ->  mate
     meeting   ->  meet
     milling   ->  mill
     messing   ->  mess

     meetings  ->  meet

     */
    private final void step1() {
        if (b[k] == 's') {
            if (ends("sses")) {
                k -= 2;
            } else if (ends("ies")) {
                setto("i");
            } else if (b[k - 1] != 's') {
                k--;
            }
        }
        if (ends("eed")) {
            if (m() > 0) {
                k--;
            }
        } else if ((ends("ed") || ends("ing")) && vowelinstem()) {
            k = j;
            if (ends("at")) {
                setto("ate");
            } else if (ends("bl")) {
                setto("ble");
            } else if (ends("iz")) {
                setto("ize");
            } else if (doublec(k)) {
                k--;
                {
                    int ch = b[k];
                    if (ch == 'l' || ch == 's' || ch == 'z') {
                        k++;
                    }
                }
            } else if (m() == 1 && cvc(k)) {
                setto("e");
            }
        }
    }

    /* step2() turns terminal y to i when there is another vowel in the stem. */
    private final void step2() {
        if (ends("y") && vowelinstem()) {
            b[k] = 'i';
        }
    }

    /* step3() maps double suffices to single ones. so -ization ( = -ize plus
     -ation) maps to -ize etc. note that the string before the suffix must give
     m() > 0. */
    private final void step3() {
        if (k == 0) {
            return;
        } /* For Bug 1 */ switch (b[k - 1]) {
            case 'a':
                if (ends("ational")) {
                    r("ate");
                    break;
                }
                if (ends("tional")) {
                    r("tion");
                    break;
                }
                break;
            case 'c':
                if (ends("enci")) {
                    r("ence");
                    break;
                }
                if (ends("anci")) {
                    r("ance");
                    break;
                }
                break;
            case 'e':
                if (ends("izer")) {
                    r("ize");
                    break;
                }
                break;
            case 'l':
                if (ends("bli")) {
                    r("ble");
                    break;
                }
                if (ends("alli")) {
                    r("al");
                    break;
                }
                if (ends("entli")) {
                    r("ent");
                    break;
                }
                if (ends("eli")) {
                    r("e");
                    break;
                }
                if (ends("ousli")) {
                    r("ous");
                    break;
                }
                break;
            case 'o':
                if (ends("ization")) {
                    r("ize");
                    break;
                }
                if (ends("ation")) {
                    r("ate");
                    break;
                }
                if (ends("ator")) {
                    r("ate");
                    break;
                }
                break;
            case 's':
                if (ends("alism")) {
                    r("al");
                    break;
                }
                if (ends("iveness")) {
                    r("ive");
                    break;
                }
                if (ends("fulness")) {
                    r("ful");
                    break;
                }
                if (ends("ousness")) {
                    r("ous");
                    break;
                }
                break;
            case 't':
                if (ends("aliti")) {
                    r("al");
                    break;
                }
                if (ends("iviti")) {
                    r("ive");
                    break;
                }
                if (ends("biliti")) {
                    r("ble");
                    break;
                }
                break;
            case 'g':
                if (ends("logi")) {
                    r("log");
                    break;
                }
        }
    }

    /* step4() deals with -ic-, -full, -ness etc. similar strategy to step3. */
    private final void step4() {
        switch (b[k]) {
            case 'e':
                if (ends("icate")) {
                    r("ic");
                    break;
                }
                if (ends("ative")) {
                    r("");
                    break;
                }
                if (ends("alize")) {
                    r("al");
                    break;
                }
                break;
            case 'i':
                if (ends("iciti")) {
                    r("ic");
                    break;
                }
                break;
            case 'l':
                if (ends("ical")) {
                    r("ic");
                    break;
                }
                if (ends("ful")) {
                    r("");
                    break;
                }
                break;
            case 's':
                if (ends("ness")) {
                    r("");
                    break;
                }
                break;
        }
    }

    /* step5() takes off -ant, -ence etc., in context <c>vcvc<v>. */
    private final void step5() {
        if (k == 0) {
            return;
        } /* for Bug 1 */ switch (b[k - 1]) {
            case 'a':
                if (ends("al")) {
                    break;
                }
                return;
            case 'c':
                if (ends("ance")) {
                    break;
                }
                if (ends("ence")) {
                    break;
                }
                return;
            //case 'e': if (ends("er")) break; return;
            case 'i':
                if (ends("ic")) {
                    break;
                }
                return;
            case 'l':
                if (ends("able")) {
                    break;
                }
                if (ends("ible")) {
                    break;
                }
                return;
            case 'n':
                if (ends("ant")) {
                    break;
                }
                if (ends("ement")) {
                    break;
                }
                if (ends("ment")) {
                    break;
                }
                /* element etc. not stripped before the m */
                if (ends("ent")) {
                    break;
                }
                return;
            case 'o':
                if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) {
                    break;
                }
                /* j >= 0 fixes Bug 2 */
                if (ends("ou")) {
                    break;
                }
                return;
            /* takes care of -ous */
            case 's':
                if (ends("ism")) {
                    break;
                }
                return;
            case 't':
                if (ends("ate")) {
                    break;
                }
                if (ends("iti")) {
                    break;
                }
                return;
            case 'u':
                if (ends("ous")) {
                    break;
                }
                return;
            case 'v':
                if (ends("ive")) {
                    break;
                }
                return;
            case 'z':
                if (ends("ize")) {
                    break;
                }
                return;
            default:
                return;
        }
        if (m() > 1) {
            k = j;
        }
    }

    /* step6() removes a final -e if m() > 1. */
    private final void step6() {
        j = k;
        if (b[k] == 'e') {
            int a = m();
            if (a > 1 || a == 1 && !cvc(k - 1)) {
                k--;
            }
        }
        if (b[k] == 'l' && doublec(k) && m() > 1) {
            k--;
        }
    }

    /**
     * Stem the word placed into the Stemmer buffer through calls to add().
     * Returns true if the stemming process resulted in a word different from
     * the input. You can retrieve the result with
     * getResultLength()/getResultBuffer() or toString().
     */
    public void stem(String input, boolean isVerb) {
        b = input.toCharArray();
        k = b.length - 1;
        if (k > 1) {
            step1();
            if (isVerb) {
                step2();
                step3();
                step4();
                step5();
            }
        }
        i_end = k + 1;
        i = 0;
    }
    /**
     * Test program for demonstrating the Stemmer. It reads text from a a list
     * of files, stems each word, and writes the result to standard output. Note
     * that the word stemmed is expected to be in lower case: forcing lower case
     * must be done outside the Stemmer class. Usage: Stemmer file-name
     * file-name ...
     */
}

class Factory {
    BackendConnection createBackendConnectionObject() {
        return new BackendConnection();
    }
    Stemmer createStemmerObject() {
        return new Stemmer();
    }
}

enum VerbType {
    producerVerb, consumerVerb, supplementaryVerb;
}

class Verb {
    String mainVerb;
    String supplementaryVerb;
    VerbType type;
}

class Service
{
    String service;
    ArrayList<String> adjectives;
}
class UserQuery
{
    String query;
    ArrayList<String> requirements;
    Verb verb;
}

class BackendConnection {
    private Hashtable<String, VerbType> verbs = new Hashtable<>();
    private FileInputStream fstream;
    private BufferedReader br;
    private ArrayList<String> producerQueries;
    String strLine;
    Hashtable<String, VerbType> getVerbs() throws FileNotFoundException, IOException {
        fstream = new FileInputStream("C:\\Users\\Praveen\\OneDrive\\NLP\\Verbs.txt");
        br = new BufferedReader(new InputStreamReader(fstream));
        strLine = new String();
        while ((strLine = br.readLine()) != null) {
            String splitLine[] = strLine.split(" ");
            VerbType vt;
            if (splitLine[1].equals("p")) {
                vt = VerbType.producerVerb;
            } else if (splitLine[1].equals("c")) {
                vt = VerbType.consumerVerb;
            } else {
                vt = VerbType.supplementaryVerb;
            }
            verbs.put(splitLine[0], vt);
        }
        return verbs;
    }
    ArrayList<String> getProducersQueries() throws FileNotFoundException, IOException
    {
        producerQueries = new ArrayList<>();
        fstream = new FileInputStream("C:\\Users\\Praveen\\OneDrive\\NLP\\ProducerQueries.txt");
        br = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        while ((strLine = br.readLine()) != null) {
            producerQueries.add(strLine);
        }
        return producerQueries;
    }
    
    void writeQueryToFile(VerbType v, String userQuery) {
        try {
            FileWriter fw = null;
            if (v == VerbType.consumerVerb) {
                fw = new FileWriter("C:\\Users\\PRAVEEN\\OneDrive\\NLP\\ConsumerQueries.txt", true);
            } else if (v == VerbType.producerVerb) {
                fw = new FileWriter("C:\\Users\\Praveen\\OneDrive\\NLP\\ProducerQueries.txt", true);
            } else {
                fw = new FileWriter("C:\\Users\\PRAVEEN\\OneDrive\\NLP\\general.txt", true);
            }
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(userQuery);
                bw.newLine();
            }
            System.out.println("Done");
        } 
        catch (IOException e) {
        }
    }
    void writeQueryToFile(UserQuery w) {
        try {
            FileWriter fw = null;
            if (w.verb.type == VerbType.consumerVerb) {
                fw = new FileWriter("C:\\Users\\PRAVEEN\\OneDrive\\NLP\\ConsumerQueries.txt", true);
            } else if (w.verb.type == VerbType.producerVerb) {
                fw = new FileWriter("C:\\Users\\PRAVEEN\\OneDrive\\NLP\\ProducerQueries.txt", true);
            } else {
                fw = new FileWriter("C:\\Users\\PRAVEEN\\OneDrive\\NLP\\general.txt", true);
            }
            String requirements="";
            for(int i=0;i<w.requirements.size();i++)
            {
                requirements += w.requirements.get(i)+",";
            }
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(w.query+";"+w.verb.supplementaryVerb+","+w.verb.mainVerb+";"+requirements);
                bw.newLine();
            }
            System.out.println("Done");
        } 
        catch (IOException e) {
        }
    }
}

class Result {
    String producerQuery;
    int relevanceCount;
    Result(String pw, int rc) {
        producerQuery = pw;
        relevanceCount = rc;
    }
}

public class Baseline_System {
    
    private static Hashtable<String, VerbType> verbs = new Hashtable<>();
    
    static String tagSentence(String input) {
        MaxentTagger tagger = new MaxentTagger("C:\\Users\\Praveen\\OneDrive\\NLP\\stanford-postagger-2015-04-20\\stanford-postagger-2015-04-20\\models\\english-left3words-distsim.tagger");
        String tagged = tagger.tagString(input);
        return tagged;
    }
    static Verb findVerb(String taggedSentence) {
        String splittedTaggedSentence[] = taggedSentence.split(" ");
        System.out.println(splittedTaggedSentence.length);
        String result = new String();
        Verb v = new Verb();
        UserQuery w = new UserQuery();
        Pattern pattern;
        Matcher matcher;
        if (taggedSentence.contains("_VB")) {
            pattern = Pattern.compile(" (.*?)_VB(.*?)_TO (.*?)_VB");
            matcher = pattern.matcher(taggedSentence);
            if (matcher.find()) {
                //v.supplementaryVerb = matcher.group(1);
                result = matcher.group(3);
                v.mainVerb = result;
                if (verbs.containsKey(result)) {
                    if(result.equalsIgnoreCase("have")||result.equalsIgnoreCase("do")) // I have to do car service, I like to have icecream
                    {
                       v.type=VerbType.consumerVerb; 
                    }
                    else
                    {
                        v.type = verbs.get(result);
                    }
                } else {
                    if (verbs.containsKey(matcher.group(1))) {
                        v.supplementaryVerb = matcher.group(1);
                        v.type = verbs.get(matcher.group(1));    //Eg : 1.I need to drive  a car : drive:neutral;need:shows consumer. 2. I offer to drive a car - shows producer
                    } else {
                        v.type = null;
                    }
                }
                return v;
            }
        }
        for (int i = 0; i < splittedTaggedSentence.length; i++) {
            //System.out.println("hello");
            pattern = Pattern.compile("(.*?)_VB");
            matcher = pattern.matcher(splittedTaggedSentence[i]);
            if (matcher.find()) {
                result = matcher.group(1);
                System.out.println("Verb is : " + result);
                v.mainVerb = result;
                if (verbs.containsKey(result)) {          // If the verb is readily available in the list (as producer or consumer) we dont care about modal verbs
                    if(v.mainVerb.equalsIgnoreCase("have")||v.mainVerb.equalsIgnoreCase("do")) // I can do flower boquet, I can have extra luggages
                    {
                       v.type =  VerbType.producerVerb;
                    }
                    else
                    {
                       v.type = verbs.get(result);
                    }
                } else {
                    Stemmer s = new Stemmer();  // if the word is unavailable, stem to get rooword of the verb and chek against the available list
                    s.stem(result, true);
                    String rootWordOfVerb = s.toString();
                    v.mainVerb = rootWordOfVerb;
                    if (verbs.containsKey(rootWordOfVerb)) {
                        v.type = verbs.get(rootWordOfVerb);
                    } else {
                        pattern = Pattern.compile("(.*?)_MD"); // If the word is still unavailable, then check for the modal verb
                        if(i>0)
                        {
                            matcher = pattern.matcher(splittedTaggedSentence[i - 1]);
                        }
                        else
                        {
                            matcher = null;
                        }
                        if (matcher!=null && matcher.find()) {                     // Works on a condition that all producer and consumer words are listed
                            v.supplementaryVerb = matcher.group(1);
                            System.out.println("You are going to provide a service ????");
                            v.type = VerbType.producerVerb;
                        } else {
                            v.type = null;
                        }
                    }
                }
                System.out.println("In MainVerb function " + result);
                if (verbs.get(result) != VerbType.supplementaryVerb) {
                    return v;
                }
            }
        }
        return v;
    }
    
    static ArrayList<String> findRequirements(String taggedSentence) {
        String splittedTaggedSentence[] = taggedSentence.split(" ");
        ArrayList<String> result = new ArrayList<>();
        Stemmer s = new Stemmer();
        Pattern pattern = Pattern.compile("(.*?)_NN");
        Pattern pattern1 = Pattern.compile("[$+-@?!]+(.*?)");
        for (int i = 0; i < splittedTaggedSentence.length; i++) {
            if (splittedTaggedSentence[i].contains("_NN")) {
                System.out.println("NNNNN");
               
                Matcher matcher = pattern.matcher(splittedTaggedSentence[i]);
                if (matcher.find()) {
                     //System.out.println("NNNNN");
                    String tRequirement=matcher.group(1);
                     s.stem(tRequirement, false);
                     tRequirement = s.toString();
                    //System.out.println("Output after the string is " + s.toString());
                     if (i < splittedTaggedSentence.length - 1) {
                        Matcher matcher1 = pattern1.matcher(splittedTaggedSentence[i + 1]);
                        if(matcher1.find())
                        {
                            System.out.println("Daiiiiiiiii");
                            tRequirement = tRequirement+matcher1.group(0);
                        i++;
                        }
                        
                    }
                    result.add(tRequirement);
                    System.out.println(result.size());
                }
            }
        }
        
        return result;
    }

    static ArrayList<Result> giveResult(UserQuery w,BackendConnection bec) throws FileNotFoundException, IOException {
        ArrayList<String> requirements = w.requirements;
        ArrayList<Result> resultQueries;
        ArrayList<String> producerQueries = new ArrayList<>();
        producerQueries = bec.getProducersQueries();
        resultQueries = new ArrayList<>();
        Stemmer s = new Stemmer();
        for (int producerQueryIterator=0;producerQueryIterator<producerQueries.size();producerQueryIterator++) {
                // Print the content on the console
                String splitStrline[] = producerQueries.get(producerQueryIterator).split(";");
                Result r = new Result(splitStrline[0], 0); //splitStrLine[0] will contain the query given by the user. Check file structure
               // String splittedQuery[] = splitStrline[0].split(" "); // separate the words in a query
                String verbs[];
                if (splitStrline.length > 1) {
                verbs = splitStrline[1].split(",");
                }
                String producerServices[]= null;
                if (splitStrline.length > 2) {
                        producerServices=splitStrline[2].split(",");
                }
                Hashtable<String,String> pServices = new Hashtable<>();
                if(producerServices!=null)
                {
                for(String pService:producerServices)
                {
                    pServices.put(pService, pService);
                }
                }
                //System.out.println(splitStrline[0]);
                //System.out.println(splitStrline[1]);
                //System.out.println("Supplementary verb is : "+w.verb.supplementaryVerb);
                
                //for (int j = 0; j < producerServices.length; j++) {
                    for (int i = 0; i < requirements.size(); i++) {
                        /*if(w.verb.supplementaryVerb==null)  // if there is no supplementary verb in consumer statement no need to check for the  main verb match
                        {  //This check is done because "I need to drive to Ooty", here drive is the main verb which is neutral, the verb 'need' makes it a consumer statement.
                            //In such cases we can match it to producer query only if the main verb matches.  
                            String tRequirement =requirements.get(i);
                            if (producerServices[j].equalsIgnoreCase(tRequirement)||producerServices[j].equalsIgnoreCase(tRequirement+"s")||producerServices[j].equalsIgnoreCase(tRequirement+"es")) {
                            r.relevanceCount++;
                            }
                        }
                        else // if there is supplementary verb then mainverb of consumer stament must match with the main verb of producer whistle
                        {
                            if (producerServices[j].equalsIgnoreCase(requirements.get(i)) && verbs[1].equalsIgnoreCase(w.verb.mainVerb)) {
                            r.relevanceCount++;
                            }
                        }
                        */
                        s.stem(requirements.get(i), false);
                        if(pServices.containsKey(s.toString()))
                        {
                           r.relevanceCount++;
                        }
                    }
               // }
                resultQueries.add(r);
            }
        return resultQueries;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {

        Factory f = new Factory();

        //For data retrival
        BackendConnection backendconnectionObject = f.createBackendConnectionObject();
        verbs = backendconnectionObject.getVerbs();

        Scanner sobj = new Scanner(System.in);
        System.out.println("Enter input : ");
        String input = sobj.nextLine();

        //POS Tagging
        String taggedSentence = tagSentence(input);
        System.out.println(taggedSentence);

        //Finding Verb
        UserQuery w = new UserQuery();
        w.query = input;
        Verb verb = findVerb(taggedSentence);
        w.verb = verb;
        ArrayList<String> requirements = findRequirements(taggedSentence);
        w.requirements =requirements;
        System.out.println("The verb is " + verb.mainVerb + " and the type is " + verb.type);
        if (verb.type == null || verb.type==VerbType.supplementaryVerb) {
            //System.out.println("Verb :" + verb.mainVerb + " Supplementary Verb : " + verb.supplementaryVerb);
            System.out.println("Do you want to provide the service or consume the service.. Press c for consuming p for providing service...");
            //byte service = sobj.nextByte();
            char cservice = sobj.next().charAt(0);
            if(cservice == 'c')
            {
                w.verb.type = VerbType.consumerVerb;
            }
            else if(cservice=='p')
            {
                w.verb.type = VerbType.producerVerb;
            }
        }
        // Write whistle to corresponding file
        backendconnectionObject.writeQueryToFile(w);
        ArrayList<Result> result = new ArrayList<>();
        if (w.verb.type == VerbType.consumerVerb) {
            result = giveResult(w,backendconnectionObject);
            Collections.sort(result, new Comparator<Result>() {
        @Override
        public int compare(Result p1, Result p2) {
            return (int) (p2.relevanceCount - p1.relevanceCount); // Ascending
        }
              });
            for (int i = 0; i < result.size(); i++) {
                Result r = result.get(i);
                System.out.println("Producer Query: " + r.producerQuery + " - Relevance count: " + r.relevanceCount);
            }
        }
    }
}
