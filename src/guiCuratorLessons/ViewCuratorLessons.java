package guiCuratorLessons;

import entityClasses.User;
import entityClasses.LessonsLearned;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewCuratorLessons Class. </p>
 * <p> Description: Cross-Contributor lesson review, manually reviewed core and terms, and
 * independent protections. Tested by TP3DiscoveryGuiTestingAutomation. Statistics open in a separate MVC workflow; AI remains a later increment.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Curator core operations
 */
public class ViewCuratorLessons {
    /** Authenticated page context and stable lesson selection. */
    protected static Stage stage;
    protected static User user;
    protected static ListView<LessonsLearned> lessons;
    /** Reviewed drafts stay separate from saved entities until explicitly submitted. */
    protected static TextField query, title;
    protected static TextArea problem, text, terms, experiences;
    /** Four independent saved decisions; whole-record unlock does not clear field flags. */
    protected static CheckBox wholeLock, titleLock, problemLock, textLock;
    /** Current record identity and corrective operation feedback. */
    protected static Label identity, status;
    protected static TabPane tabs;
    private ViewCuratorLessons() { }
    /**
     * Authorized entry; tested by TP3DiscoveryGuiTestingAutomation.
     * @param target application stage
     * @param account authenticated account snapshot
     */
    public static void displayCuratorLessons(Stage target, User account) {
        var current = guiTools.GUISingleRoleDispatch.requireRoleForView(target, account, guiTools.RoleNames.CURATOR);
        if (current == null) return;
        stage = target; user = current;
        query = new TextField(); query.setPromptText("Search core and saved terms"); query.setId("curator-query");
        Button search = new Button("Search"), all = new Button("Show All"), reload = new Button("Reload Selected");
        Button home = new Button("Curator Home");
        Button statistics = new Button("Statistics and Visibility");
        statistics.setOnAction(e -> ControllerCuratorLessons.statistics());
        search.setOnAction(e -> ControllerCuratorLessons.search()); query.setOnAction(e -> ControllerCuratorLessons.search());
        all.setOnAction(e -> { query.clear(); ControllerCuratorLessons.search(); });
        reload.setOnAction(e -> ControllerCuratorLessons.read());
        home.setOnAction(e -> guiCurator.ViewCuratorHome.displayCuratorHome(stage, user));
        lessons = new ListView<>(); lessons.setId("curator-lessons"); lessons.setPrefWidth(230);
        lessons.setPlaceholder(new Label("No matching lessons."));
        lessons.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (!ControllerCuratorLessons.refreshing) ControllerCuratorLessons.read();
        });
        title = new TextField(); title.setId("curator-title");
        problem = area("curator-problem"); text = area("curator-text"); terms = area("curator-terms");
        experiences = area("curator-experiences"); experiences.setEditable(false);
        Button save = new Button("Save Reviewed Core"); save.setOnAction(e -> ControllerCuratorLessons.refine());
        VBox core = new VBox(6, new Label("Title (maximum 500 characters)"), title,
                new Label("Problem or Situation"), problem, new Label("Lesson Learned"), text, save);
        VBox.setVgrow(problem, Priority.ALWAYS); VBox.setVgrow(text, Priority.ALWAYS);
        Button saveTerms = new Button("Save Reviewed Terms"), clearTerms = new Button("Clear Terms");
        saveTerms.setOnAction(e -> ControllerCuratorLessons.saveTerms(false)); clearTerms.setOnAction(e -> ControllerCuratorLessons.saveTerms(true));
        Label termHelp = new Label("One term per line. Remove blank entries before saving. Clear Terms removes all terms."); termHelp.setWrapText(true);
        VBox termPane = new VBox(10, termHelp, terms, new HBox(10, saveTerms, clearTerms)); VBox.setVgrow(terms, Priority.ALWAYS);
        wholeLock = new CheckBox("Whole Lesson"); titleLock = new CheckBox("Title");
        problemLock = new CheckBox("Problem or Situation"); textLock = new CheckBox("Lesson Learned");
        wholeLock.setId("curator-whole-lock"); titleLock.setId("curator-title-lock");
        problemLock.setId("curator-problem-lock"); textLock.setId("curator-text-lock");
        Button locks = new Button("Save Protections"); locks.setOnAction(e -> ControllerCuratorLessons.locks());
        VBox protection = new VBox(10, new Label("Protect Contributor changes. Field locks are independent."), wholeLock, titleLock, problemLock, textLock, locks);
        tabs = new TabPane(tab("Core", core), tab("Experiences", experiences), tab("Related Terms", termPane), tab("Protections", protection));
        HBox center = new HBox(10, lessons, tabs); HBox.setHgrow(tabs, Priority.ALWAYS); VBox.setVgrow(center, Priority.ALWAYS);
        identity = new Label("Select a lesson to review."); identity.setId("curator-identity");
        status = new Label(); status.setId("curator-status"); status.setWrapText(true);
        Label heading = new Label("Curator Lesson Review"); guiTools.ASUTheme.stylePageTitle(heading);
        VBox root = new VBox(10, heading, new HBox(10, query, search, all, reload), identity, center, status, new HBox(10, statistics, home));
        root.setPadding(new Insets(20)); Scene scene = new Scene(root, 1000, 720); guiTools.ASUTheme.apply(scene);
        stage.setScene(scene); stage.setTitle("CSE 360: Curator Lessons"); ControllerCuratorLessons.clear(); ControllerCuratorLessons.search(); stage.show();
    }
    private static TextArea area(String id) { TextArea area = new TextArea(); area.setWrapText(true); area.setId(id); return area; }
    private static Tab tab(String title, javafx.scene.Node content) { Tab tab = new Tab(title, content); tab.setClosable(false); return tab; }
}
