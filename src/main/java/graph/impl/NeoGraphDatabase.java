package graph.impl;

import events.NeoHelpers;
import graph.TimeStampExpander;
import graph.UserItemRelationship;
import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;
import graph.exceptions.ObelixNodeNotFoundException;
import org.neo4j.cypher.EntityNotFoundException;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static events.NeoHelpers.getUserNode;
import static events.NeoHelpers.makeSureTheUserDoesNotExceedMaxRelationshipsLimit;

public class NeoGraphDatabase implements GraphDatabase {
    GraphDatabaseService neoDb;
    private final static Logger LOGGER = LoggerFactory.getLogger(NeoGraphDatabase.class.getName());

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public NeoGraphDatabase(String path, Boolean enableNeo4jWebServer){
        GraphDatabaseService db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(path)
                .newGraphDatabase();

        registerShutdownHook(db);
        this.neoDb = db;

        if (enableNeo4jWebServer) {
            // Start Neo4jWebserver
            WrappingNeoServerBootstrapper neoServerBootstrapper;
            GraphDatabaseAPI api = (GraphDatabaseAPI) neoDb;

            ServerConfigurator config = new ServerConfigurator(api);
            config.configuration()
                    .addProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, "127.0.0.1");
            config.configuration()
                    .addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, "7575");

