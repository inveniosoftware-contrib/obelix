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
    private int bibrecID;

    @Key("timestamp")
    private String timestamp;

    public final int getBibdocID() {
        return bibdocID;
    }

    public final int getUserID() {
        return userID;
    }

    public final String getClientHost() {
        return clientHost;
    }

    public final int getBibrecID() {
        return bibrecID;
    }

    public final String getTimestamp() {
        return timestamp;
    }
}
