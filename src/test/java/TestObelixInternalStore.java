import junit.framework.TestCase;
import store.impl.InternalObelixStore;
import store.impl.ObelixStoreElement;
import store.interfaces.ObelixStore;

public class TestObelixInternalStore extends TestCase {
    public void testInternalStoreSingleSetAndGet() {

        ObelixStoreElement obelixStoreElement = new ObelixStoreElement("key", "val");

        ObelixStore obelixStore = new InternalObelixStore();
        obelixStore.set("a", obelixStoreElement);

        assertEquals(obelixStore.get("a").getData(), obelixStoreElement.getData());
        assertEquals(obelixStore.get("a"), obelixStoreElement);
    }

    public void testInternalStoreMultipleSetAndGet() {
        ObelixStore obelixStore = new InternalObelixStore();

        ObelixStoreElement element1 = new ObelixStoreElement("a", "1");

        obelixStore.set("a", element1);
        obelixStore.set("b", new ObelixStoreElement("b", "2"));
        obelixStore.set("c", new ObelixStoreElement("c", "3"));

        assertEquals(obelixStore.get("a"), new ObelixStoreElement("a", "1"));
        assertEquals(obelixStore.get("b"), new ObelixStoreElement("b", "2"));
        assertEquals(obelixStore.get("c"), new ObelixStoreElement("c", "3"));
    }


    public void testInternalStoreReuseKeys() {
        ObelixStore obelixStore = new InternalObelixStore();

        obelixStore.set("a", new ObelixStoreElement("a", "1"));
        obelixStore.set("a", new ObelixStoreElement("b", "2"));

        assertEquals(obelixStore.get("a"), new ObelixStoreElement("b", "2"));
    }
}
