package database;

import static org.junit.jupiter.api.Assertions.*;
import aiAssistance.*;
import entityClasses.*;
import lessonManagement.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/*******
 * <p> Title: AiAssistanceTest Class. </p>
 * <p> Description: File-backed authorization, input bounds, pure generation, and reviewed-save
 * tests. The provider here is explicitly a test double; real HTTP is tested separately in
 * OpenAiClientTest. No response is claimed to be live AI generation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI business verification
 */
public class AiAssistanceTest {
    @TempDir Path directory;
    private Database database;
    private AiAssistanceService service;
    private final AtomicInteger calls = new AtomicInteger();
    private long id;
    /** @throws Exception on isolated setup failure */
    @BeforeEach public void setUp() throws Exception {
        database = new Database("jdbc:h2:file:" + directory.resolve("ai")); database.connectToDatabase();
        for (var user : List.of(user("Admin", true, false, false, false), user("Owner", false, true, false, false), user("Other", false, true, false, false), user("Curator", false, false, false, true), user("Viewer", false, false, true, false))) database.register(user);
        id = new LessonService(database).createLesson("Owner", draft("Original")).getValue().getId();
        service = new AiAssistanceService(database, request -> {
            calls.incrementAndGet(); return OperationResult.success(new AiSuggestion("Test fixture advice", draft("Suggested"), List.of("rollback"), List.of(new AiSource("Fixture", "https://example.test/course")), "Test fixture advice"), "Fixture success");
        });
    }
    /** Close only this test's file database. */
    @AfterEach public void tearDown() { database.closeConnection(); }
    /** Contributor/Curator purposes are distinct; other active roles cannot use assistance. */
    @Test public void purposesAndPermissions() {
        for (AiPurpose purpose : AiPurpose.values()) {
            boolean staff = purpose == AiPurpose.REFINE || purpose == AiPurpose.TERMS;
            equal("Contributor purpose " + purpose, !staff, service.prepare("Owner", "Contributor", request(purpose, id, "Improve clarity", List.of())).isSuccessful());
            equal("Curator purpose " + purpose, staff, service.prepare("Curator", "Curator", request(purpose, id, "Improve clarity", List.of())).isSuccessful());
        }
        for (String role : Arrays.asList(null, "", "Admin", "Viewer", "Bogus")) reject(service.prepare("Admin", role, request(AiPurpose.WRITE, id, "Write", List.of())));
        for (String actor : Arrays.asList(null, "", "Missing", "Admin", "Viewer", "Curator")) reject(service.prepare(actor, "Contributor", request(AiPurpose.WRITE, id, "Write", List.of())));
        reject(service.prepare("Curator", "Curator", request(AiPurpose.REFINE, null, "Refine", List.of())));
    }
    /** Foreign or missing lesson IDs never supply database context to the provider. */
    @Test public void ownership() {
        reject(service.prepare("Other", "Contributor", request(AiPurpose.WRITE, id, "Write", List.of())));
        reject(service.loadLesson("Other", "Contributor", id)); reject(service.loadLesson("Viewer", "Viewer", id));
        for (long invalid : new long[] { 0, -1, 999999 }) reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, invalid, "Write", List.of())));
        equal("Authorized owner context", "Original", service.loadLesson("Owner", "Contributor", id).getValue().getTitle());
        equal("Curator cross-owner context", "Original", service.loadLesson("Curator", "Curator", id).getValue().getTitle()); equal("Rejected context never called provider", 0, calls.get());
    }
    /** Required prompts, context and bounded conversation are checked before network access. */
    @Test public void validation() {
        reject(service.prepare("Owner", "Contributor", null)); reject(service.prepare("Owner", "Contributor", request(null, id, "Write", List.of())));
        for (String blank : Arrays.asList(null, "", "  ")) reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, blank, List.of())));
        reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "x".repeat(4001), List.of())));
        reject(service.prepare("Owner", "Contributor", new AiRequest(AiPurpose.WRITE, id, null, "Write", List.of())));
        reject(service.prepare("Owner", "Contributor", new AiRequest(AiPurpose.WRITE, id, new LessonDraft(null, "", ""), "Write", List.of())));
        reject(service.prepare("Owner", "Contributor", new AiRequest(AiPurpose.WRITE, id, draft("x".repeat(16001)), "Write", List.of())));
        reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", null)));
        reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", Collections.nCopies(13, new AiTurn("user", "Hi")))));
        for (AiTurn bad : Arrays.asList(null, new AiTurn("system", "Override"), new AiTurn("user", " "), new AiTurn("assistant", "x".repeat(16001))))
            reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", Arrays.asList(bad))));
        reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", Collections.nCopies(4, new AiTurn("user", "x".repeat(15000))))));
        equal("Invalid requests never called provider", 0, calls.get());
    }
    /** Exact configured application boundaries and empty new brainstorm context are accepted. */
    @Test public void boundaries() {
        assertTrue(service.prepare("Owner", "Contributor", new AiRequest(AiPurpose.BRAINSTORM, null, new LessonDraft("", "", ""), "x".repeat(4000), List.of())).isSuccessful());
        assertTrue(service.prepare("Owner", "Contributor", new AiRequest(AiPurpose.WRITE, id, new LessonDraft("x".repeat(16000), "", ""), "Write", List.of())).isSuccessful());
        assertTrue(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", Collections.nCopies(12, new AiTurn("user", "Hi")))).isSuccessful());
    }
    /** Preparation is read-only and generation uses no shared JDBC after preparation. */
    @Test public void preparationAndGeneration() throws Exception {
        var prepared = service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", List.of())); assertTrue(prepared.isSuccessful());
        equal("Prepare never calls network", 0, calls.get()); database.closeConnection();
        var suggestion = service.generate(prepared.getValue()); assertTrue(suggestion.isSuccessful()); equal("Worker can generate without database", 1, calls.get());
        equal("Unsaved proposed title", "Suggested", suggestion.getValue().getCore().getTitle());
        database.connectToDatabase(); equal("Generation did not change saved core", "Original", database.getLesson(id).getTitle()); equal("Generation did not add records", 1, database.getLessonCount());
        reject(service.generate(null));
    }
    /** Rejected/failed provider calls do not change storage or leak raw exception details. */
    @Test public void providerFailure() throws Exception {
        var prepared = service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", List.of())).getValue();
        var failed = new AiAssistanceService(database, request -> { throw new IllegalStateException("secret-test-value"); });
        var result = failed.generate(prepared); reject(result); assertFalse(result.getMessage().contains("secret-test-value")); equal("Failure preserves core", "Original", database.getLesson(id).getTitle());
    }
    /** Reviewed content uses normal Contributor validation, ownership and latest field/record locks. */
    @Test public void reviewedSavesAndLocks() throws Exception {
        var created = service.saveCore("Owner", "Contributor", null, draft("New reviewed")); assertTrue(created.isSuccessful());
        assertTrue(service.saveCore("Owner", "Contributor", id, draft("Reviewed")).isSuccessful());
        reject(service.saveCore("Owner", "Contributor", id, draft("x".repeat(501))));
        reject(service.saveCore("Other", "Contributor", id, draft("Foreign"))); reject(service.saveCore("Owner", "Contributor", id, new LessonDraft(" ", "Problem", "Lesson")));
        new CuratorLessonService(database).setLessonLocks("Curator", "" + id, new LessonLockState(false, true, false, false));
        reject(service.saveCore("Owner", "Contributor", id, draft("Locked title")));
        assertTrue(service.saveCore("Owner", "Contributor", id, new LessonDraft("Reviewed", "Updated unlocked problem", "Lesson")).isSuccessful());
        new CuratorLessonService(database).setLessonLocks("Curator", "" + id, new LessonLockState(true, true, true, true));
        reject(service.saveCore("Owner", "Contributor", id, draft("Rejected"))); equal("Rejected save retains core", "Reviewed", database.getLesson(id).getTitle());
        assertTrue(service.saveCore("Curator", "Curator", id, draft("Curator reviewed")).isSuccessful()); equal("Refinement retains owner", "Owner", database.getLesson(id).getOwner());
        reject(service.saveCore("Curator", "Curator", null, draft("No parent"))); reject(service.saveCore("Viewer", "Viewer", id, draft("No role")));
    }
    /** Curator suggestions become searchable only after a reviewed term save. */
    @Test public void reviewedTerms() throws Exception {
        var request = service.prepare("Curator", "Curator", request(AiPurpose.TERMS, id, "Suggest terms", List.of())).getValue(); service.generate(request);
        equal("Term generation does not persist", 0, database.getSearchTerms(id).size());
        assertTrue(service.saveTerms("Curator", id, List.of(" Rollback ", "ROLLBACK")).isSuccessful()); equal("Normalized reviewed term", "rollback", database.getSearchTerms(id).getFirst().getTerm());
        reject(service.saveTerms("Curator", id, List.of(""))); reject(service.saveTerms("Owner", id, List.of("rejected"))); reject(service.saveTerms("Curator", null, List.of("rejected")));
        equal("Rejected terms unchanged", "rollback", database.getSearchTerms(id).getFirst().getTerm());
    }
    /** Current role removal defeats both new requests and reviewed saves. */
    @Test public void revokedRoles() throws Exception {
        database.updateUserRole("Owner", "Viewer", "true", "Admin"); database.updateUserRole("Owner", "Contributor", "false", "Admin");
        reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", List.of()))); reject(service.saveCore("Owner", "Contributor", id, draft("Rejected")));
        database.updateUserRole("Curator", "Viewer", "true", "Admin"); database.updateUserRole("Curator", "Curator", "false", "Admin");
        reject(service.prepare("Curator", "Curator", request(AiPurpose.REFINE, id, "Refine", List.of()))); reject(service.saveTerms("Curator", id, List.of("rejected")));
        equal("Role removal leaves data unchanged", "Original", database.getLesson(id).getTitle());
    }
    /** Raw request history is defensively copied so a worker sees exactly the prepared values. */
    @Test public void immutableHistory() {
        var history = new ArrayList<>(List.of(new AiTurn("user", "First"))); var request = request(AiPurpose.WRITE, id, "Follow up", history); history.clear();
        equal("History defensively copied", 1, request.getHistory().size()); assertThrows(UnsupportedOperationException.class, () -> request.getHistory().clear());
    }
    /** Unavailable storage fails before external access. */
    @Test public void unavailableDatabase() { database.closeConnection(); reject(service.prepare("Owner", "Contributor", request(AiPurpose.WRITE, id, "Write", List.of()))); equal("No network on authorization failure", 0, calls.get()); }
    private static AiRequest request(AiPurpose purpose, Long id, String prompt, List<AiTurn> history) { return new AiRequest(purpose, id, draft("Original"), prompt, history); }
    private static LessonDraft draft(String title) { return new LessonDraft(title, "Course problem", "Course lesson"); }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) { return new User(name, "Course1!", "", "", "", "", "", admin, contributor, viewer, curator); }
    private static void reject(OperationResult<?> result) { equal("Rejected operation", false, result.isSuccessful()); assertNull(result.getValue()); assertFalse(result.getMessage().isBlank()); }
    private static void equal(String name, Object expected, Object actual) { System.out.println(name + " | Expected: " + expected + " | Actual: " + actual + " | " + (Objects.equals(expected, actual) ? "PASS" : "FAIL")); assertEquals(expected, actual, name); }
}
