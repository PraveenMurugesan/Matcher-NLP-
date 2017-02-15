package postagger;

import java.io.FileNotFoundException;
import java.io.IOException;

//Factory Class to create Objects
public class Factory {

    UserQueryProcessing createQueryProcessingObject(Factory f) throws FileNotFoundException, IOException {
        return new UserQueryProcessing(f);
    }

    BackendConnection createBackendConnectionObject() {
        return new BackendConnection();
    }

    Stemmer createStemmerObject() {
        return new Stemmer();
    }

    UserQuery createUserQueryObject() {
        return new UserQuery();
    }

    Result createResultObject(String producerQuery) {
        return new Result(producerQuery);
    }

    Verb createVerbObject() {
        return new Verb();
    }

    Service createServiceObject() {
        return new Service();
    }
}

