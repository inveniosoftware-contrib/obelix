package graph.impl;

import graph.interfaces.GraphNode;

public abstract class NeoGraphNode implements GraphNode {

    final NeoGraphDatabase neoObelixGraphDatabase;
    final String id;

    public NeoGraphNode(NeoGraphDatabase neoObelixGraphDatabase, String id) {
        this.neoObelixGraphDatabase = neoObelixGraphDatabase;
        this.id = id;
    }

    public String getID() {
        return this.id;
    }

    public String getNeighbors() {
        return null;
    }
}