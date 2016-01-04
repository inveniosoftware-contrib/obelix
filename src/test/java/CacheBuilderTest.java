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
import obelix.ObelixCache;
import obelix.ObelixFeeder;
import obelix.impl.ObelixRecommender;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import queue.impl.InternalObelixQueue;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;
import store.impl.InternalObelixStore;
import store.impl.ObelixStoreElement;
import store.impl.RedisObelixStore;
import store.interfaces.ObelixStore;
import utils.JsonTransformer;

public class CacheBuilderTest extends TestCase {

    GraphDatabaseService neoDb;
    GraphDatabase graphDb;
    ObelixQueue obelixQueue;
    ObelixQueue obelixCacheQueue;
    ObelixStore obelixStore;

    public void tearDown() throws Exception {
        neoDb.shutdown();
    }

    public void setUp() throws Exception {
        super.setUp();

        neoDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        graphDb = new NeoGraphDatabase(neoDb);
        obelixQueue = new InternalObelixQueue();
        obelixStore = new InternalObelixStore();
        obelixCacheQueue = new InternalObelixQueue();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "1");
        jsonObject.put("item", "1");
        jsonObject.put("timestamp", "1421027564617");

        obelixQueue.push(new ObelixQueueElement(jsonObject));
    }

    public void testThatFeedingTriggersCacheBuilder() throws Exception {
        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        ObelixCache obelixCache = new ObelixCache(graphDb, obelixCacheQueue, obelixStore, false, "10", 10);
        obelixFeeder.feed();
        assertEquals(graphDb.getRelationships("1", "1", null, null, false).size(), 1);
        assertEquals(obelixCacheQueue.getAll().size(), 1);
        obelixCache.buildCacheFromCacheQueue();
        assertEquals(obelixCacheQueue.getAll().size(), 0);
    }

    public void testThatCacheActuallyCreatesCachedEntryInternalStorage() throws Exception {
        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        ObelixCache obelixCache = new ObelixCache(graphDb, obelixCacheQueue, obelixStore, false, "10", 10);
        obelixFeeder.feed();
        assertEquals(graphDb.getRelationships("1", "1", null, null, false).size(), 1);
        obelixCache.buildCacheFromCacheQueue();

        JsonTransformer jsonTransformer = new JsonTransformer();

        assertEquals(
                new ObelixStoreElement(jsonTransformer.render(new ObelixRecommender(graphDb).recommend("1"))),
                obelixStore.get("recommendations::1"));

    }

    public void testThatCacheActuallyCreatesCachedEntryRedisStorage() throws Exception {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "events.pageviews");
        jsonObject.put("user", "2");
        jsonObject.put("item", "1");
        jsonObject.put("timestamp", "1421027564617");

        obelixQueue.push(new ObelixQueueElement(jsonObject));
        obelixStore = new RedisObelixStore("localhost");

        ObelixFeeder obelixFeeder = new ObelixFeeder(graphDb, 10, obelixQueue, obelixCacheQueue, 1);
        ObelixCache obelixCache = new ObelixCache(graphDb, obelixCacheQueue, obelixStore, false, "10", 10);
        obelixFeeder.feed();
        assertEquals(graphDb.getRelationships("2", "1", null, null, false).size(), 1);
        obelixCache.buildCacheFromCacheQueue();

        JsonTransformer jsonTransformer = new JsonTransformer();

        assertEquals(
                new ObelixStoreElement(jsonTransformer.render(new ObelixRecommender(graphDb).recommend("2"))),
                obelixStore.get("recommendations::2"));
    }
}
