package matcher;

import postagger.Service;
import postagger.Verb;

/*
UserQuery has three attributes
Query given by users
Verb in the sentence
Service object
*/

class UserQuery {

    private String query;
    private Verb verb;
    private Service service;
    private boolean isQuestionformat;
    
    String getQuery()
    {
        return query;
    }
    Verb getVerb()
    {
        return verb;
    }
    Service getService()
    {
        return service;
    }
    boolean getFlag()
    {
        return isQuestionformat;
    }
    void setFlag(boolean flag)
    {
        isQuestionformat=flag;
    }
    void setQuery(String q)
    {
      query = q;  
    }
    void setVerb(Verb v)
    {
        verb =v;
    }
    void setService(Service s)
    {
        service = s;
    }
}

