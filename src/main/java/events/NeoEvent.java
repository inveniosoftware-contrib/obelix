package events;


import org.neo4j.graphdb.GraphDatabaseService;

public interface NeoEvent {

    public Long getUser();
    public String print();

    public boolean validate();
    public void execute(GraphDatabaseService graphDb);

}
