package obelix;


public class Obelix {

    public final int numberOfRelationships;
    public final String neo4jStore;
    public final String redisQueueName;

    public static class Builder {

        public int numberOfRelationships;
        public String neo4jstore;
        public String redisQueueName;

        public Builder setMaxNumberOfRelationships(int numberOfRelationships) {
            this.numberOfRelationships = numberOfRelationships;
            return this;
        }

        public Builder setNeo4jStore(String neo4jstore) {
            this.neo4jstore = neo4jstore;
            return this;
        }

        public Builder setRedisQueueName(String redisQueueName) {

            if(redisQueueName == null || redisQueueName.length() < 5) {
                throw new IllegalArgumentException("The Redis Queue name needs to be longer than 5 characters");
            }

            this.redisQueueName = redisQueueName;

            return this;
        }

        public Obelix createObelix() {
            return new Obelix(this);
        }
    }

    public Obelix() {
        throw new IllegalArgumentException();
    }

    public Obelix(Builder builder) {
        this.numberOfRelationships = builder.numberOfRelationships;
        this.neo4jStore = builder.neo4jstore;
        this.redisQueueName = builder.redisQueueName != null ? builder.redisQueueName : "logentries";

    }

    public void run() {

    }

}
