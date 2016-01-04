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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class TestMainParseArguments extends TestCase {
    private Main obelixMain;
    private Method mParseArguments;
    private static String METHOD_PARSEARGUMENTS = "parseArguments";
    private Field neoLocation;
    private Field redisQueuePrefix;

    public void setUp() throws Exception {
        obelixMain = new Main();
        mParseArguments = obelixMain.getClass().getDeclaredMethod(METHOD_PARSEARGUMENTS, String[].class);
        mParseArguments.setAccessible(true);

        neoLocation = obelixMain.getClass().getDeclaredField("neoLocation");
        neoLocation.setAccessible(true);

        redisQueuePrefix = obelixMain.getClass().getDeclaredField("redisQueuePrefix");
        redisQueuePrefix.setAccessible(true);
    }

    public void testArguments() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException{
        String[] args = { "--neo4jStore", "/home/test/graph.db", "--redis-queue-prefix",
                            "obelix::", "--web-port", "1234" };
        Boolean method = (Boolean) mParseArguments.invoke(obelixMain, new Object[] {args});

        assertTrue(method);
        assertEquals("/home/test/graph.db", (String) neoLocation.get(obelixMain));
        assertEquals("obelix::", (String) redisQueuePrefix.get(obelixMain));
    }

    public void testEmptyPrefix() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException{
        String[] args = { "--neo4jStore", "/home/test/graph.db", "--redis-queue-prefix", "","--web-port", "1234" };
        Boolean method = (Boolean) mParseArguments.invoke(obelixMain, new Object[] {args});

        assertTrue(method);
        assertEquals("/home/test/graph.db", (String) neoLocation.get(obelixMain));
        assertEquals("", (String) redisQueuePrefix.get(obelixMain));
    }

    public void testUnknownParameter() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException{
        String[] args = { "--unknown", "/home/test/graph.db", "--redis-queue-prefix", "","--web-port", "1234" };
        Boolean method = (Boolean) mParseArguments.invoke(obelixMain, new Object[] {args});

        assertFalse(method);
    }
}
