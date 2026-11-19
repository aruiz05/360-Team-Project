package database;

import static org.junit.jupiter.api.Assertions.*;
import entityClasses.*;
import lessonManagement.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/*******
 * <p> Title: TP3StatisticsTest Class. </p>
 * <p> Description: File-backed tests of pure calculations, reviewed inputs, immutable snapshots,
 * current-role access, owned aggregate projections, rollback, cascades, migration, concurrency
 * and independent processes. All course data is invented. Expected/actual checks and JUnit
 * scenario totals are printed; no production database is used.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics verification
 */
public class TP3StatisticsTest {
    @TempDir Path directory;
    private Database database;
    private StatisticsService service;
    private LessonService lessons;
    private String url, destination;
    private long otherLesson, first, second;
    /** @throws Exception on fixture setup failure */
    @BeforeEach public void setUp() throws Exception {
        url = "jdbc:h2:file:" + directory.resolve("statistics"); database = new Database(url); database.connectToDatabase();
        for (var user : List.of(user("Admin", true, false, false, false), user("Owner", false, true, false, false),
                user("Other", false, true, false, false), user("Curator", false, false, false, true), user("Viewer", false, false, true, false))) database.register(user);
        lessons = new LessonService(database); service = new StatisticsService(database);
        destination = "" + lessons.createLesson("Owner", new LessonDraft("H2 Persistence", "Course project restart", "Use persistent transactions")).getValue().getId();
        otherLesson = lessons.createLesson("Other", new LessonDraft("JavaFX validation", "Course UI input", "Validate required input")).getValue().getId();
        first = lessons.createExperience(destination, "Owner", draft("30", "40")).getValue().getId();
        second = lessons.createExperience("" + otherLesson, "Other", draft("50", "80")).getValue().getId();
    }
    /** Close only this test's temporary persistent database. */
    @AfterEach public void tearDown() { database.closeConnection(); }
    /** Cross-lesson review, duplicate deduplication, saved evidence and hidden defaults. */
    @Test public void previewAndSave() throws Exception {
        equal("All Curator source attempts", 2, service.listSources("Curator").getValue().size());
        var preview = service.preview("Curator", destination, List.of(first, second, first)); assertTrue(preview.isSuccessful());
        checkValues(preview.getValue().getValues(), 2, 40, 40, 30, 50, 60, 60, 40, 80);
        equal("Preview writes no summaries", 0, count("statisticsSummaries"));
        equal("Preview deduplicates sources", 2, preview.getValue().getSources().size());
        var saved = save(); assertTrue(saved.getId() > 0); assertFalse(saved.isVisibleToContributor()); assertFalse(saved.isStale());
        equal("Destination retained", Long.parseLong(destination), saved.getDestinationLessonId()); assertNotNull(saved.getCreatedAt());
        equal("Header count", 1, count("statisticsSummaries")); equal("Source count", 2, count("statisticsSourceSnapshots"));
        checkValues(saved.getValues(), 2, 40, 40, 30, 50, 60, 60, 40, 80);
        for (var source : saved.getSources()) {
            assertTrue(source.getId() > 0); equal("Source parent summary", saved.getId(), source.getSummaryId());
            equal("Initial live reference", source.getOriginalExperienceId(), source.getLiveExperienceId()); equal("Fingerprint SHA length", 64, source.getSourceFingerprint().length());
        }
        equal("Read saved snapshot", saved.getId(), service.readSummary("Curator", "" + saved.getId()).getValue().getId());
        equal("List destination snapshots", 1, service.listSummaries("Curator", destination).getValue().size());
        equal("Default hidden", 0, service.sharedSummaries("Owner", destination).getValue().size());
    }
    /** Odd/even medians, zero and large representable means avoid intermediate arithmetic overflow. */
    @Test public void calculationBoundaries() {
        var even = StatisticsCalculator.calculate(List.of(source(1, 0, 0), source(2, 10, 20))).getValue();
        checkValues(even, 2, 5, 5, 0, 10, 10, 10, 0, 20);
        var odd = StatisticsCalculator.calculate(List.of(source(1, 70, 120), source(2, 30, 40), source(3, 50, 80))).getValue();
        checkValues(odd, 3, 50, 50, 30, 70, 80, 80, 40, 120);
        var maximum = StatisticsCalculator.calculate(List.of(source(1, Double.MAX_VALUE, Double.MAX_VALUE), source(2, Double.MAX_VALUE, Double.MAX_VALUE))).getValue();
        equal("Large finite mean", Double.MAX_VALUE, maximum.getMeanElapsedMinutes()); equal("Large finite median", Double.MAX_VALUE, maximum.getMedianEffortMinutes());
        var tiny = StatisticsCalculator.calculate(List.of(source(1, Double.MIN_VALUE, 0), source(2, Double.MIN_VALUE, 0))).getValue();
        equal("Small representable median", Double.MIN_VALUE, tiny.getMedianElapsedMinutes());
    }
    /** Invalid calculation inputs are rejected in full rather than silently excluded. */
    @Test public void invalidNumbers() {
        reject(StatisticsCalculator.calculate(null)); reject(StatisticsCalculator.calculate(List.of())); reject(StatisticsCalculator.calculate(List.of(source(1, 1, 1))));
        reject(StatisticsCalculator.calculate(Arrays.asList(source(1, 1, 1), null)));
        reject(StatisticsCalculator.calculate(List.of(source(1, 1, 1), source(1, 2, 2))));
        for (double value : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            reject(StatisticsCalculator.calculate(List.of(source(1, value, 1), source(2, 2, 2))));
            reject(StatisticsCalculator.calculate(List.of(source(1, 1, value), source(2, 2, 2))));
        }
    }
    /** Required selections, positive IDs and destination existence are checked before storage. */
    @Test public void invalidSelections() throws Exception {
        for (String id : Arrays.asList(null, "", " ", "0", "-1", "bad", "1 OR 1=1", "9223372036854775808", "999999")) {
            reject(service.preview("Curator", id, List.of(first, second))); reject(service.listSummaries("Curator", id));
            reject(service.readSummary("Curator", id)); reject(service.setVisibility("Curator", id, true)); reject(service.sharedSummaries("Owner", id));
        }
        for (List<Long> ids : Arrays.asList(null, List.<Long>of(), List.of(first), List.of(first, first), Arrays.asList(first, null), List.of(first, -1L), List.of(first, 999999L)))
            reject(service.preview("Curator", destination, ids));
        equal("Rejected selection writes nothing", 0, count("statisticsSummaries"));
    }
    /** Reviewed input fingerprints must cover exactly the selected source set. */
    @Test public void invalidReviewedRequests() throws Exception {
        reject(service.save("Curator", null));
        var valid = preview().toRequest(); reject(service.save("Curator", new StatisticsRequest(valid.getDestinationLessonId(), valid.getSourceExperienceIds(), null)));
        reject(service.save("Curator", new StatisticsRequest(valid.getDestinationLessonId(), valid.getSourceExperienceIds(), Map.of())));
        Map<Long, String> extra = new HashMap<>(valid.getReviewedFingerprints()); extra.put(999L, "not a source");
        reject(service.save("Curator", new StatisticsRequest(valid.getDestinationLessonId(), valid.getSourceExperienceIds(), extra)));
        reject(service.save("Curator", new StatisticsRequest(0, valid.getSourceExperienceIds(), valid.getReviewedFingerprints())));
        reject(service.save("Curator", new StatisticsRequest(valid.getDestinationLessonId(), null, valid.getReviewedFingerprints())));
        equal("Rejected reviewed requests preserve count", 0, count("statisticsSummaries"));
    }
    /** A changed live input cannot be saved using an older reviewed preview. */
    @Test public void stalePreview() throws Exception {
        var request = preview().toRequest(); assertTrue(lessons.updateExperience("" + second, "Other", draft("60", "90")).isSuccessful());
        reject(service.save("Curator", request)); equal("Stale preview writes no header", 0, count("statisticsSummaries"));
        var fresh = save(); equal("Reviewed new inputs accepted", 45.0, fresh.getValues().getMeanElapsedMinutes());
    }
    /** Deleting a selected source after preview rejects save without partial records. */
    @Test public void deletedPreviewSource() throws Exception {
        var request = preview().toRequest(); assertTrue(lessons.deleteExperience("" + second, "Other").isSuccessful());
        reject(service.save("Curator", request)); equal("Deleted preview leaves no rows", 0, count("statisticsSummaries"));
    }
    /** Historical source IDs/values survive later changes and source lesson deletion. */
    @Test public void sourceChanges() throws Exception {
        var saved = save(); assertTrue(lessons.updateExperience("" + second, "Other", draft("90", "120")).isSuccessful());
        var changed = service.readSummary("Curator", "" + saved.getId()).getValue(); assertTrue(changed.isStale());
        equal("Historical mean unchanged", 40.0, changed.getValues().getMeanElapsedMinutes());
        assertTrue(lessons.deleteLesson("" + otherLesson, "Other").isSuccessful());
        var missing = service.readSummary("Curator", "" + saved.getId()).getValue(); assertTrue(missing.isStale());
        var source = missing.getSources().stream().filter(x -> x.getOriginalExperienceId() == second).findFirst().orElseThrow();
        equal("Historical lesson provenance retained", otherLesson, source.getOriginalLessonId()); equal("Historical input retained", 50.0, source.getElapsedMinutes());
        assertNull(source.getLiveExperienceId()); equal("Historical source count retained", 2, missing.getSources().size());
        equal("List includes staleness", true, service.listSummaries("Curator", destination).getValue().getFirst().isStale());
    }
    /** Member name/order changes are tracked even when aggregate effort totals stay equal. */
    @Test public void fingerprintCoversOrderedEfforts() throws Exception {
        var request = preview().toRequest(); var changed = new ExperienceDraft("What", "How", "50", List.of(new EffortInput("Different member", "80")));
        assertTrue(lessons.updateExperience("" + second, "Other", changed).isSuccessful()); reject(service.save("Curator", request));
        var saved = save(); assertFalse(saved.isStale());
        assertTrue(lessons.updateExperience("" + second, "Other", new ExperienceDraft("What", "How", "50", List.of(new EffortInput("A", "40"), new EffortInput("B", "40")))).isSuccessful());
        assertTrue(service.readSummary("Curator", "" + saved.getId()).getValue().isStale());
    }
    /** Visibility is explicit, owner-scoped and projected without foreign evidence. */
    @Test public void visibilityAndPrivacy() {
        var saved = save(); equal("New summary hidden", 0, service.sharedSummaries("Owner", destination).getValue().size());
        var shared = service.setVisibility("Curator", "" + saved.getId(), true); assertTrue(shared.isSuccessful()); assertTrue(shared.getValue().isVisibleToContributor());
        var values = service.sharedSummaries("Owner", destination).getValue(); equal("Owner receives one shared aggregate", 1, values.size());
        equal("Shared aggregate mean", 40.0, values.getFirst().getValues().getMeanElapsedMinutes());
        equal("Safe projection fields", Set.of("id", "createdAt", "values"), Arrays.stream(SharedStatistics.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet()));
        reject(service.sharedSummaries("Other", destination)); reject(service.sharedSummaries("Viewer", destination)); reject(service.sharedSummaries("Curator", destination));
        var hidden = service.setVisibility("Curator", "" + saved.getId(), false).getValue();
        equal("Hide applied on next read", 0, service.sharedSummaries("Owner", destination).getValue().size());
        equal("Visibility preserves calculations", saved.getValues().toString(), hidden.getValues().toString()); equal("Visibility preserves time", saved.getCreatedAt(), hidden.getCreatedAt());
    }
    /** Wrong or missing roles cannot read Curator evidence or mutate sharing. */
    @Test public void permissions() throws Exception {
        var request = preview().toRequest(); var saved = save();
        for (String actor : Arrays.asList(null, "", "Missing", "Admin", "Viewer", "Owner")) {
            reject(service.listSources(actor)); reject(service.preview(actor, destination, List.of(first, second))); reject(service.save(actor, request));
            reject(service.listSummaries(actor, destination)); reject(service.readSummary(actor, "" + saved.getId())); reject(service.setVisibility(actor, "" + saved.getId(), true));
        }
        equal("Rejected sharing leaves hidden", false, service.readSummary("Curator", "" + saved.getId()).getValue().isVisibleToContributor());
        equal("Rejected operations preserve count", 1, count("statisticsSummaries"));
    }
    /** Old service/page snapshots cannot bypass removed roles. */
    @Test public void revokedRoles() throws Exception {
        var request = preview().toRequest(); var saved = save(); service.setVisibility("Curator", "" + saved.getId(), true);
        database.updateUserRole("Curator", "Viewer", "true", "Admin"); database.updateUserRole("Curator", "Curator", "false", "Admin");
        reject(service.listSources("Curator")); reject(service.preview("Curator", destination, List.of(first, second))); reject(service.save("Curator", request));
        reject(service.readSummary("Curator", "" + saved.getId())); reject(service.listSummaries("Curator", destination)); reject(service.setVisibility("Curator", "" + saved.getId(), false));
        database.updateUserRole("Owner", "Viewer", "true", "Admin"); database.updateUserRole("Owner", "Contributor", "false", "Admin"); reject(service.sharedSummaries("Owner", destination));
        equal("Revoked mutations leave count", 1, count("statisticsSummaries"));
    }
    /** A child insertion failure rolls back header and every previously inserted source. */
    @Test public void rollback() throws Exception {
        Database faulty = new Database(url) {
            private int inserts;
            @Override protected void insertStatisticsSource(long id, StatisticsSourceSnapshot source) throws SQLException {
                super.insertStatisticsSource(id, source); if (++inserts == 2) throw new SQLException("Injected second child failure");
            }
        }; faulty.connectToDatabase();
        try { reject(new StatisticsService(faulty).save("Curator", preview().toRequest())); }
        finally { faulty.closeConnection(); }
        equal("Failed child rolled back header", 0, count("statisticsSummaries")); equal("Failed child rolled back children", 0, count("statisticsSourceSnapshots"));
        var saved = save();
        Database failedVisibility = new Database(url) {
            @Override public boolean updateSummaryVisibility(long id, boolean visible) throws SQLException { super.updateSummaryVisibility(id, visible); throw new SQLException("Injected post-write failure"); }
        }; failedVisibility.connectToDatabase();
        try { reject(new StatisticsService(failedVisibility).setVisibility("Curator", "" + saved.getId(), true)); }
        finally { failedVisibility.closeConnection(); }
        assertFalse(service.readSummary("Curator", "" + saved.getId()).getValue().isVisibleToContributor());
        assertThrows(SQLException.class, () -> database.updateSummaryVisibility(saved.getId(), true));
        assertThrows(SQLException.class, () -> database.insertStatisticsSummary(saved));
    }
    /** Nested save and visibility operations must not commit an enclosing failed operation. */
    @Test public void enclosingRollback() throws Exception {
        reject(database.inTransaction(() -> {
            var saved = service.save("Curator", preview().toRequest()); assertTrue(saved.isSuccessful());
            assertTrue(service.setVisibility("Curator", "" + saved.getValue().getId(), true).isSuccessful()); return OperationResult.failure("Injected later failure");
        })); equal("Outer rollback header", 0, count("statisticsSummaries")); equal("Outer rollback children", 0, count("statisticsSourceSnapshots"));
    }
    /** Database uniqueness prevents duplicate attempt snapshots within one summary. */
    @Test public void sourceUniquenessConstraint() throws Exception {
        var preview = preview(); var duplicate = new StatisticsSummary(0, Long.parseLong(destination), java.time.Instant.now(), true, preview.getValues(), List.of(preview.getSources().getFirst(), preview.getSources().getFirst()), false);
        assertThrows(SQLException.class, () -> database.inTransaction(() -> database.insertStatisticsSummary(duplicate)));
        equal("Duplicate child rolls back header", 0, count("statisticsSummaries"));
    }
    /** A deleted destination cascades its summaries/children but leaves foreign source attempts intact. */
    @Test public void destinationDeletion() throws Exception {
        var saved = save(); assertTrue(lessons.deleteLesson(destination, "Owner").isSuccessful()); reject(service.readSummary("Curator", "" + saved.getId()));
        equal("Destination cascade header", 0, count("statisticsSummaries")); equal("Destination cascade children", 0, count("statisticsSourceSnapshots")); equal("Foreign source retained", 1, database.getExperienceCount());
    }
    /** Empty/single/large evidence collections have no fixed selection limit. */
    @Test public void collectionSizes() throws Exception {
        lessons.deleteExperience("" + first, "Owner"); lessons.deleteExperience("" + second, "Other"); equal("Empty sources", 0, service.listSources("Curator").getValue().size());
        database.inTransaction(() -> {
            for (int n = 0; n < 160; n++) database.createExperience(new ExperienceRecord(0, Long.parseLong(destination), "Owner", "What", "How", n,
                    List.of(new TeamMemberEffort("Sam", n)), false, false, false, false, false)); return true;
        });
        var sources = service.listSources("Curator").getValue(); equal("Large source list", 160, sources.size());
        var preview = service.preview("Curator", destination, sources.stream().map(ExperienceRecord::getId).toList()).getValue(); equal("Large preview counts every attempt", 160, preview.getValues().getAttemptCount());
        equal("Large mean", 79.5, preview.getValues().getMeanElapsedMinutes());
        assertTrue(service.save("Curator", preview.toRequest()).isSuccessful()); equal("Large source snapshot count", 160, count("statisticsSourceSnapshots"));
    }
    /** Capture rejects corrupt numeric/effort records and overflow before any persistence. */
    @Test public void invalidStoredSources() throws Exception {
        for (double bad : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY }) {
            try (var connection = DriverManager.getConnection(url, "sa", ""); var query = connection.prepareStatement("UPDATE experienceRecords SET elapsedMinutes=? WHERE id=?")) { query.setDouble(1, bad); query.setLong(2, second); query.executeUpdate(); }
            reject(service.preview("Curator", destination, List.of(first, second)));
        }
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) { sql.execute("UPDATE experienceRecords SET elapsedMinutes=50 WHERE id=" + second); sql.execute("DELETE FROM experienceEfforts WHERE experienceId=" + second); }
        reject(service.preview("Curator", destination, List.of(first, second)));
        for (var member : List.of(new TeamMemberEffort(" ", 10), new TeamMemberEffort("Sam", Double.NaN), new TeamMemberEffort("Sam", -1))) {
            database.updateExperience(new ExperienceRecord(second, otherLesson, "Other", "What", "How", 50, List.of(member), false, false, false, false, false));
            reject(service.preview("Curator", destination, List.of(first, second)));
        }
        database.updateExperience(new ExperienceRecord(second, otherLesson, "Other", "What", "How", 50,
                List.of(new TeamMemberEffort("A", Double.MAX_VALUE), new TeamMemberEffort("B", Double.MAX_VALUE)), false, false, false, false, false));
        reject(service.preview("Curator", destination, List.of(first, second))); equal("Invalid data writes no summaries", 0, count("statisticsSummaries"));
    }
    /** Parent locks serialize preview/save with Contributor source changes across two connections. */
    @Test public void concurrentInputs() throws Exception {
        Database other = new Database(url); other.connectToDatabase(); ExecutorService worker = Executors.newSingleThreadExecutor();
        AtomicReference<Future<OperationResult<ExperienceRecord>>> pending = new AtomicReference<>(); CountDownLatch started = new CountDownLatch(1);
        try {
            database.inTransaction(() -> {
                var request = preview().toRequest();
                pending.set(worker.submit(() -> { started.countDown(); return new LessonService(other).updateExperience("" + second, "Other", draft("90", "120")); }));
                assertTrue(assertDoesNotThrow(() -> started.await(2, TimeUnit.SECONDS))); assertThrows(TimeoutException.class, () -> pending.get().get(150, TimeUnit.MILLISECONDS));
                assertTrue(service.save("Curator", request).isSuccessful()); return true;
            }); assertTrue(pending.get().get(5, TimeUnit.SECONDS).isSuccessful());
            var saved = service.listSummaries("Curator", destination).getValue().getFirst(); equal("Concurrent change does not rewrite snapshot", 40.0, saved.getValues().getMeanElapsedMinutes()); assertTrue(saved.isStale());
        } finally { worker.shutdownNow(); other.closeConnection(); }
    }
    /** Repeatable migration preserves populated accounts, lessons, terms and effort records. */
    @Test public void migration() throws Exception {
        new CuratorLessonService(database).saveTerms("Curator", destination, List.of("rollback")); database.closeConnection();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) { sql.execute("DROP TABLE statisticsSourceSnapshots"); sql.execute("DROP TABLE statisticsSummaries"); }
        for (int n = 0; n < 2; n++) {
            database.connectToDatabase(); equal("Migration keeps accounts", "Owner", database.findUserForRoleAccess("Owner").getUserName()); equal("Migration keeps experiences", 2, database.getExperienceCount());
            equal("Migration keeps terms", "rollback", database.getSearchTerms(Long.parseLong(destination)).getFirst().getTerm()); equal("Migration creates empty summary table", 0, count("statisticsSummaries"));
            if (n == 0) database.closeConnection();
        }
    }
    /** Returned collections and reviewed requests do not retain caller-mutable collections. */
    @Test public void immutableData() {
        var preview = preview(); var ids = new ArrayList<>(List.of(first, second)); var map = new HashMap<>(preview.toRequest().getReviewedFingerprints());
        var request = new StatisticsRequest(Long.parseLong(destination), ids, map); ids.clear(); map.clear(); equal("Request IDs defensively copied", 2, request.getSourceExperienceIds().size());
        equal("Request fingerprints defensively copied", 2, request.getReviewedFingerprints().size()); assertThrows(UnsupportedOperationException.class, () -> request.getSourceExperienceIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> preview.getSources().clear()); assertThrows(UnsupportedOperationException.class, () -> save().getSources().clear());
    }
    /** Whole lesson/experience locks protect Contributor changes, not authorized Curator statistics reads. */
    @Test public void protectedSourcesRemainReviewable() throws Exception {
        new CuratorLessonService(database).setLessonLocks("Curator", destination, new LessonLockState(true, true, true, true)); database.setExperienceLocksForTesting(second, true, true, true, true, true);
        assertTrue(service.save("Curator", preview().toRequest()).isSuccessful()); equal("Protected records unchanged", 2, database.getExperienceCount());
    }
    /** Recalculation requires an explicitly reviewed new snapshot; old saved evidence remains unchanged. */
    @Test public void explicitRecalculation() {
        var original = save(); lessons.updateExperience("" + second, "Other", draft("90", "120")); var recalculated = save();
        assertNotEquals(original.getId(), recalculated.getId()); equal("New reviewed calculation", 60.0, recalculated.getValues().getMeanElapsedMinutes());
        equal("Old snapshot still historical", 40.0, service.readSummary("Curator", "" + original.getId()).getValue().getValues().getMeanElapsedMinutes());
    }
    /** Separate JVMs prove saved values, sharing, historical deletion, hiding and destination cascade. */
    @Test public void persistenceAcrossProcesses() throws Exception {
        for (String phase : List.of("create", "verify", "delete-source", "verify-history", "hide", "verify-hidden", "delete-destination", "verify-deleted")) {
            Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Duser.timezone=" + (phase.equals("create") ? "UTC" : "Pacific/Honolulu"), "-cp", System.getProperty("tp3.test.classpath"),
                    "database.TP3StatisticsPersistenceProcess", phase, directory.resolve("separate-process").toString()).redirectErrorStream(true).start();
            if (!process.waitFor(20, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Child JVM timed out"); }
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); assertEquals(0, process.exitValue(), output); assertTrue(output.contains("PASS " + phase), output);
        }
    }
    /** Storage unavailable errors return no saved/previously authorized data. */
    @Test public void unavailableStorage() {
        var request = preview().toRequest(); database.closeConnection(); reject(service.listSources("Curator")); reject(service.preview("Curator", destination, List.of(first, second)));
        reject(service.save("Curator", request)); reject(service.listSummaries("Curator", destination)); reject(service.readSummary("Curator", "1")); reject(service.setVisibility("Curator", "1", true)); reject(service.sharedSummaries("Owner", destination));
    }
    private StatisticsPreview preview() { var result = service.preview("Curator", destination, List.of(first, second)); assertTrue(result.isSuccessful(), result.getMessage()); return result.getValue(); }
    private StatisticsSummary save() { var result = service.save("Curator", preview().toRequest()); assertTrue(result.isSuccessful(), result.getMessage()); return result.getValue(); }
    private int count(String table) throws SQLException { try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement(); var rows = sql.executeQuery("SELECT COUNT(*) FROM " + table)) { rows.next(); return rows.getInt(1); } }
    private static StatisticsSourceSnapshot source(long id, double elapsed, double effort) { return new StatisticsSourceSnapshot(0, 0, 1, id, id, elapsed, effort, "test"); }
    private static ExperienceDraft draft(String elapsed, String effort) { return new ExperienceDraft("Tested persistence", "Restarted course app", elapsed, List.of(new EffortInput("Sam", effort))); }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) { return new User(name, "Course1!", "", "", "", "", "", admin, contributor, viewer, curator); }
    private static void reject(OperationResult<?> result) { equal("Rejected operation", false, result.isSuccessful()); assertNull(result.getValue()); assertFalse(result.getMessage().isBlank()); }
    private static void equal(String name, Object expected, Object actual) { System.out.println(name + " | Expected: " + expected + " | Actual: " + actual + " | " + (Objects.equals(expected, actual) ? "PASS" : "FAIL")); assertEquals(expected, actual, name); }
    private static void checkValues(StatisticsValues value, int count, double mean, double median, double min, double max, double effortMean, double effortMedian, double effortMin, double effortMax) {
        equal("Attempt count", count, value.getAttemptCount()); equal("Elapsed mean", mean, value.getMeanElapsedMinutes()); equal("Elapsed median", median, value.getMedianElapsedMinutes());
        equal("Elapsed minimum", min, value.getMinElapsedMinutes()); equal("Elapsed maximum", max, value.getMaxElapsedMinutes()); equal("Effort mean", effortMean, value.getMeanEffortMinutes());
        equal("Effort median", effortMedian, value.getMedianEffortMinutes()); equal("Effort minimum", effortMin, value.getMinEffortMinutes()); equal("Effort maximum", effortMax, value.getMaxEffortMinutes());
    }
}
