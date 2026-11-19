package aiAssistance;

/*******
 * <p> Title: AiSource Class. </p>
 * <p> Description: A provider-returned web citation; never fabricated from generated text. Public contracts are validated by AiAssistanceTest,
 * OpenAiClientTest and TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Real AI assistance
 */
public class AiSource {
    /** Citation label. */
    private final String title;
    /** Provider-returned HTTP or HTTPS source URL. */
    private final String url;
    /**
     * Construct immutable assistance data; tested by AiAssistanceTest.
     * @param title Citation label.
     * @param url Provider-returned HTTP or HTTPS source URL.
     */
    public AiSource(String title, String url) {
        this.title = title;
        this.url = url;
    }
    /** @return Citation label. Tested by AiAssistanceTest and OpenAiClientTest. */
    public String getTitle() { return title; }
    /** @return Provider-returned HTTP or HTTPS source URL. Tested by AiAssistanceTest and OpenAiClientTest. */
    public String getUrl() { return url; }
}
