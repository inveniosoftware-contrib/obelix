package metrics;

import com.google.gson.JsonObject;
import graph.ItemGraph;
import graph.RelationGraph;
import graph.UserGraph;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import queue.interfaces.ObelixQueue;
import store.impl.InternalObelixStore;
import store.impl.ObelixStoreElement;
import store.impl.RedisObelixStore;
import store.interfaces.ObelixStore;

import java.util.HashMap;
import java.util.Map;

public class MetricsCollector implements Runnable {

    private final String metricsSaveLocation;
    private final GraphDatabaseService graphDb;
    private final ObelixQueue redisQueueManager;
    private final ObelixQueue usersCacheQueue;
    private final ObelixStore storage;

    private Map<String, Integer> metrics = new HashMap<>();
    private Map<String, Integer> totalMetrics = new HashMap<>();

    public MetricsCollector(boolean enableMetrics, String metricsSaveLocation,
                            GraphDatabaseService graphDb,
                            ObelixQueue redisQueueManager,
                            ObelixQueue usersCacheQueue) {

        if(!enableMetrics) {
            this.storage = new InternalObelixStore();
            this.metricsSaveLocation = metricsSaveLocation;
            this.graphDb = graphDb;
            this.redisQueueManager = redisQueueManager;
            this.usersCacheQueue = usersCacheQueue;
        }
        else {

            this.storage = new RedisObelixStore();
            this.metricsSaveLocation = metricsSaveLocation;
            this.graphDb = graphDb;
            this.redisQueueManager = redisQueueManager;
            this.usersCacheQueue = usersCacheQueue;
            loadStoredMetrics();
        }
    }

    private void loadStoredMetrics() {

        ObelixStoreElement obelixMetrics = this.storage.get("total_metrics");
        if (obelixMetrics != null) {

            if (obelixMetrics.data.has("recommendations_built")) {

                this.totalMetrics.put("recommendations_built",
                        obelixMetrics.data.getInt("recommendations_built"));

                this.totalMetrics.put("feeded",
                        obelixMetrics.data.getInt("feeded"));
            }
        }
    }

    private void saveTotalMetrics() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("recommendations_built", this.totalMetrics.get("recommendations_built"));
        jsonObject.put("feeded", this.totalMetrics.get("feeded"));
        this.storage.set("total_metrics", new ObelixStoreElement(jsonObject));
    }

    private void saveMetrics() {
        JSONObject jsonObject = new JSONObject();

        for(String key : this.metrics.keySet()) {
            jsonObject.put(key, this.metrics.get(key));
        }

        for(String key : this.totalMetrics.keySet()) {
            jsonObject.put("total_" + key, this.totalMetrics.get(key));
        }

        this.storage.set("metrics", new ObelixStoreElement(jsonObject));
    }

    public void printMetrics() {
        JsonObject object = new JsonObject();

        for (String key : this.metrics.keySet()) {
            if (this.totalMetrics.containsKey(key)) {
                object.addProperty("total_" + key, this.totalMetrics.get(key));
            }

            object.addProperty(key, this.metrics.get(key));
        }

        saveMetrics();
        saveTotalMetrics();
        resetMetrics();

    }

    private void resetMetrics() {
        for (String key : metrics.keySet()) {
            if (this.metrics.containsKey(key)) {
                this.metrics.put(key, 0);
            }
        }
    }

    private synchronized void addTotalMetricValue(String key, int value) {
        if (!this.totalMetrics.containsKey(key)) {
            this.totalMetrics.put(key, 0);
        }
        this.totalMetrics.put(key, this.totalMetrics.get(key) + value);
    }

    public synchronized void addAccumalativeMetricValue(String key, int value) {
        if (!this.metrics.containsKey(key)) {
            this.metrics.put(key, 0);
        }

        this.metrics.put(key, this.metrics.get(key) + value);
        this.addTotalMetricValue(key, value);
    }

    public synchronized void addStaticMetricValue(String key, int value) {
        if (!this.metrics.containsKey(key)) {
            this.metrics.put(key, 0);
        }

        this.metrics.put(key, value);

    }

    private synchronized void addQueueStats() {
        addStaticMetricValue("logentries_queue_size", redisQueueManager.getAll().size());
        addStaticMetricValue("cache_queue_size", usersCacheQueue.getAll().size());
    }

    private synchronized void addGraphStats() {
        UserGraph userGraph = new UserGraph(graphDb);
        RelationGraph relationGraph = new RelationGraph(graphDb);
        ItemGraph itemGraph = new ItemGraph(graphDb);

        addStaticMetricValue("all_users_count", userGraph.getAll().size());
        addStaticMetricValue("all_items_count", itemGraph.getAll().size());
        addStaticMetricValue("all_relationships_count", relationGraph.getAll().size());
    }

    @Override
    public void run() {

        while (true) {
            addQueueStats();
            addGraphStats();
            this.printMetrics();
            //this.resetMetrics();

            try {
                // wait for 5 seconds
                Thread.sleep(300000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
