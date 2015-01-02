import graph.ItemGraph;
import graph.RelationGraph;
import graph.UserGraph;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;

import static spark.Spark.get;
import static spark.Spark.port;

public class WebInterface implements Runnable {

    public static final Label LABEL_ITEM = DynamicLabel.label("Item");
    public static final Label LABEL_USER = DynamicLabel.label("User");

    GraphDatabaseService graphDb;
    UserGraph userGraph;
    ItemGraph itemsGraph;
    RelationGraph relationGraph;
    int webPort;

    public WebInterface(GraphDatabaseService graphdb, int webPort) {
        this.graphDb = graphdb;
        this.userGraph = new UserGraph(graphdb);
        this.itemsGraph = new ItemGraph(graphdb);
        this.relationGraph = new RelationGraph(graphdb);
        this.webPort = webPort;
    }

    public void run() {
        port(this.webPort);
        WebInterfaceAuth.enableCORS("*", "*", "*");
        WebInterfaceAuth.requireValidToken();

        // All users, items, relationships

        get("/*/*/*/users/all", "application/json", (request, response) ->
                this.userGraph.getAll(), new JsonTransformer());

        get("/*/*/*/items/all", "application/json", (request, response) ->
                this.itemsGraph.getAll(), new JsonTransformer());

        get("/*/*/*/relationships/all", "application/json", (request, response) ->
                this.relationGraph.getAll(), new JsonTransformer());

        // Recommendations

        get("/*/*/*/users/:userID/relationships/:depth", "application/json", (request, response) ->
                this.userGraph.relationships(request.params("userID"), request.params("depth")), new JsonTransformer());

        get("/*/*/*/users/:userID/relationships/:depth/:since/:until", "application/json", (request, response) ->
                this.userGraph.relationships(
                        request.params("userID"), request.params("depth"),
                        request.params("since"), request.params("until")), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend/:depth", "application/json", (request, response) ->
                this.userGraph.recommend(request.params("userID"), request.params("depth")), new JsonTransformer());

        get("/*/*/*/users/:userID/recommend/:depth/:since/:until/:importanceFactor", "application/json", (request, response) ->
                this.userGraph.recommend(
                        request.params("userID"), request.params("depth"),
                        request.params("since"), request.params("until"),
                        request.params("importanceFactor")), new JsonTransformer());



    }
}