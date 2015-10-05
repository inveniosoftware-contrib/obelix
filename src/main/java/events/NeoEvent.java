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
