package graph;


import graph.interfaces.GraphDatabase;

import java.util.List;


public class ItemGraph {

    private final GraphDatabase graphdb;

    public ItemGraph(final GraphDatabase graphdbInput) {
        this.graphdb = graphdbInput;
    }

    public final List<String> getAll() {
        return this.graphdb.getAllNodeIds("Item");
    }

}
