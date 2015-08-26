package obelix;

import graph.exceptions.ObelixNodeNotFoundException;
import graph.interfaces.GraphDatabase;
import metrics.MetricsCollector;
import obelix.impl.ObelixRecommender;
import obelix.interfaces.Recommender;
import org.neo4j.graphdb.TransactionFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;
import store.impl.ObelixStoreElement;
import store.interfaces.ObelixStore;
import utils.JsonTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


public class ObelixCache implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObelixCache.class.getName());
    public static final int LIMIT_CACHE_BUILDS_BEFORE_SLEEP = 50;
    public static final int SLEEP_MS_BETWEEN_EACH_CACHE_BUILD = 300;
    private final MetricsCollector metricsCollector;

    private final GraphDatabase graphDb;
    private final Recommender recommender;
    private final ObelixQueue redisQueueManager;
    private final ObelixStore redisStoreManager;

    private final String recommendationDepth;
    private final int maxRelationships;
    private final boolean buildForAllUsersOnStartup;

    public ObelixCache(final GraphDatabase graphDbInput,
                       final MetricsCollector metricsCollectorInput,
                       final ObelixQueue usersCacheQueueInput,
                       final ObelixStore obelixStoreInput,
                       final boolean buildForAllUsersOnStartupInput,
                       final String recommendationDepthInput,
                       final int maxRelationshipsInput) {

        this.metricsCollector = metricsCollectorInput;
        this.redisStoreManager = obelixStoreInput;
        this.graphDb = graphDbInput;
        this.redisQueueManager = usersCacheQueueInput;
        this.recommender = new ObelixRecommender(graphDbInput);
        this.buildForAllUsersOnStartup = buildForAllUsersOnStartupInput;
        this.recommendationDepth = recommendationDepthInput;
        this.maxRelationships = maxRelationshipsInput;
    }

    public ObelixCache(final GraphDatabase graphDbInput,
                       final ObelixQueue usersCacheQueueInput,
                       final ObelixStore obelixStoreInput,
                       final boolean buildForAllUsersOnStartupInput,
                       final String recommendationDepthInput,
                       final int maxRelationshipsInput) {

        this(graphDbInput, null, usersCacheQueueInput, obelixStoreInput,
                buildForAllUsersOnStartupInput, recommendationDepthInput,
                maxRelationshipsInput);
    }

    private void buildSettingsCache() {
        //redisStoreManager.set("settings", new JsonTransformer().render(this.clientSettings));
    }

    private void buildCacheForUser(final String userid) {
        LOGGER.info("Building recommendations for user id: " + userid);

        Map<String, Double> recommendations;

        try {
            recommendations = this.recommender.recommend(userid, this.recommendationDepth);
            JsonTransformer jsonTransformer = new JsonTransformer();
            redisStoreManager.set(
                    "recommendations::" + userid,
                    new ObelixStoreElement(jsonTransformer.render(recommendations)));

            if (metricsCollector != null) {
                this.metricsCollector.addAccumalativeMetricValue("recommendations_built", 1);
            }

        } catch (ObelixNodeNotFoundException | NoSuchElementException
                | IllegalArgumentException e) {

            LOGGER.info("Recommendations for user " + userid
                    + " failed to build..! Can't find the user");
        }
    }

    public final void run() {

        while (true) {

            try {
                buildSettingsCache();

                if (this.buildForAllUsersOnStartup) {
                    LOGGER.info("We're going to build the cache for all users!");

                    int usersAdded = 0;
                    for (String userid : this.graphDb.getAllUserIds()) {
                        redisQueueManager.push(new ObelixQueueElement("user_id", userid));
                        usersAdded += 1;
                    }
                    LOGGER.info("Building cache for " + usersAdded + " users!");
                    LOGGER.info("But first we should clean up relationships for all users");

                    for (String user : this.graphDb.getAllUserIds()) {
                        try {
                            graphDb.makeSureUserItemLimitNotExceeded(
                                    user, maxRelationships);
                        } catch (ObelixNodeNotFoundException e) {
                            LOGGER.error("make sure users does not exceed max limit rel", e);
                        }
                    }
                }

                while (true) {
                    try {

                        int secondsDalay = SLEEP_MS_BETWEEN_EACH_CACHE_BUILD;
                        LOGGER.debug("CacheBuilder paused for "
                                     + secondsDalay + " ms");

                        buildCacheFromCacheQueue();
                        Thread.sleep(SLEEP_MS_BETWEEN_EACH_CACHE_BUILD);

                    } catch (Exception e) {
                        break;
                    }

                }

            } catch (Exception e) {
                LOGGER.error("ObelixCache Exception", e);
                LOGGER.info("Restarting ObelixCache.run()!");
            }
        }
    }

    public final void buildCacheFromCacheQueue() {
        List<String> allUsers = this.graphDb.getAllUserIds();
        List<String> usersHandledAlready = new ArrayList<>();

        int imported = 0;
        while (true) {
            ObelixQueueElement user = redisQueueManager.pop();

            if (user == null) {
                break;
            }

            String userID = (String) user.getData().get("user_id");

            if (userID.equals("") || userID.equals("0")) {
                break;
            }

            // We only create the cache for unique users every 50 entry imported.
            if (usersHandledAlready.contains(userID)) {
                LOGGER.info("Skipping creation of recommendations for " + user);
                continue;
            }

            if (allUsers.contains(userID)) {
                imported += 1;
                try {
                    buildCacheForUser(userID);
                    usersHandledAlready.add(userID);
                } catch (TransactionFailureException e) {
                    LOGGER.error("Pushing user [" + userID + "]back on the queue because: "
                            + e.getMessage());

                    usersHandledAlready.remove(userID);
                    redisQueueManager.push(user);
                }
            }

            if (imported >= LIMIT_CACHE_BUILDS_BEFORE_SLEEP) {
                break;
            }
        }

        if (imported > 0) {
            LOGGER.debug("Built " + imported + " recommendation caches!");
        }
    }
}
