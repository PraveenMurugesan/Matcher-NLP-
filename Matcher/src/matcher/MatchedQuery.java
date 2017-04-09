package matcher;

class MatchedQuery {

    private String producerQuery;
    private double relevanceCount; // To denote how close a consumer's query match with the producer query. 

    MatchedQuery(String producerQuery) {
        this.producerQuery = producerQuery;
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
        this.relevanceCount = this.relevanceCount+value;
    }
}

