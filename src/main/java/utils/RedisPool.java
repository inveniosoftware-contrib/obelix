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
package utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;


public class RedisPool {

    public static final int REDIS_PORT_NUMBER = 6379;
    public static final int REDIS_TIMEOUT = 500000;
    private String redisHost = "localhost";
    private JedisPool pool;

    public RedisPool(final String redisHost) {
        this.redisHost = redisHost;
        this.connect();
    }

    private synchronized void connect() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        this.pool = new JedisPool(config, redisHost, REDIS_PORT_NUMBER, REDIS_TIMEOUT);
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
