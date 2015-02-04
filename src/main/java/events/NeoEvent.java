package events;


import org.neo4j.graphdb.GraphDatabaseService;

public interface NeoEvent {

    public String getUser();
    public String getType();
    public String getItem();
    public String getTimestamp();

    public boolean validate();
    public void execute(GraphDatabaseService graphDb, int maxRelationships);

}