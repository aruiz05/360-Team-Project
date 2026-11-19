package integration;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/*******
 * <p> Title: TP3ModularSmoke Class. </p>
 * <p> Description: Starts the actual TP3 application with production classes on the module
 * path and an isolated home. Verifies first-run scene creation and H2 initialization, then
 * exits. Separate from classpath GUI tests so module readability is exercised as deployed.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Modular launch verification
 */
public class TP3ModularSmoke extends Application {
    /** @param args JavaFX arguments */
    public static void main(String[] args) { launch(args); }
    /**
     * @param stage isolated application stage
     * @throws Exception on startup or any failed smoke check
     */
    @Override public void start(Stage stage) throws Exception {
        try {
            new applicationMain.TP3().start(stage);
            check("TP3 modular first-run launch", stage.getTitle().contains("First User Account Setup"));
            check("TP3 modular scene displayed", stage.getScene() != null && stage.isShowing());
            check("TP3 modular H2 initialization", applicationMain.FoundationsMain.database.getLessonCount() == 0);
            System.out.println("Modular launch checks passed: 3; failed: 0");
        } finally { applicationMain.FoundationsMain.database.closeConnection(); stage.close(); Platform.exit(); }
    }
    private static void check(String name, boolean actual) { System.out.println(name + " | Expected: true | Actual: " + actual + " | " + (actual ? "PASS" : "FAIL")); if (!actual) throw new AssertionError(name); }
}
