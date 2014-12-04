package es;


import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ESClient {
    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();

    String hostname;

    public ESClient(String hostname) {
        this.hostname = hostname;
    }

    public static class ServerUrl extends GenericUrl {
        public ServerUrl(String encodedUrl) {
            super(encodedUrl);
        }
    }

    private HttpRequestFactory getRequestFactory() {
        return HTTP_TRANSPORT.createRequestFactory(
                request -> {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                });
    }

    private Result fetchResult(String size, String scrollID) throws IOException {
        ServerUrl url = new ServerUrl(this.hostname + "/_search?size=" + size + "&scroll=1m");
        HttpRequest request;
        String sortDescending;

        if (scrollID != null) {
            url = new ServerUrl(this.hostname + "/_search/scroll/" + scrollID + "?scroll=1m&size=" + size);
            request = getRequestFactory().buildGetRequest(url);

        } else {

            sortDescending = "{\n" +
                    "\"query\":{\"match_all\":{}},\n" +
                    "\"sort\": {\n" +
                    "    \"timestamp\": {\n" +
                    "        \"order\": \"desc\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            request = getRequestFactory().buildPostRequest(url,
                    ByteArrayContent.fromString("application/json", sortDescending));

        }

        return request.execute().parseAs(Result.class);
    }

    public List<Hit> fetchLatest(String size, int limit) throws IOException {
        //System.out.println("Fetching last " + limit + " entries");
        List<Hit> finalResult = new ArrayList<>();

        Queue<Hit> queue = new LinkedList<>();

        Result result = fetchResult(size, null);
        queue.addAll(result.getResult());

        String scrollID = result.scrollID;

        while (!queue.isEmpty()) {
            finalResult.add(queue.remove());

            if (finalResult.size() >= limit) {
                return finalResult;
            }

            if (queue.isEmpty() && scrollID != null) {
                Result next = fetchResult(size, scrollID);
                queue.addAll(next.getResult());
                scrollID = next.scrollID;
            }
        }

        //System.out.println("Done fetching last " + finalResult.size() + " entries");
        return finalResult;
    }

    public List<Hit> fetchAllUntilKeyFound(String key) throws IOException {
        System.out.println("Fetching all entries since key: " + key + " appeared");
        List<Hit> finalResult = new ArrayList<>();

        Queue<Hit> queue = new LinkedList<>();
        Result result = fetchResult("100", null);
        queue.addAll(result.getResult());
        String scrollID = result.scrollID;

        while (!queue.isEmpty()) {
            Hit hit = queue.remove();
            finalResult.add(hit);

            if (hit.getId().equals(key)) {
                return finalResult;
            }

            if (queue.isEmpty() && scrollID != null) {
                Result next = fetchResult("100", scrollID);
                queue.addAll(next.getResult());
                scrollID = next.scrollID;
            }
        }

        System.out.println("Done fetching all entries (" + finalResult.size() + ")) since key: " + key + " appeared");
        return null;

    }
}