package com.screenshotcenter;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * ScreenshotCenter Java SDK client.
 *
 * <pre>
 * ScreenshotCenterClient client = new ScreenshotCenterClient("your_api_key");
 * JSONObject shot   = client.screenshot().create("https://example.com", null);
 * JSONObject result = client.waitFor(shot.getLong("id"), null);
 * </pre>
 */
public class ScreenshotCenterClient {

    static final String DEFAULT_BASE_URL = "https://api.screenshotcenter.com/api/v1";
    static final long DEFAULT_INTERVAL_MS = 2000L;
    static final long DEFAULT_TIMEOUT_MS  = 120_000L;

    private final String apiKey;
    private final String baseUrl;
    private final HttpTransport transport;

    private final ScreenshotNamespace screenshotNs;
    private final BatchNamespace batchNs;
    private final AccountNamespace accountNs;

    public ScreenshotCenterClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, new DefaultHttpTransport(30_000));
    }

    public ScreenshotCenterClient(String apiKey, String baseUrl, HttpTransport transport) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        this.apiKey    = apiKey;
        this.baseUrl   = baseUrl.replaceAll("/+$", "");
        this.transport = transport;

        this.screenshotNs = new ScreenshotNamespace(this);
        this.batchNs      = new BatchNamespace(this);
        this.accountNs    = new AccountNamespace(this);
    }

    public ScreenshotNamespace screenshot() { return screenshotNs; }
    public BatchNamespace batch()           { return batchNs; }
    public AccountNamespace account()       { return accountNs; }

    // ── Polling ───────────────────────────────────────────────────────────────

    /**
     * Polls a screenshot until it reaches {@code finished} or {@code error}.
     *
     * @param id         screenshot ID
     * @param intervalMs milliseconds between polls (default 2000)
     * @param timeoutMs  maximum wait in milliseconds (default 120000)
     */
    public JSONObject waitFor(long id, Long intervalMs, Long timeoutMs) throws IOException {
        long interval = intervalMs != null ? intervalMs : DEFAULT_INTERVAL_MS;
        long timeout  = timeoutMs  != null ? timeoutMs  : DEFAULT_TIMEOUT_MS;
        long deadline = System.currentTimeMillis() + timeout;

        while (true) {
            JSONObject s = screenshotNs.info(id);
            String status = s.optString("status");
            if ("finished".equals(status)) return s;
            if ("error".equals(status)) {
                throw new ScreenshotFailedError(id, s.optString("error", null));
            }
            if (System.currentTimeMillis() + interval > deadline) {
                throw new TimeoutError(id, timeout);
            }
            try { Thread.sleep(interval); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // ── Internal HTTP helpers ─────────────────────────────────────────────────

    JSONObject get(String endpoint, Map<String, String> params) throws IOException {
        String url = buildUrl(endpoint, params);
        HttpResponse resp = transport.execute("GET", url, null, null);
        return parseJsonResponse(resp);
    }

    byte[] getBytes(String endpoint, Map<String, String> params) throws IOException {
        String url = buildUrl(endpoint, params);
        HttpResponse resp = transport.execute("GET", url, null, null);
        if (resp.getStatus() < 200 || resp.getStatus() >= 300) {
            JSONObject json = safeParseJson(resp.getBody());
            throw ApiError.fromJSON(json, resp.getStatus());
        }
        return resp.getBody();
    }

    JSONObject post(String endpoint, byte[] body, String contentType, Map<String, String> params) throws IOException {
        String url = buildUrl(endpoint, params);
        HttpResponse resp = transport.execute("POST", url, body, contentType);
        return parseJsonResponse(resp);
    }

    String getApiKey() { return apiKey; }
    String getBaseUrl() { return baseUrl; }

    // ── Private helpers ───────────────────────────────────────────────────────

    String buildUrl(String endpoint, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl).append(endpoint).append("?key=").append(encode(apiKey));
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getValue() != null) {
                    sb.append("&").append(encode(e.getKey())).append("=").append(encode(e.getValue()));
                }
            }
        }
        return sb.toString();
    }

    private JSONObject parseJsonResponse(HttpResponse resp) throws IOException {
        JSONObject json = safeParseJson(resp.getBody());
        if (resp.getStatus() < 200 || resp.getStatus() >= 300) {
            throw ApiError.fromJSON(json, resp.getStatus());
        }
        if (json.has("success") && !json.getBoolean("success")) {
            throw ApiError.fromJSON(json, resp.getStatus());
        }
        return json.has("data") ? json.getJSONObject("data") : json;
    }

    private JSONObject safeParseJson(byte[] body) {
        try {
            return new JSONObject(new String(body, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static String encode(String v) {
        try {
            return URLEncoder.encode(v, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return v;
        }
    }
}
