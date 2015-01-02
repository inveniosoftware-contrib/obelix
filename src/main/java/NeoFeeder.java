import events.EventFactory;
import events.NeoEvent;
import org.neo4j.cypher.EntityNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.DeadlockDetectedException;

public class NeoFeeder implements Runnable {

    GraphDatabaseService graphDb;
    RedisQueueManager redisQueueManager;
    int maxRelationships;
    int workerID;

    public NeoFeeder(GraphDatabaseService graphDb, int maxRelationships, RedisQueueManager redisQueueManager, int workerID) {
        this.redisQueueManager = redisQueueManager;
        this.maxRelationships = maxRelationships;
        this.graphDb = graphDb;
        this.workerID = workerID;
    }

    public void run() {

        System.out.println("Starting worker: " + workerID);

        int count = 0;

        while (true) {

            String result = redisQueueManager.pop();

            if (result != null) {
                NeoEvent event = EventFactory.build(graphDb, result);

                if (event != null) {

                    try (Transaction tx = graphDb.beginTx()) {
                        event.execute(graphDb, maxRelationships);
                        tx.success();
                    } catch (TransactionFailureException e) {
                        System.err.println("TransactionFailureException, need to restart");
                        redisQueueManager.rpush(result);
                        System.exit(0);
                    } catch (NotFoundException e) {
                        System.err.println("Not found exception, pushing the element back on the queue. " + e.getMessage() + ": " + result);
                        redisQueueManager.rpush(result);
                        continue;
                    } catch (DeadlockDetectedException e) {
                        System.err.println("Deadlock found exception, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.rpush(result);
                        continue;
                    } catch (EntityNotFoundException e) {
                        System.err.println("EntityNotFoundException, pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.rpush(result);
                        continue;
                    } catch (Exception e) {
                        System.err.println("Unknown exception.. pushing the element back on the queue" + e.getMessage() + ": " + result);
                        redisQueueManager.rpush(result);
                        continue;
                    }

                    count += 1;

                    if (count % 1000 == 0) {
                        System.out.println("WorkerID: " + workerID + " imported " + count + " entries from redis");
                    }
                }
            }
        }
    }
}