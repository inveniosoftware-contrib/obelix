package events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventFactory {

    private EventFactory() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EventFactory.class.getName());
    public static final String EVENTS_DOWNLOADS = "events.downloads";
    public static final String EVENTS_DOWNLOADS_LOG = "download-log";
    public static final String EVENTS_PAGEVIEW = "events.pageviews";

    public static NeoEvent build(final String s) {

        NeoEvent event;
        String type;

        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(s).getAsJsonObject();

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
            default:
                event = null;
        }

        if (event != null && event.validate()) {
            return event;
        }

        return null;

    }
}
