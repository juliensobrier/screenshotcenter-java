package com.screenshotcenter;

import org.json.JSONObject;

import java.io.IOException;

/** Account-related API methods. */
public class AccountNamespace {

    private final ScreenshotCenterClient client;

    AccountNamespace(ScreenshotCenterClient client) {
        this.client = client;
    }

    /** Returns account details including credit balance. */
    public JSONObject info() throws IOException {
        return client.get("/account/info", null);
    }
}
