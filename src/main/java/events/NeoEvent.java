package events;


import graph.exceptions.ObelixInsertException;
import graph.interfaces.GraphDatabase;

public interface NeoEvent {

    public String getUser();
    public String getType();
    public String getItem();
    public String getTimestamp();

    public boolean validate();
    public void execute(GraphDatabase graphDb, int maxRelationships) throws ObelixInsertException;

}