/**
 * This file is part of Obelix.
 *
 * Obelix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Obelix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Obelix.  If not, see <http://www.gnu.org/licenses/>.
 */
package metrics;

import com.google.gson.JsonObject;
import graph.ItemGraph;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.interfaces.ObelixQueue;
import store.impl.InternalObelixStore;
import store.impl.ObelixStoreElement;
import store.impl.RedisObelixStore;
import store.interfaces.ObelixStore;
import graph.interfaces.GraphDatabase;

import java.util.HashMap;
import java.util.Map;

public class MetricsCollector implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class.getName());


    public static final int SLEEP_BETWEEN_COLLECTION_OF_METRICS = 300000;
    private final GraphDatabase graphDb;
    private final ObelixQueue redisQueueManager;
    private final ObelixQueue usersCacheQueue;
    private final ObelixStore storage;

    private Map<String, Integer> metrics = new HashMap<>();
    private Map<String, Integer> totalMetrics = new HashMap<>();

    public MetricsCollector(final boolean enableMetrics,
                            final GraphDatabase graphDb,
                            final ObelixQueue redisQueueManager,
                            final ObelixQueue usersCacheQueue, final String redisHost) {

        if (!enableMetrics) {
            this.storage = new InternalObelixStore();
            this.graphDb = graphDb;
            this.redisQueueManager = redisQueueManager;
            this.usersCacheQueue = usersCacheQueue;
        } else {

            this.storage = new RedisObelixStore(redisHost);
            this.graphDb = graphDb;
            this.redisQueueManager = redisQueueManager;
            this.usersCacheQueue = usersCacheQueue;
            loadStoredMetrics();
        }
    }

    private void loadStoredMetrics() {

        ObelixStoreElement obelixMetrics = this.storage.get("total_metrics");
        if (obelixMetrics != null && obelixMetrics.getData().has("recommendations_built")) {

            this.totalMetrics.put("recommendations_built",
                    obelixMetrics.getData().getInt("recommendations_built"));

            this.totalMetrics.put("feeded", obelixMetrics.getData().getInt("feeded"));
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

        this.metrics.forEach(jsonObject::put);
        this.totalMetrics.forEach((key, val) -> jsonObject.put("total_" + key, val));

        this.storage.set("metrics", new ObelixStoreElement(jsonObject));
    }

    public final void printMetrics() {
        JsonObject object = new JsonObject();


        this.metrics.forEach((key, val) -> {
                    if (this.totalMetrics.containsKey(key)) {
                        object.addProperty("total_" + key, val);
                    }
                    object.addProperty(key, val);
                }
        );

        saveMetrics();
        saveTotalMetrics();
        resetMetrics();

    }

    private void resetMetrics() {
        metrics.keySet().stream().filter(this.metrics::containsKey).forEach(key ->
                        this.metrics.put(key, 0)
        );
    }

    private synchronized void addTotalMetricValue(final String key, final int value) {
        if (!this.totalMetrics.containsKey(key)) {
            this.totalMetrics.put(key, 0);
        }
        this.totalMetrics.put(key, this.totalMetrics.get(key) + value);
    }

    public final synchronized void addAccumalativeMetricValue(final String key, final int value) {
        if (!this.metrics.containsKey(key)) {
            this.metrics.put(key, 0);
        }

        this.metrics.put(key, this.metrics.get(key) + value);
        this.addTotalMetricValue(key, value);
    }

    public final synchronized void addStaticMetricValue(final String key, final int value) {
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
        ItemGraph itemGraph = new ItemGraph(graphDb);

        addStaticMetricValue("all_users_count", graphDb.getAllUserIds().size());
        addStaticMetricValue("all_items_count", itemGraph.getAll().size());
        addStaticMetricValue("all_relationships_count", graphDb.getRelationships().size());
    }

    public final void run() {

        while (true) {
            addQueueStats();
            addGraphStats();
            this.printMetrics();

            try {
                Thread.sleep(SLEEP_BETWEEN_COLLECTION_OF_METRICS);
            } catch (InterruptedException e) {
                LOGGER.error("Stopped collecting metrics.");
                e.printStackTrace();
                break;
            }
        }
    }
}
