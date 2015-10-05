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
package utils;

import com.google.api.client.util.Key;

public class Source {

    @Key("id_bibdoc")
    private int bibdocID;

    @Key("id_user")
    private int userID;

    @Key("client_host")
    private String clientHost;

    @Key("id_bibrec")
    private int bibrecID;

    @Key("timestamp")
    private String timestamp;

    public final int getBibdocID() {
        return bibdocID;
    }

    public final int getUserID() {
        return userID;
    }

    public final String getClientHost() {
        return clientHost;
    }

    public final int getBibrecID() {
        return bibrecID;
    }

    public final String getTimestamp() {
        return timestamp;
    }
}
