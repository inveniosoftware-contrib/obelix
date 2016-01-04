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
    private static final Logger LOGGER = LoggerFactory.getLogger(ObelixRecommender.class.getName());
    public static final double INCLUDE_RECOMMENDATION_TRESHOLD = 0.15;
    private GraphDatabase graphdb;

    public ObelixRecommender(final GraphDatabase graphdbInput) {
        this.graphdb = graphdbInput;
    }

    @Override
    public final Map<String, Double> recommend(final String userID)
            throws ObelixNodeNotFoundException {
        return recommend(userID, "3", null, null, null);
    }

    @Override
    public final Map<String, Double> recommend(final String userID, final String depth)
            throws ObelixNodeNotFoundException {

        return recommend(userID, depth, null, null, null);
    }

    @Override
    public final Map<String, Double> recommend(final String userID,
                                               final String depth,
                                               final String sinceTimestamp,
                                               final String untilTimestamp,
                                               final String importanceFactor)
            throws ObelixNodeNotFoundException {

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
                userItemRelationships = graphdb.getRelationships(
                        userID, depth, sinceTimestamp, untilTimestamp, false);

            } catch (NotFoundException e) {
                errorFetchingRelationships = true;
                LOGGER.error("Relationships not found, we have to try again! ("
                        + e.getMessage() + ")");

                LOGGER.error(userID + "-" + depth + "-" + sinceTimestamp
                        + "-" + untilTimestamp + "-" + importanceFactor);
            }
        }

        if (errorFetchingRelationships) {
            LOGGER.error("Fetching relationships OK now for:");
            LOGGER.error(userID + "-" + depth + "-" + sinceTimestamp
                    + "-" + untilTimestamp + "-" + importanceFactor);
        }

        double maxScore = 0.0;
        double score;

        for (UserItemRelationship it : userItemRelationships) {
            String itemID = it.getItemid();
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

            if (normalizedScore > INCLUDE_RECOMMENDATION_TRESHOLD) {
                normalizedResult.put(entry.getKey(), entry.getValue() / maxScore);
            }
        }

        return Helpers.sortedHashMap(normalizedResult, true);
    }
}
