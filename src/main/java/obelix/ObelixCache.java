package obelix;

import graph.UserGraph;
import graph.exceptions.ObelixNodeNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
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

import static events.NeoHelpers.getUserNode;
import static events.NeoHelpers.makeSureTheUserDoesNotExceedMaxRelationshipsLimit;

public class ObelixCache implements Runnable {


    private final static Logger LOGGER = LoggerFactory.getLogger(ObelixCache.class.getName());

    GraphDatabaseService graphDb;
    ObelixQueue redisQueueManager;
    ObelixStore redisStoreManager;
    UserGraph userGraph;

    Map<String, String> clientSettings;

    String recommendationDepth;
    int maxRelationships;
    boolean buildForAllUsersOnStartup;

    public ObelixCache(GraphDatabaseService graphDb, ObelixQueue usersCacheQueue,
                       ObelixStore obelixStore,
                       boolean buildForAllUsersOnStartup, String recommendationDepth,
                       int maxRelationships, Map<String, String> clientSettings) {

        this.redisStoreManager = obelixStore;
        this.graphDb = graphDb;
        this.redisQueueManager = usersCacheQueue;
        this.userGraph = new UserGraph(graphDb);
        this.buildForAllUsersOnStartup = buildForAllUsersOnStartup;
        this.recommendationDepth = recommendationDepth;
        this.maxRelationships = maxRelationships;
        this.clientSettings = clientSettings;
    }

    private void buildSettingsCache() {

        //redisStoreManager.set("settings", new JsonTransformer().render(this.clientSettings));
    }

    private void buildCacheForUser(String userid) {

        LOGGER.info("Building cache for userid: " + userid);

        Map<String, Double> recommendations;

        try {
            recommendations = this.userGraph.recommend(userid, this.recommendationDepth);
            JsonTransformer jsonTransformer = new JsonTransformer();
            redisStoreManager.set(
                    "recommendations::" + userid,
                    new ObelixStoreElement(jsonTransformer.render(recommendations)));

        } catch (ObelixNodeNotFoundException | NoSuchElementException | IllegalArgumentException e) {
            LOGGER.info("Cache for user " + userid + " failed to build..! Can't find the user");
        }
    }

    public void run() {

        try {

            buildSettingsCache();

            if (this.buildForAllUsersOnStartup) {
                LOGGER.info("We're going to build the cache for all users!");

                int usersAdded = 0;
                try (Transaction tx = graphDb.beginTx()) {
                    for (String userid : this.userGraph.getAll()) {
                        redisQueueManager.push(new ObelixQueueElement("user_id", userid));
                        usersAdded += 1;
                    }
                    tx.success();
                }

                LOGGER.info("Building cache for " + usersAdded + " users!");
                LOGGER.info("But first we should clean up relationships for all users");

                try (Transaction tx = graphDb.beginTx()) {
                    for (String user : this.userGraph.getAll()) {
                        try {
                            makeSureTheUserDoesNotExceedMaxRelationshipsLimit(
                                    graphDb, getUserNode(graphDb, user), maxRelationships);
                        } catch (ObelixNodeNotFoundException e) {
                            e.printStackTrace();
                        }
                        tx.success();
                    }
                }
            }

            while (true) {
                buildCacheFromCacheQueue();

                int secondsDalay = 2;

                LOGGER.info("CacheBuilder paused for " + secondsDalay + " seconds");

                try {
                    Thread.sleep(secondsDalay * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            LOGGER.error("ObelixCache Exception", e);
            LOGGER.info("Restarting ObelixCache.run()!");
            this.run();
        }
    }

    public void buildCacheFromCacheQueue() {
        List<String> allUsers = this.userGraph.getAll();
        List<String> usersHandledAlready = new ArrayList<>();

        int imported = 0;
        while (true) {
            ObelixQueueElement user = redisQueueManager.pop();

            if (user == null) {
                break;
            }

            String userID = (String) user.data.get("user_id");

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

                try (Transaction tx = graphDb.beginTx()) {
                    buildCacheForUser(userID);
                    usersHandledAlready.add(userID);
                    tx.success();
                } catch (TransactionFailureException e) {
                    LOGGER.error("Pushing user [" + userID + "]back on the queue because: " + e.getMessage());
                    usersHandledAlready.remove(userID);
                    redisQueueManager.push(user);
                }
            }

            if (imported >= 50) {
                break;
            }
        }

        if (imported > 0) {
            LOGGER.info("Built " + imported + " recommendation caches!");
        }
    }
}
