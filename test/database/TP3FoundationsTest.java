package database;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import entityClasses.*;
import guiTools.RoleNames;
import lessonManagement.*;
import roleManagement.RoleAccessService;

/*******
 * <p> Title: TP3FoundationsTest Class. </p>
 * <p> Description: JUnit coverage for assigned-role entry, backward-compatible role persistence,
 * non-mutating authorization, migration, protected saves, nested transactions, and rollback.
 * Every database uses a temporary file. Expected and actual assertion values are reported by
 * JUnit; successful scenario results and counts are printed by the supplied runner.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-18 Phase 3 foundation tests
 */
public class TP3FoundationsTest {
    @TempDir Path directory;
    private Database database;
    private String url;
    private LessonService lessons;
    private RoleAccessService access;

    /** @throws Exception when an isolated fixture cannot be established */
    @BeforeEach public void setUp() throws Exception {
        url = "jdbc:h2:file:" + directory.resolve("foundations").toAbsolutePath();
        database = new Database(url);
        database.connectToDatabase();
        database.register(user("Admin", true, false, false, false));
        database.register(user("Owner", false, true, false, false));
        database.register(user("Other", false, true, false, false));
        database.register(user("Viewer", false, false, true, false));
        database.register(user("Curator", false, false, false, true));
        lessons = new LessonService(database);
        access = new RoleAccessService(database);
    }

    /** Close the test connection; TempDir owns only this test's files. */
    @AfterEach public void tearDown() { database.closeConnection(); }

    /** Validate legacy aliases, new assignments, and the complete role count. */
    @Test public void roleNamesAndAccounts() {
        User account = user("All", true, true, true, true);
        assertEquals(4, account.getNumRoles());
        assertEquals(4, database.getNumberOfRoles(account));
        assertEquals(List.of("Admin", "Contributor", "Viewer", "Curator"), RoleNames.assignedRoles(account));
        assertEquals("Role1", RoleNames.toStoredRole("Contributor"));
        assertEquals("Role2", RoleNames.toStoredRole("Viewer"));
        assertEquals("Viewer", RoleNames.toDisplayRole("Role2"));
        assertFalse(RoleNames.isSupported(null));
        assertFalse(RoleNames.isSupported("<Select a role>"));
        account.setCuratorRole(false);
        assertEquals(3, account.getNumRoles());
    }

    /** An unavailable connection must fail closed with a helpful result, not a null dereference. */
    @Test public void unavailableDatabaseFailsClosed() throws Exception {
        Database disconnected = new Database("jdbc:h2:file:" + directory.resolve("unused"));
        assertFalse(new RoleAccessService(disconnected).requireRole("Owner", "Contributor").isSuccessful());
        assertFalse(new LessonService(disconnected).createLesson("Owner", draft("Rejected")).isSuccessful());
        assertThrows(SQLException.class, () -> disconnected.inTransaction(() -> true));
        database.closeConnection();
        assertFalse(access.rolesFor("Owner").isSuccessful());
    }

    /** Old screen flags must not authorize an unassigned or removed role. */
    @Test public void dispatchUsesPersistedRoles() throws Exception {
        assertEquals("Viewer", access.singleRole("Viewer").getValue());
        assertTrue(access.requireRole("Viewer", "Role2").isSuccessful());
        assertFalse(access.requireRole("Viewer", "Curator").isSuccessful());
        assertTrue(database.updateUserRole("Viewer", "Curator", "true", "Admin"));
        assertFalse(access.singleRole("Viewer").isSuccessful());
        assertEquals(List.of("Viewer", "Curator"), access.rolesFor("Viewer").getValue());
        assertTrue(database.updateUserRole("Viewer", "Viewer", "false", "Admin"));
        assertFalse(access.requireRole("Viewer", "Viewer").isSuccessful());
        assertEquals("Curator", access.singleRole("Viewer").getValue());
        assertFalse(access.rolesFor("Missing").isSuccessful());
        assertFalse(access.rolesFor(" ").isSuccessful());
        database.register(user("NoRole", false, false, false, false));
        assertFalse(access.singleRole("NoRole").isSuccessful());
    }

