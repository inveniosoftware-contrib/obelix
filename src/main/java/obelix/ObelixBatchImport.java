package obelix;

import events.EventFactory;
import events.NeoEvent;
import events.NeoHelpers;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.impl.ObelixQueueElement;
import queue.impl.RedisObelixQueue;
import queue.interfaces.ObelixQueue;

import java.util.HashMap;
import java.util.Map;


public final class ObelixBatchImport {

    public static final int IMPORTS_BETWEEN_EACH_LOG_MESSAGE = 1000;
    public static final int NEO_CACHE_CAPACITY = 10000000;

    private ObelixBatchImport() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ObelixBatchImport.class.getName());

    private static void registerShutdownHook(final BatchInserter graphDb,
                                             final BatchInserterIndexProvider indexProvider) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
                indexProvider.shutdown();
            }
        });
    }

    private static long getOrCreateUserNodeID(final String userid,
                                              final Map<String, Long> usersNodesMap,
                                              final BatchInserterIndex usersIndex,
                                              final BatchInserter inserter,
                                              final Label label) {

        long userNodeID;

        if (usersNodesMap.containsKey(userid)) {
            userNodeID = usersNodesMap.get(userid);
        } else {
            userNodeID = inserter.createNode(MapUtil.map("node_id", userid), label);
            usersNodesMap.put(userid, userNodeID);
            usersIndex.add(userNodeID, MapUtil.map("node_id", userid));
        }

        return userNodeID;

    }

    private static long getOrCreateItemNodeID(final String itemid,
                                              final Map<String, Long> itemsNodesMap,
                                              final BatchInserterIndex itemsIndex,
                                              final BatchInserter inserter,
                                              final Label label) {

        long itemNodeID;

        if (itemsNodesMap.containsKey(itemid)) {
            itemNodeID = itemsNodesMap.get(itemid);
        } else {
            itemNodeID = inserter.createNode(MapUtil.map("node_id", itemid), label);
            itemsNodesMap.put(itemid, itemNodeID);
            itemsIndex.add(itemNodeID, MapUtil.map("node_id", itemid));
        }

        return itemNodeID;

    }

    private static long getOrCreateRelatinshipID(final String timestamp,
                                                 final long userNodeid,
                                                 final long itemNodeid,
                                                 final Map<String, Long> relationshipsMap,
                                                 final BatchInserterIndex relationshipIndex,
                                                 final BatchInserter inserter,
                                                 final RelationshipType relType) {

        long relationID;
        String uniqueRelationID = userNodeid + itemNodeid + timestamp;
        if (relationshipsMap.containsKey(uniqueRelationID)) {
            relationID = relationshipsMap.get(uniqueRelationID);
        } else {

            relationID = inserter.createRelationship(userNodeid, itemNodeid, relType,
                    MapUtil.map("timestamp", timestamp));

            relationshipsMap.put(uniqueRelationID, relationID);
            relationshipIndex.add(relationID, MapUtil.map("useritem", uniqueRelationID));
        }

        return relationID;

    }

    private static NeoEvent getEvent(final String result) {
        return EventFactory.build(result);
    }

    public static void run(final String neo4storage,
            final String redisQueueName, final String redisHost) {

        ObelixQueue redisQueueManager = new RedisObelixQueue(redisQueueName, redisHost);

        Label userLabel = DynamicLabel.label("User");
        Label itemLabel = DynamicLabel.label("Item");

        Map<String, Long> usersNodesMap = new HashMap<>();
        Map<String, Long> itemsNodesMap = new HashMap<>();
        Map<String, Long> relationshipsMap = new HashMap<>();

        Map<String, String> config = new HashMap<>();
        config.put("neostore.nodestore.db.mapped_memory", "2G");
        config.put("neostore.relationshipstore.db.mapped_memory", "9G");
        config.put("neostore.propertystore.db.mapped_memory", "800M");
        config.put("neostore.propertystore.db.strings.mapped_memory", "800M");
        config.put("neostore.propertystore.db.arrays.mapped_memory", "500M");

        BatchInserter inserter;

        inserter = BatchInserters.inserter(neo4storage, config);

        BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(inserter);

        registerShutdownHook(inserter, indexProvider);

        BatchInserterIndex usersIndex = indexProvider.nodeIndex("users",
                MapUtil.stringMap("type", "exact"));
        usersIndex.setCacheCapacity("node_id", NEO_CACHE_CAPACITY);

        BatchInserterIndex itemsIndex = indexProvider.nodeIndex("items",
                MapUtil.stringMap("type", "exact"));
        usersIndex.setCacheCapacity("node_id", NEO_CACHE_CAPACITY);

        BatchInserterIndex relationshipIndex = indexProvider.relationshipIndex("relationships",
                MapUtil.stringMap("type", "exact"));
        usersIndex.setCacheCapacity("timestamp", NEO_CACHE_CAPACITY);

        boolean notFinised = true;

        int c = 0;
        while (notFinised) {

            ObelixQueueElement result = redisQueueManager.pop();

            if (result == null) {
                notFinised = false;
            }

            if (result != null) {

                NeoEvent event = getEvent(result.toString());

                if (event == null) {
                    continue;
                }

                long userid = getOrCreateUserNodeID(event.getUser(), usersNodesMap,
                        usersIndex, inserter, userLabel);

                long itemid = getOrCreateItemNodeID(event.getItem(), itemsNodesMap,
                        itemsIndex, inserter, itemLabel);


                getOrCreateRelatinshipID(event.getTimestamp(),
                        userid, itemid, relationshipsMap, relationshipIndex,
                        inserter, NeoHelpers.RelTypes.VIEWED);

            }

            c += 1;

            if (c % IMPORTS_BETWEEN_EACH_LOG_MESSAGE == 0) {
                LOGGER.info("Imported " + c);
            }
        }
    }

    public static void main(final String... args) {
        ObelixBatchImport.run("graph.db", "logentries", "localhost");
    }

}
