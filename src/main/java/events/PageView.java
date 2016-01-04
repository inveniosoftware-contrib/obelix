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
