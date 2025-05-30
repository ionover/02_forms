package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Request {

    private final String method;
    private final String path;
    private final String queryString;
    private final Map<String, String> headers;
    private final InputStream body;
    private final List<NameValuePair> queryParams;
    private List<NameValuePair> postParams;

    public Request(String method, String fullPath, Map<String, String> headers, InputStream body) {
        this.method = method;

        // Разделяем путь и query string
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex != -1) {
            this.path = fullPath.substring(0, queryIndex);
            this.queryString = fullPath.substring(queryIndex + 1);
            this.queryParams = URLEncodedUtils.parse(this.queryString, StandardCharsets.UTF_8);
        } else {
            this.path = fullPath;
            this.queryString = "";
            this.queryParams = Collections.emptyList();
        }

        this.headers = headers;
        this.body = body;
        this.postParams = Collections.emptyList();
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    /**
     * Парсит тело запроса, если оно представлено в формате x-www-form-urlencoded.
     */
    private void parsePostParams() {

        String contentType = headers.get("Content-Type");
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
                StringBuilder bodyContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    bodyContent.append(line);
                }

                if (!bodyContent.isEmpty()) {
                    postParams = URLEncodedUtils.parse(bodyContent.toString(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                System.err.println("Error parsing POST parameters: " + e.getMessage());
            }
        }
    }

    public List<String> getPostParamValuesByName(String name) {
        parsePostParams();
        return postParams.stream()
                         .filter(param -> param.getName().equals(name))
                         .map(NameValuePair::getValue)
                         .toList();
    }

    public List<NameValuePair> getPostParams() {
        parsePostParams();
        return postParams;
    }
}
