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
