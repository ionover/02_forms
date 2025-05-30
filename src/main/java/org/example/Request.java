package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.InputStream;
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

    /**
     * Возвращает значение параметра запроса по его имени.
     *
     * @param name имя параметра
     *
     * @return Optional со значением параметра или пустой Optional, если параметр не найден
     */
    public Optional<String> getQueryParam(String name) {
        return queryParams.stream()
                          .filter(param -> param.getName().equals(name))
                          .map(NameValuePair::getValue)
                          .findFirst();
    }

    /**
     * Возвращает все параметры запроса.
     *
     * @return список пар имя-значение параметров запроса
     */
    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }
}
