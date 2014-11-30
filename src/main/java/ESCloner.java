import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ESCloner {

    public static void main(String[] args) {

        ESClient clientCERN = new ESClient("cds-statistics", "127.0.0.1", 8300, true);
        ESClient clientLocal = new ESClient("elasticsearch", "localhost", 9300, false);

        QueryBuilder qb = termQuery("_type", "download-log");
        FilterBuilder filter = FilterBuilders.boolFilter()
                .mustNot(FilterBuilders.termFilter("id_user", 0));

        SearchResponse scrollResp = clientCERN.search(qb, filter);

        while (true) {

            List<Map<String, String>> results = new ArrayList<>();

            System.out.println(scrollResp.getHits().getTotalHits());

            for (SearchHit hit : scrollResp.getHits()) {

                Map<String, String> map = new HashMap<>();

                map.put("id_bibrec", Integer.toString((Integer) hit.sourceAsMap().get("id_bibrec")));
                map.put("id_bibdoc", Integer.toString((Integer) hit.sourceAsMap().get("id_bibdoc")));
                map.put("id_user", Integer.toString((Integer) hit.sourceAsMap().get("id_user")));
                map.put("file_format", (String) hit.sourceAsMap().get("file_format"));
                map.put("timestamp", (String) hit.sourceAsMap().get("@timestamp"));
                map.put("file_version", Integer.toString((Integer) hit.sourceAsMap().get("file_version")));
                map.put("client_host", (String) hit.sourceAsMap().get("client_host"));
                results.add(map);
            }

            try {
                clientLocal.bulkIndex("cds-stats", results);
            } catch (Exception e) {
                e.printStackTrace();
            }

            scrollResp = clientCERN.getClient().prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();

            //Break condition: No hits are returned
            if (scrollResp.getHits().getHits().length == 0) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
