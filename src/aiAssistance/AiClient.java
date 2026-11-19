package aiAssistance;

import lessonManagement.OperationResult;

/*******
 * <p> Title: AiClient Interface. </p>
 * <p> Description: External generation only, with no database or GUI dependency. Tested by
 * OpenAiClientTest using real local HTTP exchanges and optional live verification.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI provider contract
 */
public interface AiClient {
    /**
     * @param request previously validated bounded request
     * @return unsaved suggestion or a helpful provider error; tested by OpenAiClientTest
     */
    OperationResult<AiSuggestion> generate(AiRequest request);
}
