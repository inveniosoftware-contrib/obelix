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
