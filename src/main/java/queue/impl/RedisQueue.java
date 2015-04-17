package queue.impl;


import queue.interfaces.ObelixQueue;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import utils.RedisPool;

import java.util.List;

public class RedisQueue implements ObelixQueue {

    private JedisPool redisPool;
    private String queueName;
    private String prefix;

    public RedisQueue(String queueName) {
        this.redisPool = new RedisPool().getRedis();
        this.queueName = queueName;
        this.prefix = "obelix:queue:";
    }

    public synchronized String pop() {
        try (Jedis jedis = this.redisPool.getResource()) {
            return jedis.lpop(this.prefix + this.queueName);
        }
    }

    public synchronized void push(String entry) {
        try (Jedis jedis = this.redisPool.getResource()) {
            jedis.rpush(this.prefix + this.queueName, entry);
        }
    }

    public synchronized List<String> getAll() {
        try (Jedis jedis = this.redisPool.getResource()) {
            return jedis.lrange(this.prefix + this.queueName, 0, jedis.llen(this.prefix + this.queueName));
        }
    }

}
