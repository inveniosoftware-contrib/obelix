package obelix;

import events.EventFactory;
import events.NeoEvent;
import org.neo4j.cypher.EntityNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;


public class ObelixFeeder implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ObelixFeeder.class.getName());

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

        LOGGER.info("Starting worker: " + workerID);

        int count = 0;

        while (true) {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ObelixQueueElement result = redisQueueManager.pop();

            if (result != null) {
                NeoEvent event = EventFactory.build(result.toString());

                if (event != null) {

                    try (Transaction tx = graphDb.beginTx()) {
                        LOGGER.info("Handling event: " + event);

                        event.execute(graphDb, maxRelationships);

                        // Tell Obelix to rebuild the cache for the user in this event!
                        // This will maintain the caches for all active users

                        if(!usersCacheQueue.getAll().contains(event.getUser())) {
                            usersCacheQueue.push(new ObelixQueueElement(event.getUser()));
                        }

                        tx.success();

                    } catch (TransactionFailureException e) {
                        LOGGER.error("TransactionFailureException, need to restart");
                        redisQueueManager.push(result);
                        //System.exit(0);
                    } catch (NotFoundException e) {
                        LOGGER.error("Not found exception, pushing the element back on the queue. " + e.getMessage() + ": " + result);
                        redisQueueManager.push(result);
                        continue;
                    } catch (DeadlockDetectedException e) {
                        LOGGER.error("Deadlock found exception, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.push(result);
                        continue;
                    } catch (EntityNotFoundException e) {
                        LOGGER.error("EntityNotFoundException, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.push(result);
                        continue;
                    }

                    count += 1;

                    if (count % 1000 == 0) {
                        LOGGER.info("WorkerID: " + workerID + " imported " + count + " entries from redis");
                    }
                }
            }
        }
    }

}
