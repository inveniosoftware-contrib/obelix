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
package graph;

public class UserItemRelationship {
    public static final int STATIC_IMPORTANCE_FACTOR = 10;
    private String itemid;
    private int depth;

    public final double getImportanceFactor(final int importanceFactorInteger) {
        return Math.max(0.0, 1.0
                / (STATIC_IMPORTANCE_FACTOR * depth * importanceFactorInteger + 1.0));
    }

    public UserItemRelationship(final String itemIdInput, final int depthInput) {
        this.itemid = itemIdInput;
        this.depth = depthInput;
    }

    public final String getItemid() {
        return itemid;
    }
}
