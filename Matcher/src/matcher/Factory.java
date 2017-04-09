package matcher;

import java.io.FileNotFoundException;
import java.io.IOException;

//Factory Class to create Objects
public class Factory {

    UserQueryProcessor createQueryProcessorObject(Factory f) throws FileNotFoundException, IOException {
        return new UserQueryProcessor(f);
    }

    BackendConnection createBackendConnectionObject(String verbsFileName,String producerQueriesFileName, String consumerQueriesFileName, String generalQueriesFileName) {
        return new BackendConnection(verbsFileName,producerQueriesFileName,consumerQueriesFileName, generalQueriesFileName);
    }

    Stemmer createStemmerObject() {
        return new Stemmer();
    }

    UserQuery createUserQueryObject() {
        return new UserQuery();
    }

    MatchedQuery createResultObject(String producerQuery) {
        return new MatchedQuery(producerQuery);
    }

    Verb createVerbObject() {
        return new Verb();
    }

    Service createServiceObject() {
        return new Service();
    }
}