            neoServerBootstrapper = new WrappingNeoServerBootstrapper(api, config);
            neoServerBootstrapper.start();
        }

        // Warm up neo4j cache
        /*
        try (Transaction tx = graphDb.beginTx()) {
            for (Node n : GlobalGraphOperations.at(graphDb).getAllNodeIds()) {
                n.getPropertyKeys();
                for (Relationship relationship : n.getRelationships()) {
                    Node start = relationship.getStartNode();
                }
            }
            tx.success();
            System.out.println("Neo4j is warmed up!");
        }*/
    }

    public NeoGraphDatabase(GraphDatabaseService db){
        this.neoDb = db;
    }
    @Override
    public void createUserNode() {
    }

    @Override
    public void createItemNode() {
    }

    @Override
    public void createRelationship() {
    }

    @Override
    public List<String> getAllUserIds(){
        return getAllNodeIds("User");
    }

    @Override
    public String cleanAllRelationships(String max) throws ObelixNodeNotFoundException {
        for (String userid : getAllNodeIds("User")) {
            LOGGER.info("Cleaning" + userid + " " + cleanRelationships(userid, max));
        }
        return "done";
    }

    @Override
    public List<String> getAllNodeIds(String nodeName)
    {
        List<String> nodes;
        try (Transaction tx = this.neoDb.beginTx()) {
            nodes = events.NeoHelpers.getAllNodes(this.neoDb, nodeName);
            tx.success();
        }

        return nodes;
    }

    public void makeSureTheUserDoesNotExceedMaxRelationshipsLimitObelix(String node, int maxRelationships) throws ObelixNodeNotFoundException {
        try (Transaction tx = this.neoDb.beginTx()) {
            makeSureTheUserDoesNotExceedMaxRelationshipsLimit(
                  this.neoDb, getUserNode(this.neoDb, node), maxRelationships);
            tx.success();
        }
    }

    public String cleanRelationships(String userid, String max) throws ObelixNodeNotFoundException {
        int relationshipCountBefore = 0;
        int relationshipCountAfter = 0;
        Node user;

        try (Transaction tx = neoDb.beginTx()) {
            user = getUserNode(neoDb, userid);
            for (Relationship rel : user.getRelationships()) {
                relationshipCountBefore += 1;
            }
            tx.success();
        }

        makeSureTheUserDoesNotExceedMaxRelationshipsLimit(neoDb, user, Integer.parseInt(max));

        try (Transaction tx = neoDb.beginTx()) {
            user = getUserNode(neoDb, userid);
            for (Relationship rel : user.getRelationships()) {
                relationshipCountAfter += 1;
            }
            tx.success();
        }
        return "Before: " + relationshipCountBefore + " - After: " + relationshipCountAfter;
    }

    public List<String> getAllRelationships_() {
        return getAllRelationships_(null);
    }

    public List<String> getAllRelationships_(String type) {

        try (Transaction tx = neoDb.beginTx()) {
            Iterable<Relationship> rel = GlobalGraphOperations.at(neoDb).getAllRelationships();

            List<String> relations_ids = new ArrayList<>();

            Long oldestTimestamp = null;
            Long newestTimestamp = null;

            for (Relationship relationship : rel) {
                try {
                    if (type != null && !relationship.getType().name().equals(type)) {
                        continue;
                    }
                    long relTimestamp = Long.parseLong(relationship.getProperty("timestamp").toString());

                    if (oldestTimestamp == null) {
                        oldestTimestamp = relTimestamp;
                    }
                    if (newestTimestamp == null) {
                        newestTimestamp = relTimestamp;
                    }
                    if (relTimestamp < oldestTimestamp)
                    {
                        oldestTimestamp = relTimestamp;
                    }
                    if (relTimestamp > newestTimestamp) {
                        newestTimestamp = relTimestamp;
                    }
                    String key = relationship.getProperty("timestamp")
                                 + ":" + relationship.getStartNode().getProperty("node_id").toString()
                                 + "-" + relationship.getEndNode().getProperty("node_id").toString();

                    relations_ids.add(key);
                } catch (NotFoundException e) {
                    LOGGER.error("Ignores one relationship, probably deleted..");
                }
            }
            //LOGGER.info("Newest timestamp: " + newestTimestamp);
            //LOGGER.info("Oldest timestamp: " + oldestTimestamp);
            tx.success();
            return relations_ids;
        }
    }

    public List<UserItemRelationship> getRelationships(String userID, String depth,
                                                    String sinceTimestamp, String untilTimestamp,
                                                    boolean removeDuplicates)

            throws ObelixNodeNotFoundException {
        try (Transaction tx = neoDb.beginTx()) {
            List<UserItemRelationship> userItemRelationships = new LinkedList<>();
            Node user = getUserNode(neoDb, userID);

            for (Path path : neoDb.traversalDescription()
                    .breadthFirst()
                    .expand(new TimeStampExpander(sinceTimestamp, untilTimestamp, depth))
                    .evaluator(Evaluators.toDepth(Integer.parseInt(depth)))
                    .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
                    .traverse(user)) {

                if (path.lastRelationship() != null) {
                    String userid = path.lastRelationship().getStartNode().getProperty("node_id").toString();
                    String itemid = path.lastRelationship().getEndNode().getProperty("node_id").toString();
                    String timestamp = path.lastRelationship().getProperty("timestamp").toString();
                    String relname = userid + itemid + String.valueOf(timestamp);

                    userItemRelationships.add(
                            new UserItemRelationship(userid, itemid,
                                    relname, timestamp, path.length()));
                }
            }
            tx.success();
            return userItemRelationships;
        }
    }

    /**
     * New Relationship between nodeFrom to nodeTo with timestamp
     * @param nodeFrom
     * @param nodeTo
     * @param timestamp
     * @param relType
     * @param maxRelationships
     */
    public void createNodeNodeRelationship(String nodeFrom, String nodeTo, RelationshipType relType, String timestamp,
                                           int maxRelationships) throws ObelixInsertException{
        try (Transaction tx = neoDb.beginTx()) {
            Node user = NeoHelpers.getOrCreateUserNode(neoDb, nodeFrom);
            Node item = NeoHelpers.getOrCreateItemNode(neoDb, nodeTo);

            if (timestamp == null) {
                throw new ObelixInsertException();
            }
            Relationship r = user.createRelationshipTo(item, relType);
            r.setProperty("timestamp", timestamp);

            makeSureTheUserDoesNotExceedMaxRelationshipsLimit(neoDb, user, maxRelationships);
            tx.success();
        } catch (TransactionFailureException e) {
            //Fixme: restart?
            LOGGER.error("TransactionFailureException, need to restart");
            throw new ObelixInsertException();
        } catch (NotFoundException e) {
            LOGGER.error("Not found exception, pushing the element back on the queue. " + e.getMessage() + ": ");
            throw new ObelixInsertException();
        } catch (DeadlockDetectedException e) {
            LOGGER.error("Deadlock found exception, pushing the element back on the queue" + e.getMessage() + ": ");
            throw new ObelixInsertException();
        } catch (EntityNotFoundException e) {
            LOGGER.error("EntityNotFoundException, pushing the element back on the queue" + e.getMessage() + ": ");
            throw new ObelixInsertException();
        }
    }
}
