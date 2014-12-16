import com.google.gson.JsonObject;
import events.NeoHelpers;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.tooling.GlobalGraphOperations;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.before;
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
                int c = 0;
                for (Relationship n : node.getRelationships()) {
                    c += 1;
                }
                System.out.println("User: " + node.getProperty("id_user") + " has read " + c + " docments");
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

    public List<JsonObject> getAllRelationshipsTuples() {

        Iterable<Relationship> users;

        try (Transaction tx = graphDb.beginTx()) {
            users = GlobalGraphOperations.at(graphDb).getAllRelationships();
            tx.success();

            List<JsonObject> relations = new ArrayList<>();

            for (Relationship node : users) {

                JsonObject obj = new JsonObject();

                obj.addProperty("source", "user_" + node.getStartNode().getProperty("id_user"));
                obj.addProperty("target", "record_" + node.getEndNode().getProperty("id_bibrec"));

                relations.add(obj);
            }

            System.out.println("Total relations: " + relations.size());

            return relations;
        }
    }

    public Map<Long, Long> getAllRelationships() {
        return getAllRelationships(null);
    }

    private double getLevelScore(int currentDepth, int maxDepth) {
        return 1 + (maxDepth / (currentDepth + 1));
    }

    public List<JsonObject> userRelationships(String userID, String depth) {
        //        {source: "Microsoft", target: "Amazon", type: "licensing"},

        int count = 0;

        try (Transaction tx = graphDb.beginTx()) {
            System.out.println(userID);
            Node user = NeoHelpers.getOrCreateUserNode(graphDb, Long.parseLong(userID));

            List<JsonObject> result = new ArrayList<>();

            Map<String, ArrayList<String>> user_records = new HashMap<>();
            Map<String, ArrayList<String>> record_users = new HashMap<>();

            Map<String, Boolean> added = new HashMap<>();

            String output = "";
            for (Path position : graphDb.traversalDescription()
                    .depthFirst()
                    .relationships(NeoHelpers.RelTypes.DOWNLOADED)
                            //.evaluator(Evaluators.fromDepth(3))
                    .evaluator(Evaluators.toDepth(Integer.parseInt(depth)))
                    .traverse(user)) {
                output += position + "\n";

                int c = 0;

                for (Relationship rel : position.relationships()) {


                    String id_user = rel.getStartNode().getProperty("id_user").toString();
                    String id_bibrec = rel.getEndNode().getProperty("id_bibrec").toString();

                    if (!user_records.containsKey(id_user)) {
                        user_records.put(id_user, new ArrayList<>());
                    }

                    if (!user_records.get(id_user).contains(id_bibrec)) {
                        user_records.get(id_user).add(id_bibrec);
                    }

                    if (!record_users.containsKey(id_bibrec)) {
                        record_users.put(id_bibrec, new ArrayList<>());
                    }

                    if (!record_users.get(id_bibrec).contains(id_user)) {
                        record_users.get(id_bibrec).add(id_user);
                    }

                }

                for (Map.Entry<String, ArrayList<String>> entry : user_records.entrySet()) {

                    if (entry.getValue().size() > 0) {

                        for (String recid : entry.getValue()) {

                            if (record_users.get(recid).size() > 0) {

                                /*if (added.containsKey(entry.getKey()+recid)) {
                                    continue;
                                }*/

                                count += 1;

                                JsonObject obj = new JsonObject();

                                obj.addProperty("source", "user_" + entry.getKey());
                                obj.addProperty("target", "record_" + recid);

                                result.add(obj);

                                added.put(entry.getKey()+recid, true);

                            }
                        }
                    }
                }
            }

            System.out.println(count);


            return result;

        }

    }

    public Map<String, Double> getRecommendedRecordsForUser(String userID) {

        try (Transaction tx = graphDb.beginTx()) {
            System.out.println(userID);
            Node user = NeoHelpers.getOrCreateUserNode(graphDb, Long.parseLong(userID));

            Map<String, Double> result = new HashMap<>();

            List<Integer> downloads = new ArrayList<>();
            Set<Integer> users = new HashSet<>();

            int depth = 2;

            String output = "";
            for (Path position : graphDb.traversalDescription()
                    .breadthFirst()
                    .relationships(NeoHelpers.RelTypes.DOWNLOADED)
                            //.evaluator(Evaluators.fromDepth(3))
                    .evaluator(Evaluators.toDepth(depth))
                    .traverse(user)) {
                output += position + "\n";

                int c = 0;

                for (Node node : position.nodes()) {
                    c += 1;
                    if (node.hasLabel(RECORDLABEL)) {
                        String key = node.getProperty("id_bibrec").toString();
                        result.putIfAbsent(key, 0.0);
                        result.put(key, result.get(key) + getLevelScore(position.length(), depth));

                    }

                    if (node.hasLabel(USERLABEL)) {
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

    private static void enableCORS(final String origin, final String methods, final String headers) {
        before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.header("Access-Control-Allow-Origin", origin);
                response.header("Access-Control-Request-Method", methods);
                response.header("Access-Control-Allow-Headers", headers);
            }
        });
    }

    public void run() {

        enableCORS("*", "*", "*");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        get("/users", "application/json", (request, response) ->
                this.getAllUsers(), new JsonTransformer());

        get("/recommended/:userid", "application/json", (request, response) ->
                this.getRecommendedRecordsForUser(request.params(":userid")), new JsonTransformer());

        get("/userrelations/:userid/:depth", "application/json", (request, response) ->
                this.userRelationships(request.params(":userid"), request.params(":depth")), new JsonTransformer());

        get("/records", "application/json", (request, response) ->
                this.getAllRecords(), new JsonTransformer());

        get("/relations", "application/json", (request, response) ->
                this.getAllRelationships(), new JsonTransformer());

        get("/relations-tuples/", "application/json", (request, response) ->
                this.getAllRelationshipsTuples(), new JsonTransformer());

        get("/relations/:type", "application/json", (request, response) ->
                this.getAllRelationships(request.params(":type")), new JsonTransformer());

    }

}
