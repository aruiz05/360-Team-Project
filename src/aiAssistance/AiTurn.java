package aiAssistance;

/*******
 * <p> Title: AiTurn Class. </p>
 * <p> Description: One in-memory conversation message; no conversation is persisted. Public contracts are validated by AiAssistanceTest,
 * OpenAiClientTest and TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Real AI assistance
 */
public class AiTurn {
    /** Only user or assistant is accepted. */
    private final String role;
    /** Message text, bounded before sending. */
    private final String text;
    /**
     * Construct immutable assistance data; tested by AiAssistanceTest.
     * @param role Only user or assistant is accepted.
     * @param text Message text, bounded before sending.
     */
    public AiTurn(String role, String text) {
        this.role = role;
        this.text = text;
    }
    /** @return Only user or assistant is accepted. Tested by AiAssistanceTest and OpenAiClientTest. */
    public String getRole() { return role; }
    /** @return Message text, bounded before sending. Tested by AiAssistanceTest and OpenAiClientTest. */
    public String getText() { return text; }
}
