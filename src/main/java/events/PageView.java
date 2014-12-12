package events;

import com.google.api.client.util.Key;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class PageView implements NeoEvent {

    /*
        "id_bibrec" : 1621895,
        "@timestamp" : 1418060677198.034,
        "id_user" : 0,
        "client_host" : "157.55.39.169",
        "level" : 20
     */

    @Key
    Long id_bibdoc;

    @Key
    Long id_user;

    @Key
    String client_host;

    @Key
    Long id_bibrec;

    @Override
    public String print() {
        return "PageView, user: " + this.getUser();
    }

    @Override
    public Long getUser() {
        return this.id_user;
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
                record.getProperty("id_bibrec").toString(), NeoHelpers.RelTypes.SEEN);

    }

}
