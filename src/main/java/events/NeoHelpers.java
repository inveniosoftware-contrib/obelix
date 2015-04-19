package events;

import graph.exceptions.ObelixNodeNotFoundException;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NeoHelpers {

    private final static Logger LOGGER = Logger.getLogger(NeoHelpers.class.getName());

    public static enum RelTypes implements RelationshipType {
        VIEWED,
        DOWNLOADED
    }

    public static String normalizedTimeStamp(String timestamp) {
        Double timeStamp = Double.parseDouble(timestamp);
        String formatted = String.format("%.4f", timeStamp);
        String[] parts = formatted.split("\\.");
        return String.valueOf(parts[0]);
    }

    public static List<String> getAllNodes(GraphDatabaseService graphDb, String labelName) {

        List<String> node_ids = new ArrayList<>();
        Label label = DynamicLabel.label(labelName);
        ResourceIterable<Node> nodes;

        try (Transaction tx = graphDb.beginTx()) {
            nodes = GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(label);

            for (Node node : nodes) {
                try {
                    node_ids.add(node.getProperty("node_id").toString());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,"Can't find a given node... skipping");
                }
            }

            tx.success();
        }

        return node_ids;

    }

    public static UniqueFactory.UniqueNodeFactory getOrCreateNode(GraphDatabaseService graphDb,
                                                                  String index,
                                                                  String labelName) {

        try (Transaction tx = graphDb.beginTx()) {
            UniqueFactory.UniqueNodeFactory result = new UniqueFactory.UniqueNodeFactory(graphDb, index) {
                @Override
                protected void initialize(Node created, Map<String, Object> properties) {
                    created.addLabel(DynamicLabel.label(labelName));
                    created.setProperty("node_id", properties.get("node_id"));
                }
            };
            tx.success();
            return result;
        }
    }

    public static Node getUserNode(GraphDatabaseService graphDb, String userID) throws ObelixNodeNotFoundException {
        IndexManager index = graphDb.index();
        Index<Node> users = index.forNodes("users");

        Node node = users.get("node_id", userID).getSingle();

        if (node == null) {
            throw new ObelixNodeNotFoundException();
        }

        return node;
    }

    public static Node getOrCreateUserNode(GraphDatabaseService graphDb, String userID) {

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

    public static Node getOrCreateItemNode(GraphDatabaseService graphDb, String itemID) {

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

    public static Node getItemNode(GraphDatabaseService graphDb, String itemID) throws ObelixNodeNotFoundException {
        IndexManager index = graphDb.index();
        Index<Node> users = index.forNodes("items");

        Node node = users.get("node_id", itemID).getSingle();

        if (node == null) {
            throw new ObelixNodeNotFoundException();
        }

        return users.get("node_id", itemID).getSingle();
    }


    public static void makeSureTheUserDoesNotExceedMaxRelationshipsLimit(GraphDatabaseService graphDb,
                                                                         Node a, int maxRelationships) {


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

            Collections.sort(relationships, new RelationshipComperator());

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

            Collections.sort(relationships, new RelationshipComperator());

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

        if(duplicatesRemoved > 0 || oldRemoved > 0) {
            LOGGER.log(Level.FINE,"Cleans up relationships for "
                            + currentUser + " deletes: "
                            + duplicatesRemoved + " duplicates and "
                            + oldRemoved + " old relationships");

        }

    }

    public static void createRealationship(GraphDatabaseService graphdb,
                                           Node user,
                                           Node item,
                                           String timestamp,
                                           RelationshipType relType,
                                           int maxRelationships) {

        if (timestamp == null) {
            return;
        }

        Relationship r = user.createRelationshipTo(item, relType);
        r.setProperty("timestamp", timestamp);

        makeSureTheUserDoesNotExceedMaxRelationshipsLimit(graphdb, user, maxRelationships);

    }
}
