package events;

import org.neo4j.graphdb.Relationship;

import java.util.Comparator;

public class RelationshipComperator implements Comparator<Relationship> {
    @Override
    public int compare(Relationship o1, Relationship o2) {
        Long timestamp_a = Long.parseLong(o1.getProperty("timestamp").toString());
        Long timestamp_b = Long.parseLong(o2.getProperty("timestamp").toString());
        return timestamp_b.compareTo(timestamp_a);
    }
}
