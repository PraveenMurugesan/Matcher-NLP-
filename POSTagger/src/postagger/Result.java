package postagger;

class Result {

    private String producerQuery;
    private double relevanceCount; // To denote how close a consumer's query match with the producer query. 

    Result(String pw) {
        producerQuery = pw;
        relevanceCount = 0;
    }
    String getProducerQuery()
    {
        return producerQuery;
    }
    double getRelevanceCount()
    {
        return relevanceCount;
    }
    void incrementRelevanceCount(double value)
    {
        this.relevanceCount =this.relevanceCount+value;
    }
}

