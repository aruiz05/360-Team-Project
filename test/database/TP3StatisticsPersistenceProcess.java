package database;

import entityClasses.*;
import lessonManagement.*;
import java.util.List;

/*******
 * <p> Title: TP3StatisticsPersistenceProcess Class. </p>
 * <p> Description: Eight independent Java-process phases verify snapshot creation, shared
 * values, source deletion history, hiding, and destination deletion. Invoked only with the
 * temporary file path supplied by TP3StatisticsTest.persistenceAcrossProcesses.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Statistics persistence verification
 */
public class TP3StatisticsPersistenceProcess {
    /**
     * @param args phase and isolated H2 file path
     * @throws Exception if saved state differs from the expected phase
     */
    public static void main(String[] args) throws Exception {
        var database = new Database("jdbc:h2:file:" + args[1]); database.connectToDatabase();
        var service = new StatisticsService(database); var lessons = new LessonService(database);
        try {
            switch (args[0]) {
            case "create" -> {
                database.register(new User("Owner", "Course1!", "", "", "", "", "", false, true, false));
                database.register(new User("Other", "Course1!", "", "", "", "", "", false, true, false));
                database.register(new User("Curator", "Course1!", "", "", "", "", "", false, false, false, true));
                var destination = lessons.createLesson("Owner", new LessonDraft("Destination", "Problem", "Lesson")).getValue();
                var foreign = lessons.createLesson("Other", new LessonDraft("Source", "Problem", "Lesson")).getValue();
                long first = lessons.createExperience("" + destination.getId(), "Owner", draft("30", "40")).getValue().getId();
                long second = lessons.createExperience("" + foreign.getId(), "Other", draft("50", "80")).getValue().getId();
                var preview = service.preview("Curator", "" + destination.getId(), List.of(first, second)); check(preview.isSuccessful());
                var saved = service.save("Curator", preview.getValue().toRequest()); check(saved.isSuccessful() && !saved.getValue().isVisibleToContributor());
                check(service.setVisibility("Curator", "" + saved.getValue().getId(), true).isSuccessful());
            }
            case "verify" -> {
                var saved = service.listSummaries("Curator", "1").getValue().getFirst(); check(!saved.isStale());
                check(saved.getValues().getMeanElapsedMinutes() == 40 && saved.getValues().getMeanEffortMinutes() == 60);
                check(saved.getSources().size() == 2 && saved.getCreatedAt() != null);
                check(saved.getCreatedAt().isAfter(java.time.Instant.now().minusSeconds(180)) && saved.getCreatedAt().isBefore(java.time.Instant.now().plusSeconds(5)));
                check(service.sharedSummaries("Owner", "1").getValue().getFirst().getValues().getAttemptCount() == 2);
                check(!service.sharedSummaries("Other", "1").isSuccessful());
            }
            case "delete-source" -> check(lessons.deleteLesson("2", "Other").isSuccessful());
            case "verify-history" -> {
                var saved = service.listSummaries("Curator", "1").getValue().getFirst(); check(saved.isStale());
                check(saved.getValues().getMeanElapsedMinutes() == 40 && saved.getSources().size() == 2);
                check(saved.getSources().stream().anyMatch(source -> source.getOriginalLessonId() == 2 && source.getLiveExperienceId() == null));
                check(service.sharedSummaries("Owner", "1").getValue().size() == 1);
            }
            case "hide" -> check(service.setVisibility("Curator", "" + service.listSummaries("Curator", "1").getValue().getFirst().getId(), false).isSuccessful());
            case "verify-hidden" -> { check(service.sharedSummaries("Owner", "1").getValue().isEmpty()); check(!service.listSummaries("Curator", "1").getValue().getFirst().isVisibleToContributor()); }
            case "delete-destination" -> check(lessons.deleteLesson("1", "Owner").isSuccessful());
            case "verify-deleted" -> {
                check(database.getLessonCount() == 0); check(database.getExperienceCount() == 0);
                try (var connection = java.sql.DriverManager.getConnection("jdbc:h2:file:" + args[1], "sa", ""); var sql = connection.createStatement()) {
                    for (String table : List.of("statisticsSummaries", "statisticsSourceSnapshots")) try (var rows = sql.executeQuery("SELECT COUNT(*) FROM " + table)) { check(rows.next() && rows.getInt(1) == 0); }
                }
            }
            default -> throw new IllegalArgumentException("Unknown phase");
            }
            System.out.println("PASS " + args[0]);
        } finally { database.closeConnection(); }
    }
    private static ExperienceDraft draft(String elapsed, String effort) { return new ExperienceDraft("What", "How", elapsed, List.of(new EffortInput("Sam", effort))); }
    private static void check(boolean success) { if (!success) throw new AssertionError("Unexpected persistent statistics state"); }
}
