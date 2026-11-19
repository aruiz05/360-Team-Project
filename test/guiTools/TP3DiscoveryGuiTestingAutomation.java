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
 * <p> Title: TP3DiscoveryGuiTestingAutomation Class. </p>
 * <p> Description: Actual JavaFX controls and isolated persistent H2 verify Viewer search,
 * details/return, stale selections, and Curator review, refinement, draft retention, terms,
 * protections and role removal. All course examples are invented. Expected values, actual
 * values, pass/fail and totals are printed. The runner must supply an isolated user.home.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 GUI integration verification
 */
public class TP3DiscoveryGuiTestingAutomation {
    private static int passed, failed;
    private static final String VIEWER = "guiViewerLessons.ViewViewerLessons", CURATOR = "guiCuratorLessons.ViewCuratorLessons";
    /**
     * @param args unused
     * @throws Exception on JavaFX startup or integration failure
     */
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("tp3.isolated.gui.test")) throw new IllegalStateException("Use the isolated test runner.");
        CountDownLatch completed = new CountDownLatch(1); AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.startup(() -> {
            try { runDiscovery(); } catch (Throwable failure) { error.set(failure); }
            finally { FoundationsMain.database.closeConnection(); completed.countDown(); }
        });
        if (!completed.await(40, TimeUnit.SECONDS)) throw new AssertionError("Discovery GUI timed out");
        Platform.exit(); if (error.get() != null) throw new AssertionError("Discovery GUI failed", error.get());
        System.out.println("Discovery GUI checks passed: " + passed + "; failed: " + failed);
        if (failed != 0) throw new AssertionError("Discovery GUI checks failed");
    }
    private static void runDiscovery() throws Exception {
        Platform.setImplicitExit(false); Stage stage = new Stage(); new FoundationsMain().start(stage);
        var database = FoundationsMain.database;
        var admin = user("Admin", true, false, false, false); var owner = user("Owner", false, true, false, false);
        var other = user("Other", false, true, false, false); var viewer = user("Viewer", false, false, true, false);
        var curator = user("Curator", false, false, false, true);
        for (var account : List.of(admin, owner, other, viewer, curator)) database.register(account);
        var contributor = new LessonService(database);
        String id = "" + contributor.createLesson("Owner", new LessonDraft("H2 Persistence", "Course problem", "Validate input")).getValue().getId();
        String otherId = "" + contributor.createLesson("Other", new LessonDraft("JavaFX Layout", "Other course problem", "Use layouts")).getValue().getId();
        contributor.createExperience(id, "Owner", new ExperienceDraft("Verified restart", "Opened H2 again", "12", List.of(new EffortInput("Sam", "10"))));
        guiViewer.ViewViewerHome.displayViewerHome(stage, viewer); button(stage, "Discover Lessons").fire();
        check("Viewer home opens discovery", true, stage.getTitle().contains("Viewer Lessons"));
        check("Viewer full collection", 2, rows(VIEWER).getItems().size());
        check("Viewer has no core save control", null, find(stage.getScene().getRoot(), "Save Reviewed Core"));
        check("Viewer detail read-only", false, area(VIEWER, "details").isEditable());
        field(VIEWER, "query", TextField.class).setText("h2 validate"); button(stage, "Search").fire();
        check("Viewer cross-field AND search", 1, rows(VIEWER).getItems().size());
        rows(VIEWER).getSelectionModel().select(0); button(stage, "Read Lesson").fire();
        check("Viewer current core details", true, area(VIEWER, "details").getText().contains("H2 Persistence"));
        check("Viewer excludes raw experience", false, area(VIEWER, "details").getText().contains("Verified restart"));
        button(stage, "Return to Results").fire();
        check("Return preserves query", "h2 validate", field(VIEWER, "query", TextField.class).getText());
        check("Return preserves stable ID", Long.parseLong(id), ((ViewerLesson) rows(VIEWER).getSelectionModel().getSelectedItem()).getId());
        field(VIEWER, "query", TextField.class).setText("absent"); button(stage, "Search").fire();
        check("Viewer empty search", 0, rows(VIEWER).getItems().size()); button(stage, "Read Lesson").fire();
        check("Missing selection corrective message", true, field(VIEWER, "status", Label.class).getText().contains("Select"));
        button(stage, "Show All").fire(); check("Show All restores collection", 2, rows(VIEWER).getItems().size());
        rows(VIEWER).getSelectionModel().select(1); contributor.deleteLesson(otherId, "Other"); button(stage, "Read Lesson").fire();
        check("Deleted selection removes stale row", 1, rows(VIEWER).getItems().size());
        check("Deleted selection clears details", "", area(VIEWER, "details").getText());
        button(stage, "Viewer Home").fire(); check("Viewer return home", true, stage.getTitle().contains("Viewer Home"));
        guiCurator.ViewCuratorHome.displayCuratorHome(stage, curator); button(stage, "Review Lessons").fire();
        check("Curator home opens review", true, stage.getTitle().contains("Curator Lessons"));
        check("Curator selection required", true, field(CURATOR, "tabs", TabPane.class).isDisabled());
        rows(CURATOR).getSelectionModel().select(0);
        check("Curator current owner", true, field(CURATOR, "identity", Label.class).getText().contains("Owner"));
        String evidence = area(CURATOR, "experiences").getText();
        check("Curator experience and units", true, evidence.contains("Verified restart") && evidence.contains("Elapsed minutes: 12.0") && evidence.contains("Sam: 10.0 person-minutes"));
        check("Curator experience lock states", true, evidence.contains("Locks - record: false"));
        field(CURATOR, "title", TextField.class).setText("   "); button(stage, "Save Reviewed Core").fire();
        check("Invalid core preserves database", "H2 Persistence", database.getLesson(Long.parseLong(id)).getTitle());
        check("Invalid draft retained", "   ", field(CURATOR, "title", TextField.class).getText());
        field(CURATOR, "title", TextField.class).setText("Reviewed persistence"); button(stage, "Save Reviewed Core").fire();
        check("Reviewed core saved", "Reviewed persistence", database.getLesson(Long.parseLong(id)).getTitle());
        check("Core owner preserved", "Owner", database.getLesson(Long.parseLong(id)).getOwner());
        field(CURATOR, "title", TextField.class).setText("Unsaved draft"); button(stage, "Reload Selected").fire();
        check("Reload Selected restores saved core", "Reviewed persistence", field(CURATOR, "title", TextField.class).getText());
        area(CURATOR, "terms").setText(" Rollback \nROLLBACK\nRecovery"); button(stage, "Save Reviewed Terms").fire();
        check("Terms normalized and deduplicated", List.of("recovery", "rollback"), database.getSearchTerms(Long.parseLong(id)).stream().map(SearchTerm::getTerm).toList());
        area(CURATOR, "terms").setText("valid\n\ninvalid"); button(stage, "Save Reviewed Terms").fire();
        check("Invalid term draft retained", "valid\n\ninvalid", area(CURATOR, "terms").getText());
        check("Invalid terms preserve saved set", 2, database.getSearchTerms(Long.parseLong(id)).size());
        field(CURATOR, "wholeLock", CheckBox.class).setSelected(true); field(CURATOR, "titleLock", CheckBox.class).setSelected(true); button(stage, "Save Protections").fire();
        check("Whole protection saved", true, database.getLesson(Long.parseLong(id)).isLessonLocked());
        field(CURATOR, "wholeLock", CheckBox.class).setSelected(false); button(stage, "Save Protections").fire();
        check("Independent title protection retained", true, database.getLesson(Long.parseLong(id)).isTitleLocked());
        check("Whole lesson unlocked", false, database.getLesson(Long.parseLong(id)).isLessonLocked());
        check("Contributor observes GUI protection", false, contributor.updateLesson(id, "Owner", new LessonDraft("Rejected", "Course problem", "Validate input")).isSuccessful());
        guiViewerLessons.ViewViewerLessons.displayViewerLessons(stage, viewer);
        field(VIEWER, "query", TextField.class).setText("rollback"); button(stage, "Search").fire();
        check("Viewer discovers saved terms", 1, rows(VIEWER).getItems().size()); rows(VIEWER).getSelectionModel().select(0); button(stage, "Read Lesson").fire();
        check("Viewer sees refined current core", true, area(VIEWER, "details").getText().contains("Reviewed persistence"));
        guiCuratorLessons.ViewCuratorLessons.displayCuratorLessons(stage, curator); rows(CURATOR).getSelectionModel().select(0);
        field(CURATOR, "query", TextField.class).setText("rollback"); button(stage, "Search").fire(); check("Curator term filter", 1, rows(CURATOR).getItems().size());
        button(stage, "Clear Terms").fire(); check("Explicit clear saved", 0, database.getSearchTerms(Long.parseLong(id)).size());
        check("Clear refreshes filtered subset", 0, rows(CURATOR).getItems().size()); check("Removed selection clears core", "", field(CURATOR, "title", TextField.class).getText());
        button(stage, "Show All").fire(); rows(CURATOR).getSelectionModel().select(0);
        database.updateUserRole("Curator", "Viewer", "true", "Admin"); database.updateUserRole("Curator", "Curator", "false", "Admin");
        field(CURATOR, "title", TextField.class).setText("Rejected after role removal"); button(stage, "Save Reviewed Core").fire();
        check("Revoked Curator stale save rejected", "Reviewed persistence", database.getLesson(Long.parseLong(id)).getTitle());
        button(stage, "Search").fire(); check("Revoked Curator clears private detail", "", area(CURATOR, "experiences").getText());
        check("Revoked Curator clears list", 0, rows(CURATOR).getItems().size());
        guiViewerLessons.ViewViewerLessons.displayViewerLessons(stage, viewer);
        database.updateUserRole("Viewer", "Curator", "true", "Admin"); database.updateUserRole("Viewer", "Viewer", "false", "Admin");
        button(stage, "Search").fire(); check("Revoked Viewer clears results", 0, rows(VIEWER).getItems().size()); stage.close();
    }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) {
        return new User(name, "Course1!", "Course", "", "Project", "Course", "course@example.test", admin, contributor, viewer, curator);
    }
    private static <T> T field(String type, String name, Class<T> expected) throws Exception {
        var field = Class.forName(type).getDeclaredField(name); field.setAccessible(true); return expected.cast(field.get(null));
    }
    private static ListView<?> rows(String type) throws Exception { return field(type, "lessons", ListView.class); }
    private static TextArea area(String type, String name) throws Exception { return field(type, name, TextArea.class); }
    private static void check(String name, Object expected, Object actual) {
        boolean success = Objects.equals(expected, actual); System.out.println(name + " | Expected: " + expected + " | Actual: " + actual + " | " + (success ? "PASS" : "FAIL"));
        if (success) passed++; else failed++;
    }
    private static Button button(Stage stage, String text) {
        Button found = find(stage.getScene().getRoot(), text); if (found == null) throw new AssertionError("Missing control: " + text); return found;
    }
    private static Button find(Node node, String text) {
        if (node instanceof Button button && button.getText().equals(text)) return button;
        // Tab content may not yet have a skin; inspect its actual controls explicitly.
        if (node instanceof TabPane tabs) for (Tab tab : tabs.getTabs()) { Button found = find(tab.getContent(), text); if (found != null) return found; }
        if (node instanceof Parent parent) for (Node child : parent.getChildrenUnmodifiable()) { Button found = find(child, text); if (found != null) return found; }
        return null;
    }
}
