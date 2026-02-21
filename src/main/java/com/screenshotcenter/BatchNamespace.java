package com.screenshotcenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch job API methods.
 */
public class BatchNamespace {

    private final ScreenshotCenterClient client;

    BatchNamespace(ScreenshotCenterClient client) {
        this.client = client;
    }

    /** Create a new batch from a list of URLs. {@code country} is required. */
    public JSONObject create(List<String> urls, String country, Map<String, String> params) throws IOException {
        if (country == null || country.isEmpty()) {
            throw new IllegalArgumentException("country is required");
        }
        String content = String.join("\n", urls);
        return postMultipart(content, country, params);
    }

    /** Create a new batch from a newline-separated string of URLs. */
    public JSONObject createFromString(String urlContent, String country, Map<String, String> params) throws IOException {
        if (country == null || country.isEmpty()) {
            throw new IllegalArgumentException("country is required");
        }
        return postMultipart(urlContent, country, params);
    }

    /** Get batch status. */
    public JSONObject info(long id) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        return client.get("/batch/info", p);
    }

    /** List batches. */
    public JSONArray list(Map<String, String> params) throws IOException {
        JSONObject raw = client.get("/batch/list", params);
        if (raw.has("_array")) return raw.getJSONArray("_array");
        return new JSONArray(raw.toString());
    }

    /** Download the batch ZIP as raw bytes. */
    public byte[] download(long id) throws IOException {
        Map<String, String> p = new HashMap<>();
        p.put("id", String.valueOf(id));
        return client.getBytes("/batch/download", p);
    }

    /** Save the batch ZIP to a local file. */
    public void saveZip(long id, String path) throws IOException {
        byte[] data = download(id);
        File file = new File(path);
        File dir  = file.getParentFile();
        if (dir != null) Files.createDirectories(dir.toPath());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    /**
     * Poll a batch until it reaches {@code finished} or {@code error}.
     */
    public JSONObject waitFor(long id, Long intervalMs, Long timeoutMs) throws IOException {
        long interval = intervalMs != null ? intervalMs : 2000L;
        long timeout  = timeoutMs  != null ? timeoutMs  : 120_000L;
        long deadline = System.currentTimeMillis() + timeout;
        while (true) {
            JSONObject b = info(id);
            String status = b.optString("status");
            if ("finished".equals(status) || "error".equals(status)) return b;
            if (System.currentTimeMillis() + interval > deadline) {
                throw new TimeoutError(id, timeout);
            }
            try { Thread.sleep(interval); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private JSONObject postMultipart(String content, String country, Map<String, String> extra) throws IOException {
        String boundary = "--------ScBoundary" + System.currentTimeMillis();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        addField(buf, boundary, "country", country);
        if (extra != null) {
            for (Map.Entry<String, String> e : extra.entrySet()) {
                addField(buf, boundary, e.getKey(), e.getValue());
            }
        }
        // File part
        buf.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        buf.write("Content-Disposition: form-data; name=\"file\"; filename=\"urls.txt\"\r\n".getBytes(StandardCharsets.UTF_8));
        buf.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        buf.write(content.getBytes(StandardCharsets.UTF_8));
        buf.write("\r\n".getBytes(StandardCharsets.UTF_8));
        buf.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return client.post("/batch/create", buf.toByteArray(), "multipart/form-data; boundary=" + boundary, null);
    }

    private void addField(ByteArrayOutputStream buf, String boundary, String name, String value) throws IOException {
        buf.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        buf.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        buf.write(value.getBytes(StandardCharsets.UTF_8));
        buf.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
