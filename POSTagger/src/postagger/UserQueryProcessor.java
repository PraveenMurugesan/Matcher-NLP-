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
class UserQueryProcessor {

	private Factory factory;
	private Stemmer stemmer;

	UserQueryProcessor(Factory factory) throws FileNotFoundException, IOException {
		this.factory = factory;
		stemmer = factory.createStemmerObject();
	}

	// Function performing POS Tagging
	String tagSentence(String input, String taggerSource) {
		
		MaxentTagger tagger = new MaxentTagger(taggerSource);
		String tagged = tagger.tagString(input);
		return tagged;
	}

	// It finds the main verb of the user query to classify the query as either producer or consumer
	Verb findVerb(String taggedSentence, BackendConnection bec) throws IOException {
		Hashtable<String, VerbType> verbs = bec.getVerbs();
		String splittedTaggedSentence[] = taggedSentence.split(" ");
		String result = new String();
		Verb verb = factory.createVerbObject();
		Pattern pattern;
		Matcher matcher;
		/*
		 * this if block handles all queries having <VERB> TO <VERB>
		 */
		if (taggedSentence.contains("_VB")) {
			pattern = Pattern.compile(" (.*?)_VB(.*?)_TO (.*?)_VB");
			matcher = pattern.matcher(taggedSentence);
			if (matcher.find()) {
				result = matcher.group(3);
				result = result.toLowerCase();
				verb.setMainVerb(result);
				if (verbs.containsKey(result)) {
					/*
					 * I have to do car service.
					 * I like to have icecream
					 */
					if (result.equalsIgnoreCase("have") || result.equalsIgnoreCase("do")) 
					{
						verb.setVerbType(VerbType.consumerVerb);
					} else {
						verb.setVerbType(verbs.get(result));
					}
				} else {
					if (verbs.containsKey(matcher.group(1))) {
						verb.setSupplementaryVerb(matcher.group(1));
						verb.setVerbType(verbs.get(matcher.group(1)));
						/*
						 * Eg:
						 * 1. I need to drive a car,
						 * drive: neutral verb
						 * need: consumer verb
						 * 
						 * 2. I offer to drive a car
						 * producer
						 */
					} else {
						verb.setVerbType(null);
					}
				}
				return verb;
			}
		}
		/*
		 * This loop is executed if the the query is not having the structure <VERB> TO <VERB>
		 */
		for (int i = 0; i < splittedTaggedSentence.length; i++) {
			pattern = Pattern.compile("(.*?)_VB");
			matcher = pattern.matcher(splittedTaggedSentence[i]);
			if (matcher.find()) {
				result = matcher.group(1);
				result = result.toLowerCase();
				verb.setMainVerb(result);
				if (verbs.containsKey(result)) { 
					/*
					 *  If the verb is readily  available in the list (as producer or consumer) 
					 *  we dont care about modal verbs
					 */
					if (verb.getMainVerb().equalsIgnoreCase("have") || verb.getMainVerb().equalsIgnoreCase("do")) {
						/*
						 * I can do flower bouquet
						 * I can have extra luggages
						 */
						verb.setVerbType(VerbType.producerVerb);
					} else {
						verb.setVerbType(verbs.get(result));
					}
				} else {
					stemmer.stem(result.toLowerCase(), true);
					String rootWordOfVerb = stemmer.toString();
					verb.setMainVerb(rootWordOfVerb);
					if (verbs.containsKey(rootWordOfVerb)) {
						verb.setVerbType(verbs.get(rootWordOfVerb));
					} else {
						pattern = Pattern.compile("(.*?)_MD"); 
						// If the verb is unavailable, then check for the modal verb
						if (i > 0) {
							matcher = pattern.matcher(splittedTaggedSentence[i - 1]);
						} else {
							matcher = null;
						}
						if (matcher != null && matcher.find()) { 
							// Works on a condition that all producer and consumer words are listed
							verb.setSupplementaryVerb(matcher.group(1));
							verb.setVerbType(VerbType.producerVerb);
						} else {
							verb.setVerbType(null);
						}
					}
				}
				if (verbs.get(result) != VerbType.supplementaryVerb) {
					return verb;
				}
			}
		}
		return verb;
	}

