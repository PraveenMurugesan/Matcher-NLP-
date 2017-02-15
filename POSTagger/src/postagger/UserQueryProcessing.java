package postagger;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//It includes all functions to process the user query
class UserQueryProcessing {

    private Factory f;
   // private Hashtable<String, VerbType> verbs;
    private Stemmer s;

    UserQueryProcessing(Factory f) throws FileNotFoundException, IOException {
        this.f = f;
        s = f.createStemmerObject();
        //verbs = bec.getVerbs();
    }
    
    // Function performing POS Tagging
    String tagSentence(String input) {
        //"C:\\Users\\Praveen\\OneDrive\\NLP\\stanford-postagger-2015-04-20\\stanford-postagger-2015-04-20\\models\\english-left3words-distsim.tagger"
        System.out.println("Enter tagger source file name");
        Scanner sObj = new Scanner(System.in);
        //String taggerSource=sObj.nextLine();
        String taggerSource = "C:\\Users\\Praveen\\OneDrive\\NLP\\stanford-postagger-2015-04-20\\stanford-postagger-2015-04-20\\models\\english-left3words-distsim.tagger";
        MaxentTagger tagger = new MaxentTagger(taggerSource);
        String tagged = tagger.tagString(input);
        return tagged;
    }

    // It find the main verb of te user query to classify the query as either producer or consumer 
    Verb findVerb(String taggedSentence,BackendConnection bec) throws IOException {
        Hashtable<String, VerbType> verbs = bec.getVerbs();
        String splittedTaggedSentence[] = taggedSentence.split(" ");
       // System.out.println(splittedTaggedSentence.length);
        String result = new String();
        Verb v = f.createVerbObject();
        Pattern pattern;
        Matcher matcher;
        if (taggedSentence.contains("_VB")) {
            pattern = Pattern.compile(" (.*?)_VB(.*?)_TO (.*?)_VB");
            matcher = pattern.matcher(taggedSentence);
            if (matcher.find()) {
                //v.supplementaryVerb = matcher.group(1);
                result = matcher.group(3);
                result = result.toLowerCase(); /////--------------------Doubt 
                v.setMainVerb(result);
                if (verbs.containsKey(result)) {
                    if (result.equalsIgnoreCase("have") || result.equalsIgnoreCase("do")) // I have to do car service, I like to have icecream
                    {
                        v.setVerbType(VerbType.consumerVerb);
                    } else {
                        v.setVerbType(verbs.get(result));
                    }
                } else {
                    if (verbs.containsKey(matcher.group(1))) {
                        v.setSupplementaryVerb(matcher.group(1));
                         v.setVerbType(verbs.get(matcher.group(1)));//Eg : 1.I need to drive  a car : drive:neutral;need:shows consumer. 2. I offer to drive a car - shows producer
                       
                    } else {
                        v.setVerbType(null);
                    }
                }
                return v;
            }
        }
        for (int i = 0; i < splittedTaggedSentence.length; i++) {
            pattern = Pattern.compile("(.*?)_VB");
            matcher = pattern.matcher(splittedTaggedSentence[i]);
            if (matcher.find()) {
                result = matcher.group(1);
                result = result.toLowerCase();
                //System.out.println("Verb is : " + result);
                v.setMainVerb(result);
                if (verbs.containsKey(result)) {          // If the verb is readily available in the list (as producer or consumer) we dont care about modal verbs
                    if (v.getMainVerb().equalsIgnoreCase("have") || v.getMainVerb().equalsIgnoreCase("do")) // I can do flower boquet, I can have extra luggages
                    {
                        v.setVerbType(VerbType.producerVerb);
                    } else {
                        v.setVerbType(verbs.get(result));
                    }
                } else {
                    s.stem(result.toLowerCase(), true);
                    String rootWordOfVerb = s.toString();
                    v.setMainVerb(rootWordOfVerb);
                    if (verbs.containsKey(rootWordOfVerb)) {
                        v.setVerbType(verbs.get(rootWordOfVerb));
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
                        if (matcher !=null && matcher.find()) {                     // Works on a condition that all producer and consumer words are listed
                            v.setSupplementaryVerb(matcher.group(1));
                            //System.out.println("You are going to provide a service ????");
                            v.setVerbType(VerbType.producerVerb); 
                        } else {
                            v.setVerbType(null); 
                        }
                    }
                }
               // System.out.println("In MainVerb function " + result);
                if (verbs.get(result) != VerbType.supplementaryVerb) {
                    return v;
                }
            }
        }
        return v;
    }
    boolean IsQueryAQuestion(String taggedSentence)
    {
        String splittedTaggedSentence[]=taggedSentence.split(" ");
        boolean isQuestion=false;
        if (splittedTaggedSentence.length > 0) {
            Pattern verbPattern = Pattern.compile("_VB.?");
            Matcher verbMatcher = verbPattern.matcher(splittedTaggedSentence[0]);
            Pattern modalPattern = Pattern.compile("_MD");
            Matcher modalMatcher = modalPattern.matcher(splittedTaggedSentence[0]);
            Pattern WHpattern = Pattern.compile("_WP"); // modified WH with WP for question tags...
            Matcher WHMatcher = WHpattern.matcher(splittedTaggedSentence[0]);
            if (verbMatcher.find()||modalMatcher.find()) {
               if(splittedTaggedSentence.length>1)
               {
                   String nextWord=splittedTaggedSentence[1];
                   if(nextWord.contains("_PRP")||nextWord.contains("anyone")||nextWord.contains("_EX"))
                   {
                       isQuestion=true;
                   }
               }
            }
            else if(WHMatcher.find())
            {
                isQuestion=true;
            }
            else if(splittedTaggedSentence[0].toLowerCase().contains("anyone")||splittedTaggedSentence[0].toLowerCase().contains("someone")) // Questions may begin with someone or anyone....
            {
                isQuestion=true;
            }
        }
        return isQuestion;
    }
    //Used to find the nouns (services) that will be offered or required
    Service findService(String taggedSentence) {
        String splittedTaggedSentence[] = taggedSentence.split(" ");
        Service service = f.createServiceObject();
        Pattern pattern = Pattern.compile("(.*?)_NN");
        Pattern pattern1 = Pattern.compile("[+]+"); // This is especially done to handle blood groups. The Stanford NLP which is used here tags the special character as a separate one
        // In order to keep O+ as O+, i have made this hacky way. Please do suggest me a better way of doing it.
        Pattern pattern2 = Pattern.compile("(.*?)_JJ");
        Pattern commonNounPattern =Pattern.compile("((A|a)ny|(S|s)ome)(one)?");
        for (int i = 0; i < splittedTaggedSentence.length; i++) {
            Matcher commonNounMatcher = commonNounPattern.matcher(splittedTaggedSentence[i]);
            if (splittedTaggedSentence[i].contains("_NN")&&!commonNounMatcher.find()) {
                Matcher matcher = pattern.matcher(splittedTaggedSentence[i]);
                if (matcher.find()) {
                    String tRequirement = matcher.group(1);
                    s.stem(tRequirement, false);
                    tRequirement = s.toString();
                    if (i < splittedTaggedSentence.length - 1) {
                        Matcher matcher1 = pattern1.matcher(splittedTaggedSentence[i + 1]);
                        if (matcher1.find()) {
                            //System.out.println("Daiiiiiiiii");
                            tRequirement = tRequirement + matcher1.group(0);
                            i++;
                        }
                    }
                    service.getServices().add(tRequirement);
                }
            } else if (splittedTaggedSentence[i].contains("_JJ") || splittedTaggedSentence[i].contains("_CD")) { //Both adjectives and cardinal numbers are taken as adjectives. Eg: I need 3 screw drivers.
                Matcher matcher = pattern2.matcher(splittedTaggedSentence[i]);
                if (matcher.find()) {
                    service.getAdjectives().add(matcher.group(1));
                }
            }
        }
        return service;
    }
    //Retrives the producer whistles and matches with the consumer query and returns the result
    ArrayList<Result> giveResult(UserQuery w, BackendConnection bec) throws FileNotFoundException, IOException {
        ArrayList<String> requirements = w.getService().getServices();
        ArrayList<String> adjectives = w.getService().getAdjectives();
        ArrayList<Result> resultQueries;
        ArrayList<String> producerQueries = new ArrayList<>();

        ArrayList<RelatedWords> requirementsRelatedWords = new ArrayList<>();
        producerQueries = bec.getProducersQueries();
        resultQueries = new ArrayList<>();
        String compositeRequirement = "";
        //System.out.println("The requirements Size is : " + requirements.size());
        for (int i = 0; i < requirements.size(); i++) {
            s.stem(requirements.get(i), false);
            //System.out.println("The word searched is " + s.toString());
            compositeRequirement = compositeRequirement + s.toString() + " ";
            findRelatedWords(s.toString(), requirementsRelatedWords);
        }
        if (requirements.size() > 1) //To find the related words of composite requirement only if the requirement is composite. Otherwise it will repetition
        {
            findRelatedWords(compositeRequirement, requirementsRelatedWords);
        }
        for (int producerQueryIterator = 0; producerQueryIterator < producerQueries.size(); producerQueryIterator++) {
            // Print the content on the console
            String splitStrline[] = producerQueries.get(producerQueryIterator).split(";");
            Result r = f.createResultObject(splitStrline[0]); //splitStrLine[0] will contain the query given by the user. Check file structure
            // String splittedQuery[] = splitStrline[0].split(" "); // separate the words in a query
            String verbs[];
            if (splitStrline.length > 1) {
                verbs = splitStrline[1].split(",");
            }
            String producerServices[] = null;
            Hashtable<String, String> pServices = new Hashtable<>();
            String compositeService = "";
            if (splitStrline.length > 2) {
                if (splitStrline[2].contains(",")) {
                    producerServices = splitStrline[2].split(",");

                    for (String pService : producerServices) {
                        compositeService = compositeService + pService + "";
                        pServices.put(pService.toLowerCase(), pService.toLowerCase());
                    }
                }
            }
            Hashtable<String, String> pAdjectives = new Hashtable<String, String>();
            if (splitStrline.length > 3) {
                String producerAdjectives[] = splitStrline[3].split(",");

                for (String pAdjective : producerAdjectives) {
                    pAdjectives.put(pAdjective.toLowerCase(), pAdjective.toLowerCase());
                }
            }
            if (producerServices != null) {
                for(String pService:pServices.keySet()) // Check fo rht epresence of NN in the sentence and get (0)
                   {
                    if(requirementsRelatedWords.size()>0)
                    {
                        if(getWordSimilarityBetweenRequirementAndRelatedWord(pService,requirementsRelatedWords.get(0).getWord())>0.8)
                        {
                            r.incrementRelevanceCount(1);
                        }
                    } 
                   }
                for (RelatedWords rw : requirementsRelatedWords) {
                    if (pServices.containsKey(rw.getWord().toLowerCase())) {
                        r.incrementRelevanceCount(1);
                    }
//                    for (String s1 : rw.getSynsetDefinitions()) {
//                        if (s1.contains(" " + compositeService + " ")) {
//                            r.incrementRelevanceCount(1);
//                        }
//                    }
//                    
                }
            //Necessary  Adjective  - To be used later
            /*if (pAdjectives.size() > 0) {
                 for (int i = 0; i < adjectives.size(); i++) {
                 if (pAdjectives.containsKey(adjectives.get(i).toLowerCase())) {
                 r.incrementRelevanceCount(1/(float)adjectives.size());
                 }
                 }
                 }*/
                resultQueries.add(r);
            }
        }
        return resultQueries;
    }
    