    /** Inspecting another account must not replace the signed-in account cache. */
    @Test public void roleLookupDoesNotChangeCurrentAccount() throws Exception {
        assertTrue(database.getUserAccountDetails("Admin"));
        assertEquals("", database.findUserForRoleAccess("Curator").getPassword());
        assertTrue(access.requireRole("Curator", "Curator").isSuccessful());
        assertEquals("Admin", database.getCurrentUsername());
        assertTrue(database.getCurrentAdminRole());
        assertFalse(database.getCurrentCuratorRole());
        assertTrue(database.updateUserRole("Owner", "Curator", "true", "Admin"));
        assertEquals("Admin", database.getCurrentUsername());
        assertFalse(database.getCurrentCuratorRole());
        assertTrue(database.getUserAccountDetails("Owner"));
        assertTrue(database.getCurrentCuratorRole());
    }

    /** Verify direct business calls cannot bypass Admin, self-removal, or final-role checks. */
    @Test public void administratorRoleChangesAreAuthorized() throws Exception {
        int accounts = database.getNumberOfUsers();
        assertFalse(database.updateUserRole("Owner", "Curator", "true", "Viewer"));
        assertFalse(database.updateUserRole("Owner", "Curator", "true", "Missing"));
        assertFalse(database.updateUserRole("Admin", "Admin", "false", "Admin"));
        assertFalse(database.updateUserRole("Curator", "Curator", "false", "Admin"));
        assertFalse(database.updateUserRole("Owner", "bad", "true", "Admin"));
        assertFalse(database.updateUserRole("Owner", "Viewer", "yes", "Admin"));
        assertFalse(database.updateUserRole("Missing", "Viewer", "true", "Admin"));
        assertFalse(database.updateUserRole(null, "Viewer", "true", "Admin"));
        assertFalse(database.updateUserRole("Owner", null, "true", "Admin"));
        assertTrue(database.updateUserRole("Owner", "Viewer", "true", "Admin"));
        assertTrue(database.updateUserRole("Owner", "Role1", "false", "Admin"));
        assertEquals(List.of("Viewer"), access.rolesFor("Owner").getValue());
        assertEquals(accounts, database.getNumberOfUsers());
    }

    /** New and legacy invitation values survive storage and invalid requests add nothing. */
    @Test public void invitationsAcceptNewAndLegacyRoles() {
        for (String role : List.of("Admin", "Contributor", "Role1", "Viewer", "Role2", "Curator")) {
            String code = database.generateInvitationCode("course@example.test", role, "Admin");
            assertNotNull(code);
            assertEquals(RoleNames.toStoredRole(role), database.getRoleGivenAnInvitationCode(code));
        }
        int count = database.getNumberOfInvitations();
        assertNull(database.generateInvitationCode("course@example.test", "Curator", "Viewer"));
        assertNull(database.generateInvitationCode(" ", "Viewer", "Admin"));
        assertNull(database.generateInvitationCode("course@example.test", "Unknown", "Admin"));
        assertEquals(count, database.getNumberOfInvitations());
    }

    /** A forced password change must finish before any assigned role can be entered. */
    @Test public void passwordResetBlocksRoleAccess() {
        assertTrue(database.setOneTimePassword("Curator", "Temporary1!"));
        assertFalse(access.requireRole("Curator", "Curator").isSuccessful());
        assertTrue(database.consumeOneTimePassword("Curator", "Temporary1!"));
        assertFalse(access.requireRole("Curator", "Curator").isSuccessful());
        assertFalse(database.updateUserRole("Owner", "Curator", "true", "Curator"));
        assertTrue(database.completePasswordReset("Curator", "Changed1!"));
        assertTrue(access.requireRole("Curator", "Curator").isSuccessful());
    }

    /** Assigned users retain all four roles through listing and a fresh Database instance. */
    @Test public void rolesSurviveReopenAndAccountListing() throws Exception {
        database.register(user("All", true, true, true, true));
        database.closeConnection();
        database = new Database(url);
        database.connectToDatabase();
        User all = database.getAllUsers().stream().filter(u -> u.getUserName().equals("All")).findFirst().orElseThrow();
        assertEquals(4, all.getNumRoles());
        assertEquals("", all.getPassword());
        assertEquals(List.of("Admin", "Contributor", "Viewer", "Curator"),
                new RoleAccessService(database).rolesFor("All").getValue());
    }