	boolean IsQueryAQuestion(String taggedSentence) {
		String splittedTaggedSentence[] = taggedSentence.split(" ");
		boolean isQuestion = false;
		if (splittedTaggedSentence.length > 0) {
			Pattern verbPattern = Pattern.compile("_VB.?");
			Matcher verbMatcher = verbPattern.matcher(splittedTaggedSentence[0]);
			Pattern modalPattern = Pattern.compile("_MD");
			Matcher modalMatcher = modalPattern.matcher(splittedTaggedSentence[0]);
			Pattern WHpattern = Pattern.compile("_WP"); // modified WH with WP for question tags...
			Matcher WHMatcher = WHpattern.matcher(splittedTaggedSentence[0]);
			if (verbMatcher.find() || modalMatcher.find()) {
				if (splittedTaggedSentence.length > 1) {
					String nextWord = splittedTaggedSentence[1];
					if (nextWord.contains("_PRP") || nextWord.contains("anyone") || nextWord.contains("_EX")) {
						isQuestion = true;
					}
				}
			} else if (WHMatcher.find()) {
				isQuestion = true;
			} else if (splittedTaggedSentence[0].toLowerCase().contains("anyone")
					|| splittedTaggedSentence[0].toLowerCase().contains("someone"))
				// Questions may begin with someone or anyone....
			{
				isQuestion = true;
			}
		}
		return isQuestion;
	}

	// Used to find the nouns (services) that will be offered or required
	Service findService(String taggedSentence) {
		String splittedTaggedSentence[] = taggedSentence.split(" ");
		Service service = factory.createServiceObject();

		Pattern patternNoun = Pattern.compile("(.*?)_NN");
		Pattern patternSymbol = Pattern.compile("[+]+"); 
		/* 
		 * This is especially done to handle blood groups. 
		 * The Stanford NLP which is used here tags the special character as a	separate one
		*/
		Pattern patternAdjective = Pattern.compile("(.*?)_JJ");
		Pattern commonNounPattern = Pattern.compile("((A|a)ny|(S|s)ome)(one)?");

		for (int i = 0; i < splittedTaggedSentence.length; i++) {
			Matcher commonNounMatcher = commonNounPattern.matcher(splittedTaggedSentence[i]);
			if (splittedTaggedSentence[i].contains("_NN") && !commonNounMatcher.find()) {
				Matcher matcherNoun = patternNoun.matcher(splittedTaggedSentence[i]);
				if (matcherNoun.find()) {
					String tempRequirement = matcherNoun.group(1);
					stemmer.stem(tempRequirement, false);
					tempRequirement = stemmer.toString();
					if (i < splittedTaggedSentence.length - 1) {
						Matcher matcher1 = patternSymbol.matcher(splittedTaggedSentence[i + 1]);
						if (matcher1.find()) {
							tempRequirement = tempRequirement + matcher1.group(0);
							i++;
						}
					}
					service.getServices().add(tempRequirement);
				}
			} else if (splittedTaggedSentence[i].contains("_JJ") || splittedTaggedSentence[i].contains("_CD")) { 
				/* Both adjectives and cardinal numbers are taken as adjectives.
				*  Eg: I need 3 screw drivers.
				*/
				Matcher matcherAdjective = patternAdjective.matcher(splittedTaggedSentence[i]);
				if (matcherAdjective.find()) {
					service.getAdjectives().add(matcherAdjective.group(1));
				}
			}
		}
		return service;
	}

