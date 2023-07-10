package com.vampbear.jappalyzer;

import org.apache.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.HttpCookie;
import java.util.*;
import java.util.stream.Collectors;

public class PageResponse {

    private final int statusCode;
    private Document document;
    private String origContent;
    private List<String> contentLines;

    private final List<String> scriptSources = new LinkedList<>();
    private final Map<String, List<String>> headers = new HashMap<>();
    private final Map<String, List<String>> cookies = new HashMap<>();
    private final Map<String, List<String>> metaMap = new HashMap<>();

    public PageResponse(String content) {
        this(200, new Header[]{}, content);
    }

    public PageResponse(int statusCode, Header[] headers, String content) {
        this.statusCode = statusCode;
        this.setHeaders(headers);
        this.processContent(content);
    }

    private void processContent(String content) {
        this.origContent = content;
        this.document = Jsoup.parse(content);
        BufferedReader bf = new BufferedReader(new StringReader(this.document.outerHtml()));
        this.contentLines = bf.lines().collect(Collectors.toList());

        Elements scripts = document.select("script");
        for (Element script : scripts) {
            String scriptSrc = script.attr("src");
            if (!scriptSrc.equals("")) {
                this.scriptSources.add(scriptSrc);
            }
        }

        Elements metas = document.select("meta");
        for (Element meta : metas) {
            String metaName = meta.attr("name");
            String metaContent = meta.attr("content");
            metaMap.putIfAbsent(metaName, new LinkedList<>());
            metaMap.get(metaName).add(metaContent);
        }
    }

    public void setHeaders(Header[] headers) {
        if (headers == null) return;
        for (Header header : headers) {
            if (header != null) {
                List<String> headerValue = new ArrayList<>();
                headerValue.add(header.getValue());
                this.headers.merge(
                        header.getName().toLowerCase(),
                        headerValue,
                        (existingList, incomingList) -> {
                            existingList.addAll(incomingList);
                            return existingList;
                        });
            }
        }
        processCookies(this.headers.get("set-cookie"));
        processCookies(this.headers.get("cookie"));
    }

    //TODO: Check for adding cookies twice with set headers and add header
    public void addHeader(String name, String value) {
        this.headers.putIfAbsent(name.toLowerCase(), new LinkedList<>());
        this.headers.get(name.toLowerCase()).add(value);

        processCookies(headers.get("set-cookie"));
        processCookies(headers.get("cookie"));
    }

    private void processCookies(List<String> cookieValues) {
        if (cookieValues == null) return;
        for (String cookieValue : cookieValues) {
            List<HttpCookie> cookies = HttpCookie.parse(cookieValue);
            for (HttpCookie cookie : cookies) {
                this.addCookie(cookie.getName(), cookie.getValue());
            }
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getOrigContent() {
        return origContent;
    }

    public Document getDocument() {
        return document;
    }

    public void addCookie(String name, String value) {
        this.cookies.computeIfAbsent(name, k -> new LinkedList<>());
        this.cookies.get(name).add(value);
    }

    public Map<String, List<String>> getCookies() {
        return this.cookies;
    }

    public List<String> getScriptSources() {
        return this.scriptSources;
    }

    public Map<String, List<String>> getMetaMap() {
        return this.metaMap;
    }

    public List<String> getContentLines() {
        return contentLines;
    }
}
