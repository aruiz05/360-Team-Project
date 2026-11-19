package database;

import entityClasses.*;
import lessonManagement.*;
import java.util.List;

/*******
 * <p> Title: TP3DiscoveryPersistenceProcess Class. </p>
 * <p> Description: Separate-JVM creation, verification, deletion and deletion verification
 * fixture for TP3DiscoveryTest.persistenceAcrossProcesses. Uses only a supplied temporary path.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 persistence verification
 */
public class TP3DiscoveryPersistenceProcess {
    /**
     * @param args phase and isolated file-backed database path
     * @throws Exception on unexpected persisted state
     */
    public static void main(String[] args) throws Exception {
        Database database = new Database("jdbc:h2:file:" + args[1]); database.connectToDatabase();
        try {
            var curator = new CuratorLessonService(database); var viewer = new ViewerLessonService(database);
            switch (args[0]) {
            case "create" -> {
                database.register(new User("Owner", "Course1!", "", "", "", "", "", false, true, false));
                database.register(new User("Reviewer", "Course1!", "", "", "", "", "", false, false, true, true));
                var contributor = new LessonService(database);
                String id = "" + contributor.createLesson("Owner", new LessonDraft("Original", "Problem", "Lesson")).getValue().getId();
                check(contributor.createExperience(id, "Owner", new ExperienceDraft("What", "How", "12", List.of(new EffortInput("Sam", "10")))).isSuccessful());
                check(curator.refineLesson("Reviewer", id, new LessonDraft("Reviewed", "Clear problem", "Reusable lesson")).isSuccessful());
                check(curator.saveTerms("Reviewer", id, List.of("Related Term")).isSuccessful());
                check(curator.setLessonLocks("Reviewer", id, new LessonLockState(true, true, false, true)).isSuccessful());
            }
            case "verify" -> {
                var cores = viewer.searchLessons("Reviewer", "related").getValue(); check(cores.size() == 1);
                String id = "" + cores.getFirst().getId(); check(cores.getFirst().getTitle().equals("Reviewed"));
                var lesson = curator.readLesson("Reviewer", id).getValue(); check(lesson.getOwner().equals("Owner"));
                check(lesson.isLessonLocked() && lesson.isTitleLocked() && !lesson.isProblemSituationLocked() && lesson.isLessonLearnedLocked());
                check(curator.readExperiences("Reviewer", id).getValue().getFirst().getTeamEfforts().getFirst().getEffortMinutes() == 10);
                check(curator.readTerms("Reviewer", id).getValue().getFirst().getTerm().equals("related term"));
            }
            case "delete" -> {
                String id = "" + viewer.listLessons("Reviewer").getValue().getFirst().getId();
                check(curator.setLessonLocks("Reviewer", id, new LessonLockState(false, false, false, false)).isSuccessful());
                check(new LessonService(database).deleteLesson(id, "Owner").isSuccessful());
            }
            case "verify-deleted" -> {
                check(viewer.listLessons("Reviewer").getValue().isEmpty()); check(database.getExperienceCount() == 0);
                try (var connection = java.sql.DriverManager.getConnection("jdbc:h2:file:" + args[1], "sa", "");
                        var sql = connection.createStatement(); var rows = sql.executeQuery("SELECT COUNT(*) FROM lessonSearchTerms")) { check(rows.next() && rows.getInt(1) == 0); }
            }
            default -> throw new IllegalArgumentException("Unknown phase");
            }
            System.out.println("PASS " + args[0]);
        } finally { database.closeConnection(); }
    }
    private static void check(boolean success) { if (!success) throw new AssertionError("Unexpected persisted state"); }
}