	/* Retrieves the producer queries and matches with the consumer queries and
	* returns the result
	*/
	ArrayList<MatchedQuery> getMatchingProducerQueries(UserQuery userQuery, BackendConnection backendConnection, String wordnetSource)
			throws FileNotFoundException, IOException {
		ArrayList<String> requirements = userQuery.getService().getServices();
		ArrayList<String> adjectives = userQuery.getService().getAdjectives();
		ArrayList<MatchedQuery> resultQueries;
		ArrayList<String> producerQueries = new ArrayList<>();

		ArrayList<RelatedWord> requirementsRelatedWords = new ArrayList<>();
		producerQueries = backendConnection.getProducersQueries();
		resultQueries = new ArrayList<>();
		String compositeRequirement = "";
		for (int i = 0; i < requirements.size(); i++) {
			stemmer.stem(requirements.get(i), false);
			compositeRequirement = compositeRequirement + stemmer.toString() + " ";
			findRelatedWords(stemmer.toString(), requirementsRelatedWords,wordnetSource);
		}
		if (requirements.size() > 1) 
		{
			/* 
			 * To find the related words of composite requirement only if the requirement
			 * is composite. Otherwise it will repetition
			 */
			findRelatedWords(compositeRequirement, requirementsRelatedWords, wordnetSource);
		}
		for (int producerQueryIterator = 0; producerQueryIterator < producerQueries.size(); producerQueryIterator++) {
			// Print the content on the console
			String splitStrline[] = producerQueries.get(producerQueryIterator).split(";");
			MatchedQuery r = factory.createResultObject(splitStrline[0]); 
			String verbs[];
			if (splitStrline.length > 1) {
				verbs = splitStrline[1].split(",");
			}

			String producerServices[] = null;
			Hashtable<String, String> producerServicesTable = new Hashtable<>();

			String compositeService = "";
			if (splitStrline.length > 2) {
				if (splitStrline[2].contains(",")) {
					producerServices = splitStrline[2].split(",");

					for (String producerService : producerServices) {
						compositeService = compositeService + producerService + "";
						producerServicesTable.put(producerService.toLowerCase(), producerService.toLowerCase());
					}
				}
			}
			Hashtable<String, String> producerAdjectivesTable = new Hashtable<String, String>();
			if (splitStrline.length > 3) {
				String producerAdjectives[] = splitStrline[3].split(",");

				for (String producerAdjective : producerAdjectives) {
					producerAdjectivesTable.put(producerAdjective.toLowerCase(), producerAdjective.toLowerCase());
				}
			}
			if (producerServices != null) {
				for (String producerService : producerServicesTable.keySet()) 
				// Check for the presence of NN in the sentence and get (0)
				{
					if (requirementsRelatedWords.size() > 0) {
						if (getWordSimilarityBetweenRequirementAndRelatedWord(producerService,
								requirementsRelatedWords.get(0).getWord()) > 0.8) {
							r.incrementRelevanceCount(1);
						}
					}
				}
				for (RelatedWord relatedWord : requirementsRelatedWords) {
					if (producerServicesTable.containsKey(relatedWord.getWord().toLowerCase())) {
						r.incrementRelevanceCount(1);
					}
				}
				resultQueries.add(r);
			}
		}
		return resultQueries;
	}

