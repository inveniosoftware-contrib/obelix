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

    @Override
    public void set(String key, ObelixStoreElement value)
    {
        try (Jedis jedis = this.redisPool.getRedis().getResource()) {
            jedis.set(this.prefix + key, value.data.toString());
        }
    }

    @Override
    public ObelixStoreElement get(String key) {
        try (Jedis jedis = this.redisPool.getRedis().getResource()) {
            return new ObelixStoreElement(jedis.get(this.prefix + key));
        }
    }

}
