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
package graph.impl;

import events.NeoHelpers;
import graph.TimeStampExpander;
import graph.UserItemRelationship;
import graph.exceptions.ObelixInsertException;
import graph.exceptions.ObelixNodeNotFoundException;
import graph.interfaces.GraphDatabase;
import org.neo4j.cypher.EntityNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static events.NeoHelpers.getUserNode;
import static events.NeoHelpers.makeSureUsersDontExceedItemLimit;

public class NeoGraphDatabase implements GraphDatabase {
    public static final String NEO4J_WEB_SERVER_LISTEN_TO = "localhost";
    private GraphDatabaseService neoDb;
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoGraphDatabase.class.getName());

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public NeoGraphDatabase(final String path,
                            final Boolean enableNeo4jWebServer,
                            final int configNeo4jWebPort) {

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
                    .addProperty(
                            Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY,
                            NEO4J_WEB_SERVER_LISTEN_TO);

            config.configuration()
                    .addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY,
                            configNeo4jWebPort);

            neoServerBootstrapper = new WrappingNeoServerBootstrapper(api, config);
            neoServerBootstrapper.start();
        }

    }

    public NeoGraphDatabase(final GraphDatabaseService db) {
        this.neoDb = db;
    }

    @Override
    public final List<String> getAllUserIds() {
        return getAllNodeIds("User");
    }

    @Override
    public final String cleanAllRelationships(final String max)
            throws ObelixNodeNotFoundException {

        for (String userid : getAllNodeIds("User")) {
            LOGGER.info("Cleaning" + userid + " " + cleanRelationships(userid, max));
        }
        return "done";
    }

    @Override
    public final List<String> getAllNodeIds(final String nodeName) {
        List<String> nodes;
        try (Transaction tx = this.neoDb.beginTx()) {
            nodes = NeoHelpers.getAllNodes(this.neoDb, nodeName);
            tx.success();
        }

        return nodes;
    }

    public final void makeSureUserItemLimitNotExceeded(final String node,
                                                       final int maxRelationships)
            throws ObelixNodeNotFoundException {

        try (Transaction tx = this.neoDb.beginTx()) {
            makeSureUsersDontExceedItemLimit(
                    this.neoDb, getUserNode(this.neoDb, node), maxRelationships);
            tx.success();
        }
    }

    public final String cleanRelationships(final String userId,
                                           final String maxRelationships)
            throws ObelixNodeNotFoundException {
        int relationshipCountBefore = 0;
        int relationshipCountAfter = 0;
        Node user;

        try (Transaction tx = neoDb.beginTx()) {
            user = getUserNode(neoDb, userId);
            relationshipCountBefore += ((Collection<?>) user.getRelationships()).size();
            tx.success();
        }

        makeSureUsersDontExceedItemLimit(neoDb, user, Integer.parseInt(maxRelationships));

        try (Transaction tx = neoDb.beginTx()) {
            user = getUserNode(neoDb, userId);
            relationshipCountAfter += ((Collection<?>) user.getRelationships()).size();
            tx.success();
        }
        return "Before: " + relationshipCountBefore + " - After: " + relationshipCountAfter;
    }

    public final List<String> getRelationships() {
        return getRelationships(null);
    }

    public final List<String> getRelationships(final String type) {

        try (Transaction tx = neoDb.beginTx()) {
            Iterable<Relationship> rel = GlobalGraphOperations.at(neoDb).getAllRelationships();

            List<String> relationshipsIds = new ArrayList<>();

            Long oldestTimestamp = null;
            Long newestTimestamp = null;

            for (Relationship relationship : rel) {
                try {
                    if (type != null && !relationship.getType().name().equals(type)) {
                        continue;
                    }
                    long relTimestamp = Long.parseLong(relationship
                            .getProperty("timestamp").toString());

                    if (oldestTimestamp == null) {
                        oldestTimestamp = relTimestamp;
                    }
                    if (newestTimestamp == null) {
                        newestTimestamp = relTimestamp;
                    }
                    if (relTimestamp < oldestTimestamp) {
                        oldestTimestamp = relTimestamp;
                    }
                    if (relTimestamp > newestTimestamp) {
                        newestTimestamp = relTimestamp;
                    }
                    String key = relationship.getProperty("timestamp")
                            + ":" + relationship.getStartNode().getProperty("node_id").toString()
                            + "-" + relationship.getEndNode().getProperty("node_id").toString();

                    relationshipsIds.add(key);
                } catch (NotFoundException e) {
                    LOGGER.error("Ignores one relationship, probably deleted..");
                }
            }
            //LOGGER.info("Newest timestamp: " + newestTimestamp);
            //LOGGER.info("Oldest timestamp: " + oldestTimestamp);
            tx.success();
            return relationshipsIds;
        }
    }

    public final List<UserItemRelationship> getRelationships(final String userID,
                                                             final String depth,
                                                             final String sinceTimestamp,
                                                             final String untilTimestamp,
                                                             final boolean removeDuplicates)

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

                    String itemid = path.lastRelationship()
                            .getEndNode().getProperty("node_id").toString();

                    userItemRelationships.add(new UserItemRelationship(itemid, path.length()));
                }
            }
            tx.success();
            return userItemRelationships;
        }
    }

    public final void createNodeNodeRelationship(final String nodeFrom,
                                                 final String nodeTo,
                                                 final RelationshipType relType,
                                                 final String timestamp,
                                                 final int maxRelationships)
            throws ObelixInsertException {

        try (Transaction tx = neoDb.beginTx()) {
            Node user = NeoHelpers.getOrCreateUserNode(neoDb, nodeFrom);
            Node item = NeoHelpers.getOrCreateItemNode(neoDb, nodeTo);

            if (timestamp == null) {
                throw new ObelixInsertException();
            }
            Relationship r = user.createRelationshipTo(item, relType);
            r.setProperty("timestamp", timestamp);

            makeSureUsersDontExceedItemLimit(neoDb, user, maxRelationships);
            tx.success();
        } catch (TransactionFailureException e) {
            //Fixme: restart?
            LOGGER.error("TransactionFailureException, need to restart");
            throw new ObelixInsertException();
        } catch (NotFoundException e) {
            LOGGER.error("Not found exception, pushing the element back on the queue. "
                    + e.getMessage() + ": ");
            throw new ObelixInsertException();
        } catch (DeadlockDetectedException e) {
            LOGGER.error("Deadlock found exception, pushing the element back on the queue"
                    + e.getMessage() + ": ");
            throw new ObelixInsertException();
        } catch (EntityNotFoundException e) {
            LOGGER.error("EntityNotFoundException, pushing the element back on the queue"
                    + e.getMessage() + ": ");
            throw new ObelixInsertException();
        }
    }
}
