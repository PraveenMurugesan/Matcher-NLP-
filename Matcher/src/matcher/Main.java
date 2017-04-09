
package matcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;



public class Main {

	public static void main(String[] args)
			throws FileNotFoundException, IOException, InstantiationException, IllegalAccessException {
		Factory factory = new Factory();

		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter user query : ");
		String input = scanner.nextLine();

		// For data retrieval

        System.out.println("Enter complete file name for verbs file");
        String verbsFileName=scanner.nextLine();
        
        System.out.println("Enter complete file name for producer queries file");
        String producerQueriesFileName=scanner.nextLine();
        
        System.out.println("Enter complete file name for consumer queries file");
        String consumerQueriesFileName=scanner.nextLine();;
        
        System.out.println("Enter complete file name for general queries file");
        String generalQueriesFileName=scanner.nextLine();
		BackendConnection backendConnectionObject = factory.createBackendConnectionObject(verbsFileName,producerQueriesFileName,consumerQueriesFileName,generalQueriesFileName);

		/* 
		 * Creating Object for UserQueryProcessor Class - This class has all
		 * important functions to process query
		 */
		UserQueryProcessor userQueryProcessor = factory.createQueryProcessorObject(factory);

		// POS Tagging
		System.out.println("Enter tagger source file name");
		String taggerSource = scanner.nextLine();

		System.out.println("Enter the path of wordnet package: ");
		String wordnetSource = scanner.nextLine();

		String taggedSentence = userQueryProcessor.tagSentence(input, taggerSource);
		System.out.println(taggedSentence);

		// Create whistle
		UserQuery userQuery = factory.createUserQueryObject();

		// Populate UserQuery variables
		userQuery.setQuery(input);

		Verb verb = userQueryProcessor.findVerb(taggedSentence, backendConnectionObject);
		userQuery.setVerb(verb);

		Service service = userQueryProcessor.findService(taggedSentence);
		userQuery.setService(service);

		System.out.println("The verb is " + verb.getMainVerb() + " and the type is " + verb.getVerbType());

		boolean isQueryAQuestion = false;

		if (verb.getVerbType() == null || verb.getVerbType() == VerbType.supplementaryVerb) {
			System.out.println(
					"Do you want to provide the service or consume the service.. Press c for consuming p for providing service...");
			char customerChoiceOfService = scanner.next().toLowerCase().charAt(0);
			if (customerChoiceOfService == 'c') {
				userQuery.getVerb().setVerbType(VerbType.consumerVerb);
			} else if (customerChoiceOfService == 'p') {
				userQuery.getVerb().setVerbType(VerbType.producerVerb);
			}
		} else {
			isQueryAQuestion = userQueryProcessor.IsQueryAQuestion(taggedSentence);
		}
		
		// Write user query to corresponding file
		backendConnectionObject.writeQueryToFile(userQuery, isQueryAQuestion);
		
		VerbType queryMainVerbType = userQuery.getVerb().getVerbType();
		
		if (queryMainVerbType == VerbType.consumerVerb && !isQueryAQuestion
				|| queryMainVerbType == VerbType.producerVerb && isQueryAQuestion) {
			ArrayList<MatchedQuery> result;
			result = userQueryProcessor.getMatchingProducerQueries(userQuery, backendConnectionObject, wordnetSource);
			Collections.sort(result, new Comparator<MatchedQuery>() {
				@Override
				public int compare(MatchedQuery p1, MatchedQuery p2) {
					return (int) (p2.getRelevanceCount() - p1.getRelevanceCount()); // Ascending
				}
			});
			for (MatchedQuery r : result) {
				System.out.println(
						"Producer Query: " + r.getProducerQuery() + " - Relevance count: " + r.getRelevanceCount());
			}
		}
	}
}
