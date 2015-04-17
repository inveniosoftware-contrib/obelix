import graph.impl.NeoObelixGraphDatabase;
import graph.interfaces.ObelixGraphDatabase;
import junit.framework.TestCase;

public class NeoTests  extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testRun() throws Exception {
        ObelixGraphDatabase obelixGraphDatabase = new NeoObelixGraphDatabase();
    }
}
