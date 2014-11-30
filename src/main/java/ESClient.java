import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ESClient {
    private Client client;
    private boolean readOnly;

    public ESClient(String host, int port) {
        this("elastcisearch", host, port, false);
    }

    public ESClient(String clusterName, String host, int port, boolean readOnly) {
        this.readOnly = readOnly;

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", clusterName).build();

        this.client = new TransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(host, port));

    }

    public Client getClient() {
        return this.client;
    }

    public SearchResponse search(QueryBuilder qb, FilterBuilder filter) {
        return getClient().prepareSearch()
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(qb)
                .setPostFilter(filter)
                .setSize(500).execute().actionGet();
    }

    public SearchResponse search(QueryBuilder qb) {
        return getClient().prepareSearch()
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(qb)
                .setSize(500).execute().actionGet();
    }

    public void bulkIndex(String index, List<Map<String, String>> map) throws Exception {

        if (this.readOnly) {
            throw new Exception("This ES cluster is in read only mode");
        }

        BulkRequestBuilder bulkRequest = getClient().prepareBulk();

        for (Map<String, String> document : map) {
            try {
                bulkRequest.add(getClient().prepareIndex(index, "download-log")
                        .setSource(jsonfyMap(document)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (map.size() > 0) {
            BulkResponse bulkResponse = bulkRequest.execute().actionGet();
            if (bulkResponse.hasFailures()) {
                System.out.println("error..");
            }
        }
    }

    private XContentBuilder jsonfyMap(Map<String, String> map) throws IOException {

        XContentBuilder json = jsonBuilder().startObject();

        for (String key : map.keySet()) {
            json.field(key, map.get(key));
        }

        json.endObject();
        return json;

    }

}
