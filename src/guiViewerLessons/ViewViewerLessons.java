package guiViewerLessons;

import entityClasses.User;
import entityClasses.ViewerLesson;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewViewerLessons Class. </p>
 * <p> Description: Read-only discovery and current core details. Return preserves query and
 * stable selection while refreshing storage. Tested by TP3DiscoveryGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Viewer discovery
 */
public class ViewViewerLessons {
    /** Authenticated page context, never entered in a form. */
    protected static Stage stage;
    protected static User user;
    /** The query and visible subset are rebuilt from current persistent state. */
    protected static TextField query;
    protected static ListView<ViewerLesson> lessons;
    /** Core-only read area and corrective feedback. */
    protected static TextArea details;
    protected static Label status;
    /** Switching panels retains the query context without retaining a stale detail record. */
    protected static VBox listPane, detailPane;
    private ViewViewerLessons() { }
    /**
     * Current-role checked entry. Tested by TP3DiscoveryGuiTestingAutomation.
     * @param target application stage
     * @param account authenticated snapshot
     */
    public static void displayViewerLessons(Stage target, User account) {
        User current = guiTools.GUISingleRoleDispatch.requireRoleForView(target, account, guiTools.RoleNames.VIEWER);
        if (current == null) return;
        stage = target; user = current;
        query = new TextField(); query.setPromptText("Search core text and related terms (all words)"); query.setId("viewer-query");
        Button search = new Button("Search"), all = new Button("Show All"), open = new Button("Read Lesson");
        Button home = new Button("Viewer Home"), back = new Button("Return to Results");
        search.setOnAction(e -> ControllerViewerLessons.search()); query.setOnAction(e -> ControllerViewerLessons.search());
        all.setOnAction(e -> { query.clear(); ControllerViewerLessons.search(); });
        open.setOnAction(e -> ControllerViewerLessons.read());
        home.setOnAction(e -> guiViewer.ViewViewerHome.displayViewerHome(stage, user));
        back.setOnAction(e -> { ControllerViewerLessons.search(); showDetails(false); });
        lessons = new ListView<>(); lessons.setId("viewer-lessons");
        lessons.setPlaceholder(new Label("No lessons match. Change the search or choose Show All."));
        details = new TextArea(); details.setEditable(false); details.setWrapText(true); details.setId("viewer-details");
        status = new Label(); status.setWrapText(true); status.setId("viewer-status");
        listPane = new VBox(10, new HBox(10, query, search, all), lessons, open);
        detailPane = new VBox(10, details, back);
        VBox.setVgrow(lessons, Priority.ALWAYS); VBox.setVgrow(details, Priority.ALWAYS);
        StackPane content = new StackPane(listPane, detailPane); VBox.setVgrow(content, Priority.ALWAYS);
        Label title = new Label("Discover Lessons"); guiTools.ASUTheme.stylePageTitle(title);
        VBox root = new VBox(10, title, content, status, home); root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 800, 600); guiTools.ASUTheme.apply(scene);
        stage.setScene(scene); stage.setTitle("CSE 360: Viewer Lessons"); showDetails(false);
        ControllerViewerLessons.search(); stage.show();
    }
    /**
     * @param detail true for core detail, false for result list; tested by discovery GUI return
     */
    protected static void showDetails(boolean detail) {
        listPane.setVisible(!detail); listPane.setManaged(!detail);
        detailPane.setVisible(detail); detailPane.setManaged(detail);
    }
}
