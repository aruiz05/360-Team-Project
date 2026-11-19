package aiAssistance;

/*******
 * <p> Title: AiPreparedRequest Class. </p>
 * <p> Description: Immutable request authorized on the application thread. Only the service
 * constructs it; the background client never reads shared JDBC. Tested by AiAssistanceTest.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI request preparation
 */
public final class AiPreparedRequest {
    private final AiRequest request;
    AiPreparedRequest(AiRequest request) { this.request = request; }
    /** @return accepted request context; tested by AiAssistanceTest.preparationAndGeneration */
    public AiRequest getRequest() { return request; }
}
