import graph.UserGraph;
import junit.framework.TestCase;
import obelix.ObelixFeeder;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import queue.impl.InternalObelixQueue;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;

import java.util.HashMap;

public class NeoFeederTest extends TestCase {

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

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "1");
        jsonObject.put("timestamp", "1421027564617");

        obelixQueue.push(new ObelixQueueElement(jsonObject));

    }

    public void testInsertOneRelationship() throws Exception {
        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        obelixFeeder.feed();
        assertEquals(new UserGraph(graphDb).allRelationships("1").size(), 1);
    }

    public void testTwoRelationships() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "2");
        jsonObject.put("timestamp", "1422027564617");

        obelixQueue.push(new ObelixQueueElement(jsonObject));

        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        obelixFeeder.feed();
        assertEquals(new UserGraph(graphDb).allRelationships("1").size(), 2);
    }

    public void testRecommendWhenWithOnlyTwoRelationships() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "2");
        jsonObject.put("timestamp", "1422027564617");

        obelixQueue.push(new ObelixQueueElement(jsonObject));

        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        obelixFeeder.feed();


        HashMap<String, Double> result = new HashMap<>();
        result.put("1", 1.0);
        result.put("2", 1.0);

        assertEquals(result, new UserGraph(graphDb).recommend("1"));
    }

    public void testRecommendationsWithTwoUsersAndThreeRelationships() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "2");
        jsonObject.put("timestamp", "1422027564617");

        obelixQueue.push(new ObelixQueueElement(jsonObject));

        jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "2");
        jsonObject.put("item", "1");
        jsonObject.put("timestamp", "1422027564617");

        obelixQueue.push(new ObelixQueueElement(jsonObject));

        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        obelixFeeder.feed();

        HashMap<String, Double> result = new HashMap<>();
        result.put("1", 1.0);
        result.put("2", 1.0);
        assertNotSame(result, new UserGraph(graphDb).recommend("1"));
        assertTrue(new UserGraph(graphDb).recommend("1").get("1") > new UserGraph(graphDb).recommend("1").get("2"));

    }


    public void testFeedThousandEqualItems() throws Exception {
        JSONObject jsonObject;

        for (int i = 0; i < 1000; i++) {
            jsonObject = new JSONObject();
            jsonObject.put("type", "events.pageviews");
            jsonObject.put("user", "1");
            jsonObject.put("item", "2");
            jsonObject.put("timestamp", "1422027564617");
            obelixQueue.push(new ObelixQueueElement(jsonObject));
        }

        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        obelixFeeder.feed();

        HashMap<String, Double> result = new HashMap<>();
        result.put("1", 1.0);
        result.put("2", 1.0);

        assertEquals(result, new UserGraph(graphDb).recommend("1"));
    }




}
