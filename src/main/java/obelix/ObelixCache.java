package obelix;

import graph.UserGraph;
import graph.exceptions.ObelixNodeNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import queue.interfaces.ObelixQueue;
import store.impl.RedisObelixStore;
import store.interfaces.ObelixStore;
import utils.JsonTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static events.NeoHelpers.getUserNode;
import static events.NeoHelpers.makeSureTheUserDoesNotExceedMaxRelationshipsLimit;

public class ObelixCache implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(ObelixCache.class.getName());

    GraphDatabaseService graphDb;
    ObelixQueue redisQueueManager;
    ObelixStore redisStoreManager;
    UserGraph userGraph;

    Map<String, String> clientSettings;

    String recommendationDepth;
    int maxRelationships;
    boolean buildForAllUsersOnStartup;

    public ObelixCache(GraphDatabaseService graphDb, ObelixQueue usersCacheQueue,
                        boolean buildForAllUsersOnStartup, String recommendationDepth,
                        int maxRelationships, Map<String, String> clientSettings) {

        this.redisStoreManager = new RedisObelixStore();
        this.graphDb = graphDb;
        this.redisQueueManager = usersCacheQueue;
        this.userGraph = new UserGraph(graphDb);
        this.buildForAllUsersOnStartup = buildForAllUsersOnStartup;
        this.recommendationDepth = recommendationDepth;
        this.maxRelationships = maxRelationships;
        this.clientSettings = clientSettings;
    }

    private void buildSettingsCache() {
        redisStoreManager.set("settings", new JsonTransformer().render(this.clientSettings));
    }

    private void buildCacheForUser(String userid) {

        LOGGER.log(Level.INFO, "Building cache for userid: " + userid);

        Map<String, Double> recommendations;
        try {
            recommendations = this.userGraph.recommend(userid, this.recommendationDepth);
            JsonTransformer jsonTransformer = new JsonTransformer();
            redisStoreManager.set(
                    "recommendations::" + userid,
                    jsonTransformer.render(recommendations));

        } catch (ObelixNodeNotFoundException | NoSuchElementException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Cache for user " + userid + " failed to build..! Can't find the user");
        }
    }

    public void run() {

        buildSettingsCache();

        if (this.buildForAllUsersOnStartup) {
            LOGGER.log(Level.INFO, "We're going to build the cache for all users!");

            int usersAdded = 0;
            try (Transaction tx = graphDb.beginTx()) {
                for (String userid : this.userGraph.getAll()) {
                    redisQueueManager.push(userid);
                    usersAdded += 1;
                }
                tx.success();
            }

            LOGGER.log(Level.INFO, "Building cache for " + usersAdded + " users!");
            LOGGER.log(Level.INFO, "But first we should clean up relationships for all users");

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
            List<String> allUsers = this.userGraph.getAll();
            List<String> usersHandledAlready = new ArrayList<>();

            int imported = 0;
            while (true) {
                String user = redisQueueManager.pop();

                if (user == null || user.equals("") || user.equals("0")) {
                    break;
                }

                // We only create the cache for unique users every 50 entry imported.
                if(usersHandledAlready.contains(user)) {
                    LOGGER.log(Level.INFO, "Skipping creation of recommendations for " + user);
                    continue;
                }

                if(allUsers.contains(user)) {
                    imported += 1;

                    try (Transaction tx = graphDb.beginTx()) {
                        buildCacheForUser(user);
                        usersHandledAlready.add(user);
                        tx.success();
                    } catch ( TransactionFailureException e) {
                        LOGGER.log(Level.WARNING, "Pushing user [" + user + "]back on the queue because: " + e.getMessage());
                        usersHandledAlready.remove(user);
                        redisQueueManager.push(user);
                    }
                }


                if (imported >= 50) {
                    break;
                }

            }

            if(imported > 0) {
                LOGGER.log(Level.INFO,"Built " + imported + " recommendation caches!");
            }

            int secondsDalay = 2;

            LOGGER.log(Level.INFO, "CacheBuilder paused for " + secondsDalay + " seconds");

            try {
                Thread.sleep(secondsDalay * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
