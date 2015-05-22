import events.NeoHelpers;
import junit.framework.TestCase;
import obelix.ObelixFeeder;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import queue.impl.InternalObelixQueue;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;

import java.util.List;

public class TestNeoHelpers extends TestCase {

    GraphDatabaseService graphDb;
    ObelixQueue obelixQueue;
    ObelixQueue obelixCacheQueue;

    public void tearDown() throws Exception {
        graphDb.shutdown();
    }

    public void setUp() throws Exception {
        super.setUp();
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        obelixQueue = new InternalObelixQueue();
        obelixCacheQueue = new InternalObelixQueue();
    }

    public void testGetAllNodesReturnsEmptyListWHenNothingIsInserted() {
        List<String> allNodes = NeoHelpers.getAllNodes(graphDb, "User");
        assertEquals(allNodes.size(), 0);
    }

    public void testGetAllNodesReturnsOneNodeIfOneInserted() {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "1");
        jsonObject.put("timestamp", "1421027564617");
        obelixQueue.push(new ObelixQueueElement(jsonObject));

        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        obelixFeeder.feed();

        List<String> allUsers = NeoHelpers.getAllNodes(graphDb, "User");
        assertEquals(allUsers.size(), 1);

        List<String> allItems = NeoHelpers.getAllNodes(graphDb, "Item");
        assertEquals(allItems.size(), 1);

    }

    public void testGetAllNodesReturnsTwoItemsAndOneUser() {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "1");
        jsonObject.put("timestamp", "1421027564617");
        obelixQueue.push(new ObelixQueueElement(jsonObject));

        jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "2");
        jsonObject.put("timestamp", "1421027564617");
        obelixQueue.push(new ObelixQueueElement(jsonObject));

        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        obelixFeeder.feed();

        List<String> allUsers = NeoHelpers.getAllNodes(graphDb, "User");
        assertEquals(allUsers.size(), 1);

        List<String> allItems = NeoHelpers.getAllNodes(graphDb, "Item");
        assertEquals(allItems.size(), 2);

    }
}
