import events.EventFactory;
import events.NeoEvent;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import redis.clients.jedis.Jedis;

public class NeoFeeder implements Runnable {

    GraphDatabaseService graphDb;

    public NeoFeeder(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public void run() {

        Jedis jedis = new Jedis("localhost", 6379);

        int count = 0;

        while (true) {

            String result = jedis.rpop("logentries");

            if (result != null) {
                NeoEvent event = EventFactory.build(graphDb, result);

                if (event != null) {

                    try (Transaction tx = graphDb.beginTx()) {
                        event.execute(graphDb);
                        tx.success();
                    } catch (TransactionFailureException e) {
                        System.err.println("TransactionFailureException, need to restart");
                        jedis.lpush("logentries", result);
                        //graphDb.shutdown();
                        System.exit(0);
                    }

                    count += 1;

                    if (count % 1000 == 0) {
                        System.out.println("Imported " + count + " entries from redis");

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

        }
    }

}