package aiAssistance;

import static org.junit.jupiter.api.Assertions.*;
import lessonManagement.*;
import java.time.Duration;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

/*******
 * <p> Title: OpenAiClientTest Class. </p>
 * <p> Description: Actual loopback HTTP verifies Responses payloads, structured output,
 * web-search citations, safe failures, cancellation, timeouts and JSON limits. Fixtures are
 * explicitly synthetic; a separate live verification command requires a configured key.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI HTTP verification
 */
public class OpenAiClientTest {
    private AiProviderFixture fixture;
    /** @throws Exception on loopback setup failure */
    @BeforeEach public void setUp() throws Exception { fixture = new AiProviderFixture(); }
    /** Close only the fixture server. */
    @AfterEach public void tearDown() { fixture.close(); }
    /** Structured output and stateless previous turns follow the real Responses wire contract. */
    @Test public void structuredAndFollowup() throws Exception {
        var request = new AiRequest(AiPurpose.WRITE, null, new LessonDraft("Quote \"\\\n", "Unicode café", "Lesson"), "Improve clarity", List.of(new AiTurn("user", "First request"), new AiTurn("assistant", "Prior response")));
        var result = fixture.client().generate(request); assertTrue(result.isSuccessful(), result.getMessage());
        assertEquals("Suggested title", result.getValue().getCore().getTitle()); assertEquals(List.of("Rollback", "Recovery"), result.getValue().getTerms());
        Map<?, ?> body = (Map<?, ?>)JsonCodec.parse(fixture.requests.poll(2, TimeUnit.SECONDS));
        assertEquals(false, body.get("store")); assertEquals("fixture-model", body.get("model")); assertEquals("Bearer fixture-key", fixture.authorization);
        List<?> input = (List<?>)body.get("input"); assertEquals(4, input.size()); assertTrue(input.toString().contains("Prior response")); assertTrue(input.toString().contains("café"));
        assertTrue(body.containsKey("text")); assertFalse(body.containsKey("tools")); assertFalse(body.containsKey("user"));
        assertEquals("json_schema", ((Map<?, ?>)((Map<?, ?>)body.get("text")).get("format")).get("type"));
    }
    /** Every structured purpose returns advice, core and terms without automatic persistence. */
    @Test public void structuredPurposes() { for (var purpose : List.of(AiPurpose.WRITE, AiPurpose.BRAINSTORM, AiPurpose.REFINE, AiPurpose.TERMS)) assertTrue(fixture.client().generate(request(purpose)).isSuccessful()); }
    /** Example requests force web search and expose only actual annotation URLs as retrieved sources. */
    @Test public void webSearchAndCitations() throws Exception {
        var result = fixture.client().generate(request(AiPurpose.EXAMPLES)); assertTrue(result.isSuccessful()); assertNull(result.getValue().getCore());
        assertEquals("https://example.test/reference", result.getValue().getSources().getFirst().getUrl()); assertEquals("Fixture reference", result.getValue().getSources().getFirst().getTitle());
        Map<?, ?> body = (Map<?, ?>)JsonCodec.parse(fixture.requests.poll(2, TimeUnit.SECONDS)); assertEquals("required", body.get("tool_choice"));
        assertEquals("web_search", ((Map<?, ?>)((List<?>)body.get("tools")).getFirst()).get("type")); assertFalse(body.containsKey("text"));
    }
    /** Generated text without annotations never becomes a claimed retrieved resource. */
    @Test public void missingAndUnsafeCitations() {
        fixture.responseOverride = AiProviderFixture.envelope("Generated guidance https://invented.test/page", List.of(Map.of("type", "url_citation", "url", "javascript:alert(1)", "title", "Unsafe")));
        var result = fixture.client().generate(request(AiPurpose.EXAMPLES)); assertTrue(result.isSuccessful()); assertTrue(result.getValue().getSources().isEmpty()); assertTrue(result.getMessage().contains("no retrieved source"));
    }
    /** Missing credentials/model fail before network access and never echo configuration secrets. */
    @Test public void missingConfiguration() {
        assertFalse(new OpenAiClient(fixture.endpoint(), "", "fixture-model", Duration.ofSeconds(1)).generate(request(AiPurpose.WRITE)).isSuccessful());
        assertFalse(new OpenAiClient(fixture.endpoint(), "fixture-key", " ", Duration.ofSeconds(1)).generate(request(AiPurpose.WRITE)).isSuccessful()); assertEquals(0, fixture.calls.get());
        assertThrows(IllegalArgumentException.class, () -> new OpenAiClient(URI.create("http://example.test/responses"), "key", "model", Duration.ofSeconds(1)));
    }
    /** HTTP failures return corrective messages without echoing provider bodies or credentials. */
    @Test public void httpFailuresAreSafe() {
        fixture.responseOverride = "fixture-key private-provider-body";
        for (int status : new int[] { 400, 401, 403, 429, 500, 503, 302 }) {
            fixture.status = status; var result = fixture.client().generate(request(AiPurpose.WRITE)); assertFalse(result.isSuccessful()); assertNull(result.getValue());
            assertFalse(result.getMessage().contains("fixture-key")); assertFalse(result.getMessage().contains("private-provider-body"));
        }
    }
    /** Failed/incomplete/refused and malformed outputs are never accepted as usable suggestions. */
    @Test public void malformedResponses() {
        for (String response : List.of("not JSON", "{}", "{\"status\":\"incomplete\",\"output\":[]}", "{\"status\":\"completed\",\"output\":[]}", AiProviderFixture.envelope("not structured JSON", List.of()),
                "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\"}]}]}")) {
            fixture.responseOverride = response; assertFalse(fixture.client().generate(request(AiPurpose.WRITE)).isSuccessful());
        }
    }
    /** Complete-body deadlines reject a stalled response. */
    @Test public void timeout() {
        fixture.delayMillis = 700; var client = new OpenAiClient(fixture.endpoint(), "fixture-key", "model", Duration.ofMillis(100));
        var result = client.generate(request(AiPurpose.WRITE)); assertFalse(result.isSuccessful()); assertTrue(result.getMessage().toLowerCase().contains("timed out") || result.getMessage().contains("connection failed"));
    }
    /** Interrupted generation cancels the HTTP future and preserves the interrupted state. */
    @Test public void interruption() throws Exception {
        fixture.delayMillis = 700; var result = new java.util.concurrent.atomic.AtomicReference<OperationResult<AiSuggestion>>();
        Thread worker = new Thread(() -> { result.set(fixture.client().generate(request(AiPurpose.WRITE))); assertTrue(Thread.currentThread().isInterrupted()); });
        worker.start(); assertNotNull(fixture.requests.poll(2, TimeUnit.SECONDS)); worker.interrupt(); worker.join(2000); assertFalse(worker.isAlive()); assertFalse(result.get().isSuccessful());
    }
    /** Provider content is bounded during receipt and generated advice is bounded independently. */
    @Test public void oversizedResponses() {
        fixture.responseOverride = "x".repeat(262145); assertFalse(fixture.client().generate(request(AiPurpose.WRITE)).isSuccessful());
        fixture.responseOverride = AiProviderFixture.envelope("x".repeat(16001), List.of()); assertFalse(fixture.client().generate(request(AiPurpose.EXAMPLES)).isSuccessful());
    }
    /** JSON escapes round-trip, and invalid syntax/nesting cannot corrupt the wire format. */
    @Test public void jsonCodec() {
        String input = "quote\" slash\\ newline\n tab\t café \uD83D\uDE00"; assertEquals(input, JsonCodec.parse(JsonCodec.encode(input)));
        for (String bad : List.of("{", "[1,]", "{\"x\":1,\"x\":2}", "01", "true garbage", "\"bad\ntext\"", "\"\\uZZZZ\"", "\"\\u-001\"", "[".repeat(66) + "0" + "]".repeat(66))) assertThrows(RuntimeException.class, () -> JsonCodec.parse(bad));
        assertEquals(new java.math.BigDecimal("-1.2E+3"), JsonCodec.parse("-1.2e+3"));
    }
    private static AiRequest request(AiPurpose purpose) { return new AiRequest(purpose, 1L, new LessonDraft("Course lesson", "Problem", "What we learned"), "Help improve this course lesson", List.of()); }
}
