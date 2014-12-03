import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class NeoImporter {

    private static enum RelTypes implements RelationshipType {
        KNOWS
    }

    public static void main(String... args) {

        //ESClient clientLocal = new ESClient("elasticsearch", "localhost", 9300, true);

        String neoLocation = "graph.db";

        GraphDatabaseService graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(neoLocation)
                .newGraphDatabase();

        /*
        registerShutdownHook(graphDb);

        QueryBuilder qb = termQuery("_type", "download-log");
        SearchResponse scrollResp = clientLocal.fetchAllUntilKeyFound(qb);

        while (true) {
            List<Map<String, String>> results = new ArrayList<>();

            try (Transaction tx = graphDb.beginTx()) {

                for (SearchHit hit : scrollResp.getHits()) {
                    Node user = graphDb.createNode();
                    user.setProperty("id_user", hit.sourceAsMap().get("id_user"));
                    user.setProperty("ip_user", hit.sourceAsMap().get("client_host"));

                    Node record = graphDb.createNode();
                    record.setProperty("id_bibdoc", hit.sourceAsMap().get("id_bibdoc"));
                    record.setProperty("id_bibrec", hit.sourceAsMap().get("id_bibrec"));
                    record.setProperty("file_format", hit.sourceAsMap().get("file_format"));

                    Relationship relationship = user.createRelationshipTo(record, RelTypes.KNOWS);
                    relationship.setProperty("timestamp", hit.sourceAsMap().get("timestamp"));

                }

                // Database operations go here
                tx.success();
            }

            scrollResp = clientLocal.fetch(scrollResp);

            if (scrollResp.getHits().getHits().length == 0) {
                break;
            }
        }
    }

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

    */
    }

}