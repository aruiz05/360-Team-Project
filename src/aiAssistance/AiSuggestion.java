package aiAssistance;

/*******
 * <p> Title: AiSuggestion Class. </p>
 * <p> Description: Unsaved AI-generated advice and optional structured replacements. Public contracts are validated by AiAssistanceTest,
 * OpenAiClientTest and TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Real AI assistance
 */
public class AiSuggestion {
    /** Generated advice or examples, labeled in the GUI. */
    private final String text;
    /** Proposed core fields or null for unstructured advice. */
    private final lessonManagement.LessonDraft core;
    /** Suggested related terms for review. */
    private final java.util.List<String> terms;
    /** Actual provider web citation annotations. */
    private final java.util.List<AiSource> sources;
    /** Assistant content retained for a bounded follow-up. */
    private final String conversationText;
    /**
     * Construct immutable assistance data; tested by AiAssistanceTest.
     * @param text Generated advice or examples, labeled in the GUI.
     * @param core Proposed core fields or null for unstructured advice.
     * @param terms Suggested related terms for review.
     * @param sources Actual provider web citation annotations.
     * @param conversationText Assistant content retained for a bounded follow-up.
     */
    public AiSuggestion(String text, lessonManagement.LessonDraft core, java.util.List<String> terms, java.util.List<AiSource> sources, String conversationText) {
        this.text = text;
        this.core = core;
        this.terms = terms == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(terms));
        this.sources = sources == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(sources));
        this.conversationText = conversationText;
    }
    /** @return Generated advice or examples, labeled in the GUI. Tested by AiAssistanceTest and OpenAiClientTest. */
    public String getText() { return text; }
    /** @return Proposed core fields or null for unstructured advice. Tested by AiAssistanceTest and OpenAiClientTest. */
    public lessonManagement.LessonDraft getCore() { return core; }
    /** @return Suggested related terms for review. Tested by AiAssistanceTest and OpenAiClientTest. */
    public java.util.List<String> getTerms() { return terms; }
    /** @return Actual provider web citation annotations. Tested by AiAssistanceTest and OpenAiClientTest. */
    public java.util.List<AiSource> getSources() { return sources; }
    /** @return Assistant content retained for a bounded follow-up. Tested by AiAssistanceTest and OpenAiClientTest. */
    public String getConversationText() { return conversationText; }
}
