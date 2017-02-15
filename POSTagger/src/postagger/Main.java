
package postagger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws FileNotFoundException, IOException, InstantiationException, IllegalAccessException {
        Factory f = new Factory();

        Scanner sobj = new Scanner(System.in);
        System.out.println("Enter user query : ");
        String input = sobj.nextLine();
        
        //For data retrival           
        BackendConnection backendConnectionObject = f.createBackendConnectionObject();
        
        //Creting Object for UserQuery Processing Class - This class has all important functions
        UserQueryProcessing wp = f.createQueryProcessingObject(f);
       
        //POS Tagging
        String taggedSentence = wp.tagSentence(input);
        System.out.println(taggedSentence);

        //Create whistle
        UserQuery w = f.createUserQueryObject();

        //Populate UserQuery variables
        w.setQuery(input);
        Verb verb = wp.findVerb(taggedSentence,backendConnectionObject);
        w.setVerb(verb);
        Service ser = wp.findService(taggedSentence);
        w.setService(ser);
        System.out.println("The verb is " + verb.getMainVerb() + " and the type is " + verb.getVerbType());
        boolean isQueryAQuestion = false;
        if (verb.getVerbType() == null || verb.getVerbType() == VerbType.supplementaryVerb) {
            System.out.println("Do you want to provide the service or consume the service.. Press c for consuming p for providing service...");
            char cservice = sobj.next().charAt(0);
            if (cservice == 'c') {
                w.getVerb().setVerbType(VerbType.consumerVerb);
            } else if (cservice == 'p') {
                w.getVerb().setVerbType(VerbType.producerVerb);
            }
        } else {
            isQueryAQuestion = wp.IsQueryAQuestion(taggedSentence);
        }
        // Write whistle to corresponding file
        backendConnectionObject.writeQueryToFile(w, isQueryAQuestion);
        VerbType queryMainVerbType = w.getVerb().getVerbType();
        if (queryMainVerbType == VerbType.consumerVerb && !isQueryAQuestion || queryMainVerbType == VerbType.producerVerb && isQueryAQuestion) {
            ArrayList<Result> result;
            result = wp.giveResult(w, backendConnectionObject);
             Collections.sort(result, new Comparator<Result>() {
        @Override
        public int compare(Result p1, Result p2) {
            return (int) (p2.getRelevanceCount() - p1.getRelevanceCount()); // Ascending
        }
              });
            for (Result r:result) {
                System.out.println("Producer Query: " + r.getProducerQuery() + " - Relevance count: " + r.getRelevanceCount());
            }

                
        }
        
    }
}
//C:\\Users\\Praveen\\OneDrive\\NLP\\Verbs.txt

//C:\\Users\\Praveen\\OneDrive\\NLP\\ProducerQueries.txt

//C:\\Users\\Praveen\\OneDrive\\NLP\\ConsumerQueries.txt

//C:\\Users\\Praveen\\OneDrive\\NLP\\GeneralQueries.txt
        
//C:\\Users\\Praveen\\OneDrive\\NLP\\stanford-postagger-2015-04-20\\stanford-postagger-2015-04-20\\models\\english-left3words-distsim.tagger
        
//C:\\Program Files (x86)\\WordNet\\3.0\\dict\\