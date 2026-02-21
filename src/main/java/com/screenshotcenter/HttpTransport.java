package com.screenshotcenter;

import java.io.IOException;

/**
 * Minimal HTTP transport interface — swap in a mock implementation for unit tests.
 */
public interface HttpTransport {

    /**
     * @param method      HTTP method (GET, POST)
     * @param url         Full URL including query string
     * @param body        Request body (may be null for GET)
     * @param contentType Content-Type header value (may be null for GET)
     * @return HTTP response
     */
    HttpResponse execute(String method, String url, byte[] body, String contentType) throws IOException;
}
