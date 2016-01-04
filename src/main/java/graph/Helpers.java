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
package graph;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class Helpers {

    private Helpers() {
    }

    public static LinkedHashMap<String, Double> sortedHashMap(
            final Map<String, Double> map, final boolean descending) {

        LinkedHashMap<String, Double> result = new LinkedHashMap<>();

        int order = 1;

        if (descending) {
            order = -1;
        }

        final int finalOrder = order;
        Stream<Map.Entry<String, Double>> sorted = map.entrySet().stream().sorted((s1, s2) ->
                s1.getValue().compareTo(s2.getValue()) * finalOrder);

        sorted.forEachOrdered(stringDoubleEntry ->
                result.put(stringDoubleEntry.getKey(), stringDoubleEntry.getValue()));

        return result;

    }
}
