package database;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import entityClasses.*;
import lessonManagement.*;

/*******
 * <p> Title: TP3DiscoveryTest Class. </p>
 * <p> Description: Isolated file-backed H2 verification of Viewer and Curator public methods.
 * Invented course data covers roles, privacy, fresh subsets, validation, relationships,
 * independent protections, rollback, migration, concurrency, and separate Java processes.
 * Readable checks show expected/actual values and pass/fail; JUnit reports scenario totals.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 discovery verification
 */
public class TP3DiscoveryTest {
    @TempDir Path directory;
    private Database database;
    private String url, id;
    private ViewerLessonService viewer;
    private CuratorLessonService curator;
    private LessonService contributor;
    /** @throws Exception if the isolated fixture cannot be established */
    @BeforeEach public void setUp() throws Exception {
        url = "jdbc:h2:file:" + directory.resolve("discovery");
        database = new Database(url); database.connectToDatabase();
        for (User account : List.of(user("Admin", true, false, false, false), user("Owner", false, true, false, false),
                user("Other", false, true, false, false), user("Viewer", false, false, true, false), user("Curator", false, false, false, true)))
            database.register(account);
        viewer = new ViewerLessonService(database); curator = new CuratorLessonService(database); contributor = new LessonService(database);
        id = "" + contributor.createLesson("Owner", draft("H2 Persistence")).getValue().getId();
    }
    /** Close only this test's file database; TempDir owns its files. */
    @AfterEach public void tearDown() { database.closeConnection(); }

