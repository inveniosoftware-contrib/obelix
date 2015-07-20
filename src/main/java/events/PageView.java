package events;

import com.google.api.client.util.Key;
import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import static events.NeoHelpers.normalizedTimeStamp;

public class PageView implements NeoEvent {

    //{"item":58329376,"ip":"85.5.192.100","type":"events.downloads","user":"58329376","file_format":"PNG;ICON","timestamp":1421027564617}

    @Key("item")
    String item;

    @Key("ip")
    String ip;

    @Key("type")
    String type;

    @Key("user")
    String user;

    @Key("timestamp")
    Long timestamp;

    @Override
    public boolean validate() {
        return user != null && item != null && !user.equals("") && !user.equals("0") && !item.equals("") && !item.equals("0");
    }

    @Override
    public void execute(GraphDatabase graphDb, int maxRelationships) throws ObelixInsertException{
        graphDb.createNodeNodeRelationship(this.user, this.item, NeoHelpers.RelTypes.VIEWED, getTimestamp(), maxRelationships);
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getItem() {
        return this.item;
    }

    @Override
    public String getTimestamp() {
        return normalizedTimeStamp(this.timestamp.toString());
    }
}
