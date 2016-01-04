/**
 * This file is part of Obelix.
 *
 * Obelix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Obelix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Obelix.  If not, see <http://www.gnu.org/licenses/>.
 */
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
