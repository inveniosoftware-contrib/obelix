import events.NeoHelpers;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.*;

import static spark.Spark.get;

public class WebInterface implements Runnable {

    public static final Label RECORDLABEL = DynamicLabel.label("Record");
    public static final Label USERLABEL = DynamicLabel.label("User");
    GraphDatabaseService graphDb;

    public WebInterface(GraphDatabaseService graphdb) {
        this.graphDb = graphdb;
    }

    public List<Long> getAllUsers() {

        Label userLabel = DynamicLabel.label("User");
        ResourceIterable<Node> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(userLabel);
            tx.success();

            List<Long> nodes = new ArrayList<>();

            for (Node node : users) {
                nodes.add((long) node.getProperty("id_user"));
            }

            System.out.println("Total users: " + nodes.size());

            return nodes;

        }

    }

    public List<Long> getAllRecords() {

        Label recordLabel = DynamicLabel.label("Record");
        ResourceIterable<Node> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(recordLabel);
            tx.success();

            List<Long> nodes = new ArrayList<>();

            for (Node node : users) {
                long userId = (long) node.getProperty("id_bibrec");
                nodes.add(userId);
            }

            System.out.println("Total records: " + nodes.size());

            return nodes;
        }
    }

    public Map<Long, Long> getAllRelationships(String type) {

        Label recordLabel = DynamicLabel.label("Record");
        Iterable<Relationship> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllRelationships();
            tx.success();

            Map<Long, Long> relations = new HashMap<>();

            for (Relationship node : users) {
                if (type != null && !type.equals(node.getType().name())) {
                    continue;
                }
                relations.put(
                        (long) node.getStartNode().getProperty("id_user"),
                        (long) node.getEndNode().getProperty("id_bibrec"));

            }

            System.out.println("Total relations: " + relations.size());


            return relations;
        }
    }

    public Map<Long, Long> getAllRelationships() {
        return getAllRelationships(null);
    }

    private double getLevelScore(int currentDepth, int maxDepth) {
        return 1+(maxDepth/(currentDepth+1));
    }

    public Map<String, Double> getRecommendedRecordsForUser(String userID) {

        try (Transaction tx = graphDb.beginTx()) {
            System.out.println(userID);
            Node user = NeoHelpers.getOrCreateUserNode(graphDb, Long.parseLong(userID));

            Map<String, Double> result = new HashMap<>();

            List<Integer> downloads = new ArrayList<>();
            Set<Integer> users = new HashSet<>();

            int depth = 4;

            String output = "";
            for (Path position : graphDb.traversalDescription()
                    .breadthFirst()
                    .relationships(NeoHelpers.RelTypes.DOWNLOADED)
                    //.evaluator(Evaluators.fromDepth(3))
                    .evaluator(Evaluators.toDepth(depth))
                    .traverse(user)) {
                output += position + "\n";

                for(Node node : position.nodes()) {

                    if(node.hasLabel(RECORDLABEL)) {
                        String key = node.getProperty("id_bibrec").toString();
                        result.putIfAbsent(key, 0.0);
                        result.put(key, result.get(key) + getLevelScore(position.length(), depth));

                    }

                    if(node.hasLabel(USERLABEL)) {
                        users.add(Integer.parseInt(node.getProperty("id_user").toString()));
                    }

                }
            }

            double maxScore = 0.0;

            for(String key : result.keySet()) {
                result.put(key, Math.log(result.get(key)));
                if(result.get(key) > maxScore) {
                    maxScore = result.get(key);
                }
            }

            for(String key : result.keySet()) {
                result.put(key, result.get(key)/maxScore);
            }

            System.out.println("Max score: " + maxScore);
            System.out.println(result);
            System.out.println(users);
            System.out.println("Number of users: " + users.size());

            //Collections.sort(downloads);

            //System.out.println(downloads);

            //System.out.println(output);

            return result;

        }
    }

    public void run() {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        get("/users", "application/json", (request, response) ->
                this.getAllUsers(), new JsonTransformer());

        get("/recommended/:userid", "application/json", (request, response) ->
                this.getRecommendedRecordsForUser(request.params(":userid")), new JsonTransformer());

        get("/records", "application/json", (request, response) ->
                this.getAllRecords(), new JsonTransformer());

        get("/relations", "application/json", (request, response) ->
                this.getAllRelationships(), new JsonTransformer());

        get("/relations/:type", "application/json", (request, response) ->
                this.getAllRelationships(request.params(":type")), new JsonTransformer());

    }

}
