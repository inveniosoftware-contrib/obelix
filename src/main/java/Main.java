import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Main {

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

    public static void main(String... args) {

        String neoLocation = "/Users/frecar/code/frecar/obelix/graph.db";
        GraphDatabaseService graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(neoLocation)
                .newGraphDatabase();

        registerShutdownHook(graphDb);

        //(new Thread(new RedisFeeder())).start();
        (new Thread(new NeoFeeder(graphDb))).start();
        (new Thread(new WebInterface(graphDb))).start();

    }
}
