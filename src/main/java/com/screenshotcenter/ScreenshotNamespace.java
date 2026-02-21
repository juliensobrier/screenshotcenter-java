package com.screenshotcenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Screenshot-related API methods.
 */
public class ScreenshotNamespace {

    private final ScreenshotCenterClient client;

    ScreenshotNamespace(ScreenshotCenterClient client) {
        this.client = client;
    }

    /** Create a new screenshot. {@code url} is required. */
    public JSONObject create(String url, Map<String, String> params) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }
        Map<String, String> p = new HashMap<>();
        p.put("url", url);
        if (params != null) p.putAll(params);
        return client.get("/screenshot/create", p);
    }

    /** Retrieve screenshot metadata by ID. */
    public JSONObject info(long id) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        return client.get("/screenshot/info", p);
    }

    /** List screenshots. */
    public JSONArray list(Map<String, String> params) throws IOException {
        JSONObject raw = client.get("/screenshot/list", params);
        // Server may return an array wrapped in an object or directly as data
        if (raw.has("_array")) return raw.getJSONArray("_array");
        return new JSONArray(raw.toString());
    }

    /** Search screenshots by URL. */
    public JSONArray search(String url, Map<String, String> params) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }
        Map<String, String> p = new HashMap<>();
        p.put("url", url);
        if (params != null) p.putAll(params);
        JSONObject raw = client.get("/screenshot/search", p);
        if (raw.has("_array")) return raw.getJSONArray("_array");
        return new JSONArray(raw.toString());
    }

    /** Get raw thumbnail bytes. */
    public byte[] thumbnail(long id, Map<String, String> params) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        if (params != null) p.putAll(params);
        return client.getBytes("/screenshot/thumbnail", p);
    }

    /** Get raw HTML snapshot bytes. */
    public byte[] html(long id) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        return client.getBytes("/screenshot/html", p);
    }

    /** Get raw PDF bytes. */
    public byte[] pdf(long id) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        return client.getBytes("/screenshot/pdf", p);
    }

    /** Get raw video bytes. */
    public byte[] video(long id) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        return client.getBytes("/screenshot/video", p);
    }

    /** Delete a screenshot and its artifacts. */
    public void delete(long id, String data) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        p.put("data", data != null ? data : "all");
        client.get("/screenshot/delete", p);
    }

    // ── File-save helpers ─────────────────────────────────────────────────────

    public void saveImage(long id, String path, Map<String, String> params) throws IOException {
        writeFile(path, thumbnail(id, params));
    }

    public void savePdf(long id, String path) throws IOException {
        writeFile(path, pdf(id));
    }

    public void saveHtml(long id, String path) throws IOException {
        writeFile(path, html(id));
    }

    public void saveVideo(long id, String path) throws IOException {
        writeFile(path, video(id));
    }

    private void writeFile(String path, byte[] data) throws IOException {
        File file = new File(path);
        File dir  = file.getParentFile();
        if (dir != null) Files.createDirectories(dir.toPath());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }
}
