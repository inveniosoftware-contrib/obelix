package es;


import com.google.api.client.util.Key;

public class Hit {

    @Key("_id")
    private String id;

    @Key("_index")
    private String index;

    @Key("_type")
    private String type;

    @Key("_source")
    private Source source;

    public String getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public String getIndex() {
        return index;
    }
}
