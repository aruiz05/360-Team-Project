package guiAiAssistance;

import aiAssistance.*;
import lessonManagement.*;

/*******
 * <p> Title: ModelAiAssistance Class. </p>
 * <p> Description: Separates application-thread JDBC preparation and reviewed saves from
 * worker-only generation. Tested by TP3AiGuiTestingAutomation and AiAssistanceTest.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI MVC
 */
public class ModelAiAssistance {
    /** Production uses the real provider; tests explicitly inject a test service. */
    private static AiAssistanceService service = new AiAssistanceService(applicationMain.FoundationsMain.database, new OpenAiClient());
    private ModelAiAssistance() { }
    /** @param request proposed task @return permitted immutable context; tested by AI GUI request checks */
    protected static OperationResult<AiPreparedRequest> prepare(AiRequest request) { return service.prepare(ViewAiAssistance.user.getUserName(), ViewAiAssistance.role, request); }
    /** @param prepared permitted input @return unsaved generated suggestion; tested by AI GUI background checks */
    protected static OperationResult<AiSuggestion> generate(AiPreparedRequest prepared) { return service.generate(prepared); }
    /** @return current authorized core save; tested by AI GUI reviewed-save and lock checks */
    protected static OperationResult<entityClasses.LessonsLearned> saveCore() {
        return service.saveCore(ViewAiAssistance.user.getUserName(), ViewAiAssistance.role, ViewAiAssistance.lessonId, ControllerAiAssistance.draft());
    }
    /** @return reviewed term save; tested by AI GUI term review */
    protected static OperationResult<java.util.List<entityClasses.SearchTerm>> saveTerms() {
        return service.saveTerms(ViewAiAssistance.user.getUserName(), ViewAiAssistance.lessonId,
                java.util.Arrays.asList(ViewAiAssistance.terms.getText().split("\\R", -1)));
    }
    /** @return current permitted saved core; tested by AI GUI entry and revoked-role checks */
    protected static OperationResult<entityClasses.LessonsLearned> load() {
        return service.loadLesson(ViewAiAssistance.user.getUserName(), ViewAiAssistance.role, ViewAiAssistance.lessonId);
    }
}
