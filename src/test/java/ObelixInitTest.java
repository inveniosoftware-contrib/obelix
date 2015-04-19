import junit.framework.TestCase;
import obelix.Obelix;

public class ObelixInitTest extends TestCase {

    public void testBuilderNumberOfRelationships() throws Exception {

        Obelix.Builder obelixBuilder = new Obelix.Builder();
        obelixBuilder.setMaxNumberOfRelationships(20);
        Obelix obelix = obelixBuilder.createObelix();

        assertEquals(20, obelix.numberOfRelationships);

    }
    public void testSetNeo4jStoreAndNumberOfRelationships() throws Exception {

        Obelix.Builder obelixBuilder = new Obelix.Builder();
        obelixBuilder.setMaxNumberOfRelationships(20);
        obelixBuilder.setNeo4jStore("/tmp/secret/path");
        Obelix obelix = obelixBuilder.createObelix();

        assertEquals(20, obelix.numberOfRelationships);
        assertEquals("/tmp/secret/path", obelix.neo4jStore);

        Obelix obelix2 = new Obelix.Builder()
                .setMaxNumberOfRelationships(3)
                .setNeo4jStore("path")
                .createObelix();

    }



}