    /** Upgrade the old schema without changing accounts, OTP fields, invitations, or lessons. */
    @Test public void legacySchemaMigrationPreservesData() throws Exception {
        String oldUrl = "jdbc:h2:file:" + directory.resolve("legacy").toAbsolutePath();
        try (Connection old = DriverManager.getConnection(oldUrl, "sa", ""); Statement sql = old.createStatement()) {
            sql.execute("CREATE TABLE userDB (id INT AUTO_INCREMENT PRIMARY KEY, userName VARCHAR(255) UNIQUE, "
                    + "password VARCHAR(255), firstName VARCHAR(255), middleName VARCHAR(255), lastName VARCHAR(255), "
                    + "preferredFirstName VARCHAR(255), emailAddress VARCHAR(255), adminRole BOOL DEFAULT FALSE, "
                    + "newRole1 BOOL DEFAULT FALSE, newRole2 BOOL DEFAULT FALSE)");
            sql.execute("INSERT INTO userDB(userName,password,newRole1,newRole2) VALUES('Legacy','Original1!',TRUE,TRUE)");
            sql.execute("CREATE TABLE InvitationCodes(code VARCHAR(10) PRIMARY KEY,emailAddress VARCHAR(255),role VARCHAR(10))");
            sql.execute("INSERT INTO InvitationCodes VALUES('legacy','old@example.test','Role2')");
        }
        Database upgraded = new Database(oldUrl);
        try {
            upgraded.connectToDatabase();
            User legacy = upgraded.findUserForRoleAccess("Legacy");
            assertEquals(List.of("Contributor", "Viewer"), RoleNames.assignedRoles(legacy));
            assertFalse(legacy.getCuratorRole());
            assertEquals("Role2", upgraded.getRoleGivenAnInvitationCode("legacy"));
            assertTrue(upgraded.getUserAccountDetails("Legacy"));
            assertEquals("Original1!", upgraded.getCurrentPassword());
            long id = upgraded.createLesson(core("Legacy", "Retained lesson"));
            upgraded.closeConnection();
            upgraded.connectToDatabase();
            assertEquals("Retained lesson", upgraded.getLesson(id).getTitle());
            assertFalse(upgraded.findUserForRoleAccess("Legacy").getCuratorRole());
        } finally { upgraded.closeConnection(); }
    }

    /** A username or a cached Contributor flag alone must not authorize lesson operations. */
    @Test public void contributorOperationsRequireCurrentRole() throws Exception {
        assertFalse(lessons.createLesson("Viewer", draft("Rejected")).isSuccessful());
        assertFalse(lessons.createLesson("Missing", draft("Rejected")).isSuccessful());
        assertFalse(lessons.createLesson(" ", draft("Rejected")).isSuccessful());
        assertEquals(0, database.getLessonCount());
        LessonsLearned lesson = lessons.createLesson("Owner", draft("Owned lesson")).getValue();
        assertTrue(database.updateUserRole("Owner", "Viewer", "true", "Admin"));
        assertTrue(database.updateUserRole("Owner", "Contributor", "false", "Admin"));
        assertFalse(lessons.listLessons("Owner").isSuccessful());
        assertFalse(lessons.filterLessons("Owner", "Owned").isSuccessful());
        assertFalse(lessons.readLesson("" + lesson.getId(), "Owner").isSuccessful());
        assertFalse(lessons.updateLesson("" + lesson.getId(), "Owner", draft("Rejected")).isSuccessful());
        assertFalse(lessons.deleteLesson("" + lesson.getId(), "Owner").isSuccessful());
        assertEquals("Owned lesson", database.getLesson(lesson.getId()).getTitle());
    }

