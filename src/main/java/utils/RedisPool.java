package utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;


public class RedisPool {

    public static final int REDIS_PORT_NUMBER = 6379;
    public static final int REDIS_TIMEOUT = 500000;
    private JedisPool pool;

    public RedisPool() {
        this.connect();
    }

    private synchronized void connect() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        this.pool = new JedisPool(config, "localhost", REDIS_PORT_NUMBER, REDIS_TIMEOUT);
    }

    private synchronized void ensureConnected() {
        try (Jedis jedis = pool.getResource()) {
            if (!jedis.isConnected()) {
                jedis.connect();
            }
        } catch (JedisConnectionException e) {
            this.connect();
        }
    }

    public final JedisPool getRedis() {
        ensureConnected();
        return this.pool;
    }

}
