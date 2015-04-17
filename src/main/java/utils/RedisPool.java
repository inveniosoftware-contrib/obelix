package utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;


public class RedisPool {

    private JedisPool pool;

    public RedisPool() {
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

    public JedisPool getRedis() {
        ensureConnected();
        return this.pool;
    }

}