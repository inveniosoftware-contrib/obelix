package graph;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.ArrayList;
import java.util.List;

public class TimeStampExpander implements PathExpander {

    private Long until;
    private Long since;
    private int depth;

    public TimeStampExpander(final String sinceInput,
                             final String untilInput,
                             final String depthInputInput) {

        this.depth = Integer.parseInt(depthInputInput);

        if (untilInput != null) {
            this.until = Long.parseLong(untilInput);
        } else {
            this.until = Long.MAX_VALUE;
        }

        if (sinceInput != null) {
            this.since = Long.valueOf(sinceInput);
        } else {
            this.since = Long.MIN_VALUE;
        }

    }

    @Override
    public final Iterable<Relationship> expand(final Path path, final BranchState state) {

        List<Relationship> result = new ArrayList<>();

        if (this.since == Long.MIN_VALUE && this.until == Long.MAX_VALUE) {
            return path.endNode().getRelationships();
        }

        if (path.length() >= this.depth) {
            return result;
        }

        for (Relationship rel : path.endNode().getRelationships()) {
            long current = Long.parseLong(rel.getProperty("timestamp").toString());
            if (until > current && current > since) {
                result.add(rel);
            }
        }

        return result;
    }

    @Override
    public final PathExpander reverse() {
        return this;
    }

}
