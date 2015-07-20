package obelix.impl;

import graph.Helpers;
import graph.UserItemRelationship;
import graph.exceptions.ObelixNodeNotFoundException;
import graph.interfaces.GraphDatabase;
import obelix.interfaces.Recommender;
import org.neo4j.graphdb.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ObelixRecommender implements Recommender {
    private final static Logger LOGGER = LoggerFactory.getLogger(ObelixRecommender.class.getName());
    GraphDatabase graphdb;

    public ObelixRecommender(GraphDatabase graphdb) {
        this.graphdb = graphdb;
    }

    @Override
    public Map<String, Double> recommend(String userID) throws ObelixNodeNotFoundException {
        return recommend(userID, "3", null, null, null);
    }

    @Override
    public Map<String, Double> recommend(String userID, String depth) throws ObelixNodeNotFoundException {
        return recommend(userID, depth, null, null, null);
    }

    @Override
    public Map<String, Double> recommend(String userID,
                                         String depth,
                                         String sinceTimestamp,
                                         String untilTimestamp,
                                         String importanceFactor) throws ObelixNodeNotFoundException {
        int importanceFactorInteger = 1;
        if (importanceFactor != null) {
            importanceFactorInteger = Integer.parseInt(importanceFactor);
        }

        Map<String, Double> result = new HashMap<>();
        Map<String, Double> normalizedResult = new HashMap<>();
        List<UserItemRelationship> userItemRelationships = null;
        boolean errorFetchingRelationships = false;

        while (userItemRelationships == null) {
            try {
                userItemRelationships = graphdb.getRelationships(userID, depth, sinceTimestamp, untilTimestamp, false);

            } catch (NotFoundException e) {
                errorFetchingRelationships = true;
                LOGGER.error("Relationships not found, we have to try again! (" + e.getMessage() + ")");
                LOGGER.error(userID + "-" + depth + "-" + sinceTimestamp + "-" + untilTimestamp + "-" + importanceFactor);
            }
        }

        if (errorFetchingRelationships) {
            LOGGER.error("Fetching relationships OK now for:");
            LOGGER.error(userID + "-" + depth + "-" + sinceTimestamp + "-" + untilTimestamp + "-" + importanceFactor);
        }

        double maxScore = 0.0;
        double score = 0.0;

        for (UserItemRelationship it : userItemRelationships) {
            String itemID = it.itemid;
            result.putIfAbsent(itemID, 0.0);
            score = result.get(itemID) + 1 + it.getImportanceFactor(importanceFactorInteger);
            score = Math.log(score);
            result.put(itemID, score);

            if (score > maxScore) {
                maxScore = score;
            }
        }

        for (Map.Entry<String, Double> entry : result.entrySet()) {
            double normalizedScore = entry.getValue() / maxScore;

            //System.out.println(entry.getKey() + " - " + entry.getValue());
            // Threshold value for including the recommendation, at least 0.1 = 10%
            if (normalizedScore > 0.15) {
                normalizedResult.put(entry.getKey(), entry.getValue() / maxScore);
            }
        }

        return Helpers.sortedHashMap(normalizedResult, true);
    }
}
