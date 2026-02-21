package com.screenshotcenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thrown when the API returns a non-2xx status or {@code success: false}.
 */
public class ApiError extends RuntimeException {

    private final int status;
    private final String code;
    private final Map<String, List<String>> fields;

    public ApiError(String message, int status, String code, Map<String, List<String>> fields) {
        super(message);
        this.status = status;
        this.code   = code;
        this.fields = fields != null ? fields : new HashMap<>();
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public Map<String, List<String>> getFields() { return fields; }

    static ApiError fromJSON(JSONObject json, int status) {
        String msg    = json.optString("error", "API error");
        String code   = json.optString("code", null);
        Map<String, List<String>> fields = new HashMap<>();
        if (json.has("fields")) {
            JSONObject f = json.getJSONObject("fields");
            for (String k : f.keySet()) {
                List<String> msgs = new ArrayList<>();
                JSONArray arr = f.getJSONArray(k);
                for (int i = 0; i < arr.length(); i++) {
                    msgs.add(arr.getString(i));
                }
                fields.put(k, msgs);
            }
        }
        return new ApiError(msg, status, code, fields);
    }
}
