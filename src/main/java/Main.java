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

        (new Thread(new RedisFeeder())).start();
        (new Thread(new NeoFeeder(graphDb))).start();
        (new Thread(new WebInterface(graphDb))).start();

        /*
        try {

            int index = 1600000;

            List<Hit> hits = esClient.fetchLatest(index);
            List<Hit> hitsKey = esClient2.fetchAllUntilKeyFound(hits.get(hits.size()-1).getId());

            int i = 0;

            System.out.println("hits    size: " + hits.size());
            System.out.println("hitsKey size: " + hitsKey.size());


            if(hits.get(hits.size()-1).getId().equals(hitsKey.get(hitsKey.size()-1).getId())) {
                System.out.println("Success!! ");
            }

            /*
            while (i < index - 1) {
                System.out.println(
                        i + " | " +
                        hits.get(i).getId().equals(hitsKey.get(i).getId()) + " | "+
                        hits.get(i).getId() + " | " +
                        hitsKey.get(i).getId());

                i += 1;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }*/


    }
}
