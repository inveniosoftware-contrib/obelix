package events;

import com.google.api.client.util.Key;
import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;

import static events.NeoHelpers.normalizedTimeStamp;

public class PageView implements NeoEvent {

    @Key("item")
    private String item;

    @Key("type")
    private String type;

    @Key("user")
    private String user;

    @Key("timestamp")
    private Long timestamp;

    @Override
    public final boolean validate() {
        if (user == null || item == null) {
            return false;
        }

        if (user.equals("") || item.equals("") || item.equals("0")) {
            return false;
        }

        return true;

    }

    @Override
    public final void execute(final GraphDatabase graphDb,
                              final int maxRelationships) throws ObelixInsertException {
        graphDb.createNodeNodeRelationship(this.user, this.item,
                NeoHelpers.RelTypes.VIEWED, getTimestamp(), maxRelationships);
    }

    @Override
    public final String getType() {
        return this.type;
    }

    @Override
    public final String getUser() {
        return this.user;
    }

    @Override
    public final String getItem() {
        return this.item;
    }

    @Override
    public final String getTimestamp() {
        return normalizedTimeStamp(this.timestamp.toString());
    }
}
