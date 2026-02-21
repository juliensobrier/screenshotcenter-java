package com.screenshotcenter;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests — only run when SCREENSHOTCENTER_API_KEY is set.
 *
 * <pre>
 * # Unit tests only (default):
 * mvn test
 *
 * # Integration tests against a local instance:
 * SCREENSHOTCENTER_API_KEY=your_key \
 * SCREENSHOTCENTER_BASE_URL=http://localhost:3000/api/v1 \
 * mvn test -Pintegration
 * </pre>
 */
class IntegrationTest {

    private ScreenshotCenterClient client;
    private final List<Long> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("SCREENSHOTCENTER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            return; // tests will be skipped via assumption
        }
        String baseUrl = System.getenv("SCREENSHOTCENTER_BASE_URL");
        client = (baseUrl != null && !baseUrl.isEmpty())
            ? new ScreenshotCenterClient(apiKey, baseUrl, new DefaultHttpTransport(30_000))
            : new ScreenshotCenterClient(apiKey);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client == null) return;
        for (long id : createdIds) {
            try { client.screenshot().delete(id, "all"); } catch (Exception ignored) {}
        }
    }

    private void assumeLive() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            System.getenv("SCREENSHOTCENTER_API_KEY") != null,
            "SCREENSHOTCENTER_API_KEY not set");
    }

    private JSONObject createAndWait(String url) throws IOException {
        JSONObject shot = client.screenshot().create(url, null);
        createdIds.add(shot.getLong("id"));
        return client.waitFor(shot.getLong("id"), 3000L, 110_000L);
    }

    @Test void testAccountInfo() throws IOException {
        assumeLive();
        JSONObject info = client.account().info();
        assertTrue(info.has("balance"));
    }

    @Test void testCreateAndWait() throws IOException {
        assumeLive();
        JSONObject result = createAndWait("https://example.com");
        assertEquals("finished", result.getString("status"));
    }

    @Test void testInfo() throws IOException {
        assumeLive();
        JSONObject shot = client.screenshot().create("https://example.com", null);
        createdIds.add(shot.getLong("id"));
        JSONObject info = client.screenshot().info(shot.getLong("id"));
        assertEquals(shot.getLong("id"), info.getLong("id"));
    }

    @Test void testList() throws IOException {
        assumeLive();
        // returns a raw JSONObject wrapping an array — use search/list endpoint
        assertNotNull(client.screenshot().list(null));
    }

    @Test void testSaveImage() throws IOException {
        assumeLive();
        JSONObject result = createAndWait("https://example.com");
        File tmp = File.createTempFile("sc_", ".png");
        tmp.deleteOnExit();
        client.screenshot().saveImage(result.getLong("id"), tmp.getAbsolutePath(), null);
        assertTrue(tmp.length() > 0);
    }

    @Test void testInvalidApiKey() {
        assumeLive();
        String baseUrl = System.getenv("SCREENSHOTCENTER_BASE_URL");
        ScreenshotCenterClient bad = (baseUrl != null)
            ? new ScreenshotCenterClient("invalid-key", baseUrl, new DefaultHttpTransport(30_000))
            : new ScreenshotCenterClient("invalid-key");
        assertThrows(ApiError.class, () -> bad.account().info());
    }

    @Test void testBatchCreateAndWait() throws IOException {
        // Requires batch worker service to be running
        assumeLive();
        JSONObject batch = client.batch().create(
            Arrays.asList("https://example.com", "https://example.org"), "us", null);
        assertTrue(batch.has("id"));
        JSONObject result = client.batch().waitFor(batch.getLong("id"), 3000L, 110_000L);
        assertTrue(Arrays.asList("finished", "error").contains(result.getString("status")));
    }
}
