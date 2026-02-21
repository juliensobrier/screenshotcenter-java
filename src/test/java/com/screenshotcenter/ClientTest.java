package com.screenshotcenter;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClientTest {

    // ── Fixture data ──────────────────────────────────────────────────────────

    private static final String SHOT_JSON = "{\"id\":1001,\"status\":\"finished\",\"url\":\"https://example.com\","
        + "\"final_url\":\"https://example.com/\",\"cost\":1,\"country\":\"us\","
        + "\"has_html\":false,\"has_pdf\":false,\"has_video\":false,\"shots\":1}";

    private static final String BATCH_JSON = "{\"id\":2001,\"status\":\"finished\",\"count\":3,\"processed\":3,\"failed\":0}";
    private static final String ACCOUNT_JSON = "{\"balance\":500.0,\"plan\":\"pro\"}";

    private HttpResponse jsonOk(String data) {
        String body = "{\"success\":true,\"data\":" + data + "}";
        return new HttpResponse(200, body.getBytes(StandardCharsets.UTF_8), "application/json");
    }

    private HttpResponse jsonError(String message, int status) {
        String body = "{\"success\":false,\"error\":\"" + message + "\"}";
        return new HttpResponse(status, body.getBytes(StandardCharsets.UTF_8), "application/json");
    }

    private HttpResponse binaryOk(String content) {
        return new HttpResponse(200, content.getBytes(StandardCharsets.UTF_8), "image/png");
    }

    private HttpTransport mockTransport(HttpResponse... responses) throws IOException {
        HttpTransport transport = Mockito.mock(HttpTransport.class);
        if (responses.length == 0) {
            return transport;
        }
        OngoingStubbing<HttpResponse> stub = when(transport.execute(anyString(), anyString(), any(), any()));
        for (HttpResponse r : responses) {
            stub = stub.thenReturn(r);
        }
        return transport;
    }

    private ScreenshotCenterClient makeClient(HttpResponse... responses) throws IOException {
        return new ScreenshotCenterClient("test-key", "https://api.screenshotcenter.com/api/v1", mockTransport(responses));
    }

    private String captureUrl(HttpTransport transport) throws IOException {
        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(transport).execute(anyString(), captor.capture(), any(), any());
        return captor.getValue();
    }

    private String readFile(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    void testRejectsEmptyApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenshotCenterClient(""));
    }

    @Test
    void testRejectsNullApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenshotCenterClient(null));
    }

    @Test
    void testDefaultBaseUrl() {
        ScreenshotCenterClient c = new ScreenshotCenterClient("key");
        assertEquals("https://api.screenshotcenter.com/api/v1", c.getBaseUrl());
    }

    @Test
    void testCustomBaseUrl() throws IOException {
        ScreenshotCenterClient c = new ScreenshotCenterClient("key", "http://localhost:3000/api/v1", mockTransport());
        assertEquals("http://localhost:3000/api/v1", c.getBaseUrl());
    }

    @Test
    void testTrailingSlashStripped() throws IOException {
        ScreenshotCenterClient c = new ScreenshotCenterClient("key", "http://example.com/api/v1/", mockTransport());
        assertFalse(c.getBaseUrl().endsWith("/"));
    }

    @Test
    void testNamespacesAvailable() {
        ScreenshotCenterClient c = new ScreenshotCenterClient("key");
        assertNotNull(c.screenshot());
        assertNotNull(c.batch());
        assertNotNull(c.account());
    }

    // ── screenshot.create ─────────────────────────────────────────────────────

    @Test
    void testCreateReturnsShot() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonOk(SHOT_JSON));
        JSONObject result = c.screenshot().create("https://example.com", null);
        assertEquals(1001L, result.getLong("id"));
        assertEquals("finished", result.getString("status"));
    }

    @Test
    void testCreateSendsUrlAndKey() throws IOException {
        HttpTransport t = mockTransport(jsonOk(SHOT_JSON));
        new ScreenshotCenterClient("test-key", "https://api.screenshotcenter.com/api/v1", t)
            .screenshot().create("https://example.com", null);
        String url = captureUrl(t);
        assertTrue(url.contains("url="), "url param missing");
        assertTrue(url.contains("key=test-key"), "key param missing");
    }

    @Test
    void testCreatePassesOptionalParams() throws IOException {
        HttpTransport t = mockTransport(jsonOk(SHOT_JSON));
        Map<String, String> params = new HashMap<>();
        params.put("country", "fr");
        params.put("shots", "3");
        new ScreenshotCenterClient("test-key", "https://api.screenshotcenter.com/api/v1", t)
            .screenshot().create("https://example.com", params);
        String url = captureUrl(t);
        assertTrue(url.contains("country=fr"));
        assertTrue(url.contains("shots=3"));
    }

    @Test
    void testCreatePassesFutureParams() throws IOException {
        HttpTransport t = mockTransport(jsonOk(SHOT_JSON));
        Map<String, String> params = new HashMap<>();
        params.put("future_param", "xyz");
        new ScreenshotCenterClient("test-key", "https://api.screenshotcenter.com/api/v1", t)
            .screenshot().create("https://example.com", params);
        String url = captureUrl(t);
        assertTrue(url.contains("future_param=xyz"));
    }

    @Test
    void testCreateRaisesOnEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenshotCenterClient("key").screenshot().create("", null));
    }

    @Test
    void testCreateRaisesApiErrorOn401() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonError("Unauthorized", 401));
        ApiError e = assertThrows(ApiError.class, () -> c.screenshot().create("https://example.com", null));
        assertEquals(401, e.getStatus());
    }

    @Test
    void testCreateApiErrorHasCode() throws IOException {
        String body = "{\"success\":false,\"error\":\"Validation failed\",\"code\":\"INVALID_PARAMS\"}";
        HttpResponse r = new HttpResponse(422, body.getBytes(StandardCharsets.UTF_8), "application/json");
        ScreenshotCenterClient c = new ScreenshotCenterClient("key", "https://api.screenshotcenter.com/api/v1", mockTransport(r));
        ApiError e = assertThrows(ApiError.class, () -> c.screenshot().create("not-a-url", null));
        assertEquals("INVALID_PARAMS", e.getCode());
    }

    // ── screenshot.info ───────────────────────────────────────────────────────

    @Test
    void testInfoReturnsShot() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonOk(SHOT_JSON));
        JSONObject result = c.screenshot().info(1001);
        assertEquals(1001L, result.getLong("id"));
    }

    @Test
    void testInfoSendsIdParam() throws IOException {
        HttpTransport t = mockTransport(jsonOk(SHOT_JSON));
        new ScreenshotCenterClient("key", "https://api.screenshotcenter.com/api/v1", t).screenshot().info(1001);
        String url = captureUrl(t);
        assertTrue(url.contains("id=1001"));
    }

    @Test
    void testInfoRaisesOn404() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonError("Not found", 404));
        ApiError e = assertThrows(ApiError.class, () -> c.screenshot().info(999));
        assertEquals(404, e.getStatus());
    }

    // ── screenshot.thumbnail ──────────────────────────────────────────────────

    @Test
    void testThumbnailReturnsBytes() throws IOException {
        ScreenshotCenterClient c = makeClient(binaryOk("PNG-DATA"));
        byte[] data = c.screenshot().thumbnail(1001, null);
        assertEquals("PNG-DATA", new String(data, StandardCharsets.UTF_8));
    }

    @Test
    void testThumbnailPassesOptions() throws IOException {
        HttpTransport t = mockTransport(binaryOk("x"));
        Map<String, String> params = new HashMap<>();
        params.put("shot", "2");
        params.put("width", "400");
        new ScreenshotCenterClient("key", "https://api.screenshotcenter.com/api/v1", t).screenshot().thumbnail(1001, params);
        String url = captureUrl(t);
        assertTrue(url.contains("shot=2"));
        assertTrue(url.contains("width=400"));
    }

    // ── save helpers ─────────────────────────────────────────────────────────

    @Test
    void testSaveImageWritesFile(@TempDir Path tmpDir) throws IOException {
        ScreenshotCenterClient c = makeClient(binaryOk("PNG-CONTENT"));
        String path = tmpDir.resolve("shot.png").toString();
        c.screenshot().saveImage(1001, path, null);
        assertTrue(new File(path).exists());
        assertEquals("PNG-CONTENT", readFile(tmpDir.resolve("shot.png")));
    }

    @Test
    void testSaveImageCreatesDirectories(@TempDir Path tmpDir) throws IOException {
        ScreenshotCenterClient c = makeClient(binaryOk("PNG"));
        String path = tmpDir.resolve("a/b/shot.png").toString();
        c.screenshot().saveImage(1001, path, null);
        assertTrue(new File(path).exists());
    }

    @Test
    void testSavePdfWritesFile(@TempDir Path tmpDir) throws IOException {
        ScreenshotCenterClient c = makeClient(binaryOk("%PDF-1.4"));
        String path = tmpDir.resolve("doc.pdf").toString();
        c.screenshot().savePdf(1001, path);
        assertEquals("%PDF-1.4", readFile(tmpDir.resolve("doc.pdf")));
    }

    @Test
    void testSaveHtmlWritesFile(@TempDir Path tmpDir) throws IOException {
        ScreenshotCenterClient c = makeClient(binaryOk("<html></html>"));
        String path = tmpDir.resolve("page.html").toString();
        c.screenshot().saveHtml(1001, path);
        assertEquals("<html></html>", readFile(tmpDir.resolve("page.html")));
    }

    // ── waitFor ───────────────────────────────────────────────────────────────

    @Test
    void testWaitForResolvesWhenFinished() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonOk(SHOT_JSON));
        JSONObject result = c.waitFor(1001L, null, null);
        assertEquals("finished", result.getString("status"));
    }

    @Test
    void testWaitForPollsUntilFinished() throws IOException {
        String proc = SHOT_JSON.replace("\"finished\"", "\"processing\"");
        HttpTransport t = mockTransport(jsonOk(proc), jsonOk(proc), jsonOk(SHOT_JSON));
        ScreenshotCenterClient c = new ScreenshotCenterClient("key", "https://api.screenshotcenter.com/api/v1", t);
        JSONObject result = c.waitFor(1001L, 1L, 30_000L);
        assertEquals("finished", result.getString("status"));
    }

    @Test
    void testWaitForRaisesScreenshotFailedError() throws IOException {
        String err = SHOT_JSON.replace("\"finished\"", "\"error\"");
        ScreenshotCenterClient c = makeClient(jsonOk(err));
        ScreenshotFailedError e = assertThrows(ScreenshotFailedError.class, () -> c.waitFor(1001L, null, null));
        assertEquals(1001L, e.getScreenshotId());
    }

    @Test
    void testWaitForRaisesTimeoutError() throws IOException {
        String proc = SHOT_JSON.replace("\"finished\"", "\"processing\"");
        HttpTransport t = mockTransport();
        when(t.execute(anyString(), anyString(), any(), any())).thenReturn(jsonOk(proc));
        ScreenshotCenterClient c = new ScreenshotCenterClient("key", "https://api.screenshotcenter.com/api/v1", t);
        assertThrows(TimeoutError.class, () -> c.waitFor(1001L, 1L, 1L));
    }

    // ── batch.create ─────────────────────────────────────────────────────────

    @Test
    void testBatchCreateFromList() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonOk(BATCH_JSON));
        JSONObject result = c.batch().create(Arrays.asList("https://example.com", "https://example.org"), "us", null);
        assertEquals(2001L, result.getLong("id"));
    }

    @Test
    void testBatchCreateUsesPost() throws IOException {
        HttpTransport t = mockTransport(jsonOk(BATCH_JSON));
        ScreenshotCenterClient c = new ScreenshotCenterClient("key", "https://api.screenshotcenter.com/api/v1", t);
        c.batch().create(Arrays.asList("https://example.com"), "us", null);
        org.mockito.ArgumentCaptor<String> methodCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(t).execute(methodCaptor.capture(), anyString(), any(), any());
        assertEquals("POST", methodCaptor.getValue());
    }

    @Test
    void testBatchCreateRaisesOnEmptyCountry() {
        assertThrows(IllegalArgumentException.class, () ->
            new ScreenshotCenterClient("key").batch().create(Arrays.asList("https://example.com"), "", null));
    }

    // ── batch.waitFor ─────────────────────────────────────────────────────────

    @Test
    void testBatchWaitForResolvesOnFinished() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonOk(BATCH_JSON));
        JSONObject result = c.batch().waitFor(2001L, null, null);
        assertEquals("finished", result.getString("status"));
    }

    @Test
    void testBatchWaitForResolvesOnError() throws IOException {
        String err = BATCH_JSON.replace("\"finished\"", "\"error\"");
        ScreenshotCenterClient c = makeClient(jsonOk(err));
        JSONObject result = c.batch().waitFor(2001L, null, null);
        assertEquals("error", result.getString("status"));
    }

    @Test
    void testBatchWaitForRaisesTimeout() throws IOException {
        String proc = BATCH_JSON.replace("\"finished\"", "\"processing\"");
        HttpTransport t = mockTransport();
        when(t.execute(anyString(), anyString(), any(), any())).thenReturn(jsonOk(proc));
        ScreenshotCenterClient c = new ScreenshotCenterClient("key", "https://api.screenshotcenter.com/api/v1", t);
        assertThrows(TimeoutError.class, () -> c.batch().waitFor(2001L, 1L, 1L));
    }

    // ── account.info ─────────────────────────────────────────────────────────

    @Test
    void testAccountInfoReturnsBalance() throws IOException {
        ScreenshotCenterClient c = makeClient(jsonOk(ACCOUNT_JSON));
        JSONObject result = c.account().info();
        assertEquals(500.0, result.getDouble("balance"), 0.001);
    }

    @Test
    void testAccountInfoSendsKey() throws IOException {
        HttpTransport t = mockTransport(jsonOk(ACCOUNT_JSON));
        new ScreenshotCenterClient("my-key", "https://api.screenshotcenter.com/api/v1", t).account().info();
        String url = captureUrl(t);
        assertTrue(url.contains("key=my-key"));
    }

    // ── Error classes ─────────────────────────────────────────────────────────

    @Test
    void testApiErrorProperties() {
        ApiError e = new ApiError("Bad request", 400, "INVALID_PARAMS", null);
        assertEquals(400, e.getStatus());
        assertEquals("INVALID_PARAMS", e.getCode());
        assertEquals("Bad request", e.getMessage());
    }

    @Test
    void testTimeoutErrorProperties() {
        TimeoutError e = new TimeoutError(1001L, 30_000L);
        assertEquals(1001L, e.getScreenshotId());
        assertEquals(30_000L, e.getTimeoutMs());
        assertTrue(e.getMessage().contains("1001"));
    }

    @Test
    void testScreenshotFailedErrorProperties() {
        ScreenshotFailedError e = new ScreenshotFailedError(1001L, "DNS failure");
        assertEquals(1001L, e.getScreenshotId());
        assertEquals("DNS failure", e.getReason());
        assertTrue(e.getMessage().contains("DNS failure"));
    }
}
