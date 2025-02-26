package network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class client {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiEndpoint;

    public client(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String sendRequest(String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
