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
