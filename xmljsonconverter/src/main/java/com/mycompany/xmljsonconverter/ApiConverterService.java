package com.mycompany.xmljsonconverter;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ApiConverterService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String baseUrl; // ex: http://localhost:8080/api

    public ApiConverterService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String xmlToJson(String xml) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/xml-to-json"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(xml))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("API error " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

    public String jsonToXml(String json, String root) throws Exception {
        String r = (root == null || root.isBlank()) ? "root" : root.trim();
        String encoded = URLEncoder.encode(r, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/json-to-xml?root=" + encoded))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("API error " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }
    
    public String jsonToXmlAutoRoot(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/json-to-xml-auto"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("API error " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

}
