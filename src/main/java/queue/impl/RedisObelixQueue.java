package queue.impl;


import queue.interfaces.ObelixQueue;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import utils.RedisPool;

import java.util.ArrayList;
import java.util.List;

public class RedisObelixQueue implements ObelixQueue {

    private JedisPool redisPool;
    private String queueName;
    private String prefix;

    public RedisObelixQueue(String queueName) {
        this.redisPool = new RedisPool().getRedis();
        this.queueName = queueName;
        this.prefix = "obelix:queue:";
    }

    public synchronized ObelixQueueElement pop() {
        try (Jedis jedis = this.redisPool.getResource()) {
            if(jedis.llen(this.prefix + this.queueName) > 0) {
                return new ObelixQueueElement(jedis.lpop(this.prefix + this.queueName));
            }
            return null;
        }
    }

    public synchronized void push(ObelixQueueElement entry) {
        try (Jedis jedis = this.redisPool.getResource()) {
            jedis.rpush(this.prefix + this.queueName, entry.data.toString());
        }
    }

    public synchronized List<ObelixQueueElement> getAll() {
        try (Jedis jedis = this.redisPool.getResource()) {
            List<ObelixQueueElement> result = new ArrayList<>();
            for(String obj : jedis.lrange(this.prefix + this.queueName, 0, jedis.llen(this.prefix + this.queueName))) {
                result.add(new ObelixQueueElement(obj));
            }
            return result;
        }
    }
}