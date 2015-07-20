package obelix;

import events.EventFactory;
import events.NeoEvent;
import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;
import metrics.MetricsCollector;
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
    private final MetricsCollector metricsCollector;

    GraphDatabase graphDb;
    ObelixQueue redisQueueManager;
    ObelixQueue usersCacheQueue;
    int maxRelationships;
    int workerID;

    public ObelixFeeder(GraphDatabase graphDb,
                        MetricsCollector metricsCollector,
                        int maxRelationships,
                        ObelixQueue redisQueueManager,
                        ObelixQueue usersCacheQueue,
                        int workerID) {

        this.metricsCollector = metricsCollector;
        this.redisQueueManager = redisQueueManager;
        this.usersCacheQueue = usersCacheQueue;
        this.maxRelationships = maxRelationships;
        this.graphDb = graphDb;
        this.workerID = workerID;
    }

    public ObelixFeeder(GraphDatabase graphDb,
                        int maxRelationships,
                        ObelixQueue redisQueueManager,
                        ObelixQueue usersCacheQueue,
                        int workerID) {

        this(graphDb, null, maxRelationships, redisQueueManager, usersCacheQueue, workerID);
    }

    public void run() {

        try {

            LOGGER.info("Starting feeder: " + workerID);

            while (true) {

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                feed();

            }
        } catch (Exception e) {
            LOGGER.error("ObelixFeeder Exception", e);
            LOGGER.info("Restarting ObelixFeeder.run()!");
            this.run();
        }
    }

    public boolean feed() {
        int count = 0;
        ObelixQueueElement result = redisQueueManager.pop();

        while (result != null) {
            NeoEvent event = EventFactory.build(result.data.toString());

            if (event != null) {
                LOGGER.debug("Handling event: " + event);

                if(metricsCollector != null) {
                    metricsCollector.addAccumalativeMetricValue("feeded", 1);
                }

                try {
                    event.execute(graphDb, maxRelationships);
                } catch (ObelixInsertException e) {
                    LOGGER.error("Insert error, pushing the element back on the queue. : " + result);
                    redisQueueManager.push(result);
                    return true;
                }

                // Tell Obelix to rebuild the cache for the user in this event!
                // This will maintain the caches for all active users
                if (!usersCacheQueue.getAll().contains(new ObelixQueueElement("user_id", event.getUser()))) {
                    usersCacheQueue.push(new ObelixQueueElement("user_id", event.getUser()));
                }
                count += 1;

                if (count % 1000 == 0) {
                    LOGGER.info("WorkerID: " + workerID + " imported " + count + " entries from redis");
                }
            }
            result = redisQueueManager.pop();
        }
        LOGGER.info("WorkerID: " + workerID + " imported " + count + " entries from redis");
        return false;
    }
}
