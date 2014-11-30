import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.params.Parameters;

import java.util.List;

public class ESClient {




    public static void main(String...args) {

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder("http://localhost:9200")
                .multiThreaded(true)
                .build());
        JestClient client = factory.getObject();

        String query = "{\"query\":{\"match_all\":{}}}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex("cds-stats")
                .setParameter(Parameters.SIZE, 2)
                .setParameter(Parameters.SCROLL, "5m")
                .build();

        String scrollId = "";

        try {
            SearchResult result = client.execute(search);
            List<SearchResult.Hit<ESLogEntry, Void>> hits = result.getHits(ESLogEntry.class);

            scrollId = result.getJsonObject().get("_scroll_id").getAsString();

            for(SearchResult.Hit<ESLogEntry, Void> hit : hits) {
                System.out.println(hit.source.id_bibdoc);
            }

            System.out.println(scrollId);
            System.out.println(result.getTotal());

            //SearchScroll searchScroll = new SearchScroll.Builder(scrollId, "1m").build();
            //System.out.println(searchScroll.getURI());

            search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex("cds-stats")
                    .setParameter(Parameters.SIZE, 2)
                    .setParameter(Parameters.SCROLL, "5m")
                    .setParameter(Parameters.SCROLL_ID, scrollId)
                    .build();

            result = client.execute(search);
            List<SearchResult.Hit<ESLogEntry, Void>> nhits = result.getHits(ESLogEntry.class);

            scrollId = result.getJsonObject().get("_scroll_id").getAsString();

            System.out.println(scrollId);

            for(SearchResult.Hit<ESLogEntry, Void> hit : nhits) {
                System.out.println(hit.source.id_bibdoc);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }




    }







}
