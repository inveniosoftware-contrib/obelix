import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.util.Arrays;

public class Main {

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public static void main(String... args) {

        String neoLocation = "graph.db";
        String redisQueueName = "logentries";
        int maxRelationships = 30;
        int workers = 1;
        int webPort = 4500;

        int carg = 0;
        for (String arg : args) {
            if (arg.equals("--neo4jstore")) {
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

        System.out.println("Starting Obelix");
        System.out.println("all args: " + Arrays.toString(args));
        System.out.println("--neo4jstore: " + neoLocation);
        System.out.println("--max-relationships: " + maxRelationships);
        System.out.println("--workers: " + workers);
        System.out.println("--redis-queue-name: " + redisQueueName);
        System.out.println("--web-port: " + webPort);

        GraphDatabaseService graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(neoLocation)
                .newGraphDatabase();

        registerShutdownHook(graphDb);

        RedisQueueManager redisQueueManager = new RedisQueueManager(redisQueueName);

        for(int i=1; i<=workers;i++) {
            (new Thread(new NeoFeeder(graphDb, maxRelationships, redisQueueManager, i))).start();
        }

        (new Thread(new WebInterface(graphDb, webPort))).start();

    }
}