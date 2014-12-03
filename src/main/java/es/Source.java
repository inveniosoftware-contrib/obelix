package es;

import com.google.api.client.util.Key;

public class Source {


    @Key("file_version")
    private String fileVersion;

    @Key("id_bibdoc")
    private String bibdocID;

    @Key("id_user")
    private String userID;

    @Key("client_host")
    private String clientHost;

    @Key("id_bibrec")
    private String bibrecID;

    @Key("file_format")
    private String fileFormat;

    public String getFileVersion() {
        return fileVersion;
    }

    public String getBibdocID() {
        return bibdocID;
    }

    public String getUserID() {
        return userID;
    }

    public String getClientHost() {
        return clientHost;
    }

    public String getBibrecID() {
        return bibrecID;
    }

    public String getFileFormat() {
        return fileFormat;
    }


    //@Key("timestamp")
    //String timestamp;

}
