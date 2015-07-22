package events;

import graph.exceptions.ObelixNodeNotFoundException;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NeoHelpers {

    private NeoHelpers() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoHelpers.class.getName());

    public enum RelTypes implements RelationshipType {
        VIEWED
    }

    public static String normalizedTimeStamp(final String timestamp) {
        Double timeStamp = Double.parseDouble(timestamp);
        String formatted = String.format("%.4f", timeStamp);
        String[] parts = formatted.split("\\.");
        return String.valueOf(parts[0]);
    }

    public static List<String> getAllNodes(final GraphDatabaseService graphDb,
                                           final String labelName) {

        List<String> nodeIds = new ArrayList<>();
        Label label = DynamicLabel.label(labelName);
        ResourceIterable<Node> nodes;

        try (Transaction tx = graphDb.beginTx()) {
            nodes = GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(label);

            for (Node node : nodes) {
                try {
                    nodeIds.add(node.getProperty("node_id").toString());
                } catch (Exception e) {
                    LOGGER.warn("Can't find a given node... skipping");
                }
            }

            tx.success();
        }

        return nodeIds;

    }


    public static Node getUserNode(final GraphDatabaseService graphDb,
                                   final String userID)
            throws ObelixNodeNotFoundException {

        IndexManager index = graphDb.index();
        Index<Node> users = index.forNodes("users");

        Node node = users.get("node_id", userID).getSingle();

        if (node == null) {
            throw new ObelixNodeNotFoundException();
        }

        return node;
    }

    public static Node getOrCreateUserNode(final GraphDatabaseService graphDb,
                                           final String userID) {

        IndexManager index = graphDb.index();
        Index<Node> usersIndex = index.forNodes("users");

        Node node = usersIndex.get("node_id", userID).getSingle();

        if (node == null) {
            node = graphDb.createNode(DynamicLabel.label("User"));
            node.setProperty("node_id", userID);
            usersIndex.add(node, "node_id", userID);
        }

        return node;

    }

    public static Node getOrCreateItemNode(final GraphDatabaseService graphDb,
                                           final String itemID) {

        IndexManager index = graphDb.index();
        Index<Node> usersIndex = index.forNodes("items");

        Node node = usersIndex.get("node_id", itemID).getSingle();

        if (node == null) {
            node = graphDb.createNode(DynamicLabel.label("Item"));
            node.setProperty("node_id", itemID);
            usersIndex.add(node, "node_id", itemID);
        }

        return node;

    }

    public static void makeSureUsersDontExceedItemLimit(final GraphDatabaseService graphDb,
                                                        final Node a,
                                                        final int maxRelationships) {


        // First we remove the duplicates, we only need to
        // iterate once because we sort them by timestamp (latest first).


        String currentUser = "";

        int duplicatesRemoved = 0;

        try (Transaction tx = graphDb.beginTx()) {

            currentUser = a.getProperty("node_id").toString();

            List<Relationship> relationships = new ArrayList<>();

            for (Relationship relationship : a.getRelationships()) {
                relationships.add(relationship);
            }

            List<String> seenAlready = new ArrayList<>();

            Collections.sort(relationships, new RelationshipComparator());

            for (Relationship rel : relationships) {
                String nodeID = rel.getEndNode().getProperty("node_id").toString();

                if (seenAlready.contains(nodeID)) {
                    duplicatesRemoved += 1;
                    rel.delete();
                } else {
                    seenAlready.add(nodeID);
                }

            }

            tx.success();
        }

        int oldRemoved = 0;

        // Then we make sure that the user does not hold more than *maxRelationships
        try (Transaction tx = graphDb.beginTx()) {

            List<Relationship> relationships = new ArrayList<>();

            for (Relationship relationship : a.getRelationships()) {
                relationships.add(relationship);
            }

            Collections.sort(relationships, new RelationshipComparator());

            int c = 0;
            for (Relationship rel : relationships) {

                if (c >= maxRelationships) {
                    oldRemoved += 1;
                    rel.delete();
                }

                c += 1;

            }

            tx.success();
        }

        if (duplicatesRemoved > 0 || oldRemoved > 0) {
            LOGGER.debug("Cleans up relationships for "
                    + currentUser + " deletes: "
                    + duplicatesRemoved + " duplicates and "
                    + oldRemoved + " old relationships");
        }

    }

}
