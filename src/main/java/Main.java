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
import graph.impl.NeoGraphDatabase;
import graph.interfaces.GraphDatabase;
import metrics.MetricsCollector;
import obelix.ObelixBatchImport;
import obelix.ObelixCache;
import obelix.ObelixFeeder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.impl.RedisObelixQueue;
import queue.interfaces.ObelixQueue;
import store.impl.RedisObelixStore;
import web.ObelixWebServer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Main {

    public static final int MAX_RELATIONSHIPS_DEFAULT = 30;
    public static final int NUMBER_OF_WORKERS_DEFAULT = 1;
    public static final int OBELIX_WEB_PORT_DEFAULT = 4500;
    public static final int NEO4J_WEB_PORT_DEFAULT = 7575;
    public static final int MAX_GRAPH_DEPTH_LIMIT = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());

    @Option(name = "--neo4jStore", usage = "Neo4j Datastore path")
    private String neoLocation = "graph.db";

    @Option(name = "--max-relationships", usage = "Set a maximum number of relationships"
            + "per node, this will remove the oldest when the limit is reached")
    private int maxRelationships = MAX_RELATIONSHIPS_DEFAULT;

    @Option(name = "--workers", usage = "Set the number of workers to read from the log queue")
    private int workers = NUMBER_OF_WORKERS_DEFAULT;

    @Option(name = "--enable-metrics", usage = "Enable metrics collection")
    private boolean enableMetrics = false;

    @Option(name = "--redis-host",
            usage = "Set the redis host")
    private String redisHost = "localhost";

    @Option(name = "--redis-queue-name",
            usage = "Set the name of the redis queue to look for new log entries")
    private String redisQueueName = "logentries";

    @Option(name = "--redis-queue-prefix", usage = "Set the redis queue prefix")
    private String redisQueuePrefix = "obelix:queue:";

    @Option(name = "--web-port", usage = "Set the http port for the Obelix HTTP API")
    private int webPort = OBELIX_WEB_PORT_DEFAULT;

    @Option(name = "--batch-import-all", usage = "Only Batch Import")
    private boolean batchImportAll = false;

    @Option(name = "--neo4j-webserver", usage = "Enable the Debug Neo4j web server")
    private boolean enableNeo4jWebServer = false;

    @Option(name = "--neo4j-webserver-port", usage = "Neo4j web server Port")
    private int configNeo4jWebPort = NEO4J_WEB_PORT_DEFAULT;

    @Option(name = "---build-cache-for-all-users-on-startup",
            usage = "Rebuild all recommendations")
    private boolean buildForAllUsersOnStartup = false;

    @Option(name = "--recommendation-depth", usage = "Set the recommendations depth, "
            + "this means how deep the graph will be traversed")
    private String recommendationDepth = "4";


    public static void main(final String... args) {
        LOGGER.warn("Restarting Obelix:main");
        new Main().runObelix(args);
    }

    private boolean parseArguments(final String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            System.err.println(e.getMessage());
            System.err.println("java Obelix [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            return false;
        }
        return true;
    }

    public void runObelix(final String[] args) {
        if (!parseArguments(args)) {
            return;
        }

        LOGGER.info("Starting Obelix");
        LOGGER.info("all args: " + Arrays.toString(args));
        LOGGER.info("--neo4jStore: " + neoLocation);
        LOGGER.info("--max-relationships: " + maxRelationships);
        LOGGER.info("--workers: " + workers);
        LOGGER.info("--redis-host: " + redisHost);
        LOGGER.info("--redis-queue-name: " + redisQueueName);
        LOGGER.info("--web-port: " + webPort);

        if (batchImportAll) {
            LOGGER.info("Starting batch import of all");
            ObelixBatchImport.run(neoLocation, redisQueueName, redisHost);
            LOGGER.info("Done importing everything! woho!");
            return;
        }

        GraphDatabase graphDb = new NeoGraphDatabase(neoLocation,
                enableNeo4jWebServer, configNeo4jWebPort);

        ObelixQueue redisQueueManager = new RedisObelixQueue(redisQueuePrefix,
                                                redisQueueName, redisHost);
        ObelixQueue usersCacheQueue = new RedisObelixQueue(redisQueuePrefix,
                                                "cache:users", redisHost);

        MetricsCollector metricsCollector = new MetricsCollector(
                enableMetrics, graphDb, redisQueueManager,
                usersCacheQueue, redisHost);

        if (enableMetrics) {
            new Thread(metricsCollector).start();
        }

        (new Thread(new ObelixFeeder(graphDb, metricsCollector, maxRelationships,
                redisQueueManager, usersCacheQueue, 1))).start();

        (new Thread(new ObelixWebServer(graphDb, webPort,
                recommendationDepth, clientSettings()))).start();

        (new Thread(new ObelixCache(graphDb, metricsCollector, usersCacheQueue,
                new RedisObelixStore(redisQueuePrefix),
                buildForAllUsersOnStartup, recommendationDepth, maxRelationships
        ))).start();
    }


    public static Map<String, String> clientSettings() {
        Map<String, String> result = new HashMap<>();
        result.put("redis_prefix", "obelix::");
        result.put("recommendations_impact", "0");
        result.put("method_switch_limit", "20");
        result.put("score_lower_limit", "0.20");
        result.put("score_upper_limit", "1");
        result.put("score_min_limit", "10");
        result.put("score_min_multiply", "4");
        result.put("score_one_result", "1.0");
        result.put("redis_timeout_recommendations_cache", "30");
        result.put("redis_timeout_search_result", "3000");
        return result;
    }
}
