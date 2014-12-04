import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;

public class WebInterface implements Runnable {

    GraphDatabaseService graphDb;

    public WebInterface(GraphDatabaseService graphdb) {
        this.graphDb = graphdb;
    }

    public List<String> getAllUsers() {

        Label userLabel = DynamicLabel.label("User");
        ResourceIterable<Node> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(userLabel);
            tx.success();

            List<String> nodes = new ArrayList<>();

            for(Node node : users) {
                String userId = (String) node.getProperty("id_user");
                nodes.add(userId);
            }

            System.out.println("Total users: " + nodes.size());

            return nodes;

        }

    }

    public List<String> getAllRecords() {

        Label recordLabel = DynamicLabel.label("Record");
        ResourceIterable<Node> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(recordLabel);
            tx.success();

            List<String> nodes = new ArrayList<>();

            for(Node node : users) {
                String userId = (String) node.getProperty("id_bibrec");
                nodes.add(userId);
            }

            System.out.println("Total records: " + nodes.size());

            return nodes;
        }
    }

    public Map<String, String> getAllRelationships() {

        Label recordLabel = DynamicLabel.label("Record");
        Iterable<Relationship> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllRelationships();
            tx.success();

            Map<String, String> relations = new HashMap<>();

            for(Relationship node : users) {
                relations.put(
                        (String)node.getStartNode().getProperty("id_user"),
                        (String)node.getEndNode().getProperty("id_bibrec"));
            }

            return relations;
        }
    }


    public void run() {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        get("/users", "application/json", (request, response) -> {
            return this.getAllUsers();
        }, new JsonTransformer());

        get("/records", "application/json", (request, response) -> {
            return this.getAllRecords();
        }, new JsonTransformer());

        get("/relations", "application/json", (request, response) -> {
            return this.getAllRelationships();
        }, new JsonTransformer());

    }

}
