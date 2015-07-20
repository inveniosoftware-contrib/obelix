package obelix.interfaces;

import graph.exceptions.ObelixNodeNotFoundException;

import java.util.Map;

public interface Recommender {
    public Map<String, Double> recommend(String userID) throws ObelixNodeNotFoundException;
    public Map<String, Double> recommend(String userID, String depth) throws ObelixNodeNotFoundException;
    public Map<String, Double> recommend(String userID,
                                         String depth,
                                         String sinceTimestamp,
                                         String untilTimestamp,
                                         String importanceFactor) throws ObelixNodeNotFoundException;
}
