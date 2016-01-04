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
import junit.framework.TestCase;
import store.impl.ObelixStoreElement;
import store.impl.RedisObelixStore;
import store.interfaces.ObelixStore;


public class TestObelixRedisStore extends TestCase {

    public void testSingleSetAndGetRedis() {

        ObelixStoreElement firstElement = new ObelixStoreElement("key", "value");

        ObelixStore obelixStore = new RedisObelixStore("localhost");
        obelixStore.set("a", firstElement);

        assertEquals(obelixStore.get("a"), firstElement);

    }

    public void testMultipleSetAndGetRedis() {
        ObelixStoreElement firstElement = new ObelixStoreElement("key", "value");
        ObelixStoreElement secondElement = new ObelixStoreElement("b", "2");

        ObelixStore obelixStore = new RedisObelixStore("localhost");
        obelixStore.set("a", firstElement);
        obelixStore.set("b", secondElement);

        assertEquals(obelixStore.get("a"), firstElement);
        assertEquals(obelixStore.get("b"), secondElement);

    }

    public void testReuseSetAndGetRedis() {
        ObelixStoreElement firstElement = new ObelixStoreElement("key", "value");
        ObelixStoreElement secondElement = new ObelixStoreElement("b", "2");

        ObelixStore obelixStore = new RedisObelixStore("localhost");
        obelixStore.set("a", firstElement);
        obelixStore.set("a", secondElement);

        assertEquals(obelixStore.get("a"), secondElement);

    }


    public void testReadJsonDataFormattedAsStringFromStore() {
        String testData = "\"{\\\"file_format\\\": \\\"page_view\\\", \\\"timestamp\\\": 1431962580.7399549, \\\"item\\\": 2016165, \\\"user\\\": \\\"58335767\\\", \\\"ip\\\": \\\"188.218.111.19\\\", \\\"type\\\": \\\"events.pageviews\\\"}\"";

        ObelixStore obelixStore = new RedisObelixStore("localhost");
        obelixStore.set("key", new ObelixStoreElement(testData));

    }

    public void testReadValidJsonDataFromStore() {
        String testData = "{\"file_format\": \"page_view\"}";

        ObelixStore obelixStore = new RedisObelixStore("localhost");
        obelixStore.set("key", new ObelixStoreElement(testData));

    }
}
