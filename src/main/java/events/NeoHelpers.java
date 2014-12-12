package events;

import org.neo4j.graphdb.*;

public class NeoHelpers {

    static Label userLabel = DynamicLabel.label("User");
    static Label recordLabel = DynamicLabel.label("Record");

    public static enum RelTypes implements RelationshipType {
        SEEN,
        DOWNLOADED
    }

    public static Node getOrCreateUserNode(GraphDatabaseService graphDb, Long userID) {

        Node user;

        ResourceIterator<Node> list =
                graphDb.findNodesByLabelAndProperty(userLabel, "id_user", userID).iterator();

        if (list.hasNext()) {
            user = list.next();
        } else {
            user = graphDb.createNode();
            user.addLabel(userLabel);
            user.setProperty("id_user", userID);
        }

        return user;

    }

    public static Node getOrCreateRecordNode(GraphDatabaseService graphDb, Long recordID) {

        Node user;

        ResourceIterator<Node> list =
                graphDb.findNodesByLabelAndProperty(recordLabel, "id_bibrec", recordID).iterator();

        if (list.hasNext()) {
            user = list.next();
        } else {
            user = graphDb.createNode();
            user.addLabel(recordLabel);
            user.setProperty("id_bibrec", recordID);
        }

        return user;

    }


    public static Relationship getOrCreateRelationship(Node a, Node b,
                                                       String propertyKey, String property,
                                                       RelationshipType relType) {

        for (Relationship relationship : a.getRelationships()) {
            if (relationship.getEndNode().getProperty(propertyKey) == property) {
                return relationship;
            }
        }

        return a.createRelationshipTo(b, relType);

    }
}
