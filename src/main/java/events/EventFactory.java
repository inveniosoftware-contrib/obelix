package events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(EventFactory.class.getName());

    public static final String EVENTS_DOWNLOADS = "events.downloads";
    public static final String EVENTS_DOWNLOADS_LOG = "download-log";
    public static final String EVENTS_PAGEVIEW = "events.pageviews";

    //"{\"file_format\": \"PDF\", \"timestamp\": 1422539383.3392179, \"item\": 1297206, \"user\": \"58359646\", \"ip\": \"137.138.125.175\", \"type\": \"events.downloads\"}"

    static public NeoEvent build(String s) {

        NeoEvent event = null;

        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(s).getAsJsonObject();
        String type;

        try {
            type = object.get("type").getAsString();
        } catch (NullPointerException e) {
            LOGGER.warn("No suitable type found for event: " + object);
            return null;
        }

        switch (type) {
            case EVENTS_DOWNLOADS:
                event = new Gson().fromJson(object, Download.class);
                break;
            case EVENTS_DOWNLOADS_LOG:
                event = new Gson().fromJson(object, Download.class);
                break;
            case EVENTS_PAGEVIEW:
                event = new Gson().fromJson(object, PageView.class);
                break;
        }

        if(event != null) {
            if(event.validate()) {
                return event;
            }
        }

        return null;

    }
}
