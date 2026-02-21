# screenshotcenter-java

Official Java SDK for the [ScreenshotCenter](https://screenshotcenter.com) API.

## Requirements

- Java ≥ 8
- Maven or Gradle

## Installation

### Maven

```xml
<dependency>
  <groupId>com.screenshotcenter</groupId>
  <artifactId>screenshotcenter</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.screenshotcenter:screenshotcenter:1.0.0'
```

## Quick start

```java
import com.screenshotcenter.ScreenshotCenterClient;
import org.json.JSONObject;

ScreenshotCenterClient client = new ScreenshotCenterClient("your_api_key");

JSONObject shot   = client.screenshot().create("https://example.com", null);
JSONObject result = client.waitFor(shot.getLong("id"), null, null);
System.out.println(result.getString("status")); // "finished"
```

## Use cases

### Geo-targeting

```java
Map<String, String> params = new HashMap<>();
params.put("country", "fr");
params.put("lang", "fr-FR");
JSONObject shot = client.screenshot().create("https://example.com", params);
```

### PDF

```java
Map<String, String> params = new HashMap<>();
params.put("pdf", "true");
JSONObject shot = client.screenshot().create("https://example.com", params);
JSONObject done = client.waitFor(shot.getLong("id"), null, null);
client.screenshot().savePdf(done.getLong("id"), "/tmp/page.pdf");
```

### Batch

```java
// Requires batch worker service to be running
List<String> urls = Arrays.asList("https://example.com", "https://example.org");
JSONObject batch  = client.batch().create(urls, "us", null);
JSONObject result = client.batch().waitFor(batch.getLong("id"), 3000L, 120_000L);
client.batch().saveZip(result.getLong("id"), "/tmp/batch.zip");
```

### Error handling

```java
import com.screenshotcenter.*;

try {
    JSONObject result = client.waitFor(id, null, 60_000L);
} catch (ScreenshotFailedError e) {
    System.err.println("Failed: " + e.getReason());
} catch (TimeoutError e) {
    System.err.println("Timed out after " + e.getTimeoutMs() + "ms");
} catch (ApiError e) {
    System.err.println("API error " + e.getStatus() + ": " + e.getMessage());
}
```

## API reference

### `new ScreenshotCenterClient(apiKey)`

Uses the default production base URL.

### `new ScreenshotCenterClient(apiKey, baseUrl, transport)`

Custom base URL and `HttpTransport` (injectable for testing).

### `client.screenshot()`

| Method | Description |
|--------|-------------|
| `create(url, params)` | Create a screenshot |
| `info(id)` | Get screenshot metadata |
| `list(params)` | List screenshots |
| `search(url, params)` | Search by URL |
| `thumbnail(id, params)` | Raw image bytes |
| `html(id)` | Raw HTML bytes |
| `pdf(id)` | Raw PDF bytes |
| `video(id)` | Raw video bytes |
| `delete(id, data)` | Delete a screenshot |
| `saveImage(id, path, params)` | Save image to disk |
| `saveHtml(id, path)` | Save HTML to disk |
| `savePdf(id, path)` | Save PDF to disk |
| `saveVideo(id, path)` | Save video to disk |

### `client.batch()`

| Method | Description |
|--------|-------------|
| `create(urls, country, params)` | Create a batch |
| `createFromString(content, country, params)` | Create from newline-separated string |
| `info(id)` | Get batch status |
| `list(params)` | List batches |
| `download(id)` | Download ZIP bytes |
| `saveZip(id, path)` | Save ZIP to disk |
| `waitFor(id, intervalMs, timeoutMs)` | Poll until done |

### `client.account()`

| Method | Description |
|--------|-------------|
| `info()` | Get account info (balance, plan) |

### `client.waitFor(id, intervalMs, timeoutMs)`

Poll a screenshot until `finished` or `error`. Pass `null` for defaults (2s interval, 120s timeout).

## Testing

### Environment variables

| Variable | Description |
|----------|-------------|
| `SCREENSHOTCENTER_API_KEY` | Required for integration tests |
| `SCREENSHOTCENTER_BASE_URL` | Override base URL (default: production) |

### Running tests

```bash
# Unit tests only (default)
mvn test

# Integration tests against a local instance
SCREENSHOTCENTER_API_KEY=your_key \
SCREENSHOTCENTER_BASE_URL=http://localhost:3000/api/v1 \
mvn test -Pintegration
```

## License

MIT — see [LICENSE](LICENSE).
