package aiAssistance;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*******
 * <p> Title: AiProviderFixture Class. </p>
 * <p> Description: Explicit loopback HTTP fixture, never a production or live AI fallback.
 * Supplies controllable wire responses for OpenAiClientTest and TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Provider integration fixture
 */
public class AiProviderFixture implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    /** Captured requests allow protocol checks without writing private context to disk. */
    public final BlockingQueue<String> requests = new LinkedBlockingQueue<>();
    public final AtomicInteger calls = new AtomicInteger();
    public volatile String responseOverride;
    public volatile int status = 200;
    public volatile long delayMillis;
    public volatile String authorization;
    /** @throws Exception when loopback networking cannot be started */
    public AiProviderFixture() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.setExecutor(executor);
        server.createContext("/responses", exchange -> {
            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                authorization = exchange.getRequestHeaders().getFirst("Authorization"); requests.add(body); calls.incrementAndGet();
                if (delayMillis > 0) try { Thread.sleep(delayMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                String response = responseOverride != null ? responseOverride : body.contains("web_search") ? examples() : structured();
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes);
            } catch (java.io.IOException ignored) { /* Cancellation may close a test request before the response. */ }
            finally { exchange.close(); }
        }); server.start();
    }
    /** @return loopback-only test endpoint */
    public URI endpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/responses"); }
    /** @return explicit HTTP test client; no production environment is modified */
    public OpenAiClient client() { return new OpenAiClient(endpoint(), "fixture-key", "fixture-model", java.time.Duration.ofSeconds(3)); }
    /** @return generated JSON fixture, not a real model response */
    public static String structured() {
        String suggestion = JsonCodec.encode(Map.of("advice", "Fixture advice: review this course example.", "title", "Suggested title", "problemSituation", "Suggested problem", "lessonLearned", "Suggested lesson", "terms", List.of("Rollback", "Recovery")));
        return envelope(suggestion, List.of());
    }
    /** @return annotated web-result fixture, not an actual retrieved page */
    public static String examples() { return envelope("Fixture-generated guidance. Inspect the cited reference.", List.of(Map.of("type", "url_citation", "url", "https://example.test/reference", "title", "Fixture reference"))); }
    /** @param text output text @param annotations citation fixtures @return wire envelope */
    public static String envelope(String text, List<?> annotations) { return JsonCodec.encode(Map.of("status", "completed", "output", List.of(Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", text, "annotations", annotations)))))); }
    /** Stop only this fixture server and its worker executor. */
    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
