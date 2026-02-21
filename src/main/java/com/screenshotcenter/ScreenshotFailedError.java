package com.screenshotcenter;

/**
 * Thrown by {@code waitFor} when the screenshot reaches {@code error} status.
 */
public class ScreenshotFailedError extends RuntimeException {

    private final long screenshotId;
    private final String reason;

    public ScreenshotFailedError(long screenshotId, String reason) {
        super("Screenshot " + screenshotId + " failed: " + (reason != null ? reason : "unknown error"));
        this.screenshotId = screenshotId;
        this.reason       = reason;
    }

    public long getScreenshotId() { return screenshotId; }
    public String getReason()     { return reason; }
}
