package database;

import static org.junit.jupiter.api.Assertions.*;
import aiAssistance.*;
import entityClasses.*;
import lessonManagement.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/*******
 * <p> Title: TP3FinalIntegrationTest Class. </p>
 * <p> Description: Exercises one combined Contributor-to-Viewer-to-Curator workflow through
 * persistent CRUD, the real AI HTTP adapter with an explicit local fixture, term discovery,
 * locks, statistics sharing, source deletion and restart. This is not live-provider proof.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Final integration verification
 */
public class TP3FinalIntegrationTest {
    @TempDir Path directory;
    private Database database;
    private LessonService lessons;
    /** @throws Exception on isolated fixture setup failure */
    @BeforeEach public void setUp() throws Exception {
        database = new Database("jdbc:h2:file:" + directory.resolve("final")); database.connectToDatabase(); lessons = new LessonService(database);
        database.register(user("Owner", false, true, false, false)); database.register(user("Other", false, true, false, false));
        database.register(user("Curator", false, false, false, true)); database.register(user("Viewer", false, false, true, false));
    }
    /** Close only the temporary test database. */
    @AfterEach public void tearDown() { database.closeConnection(); }
    /** Verify the same records remain coherent across every new subsystem and restart. */
    @Test public void completeRoleWorkflow() throws Exception {
        long destination = lessons.createLesson("Owner", draft("Persistent H2")).getValue().getId();
        long foreign = lessons.createLesson("Other", draft("JavaFX validation")).getValue().getId();
        long first = lessons.createExperience("" + destination, "Owner", experience("30", "40")).getValue().getId();
        long second = lessons.createExperience("" + foreign, "Other", experience("50", "80")).getValue().getId();
        var viewer = new ViewerLessonService(database); var curator = new CuratorLessonService(database); var statistics = new StatisticsService(database);
        equal("Contributors retain isolated lists", 1, lessons.listLessons("Owner").getValue().size()); equal("Viewer sees both core records", 2, viewer.listLessons("Viewer").getValue().size());
        try (var fixture = new AiProviderFixture()) {
            var ai = new AiAssistanceService(database, fixture.client());
            var prepared = ai.prepare("Curator", "Curator", new AiRequest(AiPurpose.REFINE, destination, draft("Persistent H2"), "Refine this course lesson", List.of()));
            assertTrue(prepared.isSuccessful()); var response = ai.generate(prepared.getValue()); assertTrue(response.isSuccessful());
            equal("AI result is not saved automatically", "Persistent H2", database.getLesson(destination).getTitle());
            curator.setLessonLocks("Curator", "" + destination, new LessonLockState(false, true, false, false));
            assertTrue(ai.saveCore("Curator", "Curator", destination, response.getValue().getCore()).isSuccessful());
            assertTrue(ai.saveTerms("Curator", destination, response.getValue().getTerms()).isSuccessful());
            equal("Curator refinement preserves Contributor ownership", "Owner", database.getLesson(destination).getOwner());
            equal("Viewer finds a saved AI term", 1, viewer.searchLessons("Viewer", "rollback").getValue().size());
        }
        assertFalse(lessons.updateLesson("" + destination, "Owner", draft("Rejected locked title")).isSuccessful());
        var preview = statistics.preview("Curator", "" + destination, List.of(first, second)); assertTrue(preview.isSuccessful());
        var saved = statistics.save("Curator", preview.getValue().toRequest()); assertTrue(saved.isSuccessful());
        equal("New summary hidden", 0, statistics.sharedSummaries("Owner", "" + destination).getValue().size());
        assertTrue(statistics.setVisibility("Curator", "" + saved.getValue().getId(), true).isSuccessful());
        equal("Shared elapsed mean", 40.0, statistics.sharedSummaries("Owner", "" + destination).getValue().getFirst().getValues().getMeanElapsedMinutes());
        assertFalse(statistics.sharedSummaries("Other", "" + destination).isSuccessful());
        database.closeConnection(); database.connectToDatabase();
        equal("Restart retains related-term discovery", 1, viewer.searchLessons("Viewer", "rollback").getValue().size());
        equal("Restart retains protection", true, database.getLesson(destination).isTitleLocked());
        equal("Restart retains sharing", 1, statistics.sharedSummaries("Owner", "" + destination).getValue().size());
        assertTrue(lessons.deleteLesson("" + foreign, "Other").isSuccessful());
        var historical = statistics.readSummary("Curator", "" + saved.getValue().getId()).getValue(); assertTrue(historical.isStale());
        equal("Deleting source keeps historical calculation", 40.0, historical.getValues().getMeanElapsedMinutes());
        curator.setLessonLocks("Curator", "" + destination, new LessonLockState(false, false, false, false));
        assertTrue(lessons.deleteLesson("" + destination, "Owner").isSuccessful()); database.closeConnection(); database.connectToDatabase();
        equal("Deletion remains persistent", 0, database.getLessonCount()); equal("Related experiences cascade", 0, database.getExperienceCount());
        assertFalse(statistics.readSummary("Curator", "" + saved.getValue().getId()).isSuccessful()); equal("Related terms cascade", 0, database.getSearchTerms(destination).size());
    }
    /** Validate storage capacity before touching data, including normalized exact boundaries. */
    @Test public void inputBoundaries() throws Exception {
        var accepted = lessons.createLesson("Owner", draft("x".repeat(500))); assertTrue(accepted.isSuccessful());
        int before = database.getLessonCount(); var rejected = lessons.createLesson("Owner", draft("x".repeat(501)));
        assertFalse(rejected.isSuccessful()); assertTrue(rejected.getMessage().contains("500")); equal("Long title leaves count unchanged", before, database.getLessonCount());
        long id = accepted.getValue().getId();
        assertTrue(lessons.createExperience("" + id, "Owner", new ExperienceDraft("What", "How", "0", List.of(new EffortInput("N".repeat(255), "0")))).isSuccessful());
        int records = database.getExperienceCount();
        var invalid = lessons.createExperience("" + id, "Owner", new ExperienceDraft("What", "How", "0", List.of(new EffortInput("N".repeat(256), "0"))));
        assertFalse(invalid.isSuccessful()); assertTrue(invalid.getMessage().contains("255")); equal("Long member name leaves experience count unchanged", records, database.getExperienceCount());
        var edited = lessons.updateLesson("" + id, "Owner", draft("x".repeat(501))); assertFalse(edited.isSuccessful()); equal("Rejected update leaves stored title intact", 500, database.getLesson(id).getTitle().length());
    }
    private static LessonDraft draft(String title) { return new LessonDraft(title, "Course problem", "Course lesson"); }
    private static ExperienceDraft experience(String elapsed, String effort) { return new ExperienceDraft("Tested restart", "Reopened file database", elapsed, List.of(new EffortInput("Sam", effort))); }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) { return new User(name, "Course1!", "", "", "", "", "", admin, contributor, viewer, curator); }
    private static void equal(String label, Object expected, Object actual) { System.out.println(label + " | Expected: " + expected + " | Actual: " + actual + " | " + (java.util.Objects.equals(expected, actual) ? "PASS" : "FAIL")); assertEquals(expected, actual, label); }
}
