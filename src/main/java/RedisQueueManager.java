import redis.clients.jedis.Jedis;

public class RedisQueueManager {

    Jedis jedis;
    String queueName;

    public RedisQueueManager(String queueName) {
        this.jedis = new Jedis("localhost", 6379);
        this.queueName = queueName;
    }

    public synchronized String pop() {
        return jedis.lpop(this.queueName);
    }

    public synchronized void rpush(String entry) {
        jedis.rpush(this.queueName, entry);
    }

}