package aiAssistance;

/*******
 * <p> Title: AiRequest Class. </p>
 * <p> Description: Raw draft context and user request; validated before external access. Public contracts are validated by AiAssistanceTest,
 * OpenAiClientTest and TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Real AI assistance
 */
public class AiRequest {
    /** Selected assistance purpose. */
    private final AiPurpose purpose;
    /** Existing lesson key or null for a new Contributor draft. */
    private final Long lessonId;
    /** Current unsaved core context. */
    private final lessonManagement.LessonDraft draft;
    /** Current request or follow-up. */
    private final String prompt;
    /** In-memory previous successful turns. */
    private final java.util.List<AiTurn> history;
    /**
     * Construct immutable assistance data; tested by AiAssistanceTest.
     * @param purpose Selected assistance purpose.
     * @param lessonId Existing lesson key or null for a new Contributor draft.
     * @param draft Current unsaved core context.
     * @param prompt Current request or follow-up.
     * @param history In-memory previous successful turns.
     */
    public AiRequest(AiPurpose purpose, Long lessonId, lessonManagement.LessonDraft draft, String prompt, java.util.List<AiTurn> history) {
        this.purpose = purpose;
        this.lessonId = lessonId;
        this.draft = draft;
        this.prompt = prompt;
        this.history = history == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(history));
    }
    /** @return Selected assistance purpose. Tested by AiAssistanceTest and OpenAiClientTest. */
    public AiPurpose getPurpose() { return purpose; }
    /** @return Existing lesson key or null for a new Contributor draft. Tested by AiAssistanceTest and OpenAiClientTest. */
    public Long getLessonId() { return lessonId; }
    /** @return Current unsaved core context. Tested by AiAssistanceTest and OpenAiClientTest. */
    public lessonManagement.LessonDraft getDraft() { return draft; }
    /** @return Current request or follow-up. Tested by AiAssistanceTest and OpenAiClientTest. */
    public String getPrompt() { return prompt; }
    /** @return In-memory previous successful turns. Tested by AiAssistanceTest and OpenAiClientTest. */
    public java.util.List<AiTurn> getHistory() { return history; }
}
