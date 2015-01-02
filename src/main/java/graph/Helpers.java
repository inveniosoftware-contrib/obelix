package graph;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Helpers {

    public static LinkedHashMap<String, Double> sortedHashMap(Map<String, Double> map, boolean descending) {

        LinkedHashMap<String, Double> result = new LinkedHashMap<>();

        int order = descending ? -1 : 1;

        Stream<Map.Entry<String, Double>> sorted = map.entrySet().stream().sorted((s1,s2) ->
                s1.getValue().compareTo(s2.getValue())*order);

        sorted.forEachOrdered(new Consumer<Map.Entry<String, Double>>() {
            public void accept(Map.Entry<String, Double> stringDoubleEntry) {
                result.put(stringDoubleEntry.getKey(), stringDoubleEntry.getValue());
            }
        });

        return result;

    }
}
