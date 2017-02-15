/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package postagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;


//Class to communicate with the backend [For file]
class BackendConnection {
    private String verbsFileName;
    private String producerQueriesFileName;
    private String consumerQueriesFileName;
    private String generalQueriesFileName;

    public BackendConnection() {
        Scanner s=new Scanner(System.in);
        
        //C:\\Users\\Praveen\\OneDrive\\NLP\\Verbs.txt

//C:\\Users\\Praveen\\OneDrive\\NLP\\ProducerQueries.txt

//C:\\Users\\Praveen\\OneDrive\\NLP\\ConsumerQueries.txt

//C:\\Users\\Praveen\\OneDrive\\NLP\\GeneralQueries.txt
        
//C:\\Users\\Praveen\\OneDrive\\NLP\\stanford-postagger-2015-04-20\\stanford-postagger-2015-04-20\\models\\english-left3words-distsim.tagger
        
//C:\\Program Files (x86)\\WordNet\\3.0\\dict\\
        System.out.println("Enter complete file name for verbs file");
        //verbsFileName= s.nextLine();
        verbsFileName="C:\\Users\\Praveen\\OneDrive\\NLP\\Verbs.txt";
        
        System.out.println("Enter complete file name for producer queries file");
        //producerQueriesFileName=s.nextLine();
        producerQueriesFileName="C:\\Users\\Praveen\\OneDrive\\NLP\\ProducerQueries.txt";
        
        System.out.println("Enter complete file name for consumer queries file");
        //consumerQueriesFileName=s.nextLine();
        consumerQueriesFileName="C:\\Users\\Praveen\\OneDrive\\NLP\\ConsumerQueries.txt";
        
        System.out.println("Enter complete file name for general queries file");
        //generalQueriesFileName=s.nextLine();
        generalQueriesFileName="C:\\Users\\Praveen\\OneDrive\\NLP\\GeneralQueries.txt";
    }
    
    
   
    // To get the verbs from the specified file 
    Hashtable<String, VerbType> getVerbs() throws FileNotFoundException, IOException {
        Hashtable<String, VerbType> verbs = new Hashtable<>();
        FileInputStream fstream = new FileInputStream(verbsFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String strLine = new String();
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
            //System.out.println("Verb is : "+splitLine[0]+" and its type is : "+vt);
            verbs.put(splitLine[0], vt);
        }
        return verbs;
    }
    
    //To get the producer queries from the specified file
    ArrayList<String> getProducersQueries() throws FileNotFoundException, IOException {
        ArrayList<String> producerQueries = new ArrayList<>();
        FileInputStream fstream = new FileInputStream(producerQueriesFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        while ((strLine = br.readLine()) != null) {
            producerQueries.add(strLine);
        }
        return producerQueries;
    }

    // To write the processed query as whistle to the file
    void writeQueryToFile(UserQuery w,boolean isQueryAQuestion) {
        try {
            FileWriter fw;
            VerbType queryMainVerbType=w.getVerb().getVerbType();
            //Consumer Verb in Question sentence => Produer Query; Producer verb in Question Sentence => Consumer Query
            if (queryMainVerbType == VerbType.consumerVerb && !isQueryAQuestion||queryMainVerbType==VerbType.producerVerb && isQueryAQuestion) {
                fw = new FileWriter(consumerQueriesFileName, true);
            } else if (queryMainVerbType == VerbType.producerVerb && !isQueryAQuestion || queryMainVerbType==VerbType.consumerVerb && isQueryAQuestion) {
                fw = new FileWriter(producerQueriesFileName, true);
            } else {
                fw = new FileWriter(generalQueriesFileName, true);
            }
            String requirements = "";
            String adjectives = "";
            for (int i = 0; i < w.getService().getServices().size(); i++) {
                requirements += w.getService().getServices().get(i) + ",";
            }
            for (int i = 0; i < w.getService().getAdjectives().size(); i++) {
                adjectives += w.getService().getAdjectives().get(i) + ",";
            }
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(w.getQuery() + ";" + w.getVerb().getSupplementaryVerb() + "," + w.getVerb().getMainVerb() + ";" + requirements + ";" + adjectives);
                bw.newLine();
            }
        } catch (IOException e) {
        }
    }
}

