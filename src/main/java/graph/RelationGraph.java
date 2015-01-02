package graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.List;

import static events.NeoHelpers.getOrCreateNode;


public class RelationGraph {

    GraphDatabaseService graphdb;

    public RelationGraph(GraphDatabaseService graphdb) {
        this.graphdb = graphdb;
    }

    public List<String> getAllRelationsForUserGivenLabel(String nodeLabel, String nodeID, String relType,
                                                         String sinceTimestamp, String untilTimestmap) {

        List<String> result = new ArrayList<>();

        try (Transaction tx = graphdb.beginTx()) {
            Node startNode = getOrCreateNode(graphdb, nodeLabel, nodeID);
            tx.success();

            for (Node node : graphdb.traversalDescription()
                    .breadthFirst()
                    .evaluator(Evaluators.excludeStartPosition())
                    //.evaluator(new TimeStampFilterEvaluator(sinceTimestamp, untilTimestmap))
                    .evaluator(Evaluators.toDepth(1))
                    .traverse(startNode).nodes()) {

                result.add(node.getProperty("node_id").toString());

            }

            return result;
        }
    }

    public List<String> getAll() {
        return getAll(null);
    }

    public List<String> getAll(String type) {

        try (Transaction tx = graphdb.beginTx()) {
            Iterable<Relationship> rel = GlobalGraphOperations.at(graphdb).getAllRelationships();

            List<String> relations_ids = new ArrayList<>();

            tx.success();

            Long oldestTimestamp = null;
            Long newestTimestamp = null;

            for (Relationship node : rel) {
                if (type != null && !node.getType().name().equals(type)) {
                    continue;
                }

                long relTimestamp = Long.parseLong(node.getProperty("timestamp").toString());

                if(oldestTimestamp == null) {
                    oldestTimestamp = relTimestamp;
                }

                if(newestTimestamp == null) {
                    newestTimestamp = relTimestamp;
                }

                if(relTimestamp < oldestTimestamp) {
                    oldestTimestamp = relTimestamp;
                }

                if(relTimestamp > newestTimestamp) {
                    newestTimestamp = relTimestamp;
                }

                String key =
                        node.getProperty("timestamp") + ":" +
                        node.getStartNode().getProperty("node_id").toString() +"-" +
                        node.getEndNode().getProperty("node_id").toString();

                relations_ids.add(key);

                /*relations.put(
                        node.getStartNode().getProperty("node_id").toString(),
                        node.getEndNode().getProperty("node_id").toString());
                */
            }

            System.out.println("Newest timestamp: " + newestTimestamp);
            System.out.println("Oldest timestamp: " + oldestTimestamp);

            return relations_ids;
        }
    }


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
