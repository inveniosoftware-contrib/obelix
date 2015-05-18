package graph;


import graph.exceptions.ObelixNodeNotFoundException;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static events.NeoHelpers.getAllNodes;
import static events.NeoHelpers.getUserNode;
import static events.NeoHelpers.makeSureTheUserDoesNotExceedMaxRelationshipsLimit;

public class UserGraph {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserGraph.class.getName());

    class UserItemRelationship {

        String userid;
        String itemid;
        String relname;
        String timestamp;
        int depth;

        public double getImportanceFactor(int importanceFactorInteger) {
            return Math.max(0.0, 1.0 / (10 * depth * importanceFactorInteger + 1.0));
        }

        public UserItemRelationship(String userid, String itemid, String relname, String timestamp, int depth) {
            this.userid = userid;
            this.itemid = itemid;
            this.relname = relname;
            this.depth = depth;
            this.timestamp = timestamp;
        }

    }

    public static final Label LABEL = DynamicLabel.label("User");

    GraphDatabaseService graphdb;

    public UserGraph(GraphDatabaseService graphdb) {
        this.graphdb = graphdb;
    }

    public List<String> getAll() {
        return getAllNodes(this.graphdb, "User");
    }

    public String cleanAllRelationships(String max) throws ObelixNodeNotFoundException {

        for (String userid : getAll()) {
            LOGGER.info("Cleaning" + userid + " " + cleanRelationships(userid, max));
        }

        return "done";

    }

    public String cleanRelationships(String userid, String max) throws ObelixNodeNotFoundException {

        int relationshipCountBefore = 0;
        int relationshipCountAfter = 0;

        Node user;

        try (Transaction tx = graphdb.beginTx()) {

            user = getUserNode(graphdb, userid);

            for (Relationship rel : user.getRelationships()) {
                relationshipCountBefore += 1;
            }
            tx.success();
        }

        makeSureTheUserDoesNotExceedMaxRelationshipsLimit(graphdb, user, Integer.parseInt(max));

        try (Transaction tx = graphdb.beginTx()) {

            user = getUserNode(graphdb, userid);

            for (Relationship rel : user.getRelationships()) {
                relationshipCountAfter += 1;
            }
            tx.success();
        }
        return "Before: " + relationshipCountBefore + " - After: " + relationshipCountAfter;

    }

    public List<UserItemRelationship> allRelationships(String userID) throws Exception {
        return relationships(userID, "1", null, null, false);
    }

    public List<UserItemRelationship> relationships(String userID, String depth) throws Exception {
        return relationships(userID, depth, null, null, false);
    }

    public List<UserItemRelationship> relationships(String userID, String depth,
                                                    String sinceTimestamp, String untilTimestamp) throws ObelixNodeNotFoundException {

        return relationships(userID, depth, sinceTimestamp, untilTimestamp, false);

    }


    public List<UserItemRelationship> relationships(String userID, String depth,
                                                    String sinceTimestamp, String untilTimestamp,
                                                    boolean removeDuplicates)

            throws ObelixNodeNotFoundException {

        try (Transaction tx = graphdb.beginTx()) {

            List<UserItemRelationship> userItemRelationships = new LinkedList<>();
            Node user = getUserNode(graphdb, userID);

            for (Path path : graphdb.traversalDescription()
                    .breadthFirst()
                    .expand(new TimeStampExpander(sinceTimestamp, untilTimestamp, depth))
                    .evaluator(Evaluators.toDepth(Integer.parseInt(depth)))
                    .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
                    .traverse(user)) {

                if (path.lastRelationship() != null) {

                    String userid = path.lastRelationship().getStartNode().getProperty("node_id").toString();
                    String itemid = path.lastRelationship().getEndNode().getProperty("node_id").toString();
                    String timestamp = path.lastRelationship().getProperty("timestamp").toString();
                    String relname = userid + itemid + String.valueOf(timestamp);

                    userItemRelationships.add(
                            new UserItemRelationship(userid, itemid,
                            relname, timestamp, path.length()));

                }
            }

            tx.success();
            return userItemRelationships;
        }
    }

    public Map<String, Double> recommend(String userID,
                                         String depth) throws ObelixNodeNotFoundException {

        return recommend(userID, depth, null, null, null);
    }

    public Map<String, Double> recommend(String userID,
                                         String depth,
                                         String sinceTimestamp,
                                         String untilTimestamp,
                                         String importanceFactor) throws ObelixNodeNotFoundException {

        try (Transaction tx = graphdb.beginTx()) {

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
                    userItemRelationships = relationships(userID, depth, sinceTimestamp, untilTimestamp);

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

            int maxRecommendatins = 100;
            int recommendationsCount = 0;

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

            tx.success();
            return Helpers.sortedHashMap(normalizedResult, true);

        }
    }
}
