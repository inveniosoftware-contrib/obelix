package web;

import graph.ItemGraph;
import graph.exceptions.ObelixNodeNotFoundException;
import graph.interfaces.GraphDatabase;
import obelix.impl.ObelixRecommender;
import obelix.interfaces.Recommender;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.DynamicLabel;
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

    GraphDatabase graphDb;
    ItemGraph itemsGraph;
    Recommender obelixRecommender;
    Map<String, String> clientSettings;
    int webPort;
    String recommendationDepth;

    public ObelixWebServer(GraphDatabase graphdb, int webPort, String recommendationDepth,
                           Map<String, String> clientSettings)  {

        this.graphDb = graphdb;
        this.itemsGraph = new ItemGraph(graphdb);
        this.obelixRecommender = new ObelixRecommender(graphDb);
        this.webPort = webPort;
        this.recommendationDepth = recommendationDepth;
        this.clientSettings = clientSettings;
    }

    public void run() {

        try {

        port(this.webPort);
        ObelixWebAuth.enableCORS("*", "*", "*");
        ObelixWebAuth.requireValidToken();

        // All users, items, relationships

        get("/*/*/*/users/all", "application/json", (request, response) ->
                this.graphDb.getAllUserIds(), new JsonTransformer());

        get("/*/*/*/items/all", "application/json", (request, response) ->
                this.itemsGraph.getAll(), new JsonTransformer());

        get("/*/*/*/relationships/all", "application/json", (request, response) ->
                this.graphDb.getAllRelationships_(), new JsonTransformer());

        // clean the relationships for users
        get("/*/*/*/users/:userID/relationships/clean/:max", "application/json", (request, response) ->
                this.graphDb.cleanRelationships(request.params("userID"),
                        request.params("max")));

        get("/*/*/*/users/clean-all-relationships/:max", "application/json", (request, response) ->
                this.graphDb.cleanAllRelationships(request.params("max")), new JsonTransformer());

        // Recommendations
        get("/*/*/*/users/:userID/relationships/:depth", "application/json", (request, response) ->
                this.graphDb.getRelationships(request.params("userID"), request.params("depth"), null, null, false), new JsonTransformer());

        get("/*/*/*/users/:userID/all-relationships", "application/json", (request, response) ->
                this.graphDb.getRelationships(request.params("userID"), "4", null, null, false), new JsonTransformer());

        get("/*/*/*/users/:userID/relationships/:depth/:since/:until", "application/json", (request, response) ->
                this.graphDb.getRelationships(
                        request.params("userID"), request.params("depth"),
                        request.params("since"), request.params("until"), false), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend/:depth", "application/json", (request, response) ->
                this.obelixRecommender.recommend(request.params("userID"), request.params("depth")), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend", "application/json", (request, response) ->
                this.obelixRecommender.recommend(request.params("userID"), recommendationDepth), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend/:depth/:since/:until/:importanceFactor", "application/json", (request, response) ->
                this.obelixRecommender.recommend(
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


        } catch (Exception e) {
            LOGGER.error("ObelixWebServer Exception", e);
        }
    }

    private Map<String, String> settings() {
        return this.clientSettings;
    }
}
