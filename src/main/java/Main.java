import obelix.ObelixBatchImport;
import obelix.ObelixCache;
import obelix.ObelixFeeder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import queue.impl.RedisObelixQueue;
import queue.interfaces.ObelixQueue;
import web.ObelixWebServer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public static void main(String... args) {

        LOGGER.log(Level.INFO, "HEIHEI");

        String neoLocation = "graph.db";
        String redisQueueName = "logentries";
        int maxRelationships = 30;
        int workers = 1;
        int webPort = 4500;

        int carg = 0;
        for (String arg : args) {
            if (arg.equals("--neo4jStore")) {
                neoLocation = args[carg + 1];
            }
            carg += 1;
        }

        carg = 0;
        for (String arg : args) {
            if (arg.equals("--max-relationships")) {
                maxRelationships = Integer.parseInt(args[carg + 1]);
            }
            carg += 1;
        }

        carg = 0;
        for (String arg : args) {
            if (arg.equals("--workers")) {
                workers = Integer.parseInt(args[carg + 1]);
            }
            carg += 1;
        }

        carg = 0;
        for (String arg : args) {
            if (arg.equals("--redis-queue-name")) {
                redisQueueName = args[carg + 1];
            }
            carg += 1;
        }

        carg = 0;
        for (String arg : args) {
            if (arg.equals("--web-port")) {
                webPort = Integer.parseInt(args[carg + 1]);
            }
            carg += 1;
        }

        boolean batchImportAll = false;

        carg = 0;
        for (String arg : args) {
            if (arg.equals("--batch-import-all")) {
                batchImportAll = true;
            }
            carg += 1;
        }
        boolean buildForAllUsersOnStartup = false;

        carg = 0;
        for (String arg : args) {
            if (arg.equals("--build-cache-for-all-users-on-startup")) {
                buildForAllUsersOnStartup = true;
            }
            carg += 1;
        }

        String recommendationDepth = "4";
        carg = 0;
        for (String arg : args) {
            if (arg.equals("--recommendation-depth")) {
                try {
                    int depth = Integer.parseInt(args[carg+1]);

                    if(depth<0 || depth > 10) {
                        throw new NumberFormatException();
                    }

                    recommendationDepth = args[carg+1];

                } catch (NumberFormatException e) {
                    System.err.println("Wrong format for --recommendation-depth option, use a number from 0-10");
                }
            }
            carg += 1;
        }

        LOGGER.log(Level.INFO, "Starting Obelix");
        LOGGER.log(Level.INFO, "all args: " + Arrays.toString(args));
        LOGGER.log(Level.INFO, "--neo4jStore: " + neoLocation);
        LOGGER.log(Level.INFO, "--max-relationships: " + maxRelationships);
        LOGGER.log(Level.INFO, "--workers: " + workers);
        LOGGER.log(Level.INFO, "--redis-queue-name: " + redisQueueName);
        LOGGER.log(Level.INFO, "--web-port: " + webPort);

        if(batchImportAll) {
            LOGGER.log(Level.INFO, "Starting batch import of all");
            ObelixBatchImport.run(neoLocation, redisQueueName);
            LOGGER.log(Level.INFO, "Done importing everything! woho!");
            System.exit(0);
        }

        GraphDatabaseService graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(neoLocation)
                .newGraphDatabase();

        registerShutdownHook(graphDb);

        ObelixQueue redisQueueManager = new RedisObelixQueue(redisQueueName);
        ObelixQueue usersCacheQueue= new RedisObelixQueue("cache::users");

        // Warm up neo4j cache
        /*
        try (Transaction tx = graphDb.beginTx()) {
            for (Node n : GlobalGraphOperations.at(graphDb).getAllNodes()) {
                n.getPropertyKeys();
                for (Relationship relationship : n.getRelationships()) {
                    Node start = relationship.getStartNode();
                }
            }
            tx.success();
            System.out.println("Neo4j is warmed up!");
        }*/

        (new Thread(new ObelixFeeder(graphDb, maxRelationships,
                redisQueueManager, usersCacheQueue, 1))).start();

        (new Thread(new ObelixWebServer(graphDb, webPort, recommendationDepth, clientSettings()))).start();

        (new Thread(new ObelixCache(graphDb,
                usersCacheQueue, buildForAllUsersOnStartup, recommendationDepth, maxRelationships,
                clientSettings()))).start();

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