	double getWordSimilarityBetweenRequirementAndRelatedWord(String requirement, String relatedWord) {
		double similarity = 0;

		URL url;

		try {
			String tURL = "http://ws4jdemo.appspot.com/ws4j?measure=wup&args="
					+ URLEncoder.encode(requirement + "#::" + relatedWord, "UTF-8");
			url = new URL(tURL);
			URLConnection conn = url.openConnection();

			// open the stream and put it into BufferedReader
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			if ((inputLine = bufferedReader.readLine()) != null) {
				Pattern pattern = Pattern.compile("(.*)\"score\":\"(.*)\",");
				Matcher matcher = pattern.matcher(inputLine);
				if (matcher.find()) {
					similarity = Double.parseDouble(matcher.group(2));
				}
			}
			bufferedReader.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return similarity;
	}

	void findHypernyms(WordNetDatabase database, String requirement, String tRequirement,
			ArrayList<RelatedWord> relatedWords, int count) {

		NounSynset nounSynset;
		NounSynset[] hypernyms;
		Synset[] synsets = database.getSynsets(tRequirement, SynsetType.NOUN);
		
		System.out.println("The definition is : " + synsets[0].getDefinition());
		System.out.println("Synset Length is " + synsets.length);
		
		for (int i = 0; i < synsets.length; i++) {
			nounSynset = (NounSynset) (synsets[i]);
			hypernyms = nounSynset.getHypernyms();
			for (int j = 0; j < hypernyms.length; j++) {
				System.out.println("Hypernym set " + j + 1 + " : " + hypernyms[j].toString());
				Pattern pattern;
				pattern = Pattern.compile("\\[([^\\]]+)");
				Matcher matcher = pattern.matcher(hypernyms[j].toString());
				if (matcher.find()) {
					List<String> hypernymWords = Arrays.asList(matcher.group(1).split(","));
					for (String ts : hypernymWords) {
						stemmer.stem(ts, false);
						double d = getWordSimilarityBetweenRequirementAndRelatedWord(requirement, stemmer.toString());
						if (d > 0.8) {
							System.out.println("The similarity is between " + requirement + " and " + stemmer.toString()
									+ " is " + d);
							
							RelatedWord r = new RelatedWord(stemmer.toString());
							ArrayList<String> definitions = r.getSynsetDefinitions();
							Synset synsets1[] = database.getSynsets(stemmer.toString(), SynsetType.NOUN);
							for (Synset si : synsets1) {
								definitions.add(si.getDefinition());
							}
							relatedWords.add(r);
							if (count < 2) {
								findHypernyms(database, requirement, ts, relatedWords, count + 1);
							}
						}
					}
				}
			}
		}
	}

	void findHyponyms(WordNetDatabase database, String requirement, String tRequirement,
			ArrayList<RelatedWord> relatedWords, int count) {
		
		NounSynset nounSynset;
		NounSynset[] hyponyms;
		Synset[] synsets = database.getSynsets(tRequirement, SynsetType.NOUN);
		for (int i = 0; i < synsets.length; i++) {
			nounSynset = (NounSynset) (synsets[i]);
			hyponyms = nounSynset.getHyponyms();

			for (int j = 0; j < hyponyms.length; j++) {
				System.out.println("Hyponym set " + j + 1 + " : " + hyponyms[j].toString());
				Pattern pattern;
				pattern = Pattern.compile("\\[([^\\]]+)");
				Matcher matcher = pattern.matcher(hyponyms[j].toString());
				if (matcher.find()) {
					List<String> hyponymWords = Arrays.asList(matcher.group(1).split(","));
					for (String ts : hyponymWords) {
						stemmer.stem(ts, false);
						double d = getWordSimilarityBetweenRequirementAndRelatedWord(requirement, stemmer.toString());
						if (d > 0.8) {
							System.out.println("The similarity is between " + requirement + " and " + stemmer.toString()
									+ " is " + d);
							RelatedWord r = new RelatedWord(stemmer.toString());
							ArrayList<String> definitions = r.getSynsetDefinitions();
							Synset synsets1[] = database.getSynsets(stemmer.toString(), SynsetType.NOUN);
							for (Synset si : synsets1) {
								definitions.add(si.getDefinition());
							}
							relatedWords.add(r);
							if (count < 2) {
								findHyponyms(database, requirement, ts, relatedWords, count + 1);
							}
						}
					}
				}
			}
		}
	}

	void findRelatedWords(String requirement, ArrayList<RelatedWord> relatedWords, String wordnetSource) {
		System.setProperty("wordnet.database.dir", wordnetSource);
		WordNetDatabase database = WordNetDatabase.getFileInstance();

		Synset[] synsets = database.getSynsets(requirement, SynsetType.NOUN);
		stemmer.stem(requirement, false);
		RelatedWord r1 = new RelatedWord(stemmer.toString());
		ArrayList<String> definitions1 = r1.getSynsetDefinitions();
		for (Synset si : synsets) {
			definitions1.add(si.getDefinition());
		}
		relatedWords.add(r1);
		if (synsets.length > 0) {

			findHyponyms(database, requirement, requirement, relatedWords, 0);
			findHypernyms(database, requirement, requirement, relatedWords, 0);
		}
	}
}
