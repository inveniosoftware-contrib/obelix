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


public class ObelixBatchImport {

    private final static Logger LOGGER = LoggerFactory.getLogger(ObelixBatchImport.class.getName());

    private static void registerShutdownHook(final BatchInserter graphDb, final BatchInserterIndexProvider indexProvider) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
                indexProvider.shutdown();
            }
        });
    }

    private static long getOrCreateUserNodeID(String userid, Map<String, Long> usersNodesMap,
                                              BatchInserterIndex usersIndex, BatchInserter inserter,
                                              Label label) {

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

    private static long getOrCreateItemNodeID(String itemid, Map<String, Long> itemsNodesMap,
                                              BatchInserterIndex itemsIndex, BatchInserter inserter,
                                              Label label) {

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

    private static long getOrCreateRelatinshipID(String userid, String itemid, String timestamp,
                                                 long userNodeid, long itemNodeid,
                                                 Map<String, Long> relationshipsMap,
                                                 BatchInserterIndex relationshipIndex,
                                                 BatchInserter inserter,
                                                 RelationshipType relType) {

        String uniqueRelationID = userid + itemid + timestamp;

        long relationID;

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

    private static NeoEvent getEvent(String result) {
        return EventFactory.build(result);
    }

    public static void run(String neo4storage, String redisQueueName) {

        long userNodeID;
        long itemNode;

        ObelixQueue redisQueueManager = new RedisObelixQueue(redisQueueName);

        Label userLabel = DynamicLabel.label("User");
        Label itemLabel = DynamicLabel.label("Item");
        RelationshipType viewed = NeoHelpers.RelTypes.VIEWED;

        Map<String, Long> usersNodesMap = new HashMap<>();
        Map<String, Long> itemsNodesMap = new HashMap<>();
        Map<String, Long> relationshipsMap = new HashMap<>();

        Map<String, String> config = new HashMap<>();
        config.put("neostore.nodestore.db.mapped_memory", "2G");
        config.put("neostore.relationshipstore.db.mapped_memory", "9G");
        config.put("neostore.propertystore.db.mapped_memory", "800M");
        config.put("neostore.propertystore.db.strings.mapped_memory", "800M");
        config.put("neostore.propertystore.db.arrays.mapped_memory", "500M");

        BatchInserter inserter = null;
        inserter = BatchInserters.inserter(neo4storage, config);

        BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(inserter);

        registerShutdownHook(inserter, indexProvider);

        BatchInserterIndex usersIndex = indexProvider.nodeIndex("users", MapUtil.stringMap("type", "exact"));
        usersIndex.setCacheCapacity("node_id", 10000000);

        BatchInserterIndex itemsIndex = indexProvider.nodeIndex("items", MapUtil.stringMap("type", "exact"));
        usersIndex.setCacheCapacity("node_id", 10000000);

        BatchInserterIndex relationshipIndex = indexProvider.relationshipIndex("relationships", MapUtil.stringMap("type", "exact"));
        usersIndex.setCacheCapacity("timestamp", 10000000);

        boolean notFinised = true;

        int c = 0;
        while (notFinised) {

            ObelixQueueElement result = redisQueueManager.pop();

            if (result == null) {
                notFinised = false;
            }

            if (result != null) {

                NeoEvent event = getEvent(result.toString());

                if(event == null) {
                    continue;
                }

                long userid = getOrCreateUserNodeID(event.getUser(), usersNodesMap,
                        usersIndex, inserter, userLabel);

                long itemid = getOrCreateItemNodeID(event.getItem(), itemsNodesMap,
                        itemsIndex, inserter, itemLabel);

                getOrCreateRelatinshipID(event.getUser(), event.getItem(), event.getTimestamp(),
                        userid, itemid, relationshipsMap, relationshipIndex,
                        inserter, NeoHelpers.RelTypes.VIEWED);

            }

            c += 1;

            if (c % 1000 == 0) {
                LOGGER.info("Imported " + c);
            }
        }
    }

    public static void main(String... args) {
        ObelixBatchImport.run("graph.db", "logentries");
    }

}
