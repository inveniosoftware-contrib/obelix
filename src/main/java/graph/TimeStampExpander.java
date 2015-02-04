package graph;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.ArrayList;
import java.util.List;

public class TimeStampExpander implements PathExpander {

    long until;
    long since;
    int depth;

    public TimeStampExpander(String since, String until, String depth) {
        this.depth = Integer.parseInt(depth);

        if(until != null) {
            this.until = Long.valueOf(until);
        } else {
            this.until = Long.MAX_VALUE;
        }

        if(since != null) {
            this.since = Long.valueOf(since);
        } else {
            this.since = Long.MIN_VALUE;
        }

    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState state) {

        List<Relationship> result = new ArrayList<>();

        if(this.since == Long.MIN_VALUE && this.until == Long.MAX_VALUE) {
            return path.endNode().getRelationships();
        }

        if(path.length() >= this.depth) {
            return result;
        }

        for(Relationship rel : path.endNode().getRelationships()) {
            long current = Long.parseLong(rel.getProperty("timestamp").toString());
            if(until > current && current > since) {
                result.add(rel);
            }
        }

        //System.out.println("relationships: " + path.relationships() );
        //System.out.println("result size: " + result.size() );
        return result;

    }

    @Override
    public PathExpander reverse() {
        return this;
    }

}
