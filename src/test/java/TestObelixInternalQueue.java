import junit.framework.TestCase;
import queue.impl.InternalObelixQueue;
import queue.impl.ObelixQueueElement;
import queue.interfaces.ObelixQueue;

public class TestObelixInternalQueue extends TestCase {

    public void testInternalQueuePushAndPop() {

        ObelixQueue obelixQueue = new InternalObelixQueue();

        obelixQueue.push(new ObelixQueueElement("a", "1"));
        obelixQueue.push(new ObelixQueueElement("b", "2"));
        obelixQueue.push(new ObelixQueueElement("c", "3"));

        assertEquals(obelixQueue.getAll().size(), 3);

        assertEquals(obelixQueue.pop(), new ObelixQueueElement("a", "1"));
        assertEquals(obelixQueue.pop(), new ObelixQueueElement("b", "2"));
        assertEquals(obelixQueue.pop(), new ObelixQueueElement("c", "3"));

    }

}
