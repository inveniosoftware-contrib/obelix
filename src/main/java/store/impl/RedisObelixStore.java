package store.impl;


import redis.clients.jedis.Jedis;
import store.interfaces.ObelixStore;
import utils.RedisPool;

public class RedisObelixStore implements ObelixStore {

    private RedisPool redisPool;
    private String prefix;

    public RedisObelixStore() {
        this.prefix = "obelix:store:";
        this.redisPool = new RedisPool();
    }

    public void set(String key, String value) {
        try (Jedis jedis = this.redisPool.getRedis().getResource()) {
            jedis.set(this.prefix + key, value);
        }
    }

    public String get(String key) {
        try (Jedis jedis = this.redisPool.getRedis().getResource()) {
            return jedis.get(this.prefix + key);
        }
    }
}
