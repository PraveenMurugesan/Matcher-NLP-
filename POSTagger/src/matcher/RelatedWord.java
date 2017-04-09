package matcher;

import java.util.ArrayList;

class RelatedWord {

    private String word;
    private ArrayList<String> synsetDefinitions;

    RelatedWord(String word) {
        this.word = word;
        synsetDefinitions = new ArrayList();
    }

    String getWord() {
        return word;
    }

    ArrayList<String> getSynsetDefinitions() {
        return synsetDefinitions;
    }
}
