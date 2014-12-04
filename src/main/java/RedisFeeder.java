import es.ESClient;
import es.Hit;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;

public class RedisFeeder implements Runnable {

    public void run() {

        ESClient esClient = new ESClient("http://localhost:9200");
        Jedis jedis = new Jedis("localhost", 6379);

        while (true) {

            List<Hit> hits = null;
            try {
                hits = esClient.fetchLatest("100", 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (Hit hit : hits) {
                jedis.lpush("logentries", hit.sourceToJson());
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
