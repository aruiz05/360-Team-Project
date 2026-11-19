package database;

import entityClasses.User;
import entityClasses.LessonsLearned;
import guiTools.RoleNames;
import java.util.List;

/*******
 * <p> Title: TP3PersistenceProcess Class. </p>
 * <p> Description: Separate-process fixture for TP3FoundationsTest.rolePersistenceAcrossProcesses.
 * Its database path always belongs to a JUnit temporary directory. No production path is used.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-18 Phase 3 persistence verification
 */
public class TP3PersistenceProcess {
    /**
     * @param args requires a phase and isolated file-backed H2 path
     * @throws Exception when persistent state differs from the expected phase
     */
    public static void main(String[] args) throws Exception {
        Database database = new Database("jdbc:h2:file:" + args[1]);
        database.connectToDatabase();
        try {
            switch (args[0]) {
            case "create" -> {
                database.register(new User("Admin", "Course1!", "", "", "", "", "", true, false, false));
                database.register(new User("Member", "Course1!", "", "", "", "", "", false, false, true, true));
                database.generateInvitationCode("course@example.test", "Curator", "Admin");
                database.createLesson(new LessonsLearned(0, "Member", "Persistent lesson", "Problem", "Lesson",
                        false, false, false, false));
            }
            case "verify" -> {
                check(RoleNames.assignedRoles(database.findUserForRoleAccess("Member")).equals(List.of("Viewer", "Curator")));
                check(database.getNumberOfInvitations() == 1 && database.getLessonCount() == 1);
            }
            case "delete" -> {
                check(database.updateUserRole("Member", "Curator", "false", "Admin"));
                check(database.deleteLesson(database.getAllLessons().getFirst().getId()));
            }
            case "verify-deleted" -> {
                check(RoleNames.assignedRoles(database.findUserForRoleAccess("Member")).equals(List.of("Viewer")));
                check(database.getLessonCount() == 0);
            }
            default -> throw new IllegalArgumentException("Unknown phase");
            }
            System.out.println("PASS " + args[0]);
        } finally { database.closeConnection(); }
    }
    private static void check(boolean passed) { if (!passed) throw new AssertionError("Unexpected persistent state"); }
}
