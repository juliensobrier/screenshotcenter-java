package com.screenshotcenter;

/**
 * Thrown by {@code waitFor} when polling exceeds the configured timeout.
 */
public class TimeoutError extends RuntimeException {

    private final long screenshotId;
    private final long timeoutMs;

    public TimeoutError(long screenshotId, long timeoutMs) {
        super("Screenshot " + screenshotId + " did not complete within " + timeoutMs + "ms");
        this.screenshotId = screenshotId;
        this.timeoutMs    = timeoutMs;
    }

    public long getScreenshotId() { return screenshotId; }
    public long getTimeoutMs()    { return timeoutMs; }
}
