package events;

import com.google.api.client.util.Key;
import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;

import static events.NeoHelpers.normalizedTimeStamp;

public class Download implements NeoEvent {

    @Key("item")
    private String item;

    @Key("type")
    private String type;

    @Key("user")
    private String user;

    @Key("file_format")
    private String fileFormat;

    @Key("timestamp")
    private Long timestamp;

    public final boolean validate() {

        if (user == null || item == null || fileFormat == null) {
            return false;
        }

        if (!fileFormat.toLowerCase().equals("pdf")) {
            return false;
        }

        if (user.equals("") || user.equals("0")) {
            return false;
        }

        return !(item.equals("") || item.equals("0"));

    }

    public final void execute(final GraphDatabase graphDb,
                              final int maxRelationships) throws ObelixInsertException {
        //Fixme: Why VIEWED?
        graphDb.createNodeNodeRelationship(this.user,
                this.item, NeoHelpers.RelTypes.VIEWED, getTimestamp(), maxRelationships);
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
