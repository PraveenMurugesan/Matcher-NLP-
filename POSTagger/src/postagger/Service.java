package postagger;

// Class Service has two variables

import java.util.ArrayList;

//services - Eg : I need a car wash center . Query generates 2 nouns namely car and wash. Both are considered as different requirement to the user and handled in that way. 
//adjective - Eg : I need cheap and efficient petrol car. Cheap and efficient are adjective and in the search, it will also get checked to match with the best possible producer query. 
class Service {  

    private ArrayList<String> services;
    private ArrayList<String> adjectives;

    Service() {
        services = new ArrayList<>();
        adjectives = new ArrayList<>();
    }
    ArrayList<String> getServices()
    {
        return services;
    }
    ArrayList<String> getAdjectives()
    {
        return adjectives;
    }
}
