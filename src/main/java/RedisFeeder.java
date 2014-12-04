import es.ESClient;
import es.Hit;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;

public class RedisFeeder implements Runnable {

    public void run() {

        ESClient esClient = new ESClient("http://localhost:9200");
        Jedis jedis = new Jedis("localhost", 6379);

        System.out.println("Let's import all we have from ES");
        List<Hit> hits = null;
        try {
            hits = esClient.fetchLatest("1500", 1500000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Hit hit : hits) {
            jedis.lpush("logentries", hit.sourceToJson());
        }

        System.out.println("Done imporating all from ES!");

        /*
        while (true) {

            List<Hit> hits = null;
            try {
                hits = esClient.fetchLatest("1500", 1500000);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (Hit hit : hits) {
                jedis.lpush("logentries", hit.sourceToJson());
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

    }
}
