package com.screenshotcenter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Default {@link HttpTransport} backed by {@link HttpURLConnection}.
 */
public class DefaultHttpTransport implements HttpTransport {

    private final int timeoutMs;

    public DefaultHttpTransport(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public HttpResponse execute(String method, String urlStr, byte[] body, String contentType) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setInstanceFollowRedirects(true);

        if (body != null && body.length > 0) {
            conn.setDoOutput(true);
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType);
            }
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
        }

        int status;
        try {
            status = conn.getResponseCode();
        } catch (IOException e) {
            status = 500;
        }

        InputStream is = (status >= 200 && status < 300)
            ? conn.getInputStream()
            : conn.getErrorStream();

        byte[] respBody = readStream(is);
        String ct = conn.getContentType();
        conn.disconnect();
        return new HttpResponse(status, respBody, ct);
    }

    private static byte[] readStream(InputStream is) throws IOException {
        if (is == null) return new byte[0];
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = is.read(chunk)) != -1) {
            buf.write(chunk, 0, read);
        }
        return buf.toByteArray();
    }
}
