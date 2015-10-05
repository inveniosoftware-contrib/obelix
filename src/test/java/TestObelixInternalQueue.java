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