    /** Revocation also blocks all experience entry points and leaves the related data intact. */
    @Test public void revokedRoleCannotUseExperienceOperations() throws Exception {
        LessonsLearned lesson = lessons.createLesson("Owner", draft("Owned")).getValue();
        ExperienceRecord record = lessons.createExperience("" + lesson.getId(), "Owner", experience("12")).getValue();
        assertTrue(database.updateUserRole("Owner", "Viewer", "true", "Admin"));
        assertTrue(database.updateUserRole("Owner", "Contributor", "false", "Admin"));
        assertFalse(lessons.createExperience("" + lesson.getId(), "Owner", experience("14")).isSuccessful());
        assertFalse(lessons.readExperience("" + record.getId(), "Owner").isSuccessful());
        assertFalse(lessons.listExperiences("" + lesson.getId(), "Owner").isSuccessful());
        assertFalse(lessons.updateExperience("" + record.getId(), "Owner", experience("15")).isSuccessful());
        assertFalse(lessons.deleteExperience("" + record.getId(), "Owner").isSuccessful());
        assertEquals(1, database.getExperienceCount());
        assertEquals(12, database.getExperience(record.getId()).getElapsedMinutes());
    }

    /** Successful nested experience work must not commit an outer operation that later fails. */
    @Test public void nestedTransactionsDoNotCommitOuterWork() throws Exception {
        long lessonId = database.createLesson(core("Owner", "Original"));
        assertThrows(SQLException.class, () -> database.inTransaction(() -> {
            database.createExperience(record(lessonId, "Owner", List.of(new TeamMemberEffort("Sam", 10))));
            database.createLesson(core("Owner", "Uncommitted"));
            throw new SQLException("Injected later failure");
        }));
        assertEquals(0, database.getExperienceCount());
        assertEquals(1, database.getLessonCount());
        assertNotNull(database.getLesson(lessonId));
    }

    /** A caught nested SQL failure rolls back only that savepoint, preserving earlier outer work. */
    @Test public void nestedFailureCanBeRecoveredWithoutPartialRows() throws Exception {
        long parent = database.createLesson(core("Owner", "Parent"));
        database.inTransaction(() -> {
            database.createLesson(core("Owner", "Outer retained"));
            assertThrows(SQLException.class, () -> database.createExperience(record(parent, "Owner",
                    List.of(new TeamMemberEffort("Sam", 2), new TeamMemberEffort("x".repeat(256), 3)))));
            assertEquals(0, database.getExperienceCount());
            return true;
        });
        assertEquals(2, database.getLessonCount());
        assertEquals(0, database.getExperienceCount());
    }

    /** Failed nested replacement restores the original experience and ordered effort entries. */
    @Test public void nestedUpdateFailureRestoresExistingEfforts() throws Exception {
        long parent = database.createLesson(core("Owner", "Parent"));
        long id = database.createExperience(record(parent, "Owner", List.of(new TeamMemberEffort("Sam", 10))));
        database.inTransaction(() -> {
            assertThrows(SQLException.class, () -> database.updateExperience(new ExperienceRecord(id,
                    parent, "Owner", "Changed", "Changed", 50,
                    List.of(new TeamMemberEffort("x".repeat(256), 20)), false, false, false, false, false)));
            return true;
        });
        ExperienceRecord saved = database.getExperience(id);
        assertEquals("Tested persistence", saved.getWhatWasDone());
        assertEquals("Sam", saved.getTeamEfforts().getFirst().getMemberName());
        assertEquals(10, saved.getTeamEfforts().getFirst().getEffortMinutes());
    }

    /** Rejected business results must roll back even when a prior nested write succeeded. */
    @Test public void rejectedResultRollsBackEarlierChanges() throws Exception {
        OperationResult<Boolean> result = database.inTransaction(() -> {
            database.createLesson(core("Owner", "Uncommitted"));
            return OperationResult.failure("Injected validation rejection");
        });
        assertFalse(result.isSuccessful());
        assertEquals(0, database.getLessonCount());
        assertTrue(lessons.createLesson("Owner", draft("Next operation")).isSuccessful());
    }

    /** A rejected transaction restores both saved roles and legacy cached account details. */
    @Test public void rejectedRoleTransactionRestoresCachedAccount() throws Exception {
        database.getUserAccountDetails("Owner");
        OperationResult<Boolean> result = database.inTransaction(() -> {
            assertTrue(database.updateUserRole("Owner", "Viewer", "true", "Admin"));
            assertTrue(database.getCurrentNewRole2());
            return OperationResult.failure("Injected later rejection");
        });
        assertFalse(result.isSuccessful());
        assertEquals("Owner", database.getCurrentUsername());
        assertFalse(database.getCurrentNewRole2());
        assertFalse(database.findUserForRoleAccess("Owner").getNewRole2());
        assertEquals("Course1!", database.getCurrentPassword());
    }

