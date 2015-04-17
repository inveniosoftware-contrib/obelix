package graph.impl;

import graph.interfaces.ObelixGraphNode;

public abstract class NeoGraphNode implements ObelixGraphNode {

    final NeoObelixGraphDatabase neoObelixGraphDatabase;
    final String id;

    public NeoGraphNode(NeoObelixGraphDatabase neoObelixGraphDatabase, String id) {
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