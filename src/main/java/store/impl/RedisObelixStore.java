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
package store.impl;


import redis.clients.jedis.Jedis;
import store.interfaces.ObelixStore;
import utils.RedisPool;

public class RedisObelixStore implements ObelixStore {

    private RedisPool redisPool;
    private String prefix;

    public RedisObelixStore(final String redisHost) {
        this.prefix = "obelix:store:";
        this.redisPool = new RedisPool(redisHost);
    }

    public RedisObelixStore(final String prefixInput, final String redisHost) {
        this.prefix = prefixInput;
        this.redisPool = new RedisPool(redisHost);
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
