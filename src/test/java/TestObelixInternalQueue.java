import junit.framework.TestCase;
import queue.impl.InternalObelixQueue;
import queue.interfaces.ObelixQueue;

public class TestObelixInternalQueue extends TestCase {

    public void testInternalQueuePushAndPop() {

        ObelixQueue obelixQueue = new InternalObelixQueue();

        obelixQueue.push("1");
        obelixQueue.push("2");
        obelixQueue.push("3");

        assertEquals(obelixQueue.getAll().size(), 3);

        assertEquals(obelixQueue.pop(), "1");
        assertEquals(obelixQueue.pop(), "2");
        assertEquals(obelixQueue.pop(), "3");

    }

}
