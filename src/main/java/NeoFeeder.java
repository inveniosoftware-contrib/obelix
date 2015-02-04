import events.EventFactory;
import events.NeoEvent;
import org.neo4j.cypher.EntityNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.DeadlockDetectedException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NeoFeeder implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(NeoFeeder.class.getName());

    GraphDatabaseService graphDb;
    RedisQueueManager redisQueueManager;
    RedisQueueManager usersCacheQueue;
    int maxRelationships;
    int workerID;

    public NeoFeeder(GraphDatabaseService graphDb, int maxRelationships,
                     RedisQueueManager redisQueueManager,
                     RedisQueueManager usersCacheQueue, int workerID) {

        this.redisQueueManager = redisQueueManager;
        this.usersCacheQueue = usersCacheQueue;
        this.maxRelationships = maxRelationships;
        this.graphDb = graphDb;
        this.workerID = workerID;
    }

    public void run() {

        LOGGER.log(Level.INFO, "Starting worker: " + workerID);

        int count = 0;

        while (true) {

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String result = redisQueueManager.pop();

            if (result != null) {
                NeoEvent event = EventFactory.build(result);

                if (event != null) {

                    try (Transaction tx = graphDb.beginTx()) {
                        LOGGER.log(Level.FINE, "Handling event: " + event);

                        event.execute(graphDb, maxRelationships);

                        // Tell Obelix to rebuild the cache for the user in this event!
                        // This will maintain the caches for all active users
                        usersCacheQueue.rpush(event.getUser());

                        tx.success();

                    } catch (TransactionFailureException e) {
                        LOGGER.log(Level.SEVERE, "TransactionFailureException, need to restart");
                        redisQueueManager.rpush(result);
                        //System.exit(0);
                    } catch (NotFoundException e) {
                        LOGGER.log(Level.SEVERE, "Not found exception, pushing the element back on the queue. " + e.getMessage() + ": " + result);
                        redisQueueManager.rpush(result);
                        continue;
                    } catch (DeadlockDetectedException e) {
                        LOGGER.log(Level.SEVERE, "Deadlock found exception, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.rpush(result);
                        continue;
                    } catch (EntityNotFoundException e) {
                        LOGGER.log(Level.SEVERE, "EntityNotFoundException, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.rpush(result);
                        continue;
                    }

                    count += 1;

                    if (count % 1000 == 0) {
                        LOGGER.log(Level.INFO, "WorkerID: " + workerID + " imported " + count + " entries from redis");
                    }
                }
            }
        }
    }
}