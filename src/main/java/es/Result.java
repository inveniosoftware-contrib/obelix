package es;

import com.google.api.client.util.Key;

import java.util.List;

public class Result {

    @Key("timed_out")
    public boolean timedOut;

    @Key("_scroll_id")
    String scrollID;

    @Key("hits")
    public Hits hits;

    public List<Hit> getResult() {
        return this.hits.hits;
    }
}
