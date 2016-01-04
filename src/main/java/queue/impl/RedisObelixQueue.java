/**
 * This file is part of Obelix.
 *
 * Obelix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Obelix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Obelix.  If not, see <http://www.gnu.org/licenses/>.
 */
package queue.impl;


import queue.interfaces.ObelixQueue;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import utils.RedisPool;

import java.util.List;
import java.util.stream.Collectors;

public class RedisObelixQueue implements ObelixQueue {

    private JedisPool redisPool;
    private String queueName;
    private String prefix;

    public RedisObelixQueue(final String queueName, final String redisHost) {
        this.redisPool = new RedisPool(redisHost).getRedis();
        this.queueName = queueName;
        this.prefix = "obelix:queue:";
    }

    public RedisObelixQueue(final String prefix, final String queueName, final String redisHost) {
        this.redisPool = new RedisPool(redisHost).getRedis();
        this.queueName = queueName;
        this.prefix = prefix;
    }

    public final synchronized ObelixQueueElement pop() {
        try (Jedis jedis = this.redisPool.getResource()) {
            if (jedis.llen(this.prefix + this.queueName) > 0) {
                return new ObelixQueueElement(jedis.lpop(this.prefix + this.queueName));
            }
            return null;
        }
    }

    public final synchronized void push(final ObelixQueueElement entry) {
        try (Jedis jedis = this.redisPool.getResource()) {
            jedis.rpush(this.prefix + this.queueName, entry.getData().toString());
        }
    }

    public final synchronized List<ObelixQueueElement> getAll() {
        try (Jedis jedis = this.redisPool.getResource()) {
            return jedis.lrange(this.prefix + this.queueName, 0,
                    jedis.llen(this.prefix + this.queueName))
                    .stream().map(ObelixQueueElement::new).collect(Collectors.toList());
        }
    }
}
