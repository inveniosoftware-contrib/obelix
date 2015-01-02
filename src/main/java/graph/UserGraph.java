package graph;


import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static events.NeoHelpers.getAllNodes;
import static events.NeoHelpers.getOrCreateUserNode;

public class UserGraph {

    class UserItemRelationship {

        String userid;
        String itemid;
        String relname;
        String timestamp;
        int depth;

        public double getImportanceFactor(int importanceFactorInteger) {
            return 1.0 / (depth * importanceFactorInteger + 1.0);
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

    public List<UserItemRelationship> relationships(String userID, String depth) {
        return relationships(userID, depth, null, null);
    }

    public List<UserItemRelationship> relationships(String userID, String depth, String sinceTimestamp, String untilTimestamp) {

        List<UserItemRelationship> userItemRelationships = new LinkedList<>();

        int currentDepth = 0;
        Label lastSeenLabel = null;

        try (Transaction tx = graphdb.beginTx()) {
            Node user = getOrCreateUserNode(graphdb, userID);

            Map<String, Boolean> added = new HashMap<>();

            for (Node node : graphdb.traversalDescription()
                    .breadthFirst()
                    .evaluator(Evaluators.toDepth(Integer.parseInt(depth)))
                    .evaluator(new TimeStampFilterEvaluator(sinceTimestamp, untilTimestamp))
                    .traverse(user).nodes()) {

                Label currentLabel = node.getLabels().iterator().next();

                if (lastSeenLabel == null) {
                    lastSeenLabel = currentLabel;
                }

                if (currentLabel.equals(UserGraph.LABEL) && lastSeenLabel.equals(ItemGraph.LABEL)) {
                    currentDepth += 1;
                }

                lastSeenLabel = currentLabel;

                if (node.hasLabel(UserGraph.LABEL)) {
                    for (Relationship rel : node.getRelationships()) {

                        long relTimestamp = Long.parseLong(rel.getProperty("timestamp").toString());

                        if (untilTimestamp != null) {
                            if (relTimestamp > Long.parseLong(untilTimestamp)) {
                                continue;
                            }
                        }

                        if (sinceTimestamp != null) {
                            if (relTimestamp < Long.parseLong(sinceTimestamp)) {
                                continue;
                            }
                        }

                        String id_user = node.getProperty("node_id").toString();
                        String id_bibrec = rel.getEndNode().getProperty("node_id").toString();


                        if (!added.containsKey(id_user + id_bibrec)) {
                            userItemRelationships.add(
                                    new UserItemRelationship(
                                            id_user, id_bibrec,
                                            rel.getType().name(),
                                            rel.getProperty("timestamp").toString(),
                                            currentDepth));
                            added.put(id_user + id_bibrec, true);
                        }
                    }
                }
            }

            tx.success();

            return userItemRelationships;
        }

    }

    public Map<String, Double> recommend(String userID,
                                         String depth) {

        return recommend(userID, depth, null, null, null);
    }

    public Map<String, Double> recommend(String userID,
                                         String depth,
                                         String sinceTimestamp,
                                         String untilTimestamp,
                                         String importanceFactor) {

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
                userItemRelationships = relationships(
                        userID, depth, sinceTimestamp, untilTimestamp);

            } catch (NotFoundException e) {
                errorFetchingRelationships = true;
                System.err.println("Relationships not found, we have to try again! (" + e.getMessage() + ")");
                System.err.println(userID + "-" + depth + "-" + sinceTimestamp + "-" + untilTimestamp + "-" + importanceFactor);
            }
        }

        if (errorFetchingRelationships) {
            System.err.println("Fetching relationships OK now for:");
            System.err.println(userID + "-" + depth + "-" + sinceTimestamp + "-" + untilTimestamp + "-" + importanceFactor);
        }

        double maxScore = 0.0;

        for (UserItemRelationship it : userItemRelationships) {
            result.putIfAbsent(it.itemid, 0.0);
            result.put(it.itemid, result.get(it.itemid) + it.getImportanceFactor(importanceFactorInteger));

            if (result.get(it.itemid) > maxScore) {
                maxScore = result.get(it.itemid);
            }
        }

