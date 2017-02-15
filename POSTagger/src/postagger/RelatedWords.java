package postagger;

import java.util.ArrayList;

class RelatedWords {

    private String word;
    private ArrayList<String> synsetDefinitions;

    RelatedWords(String s) {
        word = s;
        synsetDefinitions = new ArrayList();
    }

    String getWord() {
        return word;
    }

    ArrayList<String> getSynsetDefinitions() {
        return synsetDefinitions;
    }
}
