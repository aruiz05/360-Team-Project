package aiAssistance;

import database.Database;
import entityClasses.LessonsLearned;
import guiTools.RoleNames;
import lessonManagement.*;
import roleManagement.RoleAccessService;
import java.sql.SQLException;
import java.util.List;

/*******
 * <p> Title: AiAssistanceService Class. </p>
 * <p> Description: Prepares permitted draft context on the application thread, performs only
 * generation in the worker, and saves reviewed content through inherited role/ownership/lock
 * rules. No provider response can write data by itself. Public methods are validated by
 * AiAssistanceTest and TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Real AI assistance
 */
public class AiAssistanceService {
    /** Application-level bounds keep interactive requests understandable and bounded. */
    public static final int MAX_PROMPT = 4000, MAX_CONTEXT = 16000, MAX_HISTORY = 12, MAX_TOTAL = 48000;
    private final Database database;
    private final AiClient client;
    /**
     * @param database shared application or isolated test database
     * @param client production HTTP provider or explicit test double
     */
    public AiAssistanceService(Database database, AiClient client) { this.database = database; this.client = client; }
    /**
     * Load only authorized core context. Tested by purposesAndPermissions and ownership.
     * @param actor authenticated user
     * @param role active Contributor or Curator role
     * @param id selected existing lesson
     * @return current permitted core or an error
     */
    public OperationResult<LessonsLearned> loadLesson(String actor, String role, long id) {
        if (RoleNames.CONTRIBUTOR.equals(role)) return new LessonService(database).readLesson(Long.toString(id), actor);
        if (RoleNames.CURATOR.equals(role)) return new CuratorLessonService(database).readLesson(actor, Long.toString(id));
        return OperationResult.failure("Select Contributor or Curator assistance.");
    }
    /**
     * Validate and authorize before any external call. Tested by validation, ownership and preparationAndGeneration.
     * @param actor authenticated user from application context
     * @param role active role
     * @param request raw task, draft, prompt and bounded history
     * @return immutable permitted request for a worker, or a corrective error
     */
    public OperationResult<AiPreparedRequest> prepare(String actor, String role, AiRequest request) {
        if (!RoleNames.CONTRIBUTOR.equals(role) && !RoleNames.CURATOR.equals(role))
            return OperationResult.failure("Select Contributor or Curator assistance.");
        try {
            return database.inTransaction(() -> {
                var access = new RoleAccessService(database).requireRole(actor, role);
                if (!access.isSuccessful()) return OperationResult.failure(access.getMessage());
                if (request == null || request.getPurpose() == null) return OperationResult.failure("Choose an assistance purpose.");
                boolean curator = RoleNames.CURATOR.equals(role);
                boolean staffTask = request.getPurpose() == AiPurpose.TERMS || request.getPurpose() == AiPurpose.REFINE;
                if (curator != staffTask) return OperationResult.failure("Choose an assistance task for your active role.");
                if (curator && request.getLessonId() == null) return OperationResult.failure("Select a saved lesson for Curator assistance.");
                if (request.getLessonId() != null) {
                    var lesson = loadLesson(actor, role, request.getLessonId());
                    if (!lesson.isSuccessful()) return OperationResult.failure(lesson.getMessage());
                }
                if (request.getPrompt() == null || request.getPrompt().isBlank()) return OperationResult.failure("Enter a request or follow-up question.");
                if (request.getPrompt().length() > MAX_PROMPT) return OperationResult.failure("Shorten the request to " + MAX_PROMPT + " characters or fewer.");
                if (request.getDraft() == null) return OperationResult.failure("Provide draft context, even if the fields are initially empty.");
                var draft = request.getDraft();
                if (draft.getTitle() == null || draft.getProblemSituation() == null || draft.getLessonLearned() == null)
                    return OperationResult.failure("Provide all three draft fields; empty text is allowed while brainstorming.");
                int context = draft.getTitle().length() + draft.getProblemSituation().length() + draft.getLessonLearned().length();
                if (context > MAX_CONTEXT) return OperationResult.failure("Shorten the draft context to " + MAX_CONTEXT + " characters or fewer.");
                if (request.getHistory() == null || request.getHistory().size() > MAX_HISTORY)
                    return OperationResult.failure("Start a new conversation; at most " + MAX_HISTORY + " previous turns are supported.");
                int total = context + request.getPrompt().length();
                for (var turn : request.getHistory()) {
                    if (turn == null || !("user".equals(turn.getRole()) || "assistant".equals(turn.getRole()))
                            || turn.getText() == null || turn.getText().isBlank() || turn.getText().length() > MAX_CONTEXT)
                        return OperationResult.failure("Start a new conversation; the previous turn data is invalid or too large.");
                    total += turn.getText().length();
                }
                if (total > MAX_TOTAL) return OperationResult.failure("Start a new conversation or shorten the draft and request.");
                return OperationResult.success(new AiPreparedRequest(request), "Request prepared.");
            });
        } catch (SQLException e) { return OperationResult.failure("AI access could not be checked. Refresh and try again."); }
    }
    /**
     * Worker-only generation; deliberately performs no JDBC operations. Tested by preparationAndGeneration.
     * @param prepared application-thread authorized request
     * @return unsaved suggestion or safe provider error
     */
    public OperationResult<AiSuggestion> generate(AiPreparedRequest prepared) {
        if (prepared == null) return OperationResult.failure("Prepare a valid request before asking AI.");
        try { return client.generate(prepared.getRequest()); }
        catch (RuntimeException e) { return OperationResult.failure("AI assistance failed. Your draft is unchanged; retry or continue manually."); }
    }
    /**
     * Persist only an explicitly reviewed draft. Tested by reviewedSavesAndLocks and revokedRoles.
     * @param actor authenticated user
     * @param role active role
     * @param lessonId existing key or null for new Contributor draft
     * @param draft reviewed core text
     * @return saved core through existing validation/ownership/protection logic
     */
    public OperationResult<LessonsLearned> saveCore(String actor, String role, Long lessonId, LessonDraft draft) {
        if (draft != null && draft.getTitle() != null && draft.getTitle().trim().length() > 500)
            return OperationResult.failure("Shorten the title to 500 characters or fewer before saving.");
        if (RoleNames.CONTRIBUTOR.equals(role)) {
            var service = new LessonService(database);
            return lessonId == null ? service.createLesson(actor, draft) : service.updateLesson(Long.toString(lessonId), actor, draft);
        }
        if (RoleNames.CURATOR.equals(role) && lessonId != null) return new CuratorLessonService(database).refineLesson(actor, Long.toString(lessonId), draft);
        return OperationResult.failure("Select an authorized lesson and role before saving.");
    }
    /**
     * Persist only reviewed terms through the current Curator path. Tested by reviewedTerms.
     * @param actor authenticated Curator
     * @param lessonId selected parent lesson
     * @param terms reviewed complete term set
     * @return saved normalized set or a corrective error
     */
    public OperationResult<List<entityClasses.SearchTerm>> saveTerms(String actor, Long lessonId, List<String> terms) {
        return new CuratorLessonService(database).saveTerms(actor, lessonId == null ? "" : Long.toString(lessonId), terms);
    }
}
