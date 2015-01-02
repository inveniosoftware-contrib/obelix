package graph;


import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;

import java.util.List;

import static events.NeoHelpers.getAllNodes;

public class ItemGraph {

    public static final Label LABEL = DynamicLabel.label("Item");

    GraphDatabaseService graphdb;

    public ItemGraph(GraphDatabaseService graphdb) {
        this.graphdb = graphdb;
    }

    public List<String> getAll() {
        return getAllNodes(this.graphdb, "Item");
    }

}
