package es;

import com.google.api.client.util.Key;

import java.util.ArrayList;

public class Hits {

    @Key
    int total;

    @Key("hits")
    public ArrayList<Hit> hits;

}
