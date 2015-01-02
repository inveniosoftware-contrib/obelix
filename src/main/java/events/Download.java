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

    @Key("id_bibrec")
    Long id_bibrec;

    @Key("id_user")
    Long id_user;

    @Key("timestamp")
    Long timestamp;

    @Override
    public boolean validate() {
        return this.id_user != null && this.id_bibrec != null && (this.id_user != 0) && (this.id_bibrec != 0);
    }

    @Override
    public void execute(GraphDatabaseService graphDb, int maxRelationships) {
        Node user = NeoHelpers.getOrCreateUserNode(graphDb, this.id_user.toString());
        Node item = NeoHelpers.getOrCreateItemNode(graphDb, this.id_bibrec.toString());

        NeoHelpers.createRealationship(user, item, this.timestamp, NeoHelpers.RelTypes.DOWNLOADED, maxRelationships);

    }

    @Override
    public String getUser() {
        return this.id_user.toString();
    }

    @Override
    public String getItem() {
        return this.id_bibrec.toString();
    }

    @Override
    public String getTimestamp() {
        return this.timestamp.toString();
    }
}
