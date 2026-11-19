package guiTools;

import aiAssistance.*;
import applicationMain.FoundationsMain;
import entityClasses.User;
import lessonManagement.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/*******
 * <p> Title: TP3AiGuiTestingAutomation Class. </p>
 * <p> Description: Real JavaFX, file-backed H2 and the real HTTP adapter against an explicit
 * loopback fixture verify generation, follow-ups, review/save, failure retention, cancellation,
 * current roles and locks. Synthetic replies are not claimed as live AI generation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI JavaFX integration verification
 */
public class TP3AiGuiTestingAutomation {
    private static final String VIEW = "guiAiAssistance.ViewAiAssistance";
    private static Stage stage;
    private static int passed, failed;
    /**
     * @param args unused
     * @throws Exception on startup or workflow failure
     */
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("tp3.isolated.gui.test")) throw new IllegalStateException("Use the isolated test runner.");
        Platform.startup(() -> Platform.setImplicitExit(false));
        try (var fixture = new AiProviderFixture()) {
            fx(() -> { stage = new Stage(); new FoundationsMain().start(stage); });
            run(fixture);
        } finally { fx(() -> { FoundationsMain.database.closeConnection(); if (stage != null) stage.close(); }); Platform.exit(); }
        System.out.println("AI GUI checks passed: " + passed + "; failed: " + failed); if (failed != 0) throw new AssertionError("AI GUI failed");
    }
    private static void run(AiProviderFixture fixture) throws Exception {
        User owner = user("Owner", false, true, false, false), curator = user("Curator", false, false, false, true), admin = user("Admin", true, false, false, false);
        fx(() -> {
            for (var account : List.of(owner, curator, admin)) FoundationsMain.database.register(account);
            var field = Class.forName("guiAiAssistance.ModelAiAssistance").getDeclaredField("service"); field.setAccessible(true);
            field.set(null, new AiAssistanceService(FoundationsMain.database, fixture.client()));
            guiContributorLessons.ViewContributorLessons.displayLessons(stage, owner); button("AI New Lesson").fire();
            check("Contributor opens new AI draft", true, stage.getTitle().contains("AI Assistance"));
            check("Contributor has three purposes", List.of(AiPurpose.WRITE, AiPurpose.BRAINSTORM, AiPurpose.EXAMPLES), combo().getItems());
            check("Generated area read-only", false, area("suggestion").isEditable());
            button("Ask AI / Follow Up").fire(); check("Blank request rejected", true, label().contains("Enter a request"));
            field("title", TextField.class).setText("My draft"); area("problem").setText("Course problem"); area("text").setText("Original explanation"); area("prompt").setText("Improve clarity");
            button("Ask AI / Follow Up").fire(); check("Duplicate request disabled", true, button("Ask AI / Follow Up").isDisabled());
        });
        awaitIdle();
        fx(() -> {
            check("AI suggestion clearly labeled", true, area("suggestion").getText().contains("AI-GENERATED"));
            check("Response leaves current draft unchanged", "My draft", field("title", TextField.class).getText());
            check("Response writes no records", 0, FoundationsMain.database.getLessonCount());
            button("Discard Suggestion").fire(); check("Discard clears only suggestion", "", area("suggestion").getText());
            check("Discard retains draft", "My draft", field("title", TextField.class).getText());
            area("prompt").setText("Make it shorter"); button("Ask AI / Follow Up").fire();
        });
        awaitIdle();
        check("Follow-up sent prior successful context", true, fixture.requests.stream().anyMatch(body -> body.contains("Make it shorter") && body.contains("Improve clarity") && body.contains("assistant")));
        fx(() -> {
            button("Accept Suggestion Into Draft").fire(); check("Acceptance changes draft only", "Suggested title", field("title", TextField.class).getText());
            check("Acceptance still writes nothing", 0, FoundationsMain.database.getLessonCount());
            field("title", TextField.class).setText(" "); button("Save Reviewed Draft").fire(); check("Invalid reviewed draft retained", " ", field("title", TextField.class).getText());
            field("title", TextField.class).setText("Reviewed AI lesson"); button("Save Reviewed Draft").fire();
            check("Explicit reviewed save persists", 1, FoundationsMain.database.getLessonCount());
        });
        long id = value(() -> FoundationsMain.database.getAllLessons().getFirst().getId());
        fixture.status = 503;
        fx(() -> { area("prompt").setText("Try again"); button("Ask AI / Follow Up").fire(); }); awaitIdle();
        fx(() -> { check("Provider failure offers recovery", true, label().contains("unavailable")); check("Failure retains draft", "Reviewed AI lesson", field("title", TextField.class).getText()); });
        fixture.status = 200; fixture.delayMillis = 600;
        fx(() -> { area("prompt").setText("Slow request"); button("Ask AI / Follow Up").fire(); button("Cancel Request").fire(); check("Cancel returns control immediately", false, button("Ask AI / Follow Up").isDisabled()); });
        Thread.sleep(750);
        fx(() -> { check("Late canceled response cannot populate output", "", area("suggestion").getText()); check("Cancellation preserves saved core", "Reviewed AI lesson", FoundationsMain.database.getLesson(id).getTitle()); });
        fixture.delayMillis = 0;
        fx(() -> { combo().setValue(AiPurpose.EXAMPLES); area("prompt").setText("Find course examples"); button("Ask AI / Follow Up").fire(); }); awaitIdle();
        fx(() -> { check("Examples expose citation links", true, area("suggestion").getText().contains("https://example.test/reference")); check("Examples do not replace core", "Reviewed AI lesson", field("title", TextField.class).getText()); });
        fx(() -> {
            new CuratorLessonService(FoundationsMain.database).setLessonLocks("Curator", "" + id, new LessonLockState(true, true, true, true));
            field("title", TextField.class).setText("Rejected locked edit"); button("Save Reviewed Draft").fire(); check("AI reviewed save respects current locks", "Reviewed AI lesson", FoundationsMain.database.getLesson(id).getTitle());
            button("Return to Lessons").fire(); check("AI return navigation", true, stage.getTitle().contains("My Lessons"));
            guiCuratorLessons.ViewCuratorLessons.displayCuratorLessons(stage, curator);
            fieldStatic("guiCuratorLessons.ViewCuratorLessons", "lessons", ListView.class).getSelectionModel().select(0); button("AI Assistance").fire();
            check("Curator purposes restricted", List.of(AiPurpose.REFINE, AiPurpose.TERMS), combo().getItems());
            area("prompt").setText("Refine"); button("Ask AI / Follow Up").fire();
        }); awaitIdle();
        fx(() -> { button("Accept Suggestion Into Draft").fire(); button("Save Reviewed Draft").fire(); check("Curator can refine protected core", "Suggested title", FoundationsMain.database.getLesson(id).getTitle()); check("AI refinement preserves owner", "Owner", FoundationsMain.database.getLesson(id).getOwner()); combo().setValue(AiPurpose.TERMS); area("prompt").setText("Suggest related terms"); button("Ask AI / Follow Up").fire(); }); awaitIdle();
        fx(() -> {
            button("Accept Suggestion Into Draft").fire(); check("Suggested terms remain unsaved", 0, FoundationsMain.database.getSearchTerms(id).size());
            button("Save Reviewed Terms").fire(); check("Reviewed terms persisted", 2, FoundationsMain.database.getSearchTerms(id).size());
            area("terms").setText("valid\n\ninvalid"); button("Save Reviewed Terms").fire(); check("Blank reviewed term rejection preserves saved set", 2, FoundationsMain.database.getSearchTerms(id).size());
            button("New Conversation").fire(); check("New conversation preserves draft", "Suggested title", field("title", TextField.class).getText());
        });
        fixture.delayMillis = 300;
        fx(() -> { area("prompt").setText("Request before revocation"); button("Ask AI / Follow Up").fire(); FoundationsMain.database.updateUserRole("Curator", "Viewer", "true", "Admin"); FoundationsMain.database.updateUserRole("Curator", "Curator", "false", "Admin"); }); awaitIdle();
        fx(() -> {
            check("Role revoked in flight prevents output display", "", area("suggestion").getText());
            area("terms").setText("rejected"); button("Save Reviewed Terms").fire(); check("Revoked role cannot save generated terms", 2, FoundationsMain.database.getSearchTerms(id).size());
        });
    }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) { return new User(name, "Course1!", "", "", "", "", "", admin, contributor, viewer, curator); }
    private static void awaitIdle() throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < end) { if (value(() -> !button("Ask AI / Follow Up").isDisabled())) return; Thread.sleep(20); }
        throw new AssertionError("AI GUI request did not complete");
    }
    private static void fx(Checked action) throws Exception { value(() -> { action.run(); return null; }); }
    private static <T> T value(Callable<T> action) throws Exception { var task = new FutureTask<T>(action); Platform.runLater(task); return task.get(10, TimeUnit.SECONDS); }
    private interface Checked { void run() throws Exception; }
    private static <T> T fieldStatic(String type, String name, Class<T> expected) throws Exception { var field = Class.forName(type).getDeclaredField(name); field.setAccessible(true); return expected.cast(field.get(null)); }
    private static <T> T field(String name, Class<T> expected) throws Exception { return fieldStatic(VIEW, name, expected); }
    private static TextArea area(String name) throws Exception { return field(name, TextArea.class); }
    @SuppressWarnings("unchecked") private static ComboBox<AiPurpose> combo() throws Exception { return (ComboBox<AiPurpose>)field("purpose", ComboBox.class); }
    private static String label() throws Exception { return field("status", Label.class).getText(); }
    private static void check(String name, Object expected, Object actual) { boolean success = Objects.equals(expected, actual); System.out.println(name + " | Expected: " + expected + " | Actual: " + actual + " | " + (success ? "PASS" : "FAIL")); if (success) passed++; else failed++; }
    private static Button button(String text) { var found = find(stage.getScene().getRoot(), text); if (found == null) throw new AssertionError("Missing control " + text); return found; }
    private static Button find(Node node, String text) {
        if (node instanceof Button b && b.getText().equals(text)) return b;
        if (node instanceof TabPane tabs) for (var tab : tabs.getTabs()) { var found = find(tab.getContent(), text); if (found != null) return found; }
        if (node instanceof Parent parent) for (var child : parent.getChildrenUnmodifiable()) { var found = find(child, text); if (found != null) return found; }
        return null;
    }
}
