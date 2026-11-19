package guiTools;

import applicationMain.FoundationsMain;
import entityClasses.*;
import lessonManagement.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

/*******
 * <p> Title: TP3StatisticsGuiTestingAutomation Class. </p>
 * <p> Description: Real JavaFX controls and isolated H2 verify statistics selection, preview,
 * explicit save, stale rejection, source review, sharing/hiding, and Contributor privacy.
 * All data is invented. Prints expected/actual results, pass/fail and totals.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Statistics GUI integration
 */
public class TP3StatisticsGuiTestingAutomation {
    private static final String CURATOR = "guiCuratorStatistics.ViewCuratorStatistics", SHARED = "guiContributorStatistics.ViewContributorStatistics";
    private static int passed, failed;
    /**
     * @param args unused
     * @throws Exception on startup or integration failure
     */
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("tp3.isolated.gui.test")) throw new IllegalStateException("Use the isolated runner.");
        CountDownLatch done = new CountDownLatch(1); AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.startup(() -> {
            try { runStatistics(); } catch (Throwable failure) { error.set(failure); }
            finally { FoundationsMain.database.closeConnection(); done.countDown(); }
        });
        if (!done.await(45, TimeUnit.SECONDS)) throw new AssertionError("Statistics GUI timed out");
        Platform.exit(); if (error.get() != null) throw new AssertionError("Statistics GUI failed", error.get());
        System.out.println("Statistics GUI checks passed: " + passed + "; failed: " + failed);
        if (failed != 0) throw new AssertionError("Statistics GUI failure");
    }
    private static void runStatistics() throws Exception {
        Platform.setImplicitExit(false); Stage stage = new Stage(); new FoundationsMain().start(stage);
        var database = FoundationsMain.database;
        User admin = user("Admin", true, false, false, false), owner = user("Owner", false, true, false, false),
                other = user("Other", false, true, false, false), curator = user("Curator", false, false, false, true);
        for (var user : List.of(admin, owner, other, curator)) database.register(user);
        var lessons = new LessonService(database); var service = new StatisticsService(database);
        var destination = lessons.createLesson("Owner", new LessonDraft("Persistence", "Course problem", "Transactions")).getValue();
        var foreign = lessons.createLesson("Other", new LessonDraft("Validation", "Course input", "Validate fields")).getValue();
        long first = lessons.createExperience("" + destination.getId(), "Owner", draft("30", "40")).getValue().getId();
        long second = lessons.createExperience("" + foreign.getId(), "Other", draft("50", "80")).getValue().getId();
        guiCuratorLessons.ViewCuratorLessons.displayCuratorLessons(stage, curator);
        button(stage, "Statistics and Visibility").fire();
        check("Destination selection required", true, stage.getTitle().contains("Curator Lessons"));
        field("guiCuratorLessons.ViewCuratorLessons", "lessons", ListView.class).getSelectionModel().select(0); button(stage, "Statistics and Visibility").fire();
        check("Curator opens statistics", true, stage.getTitle().contains("Curator Statistics"));
        check("Cross-lesson source collection", 2, rows("sources").getItems().size());
        check("Save disabled before review", true, button(stage, "Save Reviewed Snapshot").isDisabled());
        button(stage, "Preview Statistics").fire(); check("Too few attempts explained", true, field(CURATOR, "status", Label.class).getText().contains("two distinct"));
        rows("sources").getSelectionModel().selectIndices(0, 1); button(stage, "Preview Statistics").fire();
        check("Preview shows correct time and effort", true, area(CURATOR, "previewText").getText().contains("mean=40.0") && area(CURATOR, "previewText").getText().contains("mean=60.0"));
        check("Preview labels units and evidence", true, area(CURATOR, "previewText").getText().contains("person-minutes") && area(CURATOR, "previewText").getText().contains("experience " + second));
        check("Preview writes no saved rows", 0, service.listSummaries("Curator", "" + destination.getId()).getValue().size());
        rows("sources").getSelectionModel().clearSelection(0); check("Selection change invalidates review", true, button(stage, "Save Reviewed Snapshot").isDisabled());
        rows("sources").getSelectionModel().select(0); button(stage, "Preview Statistics").fire(); button(stage, "Save Reviewed Snapshot").fire();
        check("Reviewed save creates snapshot", 1, rows("summaries").getItems().size());
        check("Saved snapshot starts hidden", true, area(CURATOR, "savedText").getText().contains("Hidden"));
        check("Saved review shows historical source IDs", true, area(CURATOR, "savedText").getText().contains("Original lesson " + foreign.getId()));
        check("Contributor initially receives no hidden summary", 0, service.sharedSummaries("Owner", "" + destination.getId()).getValue().size());
        button(stage, "Share Summary").fire(); check("GUI share persists", 1, service.sharedSummaries("Owner", "" + destination.getId()).getValue().size());
        guiContributorLessonDetails.ViewContributorLessonDetails.displayLessonDetails(stage, owner, destination); button(stage, "Shared Statistics").fire();
        check("Contributor details opens shared view", true, stage.getTitle().contains("Shared Statistics"));
        check("Contributor aggregates read-only", false, area(SHARED, "values").isEditable());
        check("Contributor sees shared aggregate units", true, area(SHARED, "values").getText().contains("Elapsed minutes") && area(SHARED, "values").getText().contains("mean=40.0"));
        check("Contributor receives no source evidence", false, area(SHARED, "values").getText().contains("Original lesson"));
        check("Contributor receives no foreign raw member data", false, area(SHARED, "values").getText().contains("Sam"));
        button(stage, "Return to Lesson Details").fire(); check("Contributor return navigation", true, stage.getTitle().contains("Lesson Details"));
        guiCuratorStatistics.ViewCuratorStatistics.displayStatistics(stage, curator, destination.getId()); rows("summaries").getSelectionModel().select(0); button(stage, "Hide Summary").fire();
        check("GUI hide persists", 0, service.sharedSummaries("Owner", "" + destination.getId()).getValue().size());
        guiContributorStatistics.ViewContributorStatistics.displayStatistics(stage, owner, destination);
        check("Hidden summary absent from Contributor screen", true, area(SHARED, "values").getText().contains("No statistics have been shared"));
        guiContributorStatistics.ViewContributorStatistics.displayStatistics(stage, other, destination);
        check("Foreign Contributor receives no aggregates", "", area(SHARED, "values").getText());
        check("Foreign Contributor receives correction", true, field(SHARED, "status", Label.class).getText().contains("own lessons"));
        guiCuratorStatistics.ViewCuratorStatistics.displayStatistics(stage, curator, destination.getId()); rows("sources").getSelectionModel().selectIndices(0, 1); button(stage, "Preview Statistics").fire();
        lessons.updateExperience("" + second, "Other", draft("60", "90")); button(stage, "Save Reviewed Snapshot").fire();
        check("Stale GUI preview cannot save", 1, service.listSummaries("Curator", "" + destination.getId()).getValue().size());
        check("Stale preview requests renewed review", true, field(CURATOR, "status", Label.class).getText().contains("changed since preview"));
        check("Stale save invalidates old preview", true, button(stage, "Save Reviewed Snapshot").isDisabled());
        button(stage, "Preview Statistics").fire(); check("Renewed preview uses current inputs", true, area(CURATOR, "previewText").getText().contains("mean=45.0"));
        button(stage, "Save Reviewed Snapshot").fire(); check("Explicit recalculation creates a new snapshot", 2, rows("summaries").getItems().size());
        rows("summaries").getSelectionModel().select(0); check("Old snapshot reports earlier source data", true, area(CURATOR, "savedText").getText().contains("Earlier source data"));
        check("Old snapshot retains its original mean", true, area(CURATOR, "savedText").getText().contains("mean=40.0"));
        rows("summaries").getSelectionModel().select(1); button(stage, "Share Summary").fire();
        database.updateUserRole("Curator", "Viewer", "true", "Admin"); database.updateUserRole("Curator", "Curator", "false", "Admin"); button(stage, "Hide Summary").fire();
        check("Revoked Curator cannot change sharing", 1, service.sharedSummaries("Owner", "" + destination.getId()).getValue().size());
        button(stage, "Refresh Summaries").fire(); check("Revoked Curator refresh clears historical evidence", "", area(CURATOR, "savedText").getText());
        check("Revoked Curator refresh clears saved list", 0, rows("summaries").getItems().size());
        button(stage, "Refresh Sources").fire(); check("Revoked Curator source refresh clears list", 0, rows("sources").getItems().size());
        guiContributorStatistics.ViewContributorStatistics.displayStatistics(stage, owner, destination);
        database.updateUserRole("Owner", "Viewer", "true", "Admin"); database.updateUserRole("Owner", "Contributor", "false", "Admin"); button(stage, "Refresh Shared Statistics").fire();
        check("Revoked Contributor refresh clears aggregate values", "", area(SHARED, "values").getText()); stage.close();
    }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) { return new User(name, "Course1!", "", "", "", "", "", admin, contributor, viewer, curator); }
    private static ExperienceDraft draft(String elapsed, String effort) { return new ExperienceDraft("What", "How", elapsed, List.of(new EffortInput("Sam", effort))); }
    private static <T> T field(String type, String name, Class<T> expected) throws Exception { var field = Class.forName(type).getDeclaredField(name); field.setAccessible(true); return expected.cast(field.get(null)); }
    private static ListView<?> rows(String name) throws Exception { return field(CURATOR, name, ListView.class); }
    private static TextArea area(String type, String name) throws Exception { return field(type, name, TextArea.class); }
    private static void check(String name, Object expected, Object actual) { boolean success = Objects.equals(expected, actual); System.out.println(name + " | Expected: " + expected + " | Actual: " + actual + " | " + (success ? "PASS" : "FAIL")); if (success) passed++; else failed++; }
    private static Button button(Stage stage, String text) { var button = find(stage.getScene().getRoot(), text); if (button == null) throw new AssertionError("Missing control: " + text); return button; }
    private static Button find(Node node, String text) {
        if (node instanceof Button button && button.getText().equals(text)) return button;
        if (node instanceof TabPane tabs) for (Tab tab : tabs.getTabs()) { var button = find(tab.getContent(), text); if (button != null) return button; }
        if (node instanceof Parent parent) for (Node child : parent.getChildrenUnmodifiable()) { var button = find(child, text); if (button != null) return button; }
        return null;
    }
}
