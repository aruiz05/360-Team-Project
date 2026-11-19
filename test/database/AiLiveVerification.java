package database;

import aiAssistance.*;
import entityClasses.User;
import lessonManagement.*;
import java.util.List;

/*******
 * <p> Title: AiLiveVerification Class. </p>
 * <p> Description: Explicit optional real-provider acceptance command. Requires a configured
 * API key and makes billable requests; never substitutes fixtures or reports a skipped call
 * as passed. All writes use a newly created isolated temporary file database.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Live AI verification
 */
public class AiLiveVerification {
    /**
     * @param args unused
     * @throws Exception if configuration, a live request or reviewed persistence fails
     */
    public static void main(String[] args) throws Exception {
        if (System.getenv("OPENAI_API_KEY") == null || System.getenv("OPENAI_API_KEY").isBlank()) throw new IllegalStateException("Set OPENAI_API_KEY before running live verification.");
        var directory = java.nio.file.Files.createTempDirectory("tp3-ai-live-");
        Database database = new Database("jdbc:h2:file:" + directory.resolve("live")); database.connectToDatabase();
        try {
            database.register(new User("Owner", "Course1!", "", "", "", "", "", false, true, false));
            database.register(new User("Curator", "Course1!", "", "", "", "", "", false, false, false, true));
            var service = new AiAssistanceService(database, new OpenAiClient());
            var initial = new LessonDraft("Persistent H2 storage", "Our course JavaFX project lost lessons between runs.", "Use file-backed H2 and transactions; validate before saving.");
            var first = ask(service, "Owner", "Contributor", new AiRequest(AiPurpose.WRITE, null, initial, "Improve clarity while preserving these facts. Use a concise title under 100 characters.", List.of()));
            var saved = service.saveCore("Owner", "Contributor", null, first.getCore()); check(saved.isSuccessful(), saved.getMessage()); long id = saved.getValue().getId();
            var followUp = ask(service, "Owner", "Contributor", new AiRequest(AiPurpose.WRITE, id, first.getCore(), "Make the lesson more concise and include a useful restart verification step.", List.of(new AiTurn("user", "Improve clarity"), new AiTurn("assistant", first.getConversationText()))));
            check(service.saveCore("Owner", "Contributor", id, followUp.getCore()).isSuccessful(), "Reviewed follow-up save failed");
            ask(service, "Owner", "Contributor", new AiRequest(AiPurpose.BRAINSTORM, id, initial, "Suggest missing context and questions to ask the team; label invented examples honestly.", List.of()));
            var examples = ask(service, "Owner", "Contributor", new AiRequest(AiPurpose.EXAMPLES, id, initial, "Find official H2 documentation or examples for persistent database URLs and transactions. Cite the retrieved URLs.", List.of()));
            check(!examples.getSources().isEmpty(), "Live examples returned no retrieved citation links; inspect provider/tool configuration.");
            new CuratorLessonService(database).setLessonLocks("Curator", "" + id, new LessonLockState(true, true, true, true));
            check(!service.saveCore("Owner", "Contributor", id, initial).isSuccessful(), "Contributor bypassed a lock");
            var refined = ask(service, "Curator", "Curator", new AiRequest(AiPurpose.REFINE, id, initial, "Refine this course lesson for a future team. Keep the factual meaning and title under 100 characters.", List.of()));
            check(service.saveCore("Curator", "Curator", id, refined.getCore()).isSuccessful(), "Curator reviewed refinement failed");
            var terms = ask(service, "Curator", "Curator", new AiRequest(AiPurpose.TERMS, id, refined.getCore(), "Suggest 3 to 6 related search terms. Keep core fields unchanged.", List.of()));
            check(!terms.getTerms().isEmpty() && service.saveTerms("Curator", id, terms.getTerms()).isSuccessful(), "Reviewed related terms failed");
            check(database.getLesson(id).getOwner().equals("Owner"), "Ownership changed");
            System.out.println("LIVE PASS: writing, follow-up, brainstorming, cited examples, Curator refinement, terms, reviewed saves and Contributor lock rejection.");
        } finally {
            database.closeConnection();
            try (var paths = java.nio.file.Files.walk(directory)) { for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(path); }
        }
    }
    private static AiSuggestion ask(AiAssistanceService service, String actor, String role, AiRequest request) {
        var prepared = service.prepare(actor, role, request); check(prepared.isSuccessful(), prepared.getMessage());
        var result = service.generate(prepared.getValue()); check(result.isSuccessful(), result.getMessage()); System.out.println("LIVE PASS: " + request.getPurpose()); return result.getValue();
    }
    private static void check(boolean success, String message) { if (!success) throw new IllegalStateException(message); }
}
