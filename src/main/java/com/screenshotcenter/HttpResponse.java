package com.screenshotcenter;

/**
 * Raw HTTP response returned by {@link HttpTransport}.
 */
public class HttpResponse {

    private final int status;
    private final byte[] body;
    private final String contentType;

    public HttpResponse(int status, byte[] body, String contentType) {
        this.status      = status;
        this.body        = body;
        this.contentType = contentType;
    }

    public int getStatus()       { return status; }
    public byte[] getBody()      { return body; }
    public String getContentType() { return contentType; }
}
