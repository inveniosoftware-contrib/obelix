/**
 * This file is part of Obelix.
 *
 * Obelix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Obelix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Obelix.  If not, see <http://www.gnu.org/licenses/>.
 */
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
