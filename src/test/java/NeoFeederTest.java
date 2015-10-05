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
import graph.impl.NeoGraphDatabase;
import graph.interfaces.GraphDatabase;
import junit.framework.TestCase;
import obelix.ObelixFeeder;
import obelix.impl.ObelixRecommender;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import queue.impl.InternalObelixQueue;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;

import java.util.HashMap;

public class NeoFeederTest extends TestCase {

    GraphDatabaseService neoDb;
    GraphDatabase graphDb;
    ObelixQueue obelixQueue;
    ObelixQueue obelixCacheQueue;

    public void tearDown() throws Exception {
        neoDb.shutdown();
    }

    public void setUp() throws Exception {
        super.setUp();
        neoDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        graphDb = new NeoGraphDatabase(neoDb);
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
        assertEquals(graphDb.getRelationships("1", "1", null, null, false).size(), 1);
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
        assertEquals(graphDb.getRelationships("1", "1", null, null, false).size(), 2);
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

        assertEquals(result, new ObelixRecommender(graphDb).recommend("1"));
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
        assertNotSame(result, new ObelixRecommender(graphDb).recommend("1"));
        assertTrue(new ObelixRecommender(graphDb).recommend("1").get("1") > new ObelixRecommender(graphDb).recommend("1").get("2"));

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

        assertEquals(result, new ObelixRecommender(graphDb).recommend("1"));
    }




}