    double getWordSimilarityBetweenRequirementAndRelatedWord(String requirement, String relatedWord) {
        double similarity = 0;

        URL url;

        try {
            String tURL = "http://ws4jdemo.appspot.com/ws4j?measure=wup&args=" + URLEncoder.encode(requirement + "#::" + relatedWord, "UTF-8");
            url = new URL(tURL);           
            URLConnection conn = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine;
            if ((inputLine = br.readLine()) != null) {
                Pattern pattern = Pattern.compile("(.*)\"score\":\"(.*)\",");
                Matcher matcher = pattern.matcher(inputLine);
                //System.out.println();
                if (matcher.find()) {
                    similarity = Double.parseDouble(matcher.group(2));
                }
            }
            br.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return similarity;
    }
    
    
    void findHypernyms(WordNetDatabase database, String requirement, String tRequirement, ArrayList<RelatedWords> relatedWords, int count) {

        NounSynset nounSynset;
        NounSynset[] hypernyms;
        //System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\WordNet\\3.0\\dict\\");
        //WordNetDatabase database = WordNetDatabase.getFileInstance();

        //if (getWordSimilarityBetweenRequirementAndRelatedWord(requirement, tRequirement) > 0.8) {
        Synset[] synsets = database.getSynsets(tRequirement, SynsetType.NOUN);
           System.out.println("The definition is : " + synsets[0].getDefinition());
        System.out.println("Synset Length is " + synsets.length);
//            for (int i = 0; i < synsets.length; i++) {
//                System.out.println("Haiiii" + synsets[i].getDefinition());
//            }
        for (int i = 0; i < synsets.length; i++) {
            nounSynset = (NounSynset) (synsets[i]);
            hypernyms = nounSynset.getHypernyms();
            for (int j = 0; j < hypernyms.length; j++) {
                    //System.out.println("The definition is  is is: " + hypernyms[j].getDefinition());
                    System.out.println("Hypernym set " + j + 1 + " : " + hypernyms[j].toString());
                Pattern pattern;
                pattern = Pattern.compile("\\[([^\\]]+)");
                Matcher matcher = pattern.matcher(hypernyms[j].toString());
                if (matcher.find()) {
                    List<String> hypernymWords = Arrays.asList(matcher.group(1).split(","));
                    for (String ts : hypernymWords) {
                        s.stem(ts, false);
                        double d = getWordSimilarityBetweenRequirementAndRelatedWord(requirement, s.toString());
                        if (d > 0.8) {
                            System.out.println("The similarity is between "+requirement+" and "+s.toString()+" is " + d);
      //System.out.println("The similarity is: "+d);
                            RelatedWords r = new RelatedWords(s.toString());
                            ArrayList<String> definitions = r.getSynsetDefinitions();
                            Synset synsets1[] = database.getSynsets(s.toString(), SynsetType.NOUN);
                            for (Synset si : synsets1) {
                                definitions.add(si.getDefinition());
                            }
                            relatedWords.add(r);
                            if (count < 2) {
                                findHypernyms(database, requirement, ts, relatedWords, count + 1);
                            }
                            //System.out.println("End of a recursion ==========================");
                        }
                    }
                }
            }
        }
    }

       void findHyponyms(WordNetDatabase database, String requirement, String tRequirement, ArrayList<RelatedWords> relatedWords, int count) {
        //ArrayList<String> relatedWords = new ArrayList<>();
        //relatedWords.add(requirement);

        NounSynset nounSynset;
        NounSynset[] hyponyms;
        //System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\WordNet\\3.0\\dict\\");
        //WordNetDatabase database = WordNetDatabase.getFileInstance();
        //if (getWordSimilarityBetweenRequirementAndRelatedWord(requirement, tRequirement) > 0.8) {
        Synset[] synsets = database.getSynsets(tRequirement, SynsetType.NOUN);
        //System.out.println("The definition is : " + synsets[0].getDefinition());

//            System.out.println("Synset Length is " + synsets.length);
//            System.out.println("The count is: " + count);
        for (int i = 0; i < synsets.length; i++) {
            nounSynset = (NounSynset) (synsets[i]);
            hyponyms = nounSynset.getHyponyms();

            for (int j = 0; j < hyponyms.length; j++) {
                //System.out.println("The definition is  is is: " + hyponyms[j].getDefinition());
                System.out.println("Hyponym set " + j + 1 + " : " + hyponyms[j].toString());
                Pattern pattern;
                pattern = Pattern.compile("\\[([^\\]]+)");
                Matcher matcher = pattern.matcher(hyponyms[j].toString());
                if (matcher.find()) {
                    List<String> hyponymWords = Arrays.asList(matcher.group(1).split(","));
                    for (String ts : hyponymWords) {
                        s.stem(ts, false);
                        double d = getWordSimilarityBetweenRequirementAndRelatedWord(requirement, s.toString());
                        if (d > 0.8) {
                            System.out.println("The similarity is between "+requirement+" and "+s.toString()+" is " + d);
                            RelatedWords r = new RelatedWords(s.toString());
                            ArrayList<String> definitions = r.getSynsetDefinitions();
                            Synset synsets1[] = database.getSynsets(s.toString(), SynsetType.NOUN);
                            for (Synset si : synsets1) {
                                definitions.add(si.getDefinition());
                            }
                            relatedWords.add(r);
                            if (count < 2) {
                                findHyponyms(database, requirement, ts, relatedWords, count + 1);
                            }
                           // System.out.println("End of a recursion ==========================");
                        }
                    }
                }
            }
        }
    }
    void findRelatedWords(String requirement,ArrayList<RelatedWords> relatedWords) {
        //System.out.println("Enter the dictionary path of wordnet: ");
        //Scanner scanner = new Scanner(System.in);
        String wordnetSource= "C:\\Program Files (x86)\\WordNet\\3.0\\dict\\";//scanner.nextLine();
        System.setProperty("wordnet.database.dir", wordnetSource);
        WordNetDatabase database = WordNetDatabase.getFileInstance();
        
        Synset[] synsets = database.getSynsets(requirement, SynsetType.NOUN);
        s.stem(requirement, false);
        RelatedWords r1=new RelatedWords(s.toString());
        ArrayList<String> definitions1=r1.getSynsetDefinitions();
        for(Synset si:synsets)
        {
            definitions1.add(si.getDefinition());
        }
        relatedWords.add(r1);
        if (synsets.length > 0) {
            
            findHyponyms(database,requirement,requirement, relatedWords,0);
            findHypernyms(database,requirement,requirement, relatedWords, 0);
        }
    }
}
