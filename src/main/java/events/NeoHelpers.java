package events;

import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NeoHelpers {

    public static enum RelTypes implements RelationshipType {
        SEEN,
        VIEWED,
        DOWNLOADED
    }

    public static List<String> getAllNodes(GraphDatabaseService graphDb, String labelName) {

        List<String> node_ids = new ArrayList<>();
        Label label = DynamicLabel.label(labelName);
        ResourceIterable<Node> nodes;

        try (Transaction tx = graphDb.beginTx()) {
            nodes = GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(label);
            tx.success();

            for (Node node : nodes) {
                node_ids.add(node.getProperty("node_id").toString());
            }

        }

        return node_ids;

    }

    public static Node getOrCreateNode(GraphDatabaseService graphDb,
                                       String labelName,
                                       String keyPropertyValue) {

        Node node;
        Label label = DynamicLabel.label(labelName);

        ResourceIterator<Node> list = graphDb.findNodesByLabelAndProperty(
                label, "node_id", keyPropertyValue).iterator();

        if (list.hasNext()) {
            node = list.next();
        } else {
            node = graphDb.createNode();
            node.addLabel(label);
            node.setProperty("node_id", keyPropertyValue);
        }

        return node;
    }

    public static Node getOrCreateUserNode(GraphDatabaseService graphDb, String userID) {
        return getOrCreateNode(graphDb, "User", userID);
    }

    public static Node getOrCreateItemNode(GraphDatabaseService graphDb, String ItemID) {
        return getOrCreateNode(graphDb, "Item", ItemID);
    }

    public static boolean deletedAndMadeSpaceForNewRelationship(Node a, Long newTimestamp, int maxRelationships) {

        Long currentRelationshipTimestamp;

        List<Long> highestTimestamps = new ArrayList<>();

        for (Relationship relationship : a.getRelationships()) {
            currentRelationshipTimestamp = Long.parseLong(relationship.getProperty("timestamp").toString());
            highestTimestamps.add(currentRelationshipTimestamp);
        }

        if (highestTimestamps.size() < maxRelationships) {
            return true;
        }

        Collections.sort(highestTimestamps);

        int countDelete = 0;
        Long timestamp = null;

        for (Long t : highestTimestamps) {
            if ((highestTimestamps.size() + 1 - countDelete) <= maxRelationships) {
                //System.out.println("count delete: " + countDelete);
                break;
            }
            countDelete += 1;
            timestamp = t;
        }

        //System.out.println(newTimestamp + " limit: " + timestamp);

        if (timestamp != null && newTimestamp > timestamp) {
            timestamp = highestTimestamps.get(countDelete);
        }


        int deletedRelationships = 0;

        if (timestamp != null) {
            for (Relationship relationship : a.getRelationships()) {
                //System.out.println("looking at " + relationship.getProperty("timestamp") + " which need to be larger than: " + timestamp);
                currentRelationshipTimestamp = Long.parseLong(relationship.getProperty("timestamp").toString());

                if (currentRelationshipTimestamp <= timestamp) {
                    System.out.println("Deleting relationship with timestamp" + currentRelationshipTimestamp + ", the newTimestamp which caused this: " + newTimestamp);
                    relationship.delete();
                    deletedRelationships += 1;

                    if (deletedRelationships > countDelete) {
                        break;
                    }

                }
            }
        } else {
            return true;
        }

        return newTimestamp > timestamp;

    }

    public static void createRealationship(Node user, Node Item,
                                           Long timestamp,
                                           RelationshipType relType,
                                           int maxRelationships) {

        boolean duplicate = false;
        boolean newTimestampMoreRecentThanOneOfTheCurrentRelationhips = false;

        int countRelationships = 0;

        for (Relationship relationship : user.getRelationships()) {
            countRelationships += 1;

            //Lets see if the relationship exists already to avoid duplicates..
            if (relationship.getEndNode().equals(Item)) {
                //Update the timestamp of the relationship if the timestamp is more recent
                if (timestamp > Long.parseLong(relationship.getProperty("timestamp").toString())) {
                    relationship.setProperty("timestamp", timestamp);
                }
                duplicate = true;
            } else if (timestamp > Long.parseLong(relationship.getProperty("timestamp").toString())) {
                newTimestampMoreRecentThanOneOfTheCurrentRelationhips = true;
            }
        }

        if (countRelationships < maxRelationships) {

            Relationship r = user.createRelationshipTo(Item, relType);
            r.setProperty("timestamp", timestamp);

        } else {
            if (newTimestampMoreRecentThanOneOfTheCurrentRelationhips
                    && !duplicate
                    && deletedAndMadeSpaceForNewRelationship(user, timestamp, maxRelationships)
                    ) {

                Relationship r = user.createRelationshipTo(Item, relType);
                r.setProperty("timestamp", timestamp);

            }
        }
    }
}