    /** Unchecked failures restore transaction state so the next operation can commit normally. */
    @Test public void runtimeFailureRollsBackAndRestoresConnection() throws Exception {
        assertThrows(IllegalStateException.class, () -> database.inTransaction(() -> {
            database.createLesson(core("Owner", "Uncommitted"));
            throw new IllegalStateException("Injected unchecked failure");
        }));
        assertEquals(0, database.getLessonCount());
        assertTrue(lessons.createLesson("Owner", draft("Saved after failure")).isSuccessful());
    }

    /** Current parent locks defeat a Contributor's older in-memory record and protect cascades. */
    @Test public void latestLocksAndOwnershipStillProtectData() throws Exception {
        LessonsLearned lesson = lessons.createLesson("Owner", draft("Original")).getValue();
        assertTrue(database.setLessonLocksForTesting(lesson.getId(), true, false, false, false));
        assertFalse(lessons.updateLesson("" + lesson.getId(), "Owner", draft("Changed")).isSuccessful());
        assertFalse(lessons.deleteLesson("" + lesson.getId(), "Owner").isSuccessful());
        assertFalse(lessons.createExperience("" + lesson.getId(), "Owner", experience("5")).isSuccessful());
        assertFalse(lessons.updateLesson("" + lesson.getId(), "Other", draft("Foreign")).isSuccessful());
        assertEquals("Original", database.getLesson(lesson.getId()).getTitle());
        assertEquals(0, database.getExperienceCount());
    }

    /** Two connections must serialize protected changes at the parent row boundary. */
    @Test public void parentLockSerializesProtectedChanges() throws Exception {
        long id = database.createLesson(core("Owner", "Original"));
        assertThrows(SQLException.class, () -> database.lockLesson(id));
        Database other = new Database(url);
        other.connectToDatabase();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        try {
            database.inTransaction(() -> {
                assertTrue(database.lockLesson(id));
                Future<Boolean> waiting = executor.submit(() -> other.inTransaction(() -> {
                    started.countDown();
                    return other.lockLesson(id);
                }));
                assertTrue(assertDoesNotThrow(() -> started.await(2, TimeUnit.SECONDS)));
                assertThrows(TimeoutException.class, () -> waiting.get(150, TimeUnit.MILLISECONDS));
                return true;
            });
            executor.shutdown();
            assertTrue(executor.awaitTermination(4, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            other.closeConnection();
        }
    }

    /** Independent JVMs verify creation, role removal, invitation data, and deletion persistence. */
    @Test public void rolePersistenceAcrossProcesses() throws Exception {
        for (String phase : List.of("create", "verify", "delete", "verify-deleted")) {
            Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("tp3.test.classpath"), "database.TP3PersistenceProcess",
                    phase, directory.resolve("process-db").toString()).redirectErrorStream(true).start();
            String output = new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(child.waitFor(20, TimeUnit.SECONDS), "Child process must finish");
            assertEquals(0, child.exitValue(), output);
            assertTrue(output.contains("PASS " + phase), output);
        }
    }

    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) {
        return new User(name, "Course1!", "Course", "", "Project", "Course", "course@example.test",
                admin, contributor, viewer, curator);
    }
    private static LessonDraft draft(String title) { return new LessonDraft(title, "Course problem", "Review the fix"); }
    private static LessonsLearned core(String owner, String title) {
        return new LessonsLearned(0, owner, title, "Course problem", "Review the fix", false, false, false, false);
    }
    private static ExperienceDraft experience(String elapsed) {
        return new ExperienceDraft("Tested persistence", "Restarted app", elapsed, List.of(new EffortInput("Sam", "10")));
    }
    private static ExperienceRecord record(long parent, String owner, List<TeamMemberEffort> efforts) {
        return new ExperienceRecord(0, parent, owner, "Tested persistence", "Restarted app", 10, efforts,
                false, false, false, false, false);
    }
}
