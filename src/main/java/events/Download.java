package events;

import com.google.api.client.util.Key;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class Download implements NeoEvent {

    /*"  id_bibrec" : 1550757,
        "file_format" : "GIF;ICON",
        "@timestamp" : 1418060693160.2239,
        "file_version" : 1,
        "id_bibdoc" : 949688,
        "id_user" : 0,
        "level" : 20,
        "client_host" : "88.192.215.8"
    */

    @Key
    Long id_bibrec;

    @Key
    Long id_bibdoc;

    @Key
    Long id_user;

    @Key
    String client_host;

    @Override
    public String print() {
        return "Download, user: " + this.getUser();
    }

    @Override
    public boolean validate() {
        return this.id_user != null && this.id_bibrec != null && (this.id_user != 0) && (this.id_bibrec != 0);
    }

    @Override
    public void execute(GraphDatabaseService graphDb) {

        Node user = NeoHelpers.getOrCreateUserNode(graphDb, this.id_user);
        Node record = NeoHelpers.getOrCreateRecordNode(graphDb, this.id_bibrec);

        NeoHelpers.getOrCreateRelationship(user, record, "id_bibrec",
                record.getProperty("id_bibrec").toString(), NeoHelpers.RelTypes.DOWNLOADED);

    }

    @Override
    public Long getUser() {
        return this.id_user;
    }

}
