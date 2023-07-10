package com.vampbear.jappalyzer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpClient {

    private Map<String, String> config;

    public HttpClient() {
        this(Collections.emptyMap());
    }

    public HttpClient(Map<String, String> config) {
        this.config = config;
    }

    public PageResponse getPageByUrl(String url) throws IOException {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(getConnectTimeout(config))
                .setConnectionRequestTimeout(getConnectTimeout(config))
                .setSocketTimeout(getConnectTimeout(config))
                .build();

        PageResponse response;
        try (CloseableHttpClient httpclient = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent(getUserAgent(config))
                .build()) {

            HttpGet httpget = new HttpGet(url);
            ResponseHandler<PageResponse> responseHandler = res -> {
                int status = res.getStatusLine().getStatusCode();

                HttpEntity entity = res.getEntity();
                String content = (entity != null) ? EntityUtils.toString(entity) : "";
                Header[] headers = res.getAllHeaders();

                return new PageResponse(status, headers, content);
            };

            response = httpclient.execute(httpget, responseHandler);
        }

        return response;
    }

    private int getConnectTimeout(Map<String, String> config) {
        if (config.containsKey("connect.timeout")) {
            try {
                return Integer.parseInt(config.get("connect.timeout"));
            } catch (NumberFormatException e) {
                return 3000;
            }
        } else {
            return 3000;
        }
    }

    private String getUserAgent(Map<String, String> config) {
        return config.getOrDefault("connect.useragent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41");
    }
}
