/**
 * This file is part of Obelix.
 *
 * Obelix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Obelix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Obelix.  If not, see <http://www.gnu.org/licenses/>.
 */
package obelix;

import events.EventFactory;
import events.NeoEvent;
import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;
import metrics.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;


public class ObelixFeeder implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObelixFeeder.class.getName());
    public static final int IMPORTS_BETWEEN_EVERY_LOG_MESSAGE = 1000;
    public static final int FEEDER_SLEEPING_INIT = 3000;
    private final MetricsCollector metricsCollector;

    private GraphDatabase graphDb;
    private ObelixQueue redisQueueManager;
    private ObelixQueue usersCacheQueue;
    private int maxRelationships;
    private int workerID;

    public ObelixFeeder(final GraphDatabase graphDbINput,
                        final MetricsCollector metricsCollectorInput,
                        final int maxRelationshipsInput,
                        final ObelixQueue redisQueueManagerInput,
                        final ObelixQueue usersCacheQueueInput,
                        final int workerIDInput) {

        this.metricsCollector = metricsCollectorInput;
        this.redisQueueManager = redisQueueManagerInput;
        this.usersCacheQueue = usersCacheQueueInput;
        this.maxRelationships = maxRelationshipsInput;
        this.graphDb = graphDbINput;
        this.workerID = workerIDInput;
    }

    public ObelixFeeder(final GraphDatabase graphDbInput,
                        final int maxRelationshipsInput,
                        final ObelixQueue redisQueueManagerInput,
                        final ObelixQueue usersCacheQueueInput,
                        final int workerIDInput) {

        this(graphDbInput, null, maxRelationshipsInput, redisQueueManagerInput,
                usersCacheQueueInput, workerIDInput);
    }

    public final void run() {

        while (true) {

            try {

                LOGGER.info("Starting feeder: " + workerID);

                while (true) {

                    try {
                        Thread.sleep(FEEDER_SLEEPING_INIT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    feed();

                }
            } catch (Exception e) {
                LOGGER.error("ObelixFeeder Exception", e);
                LOGGER.info("Restarting ObelixFeeder.run()!");
            }
        }
    }

    public final boolean feed() {
        int count = 0;
        ObelixQueueElement result = redisQueueManager.pop();

        while (result != null) {
            NeoEvent event = EventFactory.build(result.getData().toString());

            if (event != null) {
                LOGGER.debug("Handling event: " + event);

                if (metricsCollector != null) {
                    metricsCollector.addAccumalativeMetricValue("feeded", 1);
                }

                try {
                    event.execute(graphDb, maxRelationships);
                } catch (ObelixInsertException e) {
                    LOGGER.error("Insert error, pushing the "
                            + "element back on the queue. : " + result);

                    redisQueueManager.push(result);
                    return true;
                }

                // Tell Obelix to rebuild the cache for the user in this event!
                // This will maintain the caches for all active users
                if (!usersCacheQueue.getAll().contains(
                        new ObelixQueueElement("user_id", event.getUser()))) {
                    usersCacheQueue.push(new ObelixQueueElement("user_id", event.getUser()));
                }
                count += 1;

                if (count % IMPORTS_BETWEEN_EVERY_LOG_MESSAGE == 0) {
                    LOGGER.info("WorkerID: " + workerID
                            + " imported " + count + " entries from redis");
                }
            }
            result = redisQueueManager.pop();
        }
        if (count > 0) {
            LOGGER.info("WorkerID: " + workerID + " imported "
                        + count + " entries from redis");
        }
        return false;
    }
}
