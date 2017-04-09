/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package matcher;

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

    public BackendConnection(String verbsFileName,String producerQueriesFileName, String consumerQueriesFileName, String generalQueriesFileName) {
        this.verbsFileName=verbsFileName;
        this.producerQueriesFileName=producerQueriesFileName;
        this.consumerQueriesFileName=consumerQueriesFileName;
        this.generalQueriesFileName=generalQueriesFileName;
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
    void writeQueryToFile(UserQuery userQuery,boolean isQueryAQuestion) {
        try {
            FileWriter fileWriter;
            VerbType queryMainVerbType=userQuery.getVerb().getVerbType();
            //Consumer Verb in Question sentence => Produer Query; Producer verb in Question Sentence => Consumer Query
            if (queryMainVerbType == VerbType.consumerVerb && !isQueryAQuestion||queryMainVerbType==VerbType.producerVerb && isQueryAQuestion) {
                fileWriter = new FileWriter(consumerQueriesFileName, true);
            } else if (queryMainVerbType == VerbType.producerVerb && !isQueryAQuestion || queryMainVerbType==VerbType.consumerVerb && isQueryAQuestion) {
                fileWriter = new FileWriter(producerQueriesFileName, true);
            } else {
                fileWriter = new FileWriter(generalQueriesFileName, true);
            }
            String requirements = new String();
            String adjectives = "";
            for (int i = 0; i < userQuery.getService().getServices().size(); i++) {
                requirements += userQuery.getService().getServices().get(i) + ",";
            }
            for (int i = 0; i < userQuery.getService().getAdjectives().size(); i++) {
                adjectives += userQuery.getService().getAdjectives().get(i) + ",";
            }
            try (BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write(userQuery.getQuery() + ";" + userQuery.getVerb().getSupplementaryVerb() + "," + userQuery.getVerb().getMainVerb() + ";" + requirements + ";" + adjectives);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
        }
    }
}

