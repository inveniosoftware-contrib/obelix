package graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class RelationGraph {

    private final static Logger LOGGER = LoggerFactory.getLogger(RelationGraph.class.getName());

    GraphDatabaseService graphdb;

    public RelationGraph(GraphDatabaseService graphdb) {
        this.graphdb = graphdb;
    }

    public List<String> getAll() {
        return getAll(null);
    }

    public List<String> getAll(String type) {

        try (Transaction tx = graphdb.beginTx()) {
            Iterable<Relationship> rel = GlobalGraphOperations.at(graphdb).getAllRelationships();

            List<String> relations_ids = new ArrayList<>();

            Long oldestTimestamp = null;
            Long newestTimestamp = null;

            for (Relationship relationship : rel) {

                try {
                    if (type != null && !relationship.getType().name().equals(type)) {
                        continue;
                    }

                    long relTimestamp = Long.parseLong(relationship.getProperty("timestamp").toString());

                    if (oldestTimestamp == null) {
                        oldestTimestamp = relTimestamp;
                    }

                    if (newestTimestamp == null) {
                        newestTimestamp = relTimestamp;
                    }

                    if (relTimestamp < oldestTimestamp) {
                        oldestTimestamp = relTimestamp;
                    }

                    if (relTimestamp > newestTimestamp) {
                        newestTimestamp = relTimestamp;
                    }

                    String key =
                            relationship.getProperty("timestamp") + ":" +
                                    relationship.getStartNode().getProperty("node_id").toString() + "-" +
                                    relationship.getEndNode().getProperty("node_id").toString();

                    relations_ids.add(key);

                } catch (NotFoundException e) {
                    LOGGER.error("Ignores one relationship, probably deleted..");
                }
            }

            //LOGGER.info("Newest timestamp: " + newestTimestamp);
            //LOGGER.info("Oldest timestamp: " + oldestTimestamp);

            tx.success();

            return relations_ids;
        }
    }

    /*
    public List<String> getAllRelationsForUserGivenLabel(String nodeLabel, String nodeID, String relType,
                                                         String sinceTimestamp, String untilTimestmap) {

        List<String> result = new ArrayList<>();

        try (Transaction tx = graphdb.beginTx()) {

            Node startNode = getOrCreateNode(graphdb, "users", "User").getOrCreate("node_id", nodeID);

            for (Node node : graphdb.traversalDescription()
                    .breadthFirst()
                    .evaluator(Evaluators.excludeStartPosition())
                    //.evaluator(new TimeStampFilterEvaluator(sinceTimestamp, untilTimestmap))
                    .evaluator(Evaluators.toDepth(1))
                    .traverse(startNode).nodes()) {

                result.add(node.getProperty("node_id").toString());

            }

            tx.success();

            return result;
        }
    }*/

/*
    public List<JsonObject> getAllRelationshipsTuples() {

        Iterable<Relationship> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllRelationships();
            tx.success();

            List<JsonObject> relations = new ArrayList<>();

            for (Relationship node : users) {

                JsonObject obj = new JsonObject();

                obj.addProperty("source", "user_" + node.getStartNode().getProperty("id_user"));
                obj.addProperty("target", "item_" + node.getEndNode().getProperty("id_bibrec"));

                relations.add(obj);
            }

            System.out.println("Total relations: " + relations.size());

            return relations;
        }
    }*/

}
