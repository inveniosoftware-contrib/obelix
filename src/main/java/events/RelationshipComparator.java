package events;

import org.neo4j.graphdb.Relationship;

import java.io.Serializable;
import java.util.Comparator;

public class RelationshipComparator implements Serializable, Comparator<Relationship> {

    @Override
    public final int compare(final Relationship o1, final Relationship o2) {
        Long timestampA = Long.parseLong(o1.getProperty("timestamp").toString());
        Long timestampB = Long.parseLong(o2.getProperty("timestamp").toString());
        return timestampB.compareTo(timestampA);
    }

}
