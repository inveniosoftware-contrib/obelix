package graph.interfaces;

import graph.exceptions.ObelixInsertException;
import org.neo4j.graphdb.GraphDatabaseService;
import graph.exceptions.ObelixNodeNotFoundException;
import graph.UserItemRelationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.List;

public interface GraphDatabase {

    public void createUserNode();

    public void createItemNode();

    public void createRelationship();

    public List<String> getAllNodeIds(String nodeName);
    public List<String> getAllUserIds();


    public String cleanRelationships(String userid, String max) throws ObelixNodeNotFoundException;
    public String cleanAllRelationships(String max) throws ObelixNodeNotFoundException;

    public List<UserItemRelationship> getRelationships(String userID, String depth,
                                                       String sinceTimestamp, String untilTimestamp,
                                                       boolean removeDuplicates) throws ObelixNodeNotFoundException;

    public void makeSureTheUserDoesNotExceedMaxRelationshipsLimitObelix(String node, int maxRelationships) throws ObelixNodeNotFoundException;

    public List<String> getAllRelationships_();
    public List<String> getAllRelationships_(String type);

    public void createNodeNodeRelationship(String nodeFrom, String nodeTo, RelationshipType relType, String timestamp,
                                           int maxRelationships) throws ObelixInsertException;

}