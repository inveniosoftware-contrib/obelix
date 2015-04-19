import junit.framework.TestCase;
import store.impl.ObelixStoreElement;
import store.impl.RedisObelixStore;
import store.interfaces.ObelixStore;


public class TestObelixRedisStore extends TestCase {

    public void testSingleSetAndGetRedis() {

        ObelixStoreElement firstElement = new ObelixStoreElement("key", "value");

        ObelixStore obelixStore = new RedisObelixStore();
        obelixStore.set("a", firstElement);

        assertEquals(obelixStore.get("a"), firstElement);

    }

    public void testMultipleSetAndGetRedis() {
        ObelixStoreElement firstElement = new ObelixStoreElement("key", "value");
        ObelixStoreElement secondElement = new ObelixStoreElement("b", "2");

        ObelixStore obelixStore = new RedisObelixStore();
        obelixStore.set("a", firstElement);
        obelixStore.set("b", secondElement);

        assertEquals(obelixStore.get("a"), firstElement);
        assertEquals(obelixStore.get("b"), secondElement);

    }

    public void testReuseSetAndGetRedis() {
        ObelixStoreElement firstElement = new ObelixStoreElement("key", "value");
        ObelixStoreElement secondElement = new ObelixStoreElement("b", "2");

        ObelixStore obelixStore = new RedisObelixStore();
        obelixStore.set("a", firstElement);
        obelixStore.set("a", secondElement);

        assertEquals(obelixStore.get("a"), secondElement);

    }

}
