package obelix;

import events.EventFactory;
import events.NeoEvent;
import org.neo4j.cypher.EntityNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.DeadlockDetectedException;
import queue.interfaces.ObelixQueue;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ObelixFeeder implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(ObelixFeeder.class.getName());

    GraphDatabaseService graphDb;
    ObelixQueue redisQueueManager;
    ObelixQueue usersCacheQueue;
    int maxRelationships;
    int workerID;

    public ObelixFeeder(GraphDatabaseService graphDb, int maxRelationships,
                     ObelixQueue redisQueueManager,
                     ObelixQueue usersCacheQueue, int workerID) {

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
                Thread.sleep(3000);
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

                        if(!usersCacheQueue.getAll().contains(event.getUser())) {
                            usersCacheQueue.push(event.getUser());
                        }

                        tx.success();

                    } catch (TransactionFailureException e) {
                        LOGGER.log(Level.SEVERE, "TransactionFailureException, need to restart");
                        redisQueueManager.push(result);
                        //System.exit(0);
                    } catch (NotFoundException e) {
                        LOGGER.log(Level.SEVERE, "Not found exception, pushing the element back on the queue. " + e.getMessage() + ": " + result);
                        redisQueueManager.push(result);
                        continue;
                    } catch (DeadlockDetectedException e) {
                        LOGGER.log(Level.SEVERE, "Deadlock found exception, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.push(result);
                        continue;
                    } catch (EntityNotFoundException e) {
                        LOGGER.log(Level.SEVERE, "EntityNotFoundException, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.push(result);
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
