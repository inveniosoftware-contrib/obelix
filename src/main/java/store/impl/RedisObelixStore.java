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

    public RedisObelixStore(final String prefixInput) {
        this.prefix = prefixInput;
        this.redisPool = new RedisPool();
    }

    @Override
    public final void set(final String key, final ObelixStoreElement value) {
        try (Jedis jedis = this.redisPool.getRedis().getResource()) {
            jedis.set(this.prefix + key, value.getData().toString());
        }
    }

    @Override
    public final ObelixStoreElement get(final String key) {
        try (Jedis jedis = this.redisPool.getRedis().getResource()) {
            return new ObelixStoreElement(jedis.get(this.prefix + key));
        } catch (NullPointerException e) {
            return null;
        }
    }
}
