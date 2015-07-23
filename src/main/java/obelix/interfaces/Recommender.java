package obelix.interfaces;

import graph.exceptions.ObelixNodeNotFoundException;

import java.util.Map;

public interface Recommender {
    Map<String, Double> recommend(String userID) throws ObelixNodeNotFoundException;
    Map<String, Double> recommend(String userID, String depth) throws ObelixNodeNotFoundException;
    Map<String, Double> recommend(String userID,
                                  String depth,
                                  String sinceTimestamp,
                                  String untilTimestamp,
                                  String importanceFactor) throws ObelixNodeNotFoundException;
}
