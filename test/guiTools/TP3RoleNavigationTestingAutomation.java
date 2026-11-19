package guiTools;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import applicationMain.FoundationsMain;
import entityClasses.User;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

/*******
 * <p> Title: TP3RoleNavigationTestingAutomation Class. </p>
 * <p> Description: Runs actual JavaFX role screens and account navigation using an isolated
 * user.home selected by the runner. Tests both dispatch paths, current-role rejection, and
 * logout. Supports the assigned-role story. A headless Glass backend may be used in CI.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-18 Phase 3 role navigation integration tests
 */
public class TP3RoleNavigationTestingAutomation {
    private static int passed;
    private static int failed;

    /**
     * @param args unused
     * @throws Exception when JavaFX cannot start or a navigation check fails
     */
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("tp3.isolated.gui.test"))
            throw new IllegalStateException("Run with an isolated user.home and tp3.isolated.gui.test=true.");
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Platform.startup(() -> {
            try { runNavigation(); }
            catch (Throwable failure) { error.set(failure); }
            finally {
                FoundationsMain.database.closeConnection();
                completed.countDown();
            }
        });
        if (!completed.await(30, TimeUnit.SECONDS)) throw new AssertionError("JavaFX navigation timed out");
        Platform.exit();
        if (error.get() != null) throw new AssertionError("JavaFX navigation failed", error.get());
        System.out.println("GUI checks passed: " + passed + "; failed: " + failed);
        if (failed != 0) throw new AssertionError("Role navigation failed");
    }

    private static void runNavigation() throws Exception {
        Platform.setImplicitExit(false);
        Stage stage = new Stage();
        new applicationMain.TP3().start(stage);
        check("Clean first-run application launch", true, stage.getTitle().contains("First User Account Setup"));
        User admin = user("Admin", true, false, false, false);
        User contributor = user("Owner", false, true, false, false);
        User viewer = user("Viewer", false, false, true, false);
        User curator = user("Curator", false, false, false, true);
        User multi = user("Multi", false, false, true, true);
        for (User user : List.of(admin, contributor, viewer, curator, multi)) FoundationsMain.database.register(user);
        for (User user : List.of(admin, contributor, viewer, curator)) {
            String role = RoleNames.assignedRoles(user).getFirst();
            login(stage, user);
            check(role + " login and single-role home", true, stage.getTitle().contains(role));
            check(role + " account return code", switch(role) {
                case "Admin" -> 1; case "Contributor" -> 2; case "Viewer" -> 3; default -> 4;
            }, FoundationsMain.activeHomePage);
        }
        GUISingleRoleDispatch.dispatchRole(stage, viewer, "Viewer");
        button(stage.getScene().getRoot(), "Account Update").fire();
        check("Viewer account update opens", true, containsLabel(stage.getScene().getRoot(), "Viewer"));
        // Account update navigation uses the real existing controller and active home code.
        Button returnButton = findButtonContaining(stage.getScene().getRoot(), "Home");
        if (returnButton == null) throw new AssertionError("Account return button is missing");
        returnButton.fire();
        check("Viewer account return", true, stage.getTitle().contains("Viewer"));
        GUISingleRoleDispatch.dispatchRole(stage, curator, "Curator");
        button(stage.getScene().getRoot(), "Account Update").fire();
        findButtonContaining(stage.getScene().getRoot(), "Home").fire();
        check("Curator account return", true, stage.getTitle().contains("Curator"));
        login(stage, multi);
        ComboBox<?> roles = findCombo(stage.getScene().getRoot());
        check("Multiple-role choices", List.of("<Select a role>", "Viewer", "Curator"), roles.getItems());
        select(roles, "Curator");
        button(stage.getScene().getRoot(), "Perform Role").fire();
        check("Multiple-role Curator dispatch", true, stage.getTitle().contains("Curator"));
        button(stage.getScene().getRoot(), "Logout").fire();
        check("Logout returns to login", true, stage.getTitle().contains("Login"));
        // Close only the test's expected modal error while its nested event loop is active.
        Platform.runLater(() -> List.copyOf(Window.getWindows()).stream()
                .filter(window -> window != stage).forEach(Window::hide));
        GUISingleRoleDispatch.dispatchRole(stage, viewer, "Curator");
        check("Unassigned Curator rejected", true, stage.getTitle().contains("Login"));
        // Admin role editing must update the selected account, not the cached actor.
        GUISingleRoleDispatch.dispatchRole(stage, admin, "Admin");
        button(stage.getScene().getRoot(), "Add/Remove Roles").fire();
        select((ComboBox<?>) field("guiAddRemoveRoles.ViewAddRemoveRoles", "combobox_SelectUser"), "Viewer");
        select((ComboBox<?>) field("guiAddRemoveRoles.ViewAddRemoveRoles", "combobox_SelectRoleToAdd"), "Curator");
        button(stage.getScene().getRoot(), "Add This Role").fire();
        check("Admin editor adds Curator", true, FoundationsMain.database.findUserForRoleAccess("Viewer").getCuratorRole());
        select((ComboBox<?>) field("guiAddRemoveRoles.ViewAddRemoveRoles", "combobox_SelectRoleToRemove"), "Curator");
        button(stage.getScene().getRoot(), "Remove This Role").fire();
        check("Admin editor removes Curator", false, FoundationsMain.database.findUserForRoleAccess("Viewer").getCuratorRole());
        for (String role : List.of("Viewer", "Curator")) {
            String code = FoundationsMain.database.generateInvitationCode("new@example.test", role, "Admin");
            guiUserLogin.ViewUserLogin.displayUserLogin(stage);
            ((javafx.scene.control.TextField) field("guiUserLogin.ViewUserLogin", "text_Invitation")).setText(code);
            button(stage.getScene().getRoot(), "Setup Account").fire();
            ((javafx.scene.control.TextField) field("guiNewAccount.ViewNewAccount", "text_Username")).setText("New" + role);
            ((javafx.scene.control.TextField) field("guiNewAccount.ViewNewAccount", "text_Password1")).setText("Course1!");
            ((javafx.scene.control.TextField) field("guiNewAccount.ViewNewAccount", "text_Password2")).setText("Course1!");
            button(stage.getScene().getRoot(), "User Setup").fire();
            check(role + " invitation account created", List.of(role),
                    RoleNames.assignedRoles(FoundationsMain.database.findUserForRoleAccess("New" + role)));
            findButtonContaining(stage.getScene().getRoot(), "Home").fire();
            check(role + " new account return", true, stage.getTitle().contains(role));
        }
        stage.close();
    }

    private static Object field(String type, String name) throws Exception {
        java.lang.reflect.Field field = Class.forName(type).getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
    private static void login(Stage stage, User user) throws Exception {
        guiUserLogin.ViewUserLogin.displayUserLogin(stage);
        ((javafx.scene.control.TextField) field("guiUserLogin.ViewUserLogin", "text_Username")).setText(user.getUserName());
        ((javafx.scene.control.TextField) field("guiUserLogin.ViewUserLogin", "text_Password")).setText(user.getPassword());
        button(stage.getScene().getRoot(), "Log In").fire();
    }
    private static User user(String name, boolean admin, boolean contributor, boolean viewer, boolean curator) {
        return new User(name, "Course1!", "Course", "", "Project", "Course", "course@example.test",
                admin, contributor, viewer, curator);
    }
    private static void check(String name, Object expected, Object actual) {
        boolean success = java.util.Objects.equals(expected, actual);
        System.out.println(name + " | Expected: " + expected + " | Actual: " + actual
                + " | " + (success ? "PASS" : "FAIL"));
        if (success) passed++; else failed++;
    }
    private static Button button(Parent root, String text) {
        Button found = findButtonContaining(root, text);
        if (found == null) throw new AssertionError("Missing button: " + text);
        return found;
    }
    private static Button findButtonContaining(Parent root, String text) {
        for (Node node : root.getChildrenUnmodifiable()) {
            if (node instanceof Button button && button.getText().contains(text)) return button;
            if (node instanceof Parent parent) {
                Button found = findButtonContaining(parent, text);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static boolean containsLabel(Parent root, String text) {
        for (Node node : root.getChildrenUnmodifiable()) {
            if (node instanceof Label label && label.getText().contains(text)) return true;
            if (node instanceof Parent parent && containsLabel(parent, text)) return true;
        }
        return false;
    }
    private static ComboBox<?> findCombo(Parent root) {
        for (Node node : root.getChildrenUnmodifiable()) if (node instanceof ComboBox<?> combo) return combo;
        throw new AssertionError("Role chooser is missing");
    }
    @SuppressWarnings("unchecked")
    private static void select(ComboBox<?> combo, String role) { ((ComboBox<String>) combo).setValue(role); }
}
