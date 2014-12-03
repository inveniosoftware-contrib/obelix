import es.ESClient;
import es.Hit;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String... args) {

        ESClient esClient = new ESClient("http://localhost:9200");
        ESClient esClient2 = new ESClient("http://localhost:9201");

        try {

            int index = 1600000;

            List<Hit> hits = esClient.fetchLatest(index);
            List<Hit> hitsKey = esClient2.fetchAllUntilKeyFound(hits.get(hits.size()-1).getId());

            int i = 0;

            System.out.println("hits    size: " + hits.size());
            System.out.println("hitsKey size: " + hitsKey.size());


            if(hits.get(hits.size()-1).getId().equals(hitsKey.get(hitsKey.size()-1).getId())) {
                System.out.println("Success!! ");
            }

            /*
            while (i < index - 1) {
                System.out.println(
                        i + " | " +
                        hits.get(i).getId().equals(hitsKey.get(i).getId()) + " | "+
                        hits.get(i).getId() + " | " +
                        hitsKey.get(i).getId());

                i += 1;
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
