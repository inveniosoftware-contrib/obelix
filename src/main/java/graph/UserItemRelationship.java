package graph;

public class UserItemRelationship {
    String userid;
    public String itemid;
    String relname;
    String timestamp;
    int depth;

    public double getImportanceFactor(int importanceFactorInteger) {
        return Math.max(0.0, 1.0 / (10 * depth * importanceFactorInteger + 1.0));
    }

    public UserItemRelationship(String userid, String itemid, String relname, String timestamp, int depth) {
        this.userid = userid;
        this.itemid = itemid;
        this.relname = relname;
        this.depth = depth;
        this.timestamp = timestamp;
    }
}
