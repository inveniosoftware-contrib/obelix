package graph;


import graph.interfaces.GraphDatabase;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;

import java.util.List;


public class ItemGraph {
    public static final Label LABEL = DynamicLabel.label("Item");

    GraphDatabase graphdb;

    public ItemGraph(GraphDatabase graphdb) {
        this.graphdb = graphdb;
    }

    public List<String> getAll() {
        return this.graphdb.getAllNodeIds("Item");
    }

}
