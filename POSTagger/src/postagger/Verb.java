package postagger;

// Verbs are classified in to three types as follows 
// Producer Verbs - provide, give, distribute
//Consumer verbs - need, want,demand
// Supllementary - am, will, be
enum VerbType {

    producerVerb, consumerVerb, supplementaryVerb;
}

class Verb {

    private String mainVerb;
    private String supplementaryVerb;
    private VerbType type;
    
    public String getMainVerb()
    {
        return mainVerb;
    }
    
     public String getSupplementaryVerb()
    {
        return supplementaryVerb;
    }
     
      public VerbType getVerbType()
    {
        return type;
    }
      public void setMainVerb(String m)
      {
          mainVerb=m;
      }
      public void setSupplementaryVerb(String s)
      {
          supplementaryVerb=s;
      }
      public void setVerbType(VerbType vt)
      {
          type =vt;
      }
}

