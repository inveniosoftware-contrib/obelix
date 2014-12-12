package events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.neo4j.graphdb.GraphDatabaseService;

public class EventFactory {

    public static final String EVENTS_DOWNLOADS = "events.downloads";
    public static final String EVENTS_DOWNLOADS_LOG = "download-log";
    public static final String EVENTS_PAGEVIEW = "events.pageviews";

    static public NeoEvent build(GraphDatabaseService graphDb, String s) {

        NeoEvent event = null;

        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(s).getAsJsonObject();
        String type;

        try {
            type = object.get("type").getAsString();
        } catch (NullPointerException e) {
            System.out.println("No type found?");
            return null;
        }

        switch (type) {
            case EVENTS_DOWNLOADS:
                event = new Gson().fromJson(object.get("source"), Download.class);
                break;
            case EVENTS_DOWNLOADS_LOG:
                event = new Gson().fromJson(object.get("source"), Download.class);
                break;
            case EVENTS_PAGEVIEW:
                event = new Gson().fromJson(object.get("source"), PageView.class);
                break;
        }

        if(event != null) {
            if(event.validate()) {
                return event;
            }
            else {
                System.out.println(event + " did not validate..");
                System.out.println(event.getUser());
            }
        }

        return null;

    }
}
