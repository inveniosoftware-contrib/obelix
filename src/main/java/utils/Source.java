package utils;

import com.google.api.client.util.Key;

public class Source {

    @Key("id_bibdoc")
    private int bibdocID;

    @Key("id_user")
    private int userID;

    @Key("client_host")
    private String clientHost;

    @Key("id_bibrec")
    public int bibrecID;

    @Key("timestamp")
    private String timestamp;

    public int getBibdocID() {
        return bibdocID;
    }

    public int getUserID() {
        return userID;
    }

    public String getClientHost() {
        return clientHost;
    }

    public int getBibrecID() {
        return bibrecID;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
