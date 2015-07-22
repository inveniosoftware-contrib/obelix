package events;


import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;

public interface NeoEvent {

    String getUser();
    String getType();
    String getItem();
    String getTimestamp();

    boolean validate();
    void execute(GraphDatabase graphDb, int maxRelationships) throws ObelixInsertException;

}
