import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.List;

public class RedisQueueManager {

    JedisPool pool;
    String queueName;
    String prefix;

    public RedisQueueManager(String queueName) {
        this.queueName = queueName;
        this.prefix = "obelix::";
        this.connect();
    }

    private synchronized void connect() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        this.pool = new JedisPool(config, "localhost", 6379, 500000);
    }

    private synchronized void ensureConnected() {
        try (Jedis jedis = pool.getResource()) {
            if(!jedis.isConnected()) {
                jedis.connect();
            }
        } catch (JedisConnectionException e) {
            this.connect();
        }
    }

    public synchronized String pop() {
        ensureConnected();

        try (Jedis jedis = pool.getResource()) {
            return jedis.lpop(this.prefix + this.queueName);
        }
    }

    public synchronized void rpush(String entry) {
        ensureConnected();

        try (Jedis jedis = pool.getResource()) {
            jedis.rpush(this.prefix + this.queueName, entry);
        }
    }

    public synchronized List<String> lrange() {
        ensureConnected();

        try (Jedis jedis = pool.getResource()) {
            return jedis.lrange(this.prefix + this.queueName, 0, jedis.llen(this.prefix + this.queueName));
        }
    }

    public synchronized String set(String key, String value) {
        ensureConnected();

        try (Jedis jedis = pool.getResource()) {
            return jedis.set(this.prefix + key, value);
        }
    }

    public synchronized String get(String key) {
        ensureConnected();

        try (Jedis jedis = pool.getResource()) {
            return jedis.get(this.prefix + key);
        }
    }

}