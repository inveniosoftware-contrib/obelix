import com.google.gson.Gson;
import es.Source;
import org.neo4j.graphdb.*;
import redis.clients.jedis.Jedis;

public class NeoFeeder implements Runnable {

    GraphDatabaseService graphDb;

    public NeoFeeder(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    private static enum RelTypes implements RelationshipType {
        READ
    }

    public void run() {

        Jedis jedis = new Jedis("localhost", 6379);

        Label userLabel = DynamicLabel.label("User");
        Label recordLabel = DynamicLabel.label("Record");

        ResourceIterator<Node> userList;
        ResourceIterator<Node> recordList;

        Gson gson;
        Source source;
        Node user;
        Node record;

        int imported = 0;

        while (true) {

            String result = jedis.rpop("logentries");

            if (result != null) {
                gson = new Gson();
                source = gson.fromJson(result, Source.class);

                try (Transaction tx = graphDb.beginTx()) {

                    userList = graphDb.findNodesByLabelAndProperty(userLabel, "id_user", source.getUserID()).iterator();

                    if (userList.hasNext()) {
                        user = userList.next();
                    } else {
                        user = graphDb.createNode();
                        user.addLabel(userLabel);
                        user.setProperty("id_user", source.getUserID());
                        user.setProperty("ip_user", source.getClientHost());
                    }

                    recordList = graphDb.findNodesByLabelAndProperty(recordLabel, "id_bibrec", source.getBibrecID()).iterator();

                    if (recordList.hasNext()) {
                        record = recordList.next();
                    } else {
                        record = graphDb.createNode();
                        record.addLabel(recordLabel);
                        record.setProperty("id_bibdoc", source.getBibdocID());
                        record.setProperty("id_bibrec", source.getBibrecID());
                        record.setProperty("file_format", source.getFileFormat());
                    }

                    boolean registed = false;

                    for (Relationship relationship : user.getRelationships()) {
                        if (relationship.getEndNode().getProperty("id_bibrec") == source.getBibrecID()) {
                            registed = true;
                        }
                    }

                    if (!registed) {
                        user.createRelationshipTo(record, RelTypes.READ);
                    }

                    // Database operations go here
                    tx.success();

                    imported += 1;

                    if (imported % 100 == 0) {
                        System.out.println("Feeded " + imported + " nodes to neo4j");
                    }
                }


                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }

    }


}
