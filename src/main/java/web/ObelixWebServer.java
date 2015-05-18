package web;

import graph.ItemGraph;
import graph.RelationGraph;
import graph.UserGraph;
import graph.exceptions.ObelixNodeNotFoundException;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import utils.JsonTransformer;

import java.util.Map;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.SparkBase.port;

public class ObelixWebServer implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ObelixWebServer.class.getName());

    public static final Label LABEL_ITEM = DynamicLabel.label("Item");
    public static final Label LABEL_USER = DynamicLabel.label("User");

    GraphDatabaseService graphDb;
    UserGraph userGraph;
    ItemGraph itemsGraph;
    RelationGraph relationGraph;
    Map<String, String> clientSettings;
    int webPort;
    String recommendationDepth;

    public ObelixWebServer(GraphDatabaseService graphdb, int webPort, String recommendationDepth,
                           Map<String, String> clientSettings)  {

        this.graphDb = graphdb;
        this.userGraph = new UserGraph(graphdb);
        this.itemsGraph = new ItemGraph(graphdb);
        this.relationGraph = new RelationGraph(graphdb);
        this.webPort = webPort;
        this.recommendationDepth = recommendationDepth;
        this.clientSettings = clientSettings;
    }

    public void run() {
        port(this.webPort);
        ObelixWebAuth.enableCORS("*", "*", "*");
        ObelixWebAuth.requireValidToken();

        // All users, items, relationships

        get("/*/*/*/users/all", "application/json", (request, response) ->
                this.userGraph.getAll(), new JsonTransformer());

        get("/*/*/*/items/all", "application/json", (request, response) ->
                this.itemsGraph.getAll(), new JsonTransformer());

        get("/*/*/*/relationships/all", "application/json", (request, response) ->
                this.relationGraph.getAll(), new JsonTransformer());

        // clean the relationships for users
        get("/*/*/*/users/:userID/relationships/clean/:max", "application/json", (request, response) ->
                this.userGraph.cleanRelationships(request.params("userID"),
                        request.params("max")));

        get("/*/*/*/users/clean-all-relationships/:max", "application/json", (request, response) ->
                this.userGraph.cleanAllRelationships(request.params("max")), new JsonTransformer());


        // Recommendations
        get("/*/*/*/users/:userID/relationships/:depth", "application/json", (request, response) ->
                this.userGraph.relationships(request.params("userID"), request.params("depth")), new JsonTransformer());

        get("/*/*/*/users/:userID/all-relationships", "application/json", (request, response) ->
                this.userGraph.allRelationships(request.params("userID")), new JsonTransformer());

        get("/*/*/*/users/:userID/relationships/:depth/:since/:until", "application/json", (request, response) ->
                this.userGraph.relationships(
                        request.params("userID"), request.params("depth"),
                        request.params("since"), request.params("until")), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend/:depth", "application/json", (request, response) ->
                this.userGraph.recommend(request.params("userID"), request.params("depth")), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend", "application/json", (request, response) ->
                this.userGraph.recommend(request.params("userID"), recommendationDepth), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend/:depth/:since/:until/:importanceFactor", "application/json", (request, response) ->
                this.userGraph.recommend(
                        request.params("userID"), request.params("depth"),
                        request.params("since"), request.params("until"),
                        request.params("importanceFactor")), new JsonTransformer());

        // Settings for search

        get("/*/*/*/settings", "application/json", (request, response) ->
                settings()
                , new JsonTransformer());


        exception(ObelixNodeNotFoundException.class, (e, request, response) -> {
            LOGGER.error(e.getMessage());
            response.status(404);
            response.body("User or item not found");
        });

        // Search result logger

        post("/*/*/*/log/search", (request, response) -> {
            try {
                new Jedis("localhost", 6379).lpush("obelix-server::log::search::result", new JSONObject(request.body()).toString());
                return "OK";
            } catch (JSONException e) {
                response.status(400);
                response.body("Bad request, we need json data");
                return response;
            }
        });

        post("/*/*/*/log/pageview", (request, response) -> {
            try {
                new Jedis("localhost", 6379).lpush("obelix-server::log::page::view", new JSONObject(request.body()).toString());
                return "OK";
            } catch (JSONException e) {
                response.status(400);
                response.body("Bad request, we need json data");
                return response;
            }
        });

    }

    private Map<String, String> settings() {
        return this.clientSettings;
    }

}
