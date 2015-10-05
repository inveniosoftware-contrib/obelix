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
package queue.impl;

import org.json.JSONObject;

public class ObelixQueueElement {

    private final JSONObject data;

    public final JSONObject getData() {
        return this.data;
    }

    public ObelixQueueElement(final JSONObject dataInput) {
        this.data = dataInput;
    }

    public ObelixQueueElement(final String key, final Object element) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, element);
        this.data = jsonObject;
    }

    public ObelixQueueElement(final String rawData) {

        String currentRawData = rawData;

        // Let's try to convert the json formatted as string..
        if (!currentRawData.startsWith("{")) {
            currentRawData = currentRawData.substring(1, rawData.length() - 1);
            currentRawData = currentRawData.replace("\\", "");
        }

        this.data = new JSONObject(currentRawData);
    }

    public final boolean equals(final Object object) {
        return object instanceof ObelixQueueElement && this.data.toString()
                .equals(((ObelixQueueElement) object).data.toString());
    }

    public final String toString() {
        return this.data.toString();
    }

    public final int hashCode() {
        return data.hashCode();
    }
}
