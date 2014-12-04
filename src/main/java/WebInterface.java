import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.List;

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

            return nodes;
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

    }

}