        for (Map.Entry<String, Double> entry : result.entrySet()) {

            double normalizedScore = entry.getValue() / maxScore;

            // Threshold value for including the recommendation, at least 0.1 = 10%
            if(normalizedScore > 0.1) {
                normalizedResult.put(entry.getKey(), entry.getValue() / maxScore);
            }

        }

        return Helpers.sortedHashMap(normalizedResult, true);

    }


    /*
    public Map<String, Double> recommend(String userID,
                                         String depth,
                                         String sinceTimestamp,
                                         String untilTimestmap) {

        Map<String, Double> result = new HashMap<>();

        try (Transaction tx = graphdb.beginTx()) {
            Node user = getOrCreateUserNode(graphdb, userID);

            for (Node node : graphdb.traversalDescription()
                    .depthFirst()
                    .evaluator(Evaluators.toDepth(Integer.parseInt(depth)))
                            //.evaluator(Evaluators.excludeStartPosition())
                            //.evaluator(Evaluators.includeWhereLastRelationshipTypeIs(RelTypes.DOWNLOADED))
                    //.evaluator(new TimeStampFilterEvaluator(sinceTimestamp, untilTimestmap))
                    .traverse(user).nodes()) {

                if (node.hasLabel(UserGraph.LABEL)) {
                    for (Relationship rel : node.getRelationships()) {

                        String id_user = node.getProperty("node_id").toString();
                        String id_bibrec = rel.getEndNode().getProperty("node_id").toString();

                        result.putIfAbsent(id_bibrec, 0.0);
                        result.put(id_bibrec, result.get(id_bibrec) + 1.0);

                    }
                }
            }

            tx.success();

        }

        return Helpers.sortedHashMap(result, true);

    }*/

     /*
    public Map<String, Double> getRecommendedItemsForUser(String userID) {

        try (Transaction tx = graphdb.beginTx()) {
            System.out.println(userID);
            Node user = getOrCreateUserNode(graphdb, Long.parseLong(userID));

            Map<String, Double> result = new HashMap<>();

            List<Integer> downloads = new ArrayList<>();
            Set<Integer> users = new HashSet<>();

            int depth = 2;

            String output = "";
            for (Path position : graphdb.traversalDescription()
                    .breadthFirst()
                    .relationships(NeoHelpers.RelTypes.DOWNLOADED)
                            //.evaluator(Evaluators.fromDepth(3))
                    .evaluator(Evaluators.toDepth(depth))
                    .traverse(user)) {
                output += position + "\n";

                int c = 0;

                for (Node node : position.nodes()) {
                    c += 1;
                    if (node.hasLabel(LABEL_ITEM)) {
                        String key = node.getProperty("id_bibrec").toString();
                        result.putIfAbsent(key, 0.0);
                        result.put(key, result.get(key) + getLevelScore(position.length(), depth));

                    }

                    if (node.hasLabel(LABEL_USER)) {
                        users.add(Integer.parseInt(node.getProperty("id_user").toString()));
                    }
                }

                System.out.println(c);

            }

            double maxScore = 0.0;

            for (String key : result.keySet()) {
                result.put(key, Math.log(result.get(key)));
                if (result.get(key) > maxScore) {
                    maxScore = result.get(key);
                }
            }

            for (String key : result.keySet()) {
                result.put(key, result.get(key) / maxScore);
            }

            System.out.println("Max score: " + maxScore);
            System.out.println("Number of users: " + users.size());

            //Collections.sort(downloads);

            //System.out.println(downloads);

            //System.out.println(output);

            return result;

        }
    }

    */


     /*
    public boolean cleanRelationshipForAllUsers() {
        try (Transaction tx = graphdb.beginTx()) {

            ResourceIterable<Node> users = GlobalGraphOperations.at(graphdb).getAllNodesWithLabel(LABEL_USER);
            tx.success();

            for(Node user : users) {
                deletedAndMadeSpaceForNewRelationship(user, Long.parseLong("0"));
            }

        }

        return true;
    }

    public boolean cleanRelationshipForUser(String userid) {

        try (Transaction tx = graphdb.beginTx()) {

            Node user = getOrCreateUserNode(this.graphdb, Long.parseLong(userid));
            tx.success();

            int count = 0;

            for(Relationship rel : user.getRelationships()) {
                count += 1;
            }

            deletedAndMadeSpaceForNewRelationship(user, Long.parseLong("1418776946000"));

            System.out.println(count);

        }

        return true;

    }*/

}