    /** Viewer gets core-only projections while inherited Contributor reads remain owned. */
    @Test public void discoveryAndPrivacy() {
        var foreign = contributor.createLesson("Other", draft("JavaFX Layout")).getValue();
        equal("Viewer sees both cores", 2, viewer.listLessons("Viewer").getValue().size());
        equal("Curator sees both owners", 2, curator.listLessons("Curator").getValue().size());
        equal("Contributor sees only owned lesson", 1, contributor.listLessons("Owner").getValue().size());
        rejected(contributor.readLesson("" + foreign.getId(), "Owner"));
        var projected = viewer.readLesson("Viewer", "" + foreign.getId()).getValue();
        equal("Projection stable ID", foreign.getId(), projected.getId()); equal("Projection title", "JavaFX Layout", projected.getTitle());
        equal("Projection problem", "Course project database", projected.getProblemSituation());
        equal("Projection text", "Validate before saving", projected.getLessonLearned());
        equal("Projection list text", foreign.getId() + " - JavaFX Layout", projected.toString());
        equal("Viewer fields exclude private data", Set.of("id", "title", "problemSituation", "lessonLearned"),
                Arrays.stream(ViewerLesson.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet()));
        assertThrows(UnsupportedOperationException.class, () -> viewer.listLessons("Viewer").getValue().clear());
        equal("Curator current owner", "Other", curator.readLesson("Curator", "" + foreign.getId()).getValue().getOwner());
    }
    /** All words may match different core fields or saved terms without duplicate lesson IDs. */
    @Test public void andSearch() {
        assertTrue(curator.saveTerms("Curator", id, List.of(" Rollback ", "ROLLBACK", "reliable storage")).isSuccessful());
        equal("Case-insensitive cross-field AND", 1, viewer.searchLessons("Viewer", "H2 COURSE validate rollback STORAGE").getValue().size());
        equal("Missing required word", 0, viewer.searchLessons("Viewer", "h2 absent").getValue().size());
        equal("Curator shared search", 1, curator.searchLessons("Curator", "STORAGE").getValue().size());
        equal("Duplicate term matches only once", 1, viewer.searchLessons("Viewer", "rollback").getValue().size());
        equal("SQL-shaped search treated literally", 0, viewer.searchLessons("Viewer", "' OR 1=1 --").getValue().size());
    }
    /** Independent searches and blank reset never accumulate a previous filter. */
    @Test public void refreshedSearch() {
        contributor.createLesson("Other", draft("JavaFX Layout"));
        equal("First subset", 1, viewer.searchLessons("Viewer", "h2").getValue().size());
        equal("Next independent subset", "JavaFX Layout", viewer.searchLessons("Viewer", "javafx").getValue().getFirst().getTitle());
        for (String query : Arrays.asList(null, "", "  \t\n")) {
            equal("Viewer blank restores all", 2, viewer.searchLessons("Viewer", query).getValue().size());
            equal("Curator blank restores all", 2, curator.searchLessons("Curator", query).getValue().size());
        }
    }
    /** Empty, one-item, and larger collections have no fixed ceiling. */
    @Test public void collectionSizes() throws Exception {
        equal("One item", 1, viewer.listLessons("Viewer").getValue().size()); database.deleteLesson(Long.parseLong(id));
        equal("Empty Viewer list", 0, viewer.listLessons("Viewer").getValue().size());
        equal("Empty Curator list", 0, curator.listLessons("Curator").getValue().size());
        database.inTransaction(() -> {
            for (int n = 0; n < 260; n++) database.createLesson(new LessonsLearned(0, "Owner", "Course " + n, "Problem", "Lesson", false, false, false, false));
            return true;
        });
        equal("Large Viewer list", 260, viewer.listLessons("Viewer").getValue().size());
        equal("Large Curator list", 260, curator.listLessons("Curator").getValue().size());
    }
    /** Invalid and absent IDs fail every path before changing stored records. */
    @Test public void invalidSelections() throws Exception {
        for (String invalid : Arrays.asList(null, "", " ", "0", "-1", "abc", "1 OR 1=1", "9223372036854775808", "99999")) {
            rejected(viewer.readLesson("Viewer", invalid)); rejected(curator.readLesson("Curator", invalid));
            rejected(curator.readTerms("Curator", invalid)); rejected(curator.readExperiences("Curator", invalid));
            rejected(curator.saveTerms("Curator", invalid, List.of("rejected")));
            rejected(curator.refineLesson("Curator", invalid, draft("Rejected")));
            rejected(curator.setLessonLocks("Curator", invalid, locks(true, true, true, true)));
        }
        equal("Invalid IDs preserve count", 1, database.getLessonCount());
        equal("Invalid IDs preserve title", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle());
    }
    /** Admin does not implicitly gain Curator or Viewer permissions. */
    @Test public void wrongRoles() throws Exception {
        for (String actor : Arrays.asList(null, "", "Missing", "Admin", "Owner", "Curator")) {
            rejected(viewer.listLessons(actor)); rejected(viewer.searchLessons(actor, "h2")); rejected(viewer.readLesson(actor, id));
        }
        for (String actor : Arrays.asList(null, "", "Missing", "Admin", "Owner", "Viewer")) allCuratorRejected(actor);
        equal("Unauthorized writes preserve title", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle());
        equal("Unauthorized writes preserve terms", 0, database.getSearchTerms(Long.parseLong(id)).size());
        assertFalse(database.getLesson(Long.parseLong(id)).hasAnyLockedField());
    }
    /** Existing services cannot use roles removed after a screen was opened. */
    @Test public void revokedRoles() throws Exception {
        database.updateUserRole("Viewer", "Curator", "true", "Admin"); database.updateUserRole("Viewer", "Viewer", "false", "Admin");
        rejected(viewer.listLessons("Viewer")); rejected(viewer.searchLessons("Viewer", "h2")); rejected(viewer.readLesson("Viewer", id));
        database.updateUserRole("Curator", "Viewer", "true", "Admin"); database.updateUserRole("Curator", "Curator", "false", "Admin");
        allCuratorRejected("Curator");
        equal("Revoked writes preserve core", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle());
    }
    /** Normalize and deduplicate a reviewed set; only explicit empty replacement clears all. */
    @Test public void termsNormalizationAndClear() {
        var saved = curator.saveTerms("Curator", id, List.of(" H2 ", "h2", "Rollback", "rollback"));
        assertTrue(saved.isSuccessful()); equal("Unique normalized terms", List.of("h2", "rollback"), terms(saved.getValue()));
        for (SearchTerm term : saved.getValue()) equal("Term parent", Long.parseLong(id), term.getLessonId());
        equal("Read persisted terms", List.of("h2", "rollback"), terms(curator.readTerms("Curator", id).getValue()));
        assertThrows(UnsupportedOperationException.class, () -> saved.getValue().clear());
        assertTrue(curator.saveTerms("Curator", id, List.of()).isSuccessful());
        equal("Explicit clear saved", 0, curator.readTerms("Curator", id).getValue().size());
        equal("Cleared terms removed from search", 0, viewer.searchLessons("Viewer", "rollback").getValue().size());
    }
    /** One invalid term rejects all proposed changes before replacing prior saved terms. */
    @Test public void invalidTerms() {
        curator.saveTerms("Curator", id, List.of("original"));
        for (List<String> invalid : Arrays.asList(null, List.of(""), List.of(" "), Arrays.asList("valid", null), List.of("valid", ""), List.of("x".repeat(501)))) {
            rejected(curator.saveTerms("Curator", id, invalid));
            equal("Invalid replacement preserves prior set", List.of("original"), terms(curator.readTerms("Curator", id).getValue()));
        }
        assertTrue(curator.saveTerms("Curator", id, List.of("x".repeat(500))).isSuccessful());
        equal("Term boundary", 500, curator.readTerms("Curator", id).getValue().getFirst().getTerm().length());
    }
    /** An actual constraint failure after DELETE rolls back the entire term replacement. */
    @Test public void sqlFailureRollback() throws Exception {
        curator.saveTerms("Curator", id, List.of("original"));
        assertThrows(SQLException.class, () -> database.inTransaction(() -> {
            database.lockLesson(Long.parseLong(id));
            database.replaceSearchTerms(Long.parseLong(id), List.of(new SearchTerm(Long.parseLong(id), "duplicate"), new SearchTerm(Long.parseLong(id), "duplicate")));
            return true;
        }));
        equal("Constraint rollback restored prior set", List.of("original"), terms(database.getSearchTerms(Long.parseLong(id))));
        assertThrows(SQLException.class, () -> database.replaceSearchTerms(Long.parseLong(id), List.of()));
        assertThrows(SQLException.class, () -> database.inTransaction(() -> {
            database.replaceSearchTerms(Long.parseLong(id), List.of(new SearchTerm(999, "wrong parent"))); return true;
        }));
        equal("Wrong relationship preserved set", List.of("original"), terms(database.getSearchTerms(Long.parseLong(id))));
    }
    /** Curator can refine locked core while retaining original identity and every related value. */
    @Test public void refinementPreservesRelationships() throws Exception {
        var record = contributor.createExperience(id, "Owner", experience()).getValue(); curator.saveTerms("Curator", id, List.of("related"));
        database.setHiddenRoleInformationForTesting(Long.parseLong(id), "Private role information");
        curator.setLessonLocks("Curator", id, locks(true, true, true, true));
        var saved = curator.refineLesson("Curator", id, new LessonDraft(" Refined ", " Clear problem ", " Reviewed lesson ")); assertTrue(saved.isSuccessful());
        var core = saved.getValue(); equal("Stable key", Long.parseLong(id), core.getId()); equal("Stable owner", "Owner", core.getOwner());
        equal("Refined core visible to Viewer", "Refined", viewer.readLesson("Viewer", id).getValue().getTitle());
        equal("Refined core visible to owner", "Refined", contributor.readLesson(id, "Owner").getValue().getTitle());
        assertTrue(core.isLessonLocked()); assertTrue(core.isTitleLocked()); assertTrue(core.isProblemSituationLocked()); assertTrue(core.isLessonLearnedLocked());
        equal("Experience count unchanged", 1, database.getExperienceCount()); equal("Related experience ID unchanged", record.getId(), curator.readExperiences("Curator", id).getValue().getFirst().getId());
        equal("Related effort unchanged", 10.0, curator.readExperiences("Curator", id).getValue().getFirst().getTeamEfforts().getFirst().getEffortMinutes());
        equal("Terms unchanged", List.of("related"), terms(curator.readTerms("Curator", id).getValue()));
        equal("Private data unchanged", "Private role information", database.getHiddenRoleInformationForTesting(Long.parseLong(id)));
        equal("Search updates with core", 0, viewer.searchLessons("Viewer", "persistence").getValue().size());
    }
    /** Every required core input and the persistent title boundary is validated before saving. */
    @Test public void invalidCore() throws Exception {
        List<LessonDraft> invalid = new ArrayList<>(); invalid.add(null);
        for (String blank : Arrays.asList(null, "", "  ")) {
            invalid.add(new LessonDraft(blank, "Problem", "Lesson")); invalid.add(new LessonDraft("Title", blank, "Lesson")); invalid.add(new LessonDraft("Title", "Problem", blank));
        }
        invalid.add(draft("x".repeat(501)));
        for (var draft : invalid) {
            rejected(curator.refineLesson("Curator", id, draft)); equal("Invalid core preserves title", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle());
            equal("Invalid core preserves count", 1, database.getLessonCount());
        }
        assertTrue(curator.refineLesson("Curator", id, draft("x".repeat(500))).isSuccessful());
        equal("Title boundary", 500, database.getLesson(Long.parseLong(id)).getTitle().length());
    }
    /** Field flags protect changed values while allowing unchanged protected values in unrelated edits. */
    @Test public void independentLocks() {
        for (int index = 0; index < 3; index++) {
            boolean title = index == 0, problem = index == 1, text = index == 2;
            var core = curator.setLessonLocks("Curator", id, locks(false, title, problem, text)).getValue();
            equal("Title flag mapping", title, core.isTitleLocked()); equal("Problem flag mapping", problem, core.isProblemSituationLocked()); equal("Text flag mapping", text, core.isLessonLearnedLocked());
            rejected(contributor.updateLesson(id, "Owner", new LessonDraft(title ? "Changed" : core.getTitle(), problem ? "Changed" : core.getProblemSituation(), text ? "Changed" : core.getLessonLearned())));
            rejected(contributor.deleteLesson(id, "Owner"));
            assertTrue(contributor.updateLesson(id, "Owner", new LessonDraft(title ? core.getTitle() : "Allowed " + index,
                    problem ? core.getProblemSituation() : "Allowed problem " + index, text ? core.getLessonLearned() : "Allowed text " + index)).isSuccessful());
        }
        curator.setLessonLocks("Curator", id, locks(true, true, false, true));
        var core = curator.setLessonLocks("Curator", id, locks(false, true, false, true)).getValue();
        assertFalse(core.isLessonLocked()); assertTrue(core.isTitleLocked()); assertTrue(core.isLessonLearnedLocked());
        rejected(curator.setLessonLocks("Curator", id, null)); assertTrue(curator.readLesson("Curator", id).getValue().isTitleLocked());
    }
    /** Whole-record protection covers core deletion and every related experience mutation. */
    @Test public void wholeLessonProtection() throws Exception {
        var record = contributor.createExperience(id, "Owner", experience()).getValue(); curator.setLessonLocks("Curator", id, locks(true, false, false, false));
        rejected(contributor.updateLesson(id, "Owner", draft("Rejected"))); rejected(contributor.deleteLesson(id, "Owner"));
        rejected(contributor.createExperience(id, "Owner", experience())); rejected(contributor.updateExperience("" + record.getId(), "Owner", experience()));
        rejected(contributor.deleteExperience("" + record.getId(), "Owner")); equal("Protected attempts unchanged", 1, database.getExperienceCount());
        curator.setLessonLocks("Curator", id, locks(false, false, false, false));
        assertTrue(contributor.deleteExperience("" + record.getId(), "Owner").isSuccessful()); assertTrue(contributor.deleteLesson(id, "Owner").isSuccessful());
    }
    /** Curator evidence reads retain exact IDs, units, effort entries and persisted locks. */
    @Test public void experienceReview() throws Exception {
        equal("Empty experience list", 0, curator.readExperiences("Curator", id).getValue().size());
        var record = contributor.createExperience(id, "Owner", experience()).getValue(); database.setExperienceLocksForTesting(record.getId(), true, true, false, true, false);
        String foreign = "" + contributor.createLesson("Other", draft("Foreign")).getValue().getId(); contributor.createExperience(foreign, "Other", experience());
        var rows = curator.readExperiences("Curator", id).getValue(); equal("Only related record", 1, rows.size());
        var row = rows.getFirst(); equal("Exact experience key", record.getId(), row.getId()); equal("Parent key", Long.parseLong(id), row.getLessonId());
        equal("Elapsed minutes", 12.0, row.getElapsedMinutes()); equal("Effort person-minutes", 10.0, row.getTeamEfforts().getFirst().getEffortMinutes());
        assertTrue(row.isExperienceLocked()); assertTrue(row.isWhatWasDoneLocked()); assertTrue(row.isElapsedTimeLocked()); assertFalse(row.isHowItWasDoneLocked());
        assertThrows(UnsupportedOperationException.class, () -> rows.clear());
    }
    /** A later failed operation rolls back all nested Curator writes without inner commits. */
    @Test public void enclosingFailureRollback() throws Exception {
        curator.saveTerms("Curator", id, List.of("original"));
        var result = database.inTransaction(() -> {
            assertTrue(curator.refineLesson("Curator", id, draft("Transient")).isSuccessful()); assertTrue(curator.saveTerms("Curator", id, List.of("transient")).isSuccessful());
            assertTrue(curator.setLessonLocks("Curator", id, locks(true, true, true, true)).isSuccessful()); return OperationResult.failure("Injected later failure");
        }); rejected(result);
        equal("Outer rollback restores title", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle());
        equal("Outer rollback restores terms", List.of("original"), terms(database.getSearchTerms(Long.parseLong(id)))); assertFalse(database.getLesson(Long.parseLong(id)).hasAnyLockedField());
        assertThrows(SQLException.class, () -> database.updateLessonLocks(Long.parseLong(id), locks(true, false, false, false)));
    }
    /** Storage errors after each kind of write must return a corrective result after rollback. */
    @Test public void serviceFailureRollback() throws Exception {
        curator.saveTerms("Curator", id, List.of("original"));
        Database faulty = new Database(url) {
            @Override public void replaceSearchTerms(long parent, List<SearchTerm> terms) throws SQLException { super.replaceSearchTerms(parent, terms); throw new SQLException("Injected post-write failure"); }
            @Override public boolean updateLesson(LessonsLearned lesson) throws SQLException { super.updateLesson(lesson); throw new SQLException("Injected post-write failure"); }
            @Override public boolean updateLessonLocks(long parent, LessonLockState locks) throws SQLException { super.updateLessonLocks(parent, locks); throw new SQLException("Injected post-write failure"); }
        }; faulty.connectToDatabase();
        try {
            var service = new CuratorLessonService(faulty); rejected(service.saveTerms("Curator", id, List.of("rejected")));
            rejected(service.refineLesson("Curator", id, draft("Rejected"))); rejected(service.setLessonLocks("Curator", id, locks(true, true, true, true)));
            equal("Failed term write rolled back", List.of("original"), terms(database.getSearchTerms(Long.parseLong(id))));
            equal("Failed core write rolled back", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle()); assertFalse(database.getLesson(Long.parseLong(id)).hasAnyLockedField());
        } finally { faulty.closeConnection(); }
    }
    /** Actual Curator protections serialize with a waiting Contributor edit at the shared parent row. */
    @Test public void concurrentLock() throws Exception {
        Database other = new Database(url); other.connectToDatabase(); ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1); AtomicReference<Future<OperationResult<LessonsLearned>>> pending = new AtomicReference<>();
        try {
            database.inTransaction(() -> {
                assertTrue(curator.setLessonLocks("Curator", id, locks(true, false, false, false)).isSuccessful());
                pending.set(worker.submit(() -> { started.countDown(); return new LessonService(other).updateLesson(id, "Owner", draft("Rejected")); }));
                assertTrue(assertDoesNotThrow(() -> started.await(2, TimeUnit.SECONDS)));
                assertThrows(TimeoutException.class, () -> pending.get().get(150, TimeUnit.MILLISECONDS)); return true;
            });
            rejected(pending.get().get(5, TimeUnit.SECONDS)); equal("Waiting edit observes committed protection", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle());
        } finally { worker.shutdownNow(); other.closeConnection(); }
    }
    /** Cascading deletion removes only the selected parent's terms and experiences. */
    @Test public void deletedSelections() throws Exception {
        curator.saveTerms("Curator", id, List.of("related")); contributor.createExperience(id, "Owner", experience());
        String other = "" + contributor.createLesson("Other", draft("Other lesson")).getValue().getId(); curator.saveTerms("Curator", other, List.of("other term"));
        assertTrue(contributor.deleteLesson(id, "Owner").isSuccessful()); equal("Cascade terms", 0, database.getSearchTerms(Long.parseLong(id)).size());
        equal("Cascade experiences", 0, database.getExperienceCount()); equal("Unrelated terms retained", List.of("other term"), terms(database.getSearchTerms(Long.parseLong(other))));
        equal("Refreshed discovery removes deleted row", 1, viewer.listLessons("Viewer").getValue().size()); rejected(viewer.readLesson("Viewer", id)); rejected(curator.readLesson("Curator", id));
    }
    /** Repeatable additive table creation leaves populated TP1 and TP2 records unchanged. */
    @Test public void populatedMigration() throws Exception {
        contributor.createExperience(id, "Owner", experience()); database.closeConnection();
        try (Connection connection = DriverManager.getConnection(url, "sa", ""); Statement sql = connection.createStatement()) { sql.execute("DROP TABLE lessonSearchTerms"); }
        for (int n = 0; n < 2; n++) {
            database.connectToDatabase(); equal("Migrated owner retained", "Owner", database.findUserForRoleAccess("Owner").getUserName());
            equal("Migrated lesson retained", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle()); equal("Migrated experience retained", 1, database.getExperienceCount());
            equal("Added term table empty", 0, database.getSearchTerms(Long.parseLong(id)).size()); if (n == 0) database.closeConnection();
        }
    }
    /** Four independent JVMs verify persistence of core, terms, locks, efforts and deletion. */
    @Test public void persistenceAcrossProcesses() throws Exception {
        for (String phase : List.of("create", "verify", "delete", "verify-deleted")) {
            Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp", System.getProperty("tp3.test.classpath"),
                    "database.TP3DiscoveryPersistenceProcess", phase, directory.resolve("process-db").toString()).redirectErrorStream(true).start();
            if (!child.waitFor(20, TimeUnit.SECONDS)) { child.destroyForcibly(); fail("Separate JVM timed out"); }
            String output = new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, child.exitValue(), output); assertTrue(output.contains("PASS " + phase), output);
            equal("Separate JVM " + phase, 0, child.exitValue());
        }
    }
    /** Closed storage must fail without returning previously authorized content. */
    @Test public void unavailableStorage() {
        database.closeConnection(); rejected(viewer.listLessons("Viewer")); rejected(viewer.readLesson("Viewer", id)); allCuratorRejected("Curator");
    }
    private void allCuratorRejected(String actor) {
        rejected(curator.listLessons(actor)); rejected(curator.searchLessons(actor, "h2")); rejected(curator.readLesson(actor, id));
        rejected(curator.readTerms(actor, id)); rejected(curator.readExperiences(actor, id)); rejected(curator.saveTerms(actor, id, List.of("rejected")));
        rejected(curator.refineLesson(actor, id, draft("Rejected"))); rejected(curator.setLessonLocks(actor, id, locks(true, true, true, true)));
    }
    private static void rejected(OperationResult<?> result) {
        equal("Rejected operation", false, result.isSuccessful()); assertNull(result.getValue()); assertFalse(result.getMessage().isBlank());
    }
    private static void equal(String name, Object expected, Object actual) {
        System.out.println(name + " | Expected: " + expected + " | Actual: " + actual + " | " + (Objects.equals(expected, actual) ? "PASS" : "FAIL")); assertEquals(expected, actual, name);
    }
    private static List<String> terms(List<SearchTerm> terms) { return terms.stream().map(SearchTerm::getTerm).toList(); }
    private static LessonLockState locks(boolean whole, boolean title, boolean problem, boolean text) { return new LessonLockState(whole, title, problem, text); }
    private static LessonDraft draft(String title) { return new LessonDraft(title, "Course project database", "Validate before saving"); }
    private static ExperienceDraft experience() { return new ExperienceDraft("Verified persistence", "Restarted application", "12", List.of(new EffortInput("Sam", "10"))); }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) {
        return new User(name, "Course1!", "Course", "", "Project", "Course", "course@example.test", admin, contributor, viewer, curator);
    }
}
