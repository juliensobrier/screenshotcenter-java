package com.screenshotcenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Crawl job API methods.
 */
public class CrawlNamespace {

    private final ScreenshotCenterClient client;

    CrawlNamespace(ScreenshotCenterClient client) {
        this.client = client;
    }

    /** Create a new crawl job. {@code url}, {@code domain}, and {@code maxUrls} are required. */
    public JSONObject create(String url, String domain, int maxUrls, Map<String, String> params) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }
        if (domain == null || domain.isEmpty()) {
            throw new IllegalArgumentException("domain is required");
        }
        JSONObject body = new JSONObject();
        body.put("url", url);
        body.put("domain", domain);
        body.put("max_urls", maxUrls);
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                body.put(e.getKey(), e.getValue());
            }
        }
        return postJson("/crawl/create", body);
    }

    /** Get crawl status and details. */
    public JSONObject info(long id) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        return client.get("/crawl/info", p);
    }

    /** List crawls. */
    public JSONArray list(Map<String, String> params) throws IOException {
        JSONObject raw = client.get("/crawl/list", params);
        if (raw.has("_array")) return raw.getJSONArray("_array");
        return new JSONArray(raw.toString());
    }

    /** Cancel a running crawl. */
    public void cancel(long id) throws IOException {
        JSONObject body = new JSONObject();
        body.put("id", id);
        postJson("/crawl/cancel", body);
    }

    /**
     * Poll a crawl until it reaches {@code finished}, {@code error}, or {@code cancelled}.
     */
    public JSONObject waitFor(long id, Long intervalMs, Long timeoutMs) throws IOException {
        long interval = intervalMs != null ? intervalMs : 2000L;
        long timeout  = timeoutMs  != null ? timeoutMs  : 120_000L;
        long deadline = System.currentTimeMillis() + timeout;
        while (true) {
            JSONObject c = info(id);
            String status = c.optString("status");
            if ("finished".equals(status) || "error".equals(status) || "cancelled".equals(status)) return c;
            if (System.currentTimeMillis() + interval > deadline) {
                throw new TimeoutError(id, timeout);
            }
            try { Thread.sleep(interval); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private JSONObject postJson(String endpoint, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        return client.post(endpoint, bytes, "application/json", null);
    }
}
