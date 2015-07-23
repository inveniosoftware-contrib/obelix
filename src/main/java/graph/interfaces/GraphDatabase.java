package graph.interfaces;

import graph.exceptions.ObelixInsertException;
import graph.exceptions.ObelixNodeNotFoundException;
import graph.UserItemRelationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.List;

public interface GraphDatabase {

    List<String> getAllNodeIds(final String nodeName);

    List<String> getAllUserIds();


    String cleanRelationships(final String userid, final String max)
            throws ObelixNodeNotFoundException;

    String cleanAllRelationships(final String max) throws ObelixNodeNotFoundException;

    List<UserItemRelationship> getRelationships(final String userID,
                                                final String depth,
                                                final String sinceTimestamp,
                                                final String untilTimestamp,
                                                final boolean removeDuplicates)
            throws ObelixNodeNotFoundException;

    void makeSureUserItemLimitNotExceeded(final String node,
                                          final int maxRelationships)
            throws ObelixNodeNotFoundException;

    List<String> getRelationships();

    List<String> getRelationships(final String type);

    void createNodeNodeRelationship(final String nodeFrom,
                                    final String nodeTo,
                                    final RelationshipType relType,
                                    final String timestamp,
                                    final int maxRelationships)
            throws ObelixInsertException;

}
