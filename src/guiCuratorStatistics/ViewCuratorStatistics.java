package guiCuratorStatistics;

import entityClasses.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewCuratorStatistics Class. </p>
 * <p> Description: Select multiple source attempts, review a preview, explicitly save a hidden
 * snapshot, inspect historical evidence and choose visibility. All controls are validated by
 * TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Statistics MVC
 */
public class ViewCuratorStatistics {
    /** Trusted page context fixes the destination independently of source selection. */
    protected static Stage stage;
    protected static User user;
    protected static long destination;
    /** Distinct selected attempts and saved snapshots remain separate collections. */
    protected static ListView<ExperienceRecord> sources;
    protected static ListView<StatisticsSummary> summaries;
    /** Review output is never an editable persistence request. */
    protected static TextArea previewText, savedText;
    protected static Label status;
    protected static Button save;
    private ViewCuratorStatistics() { }
    /**
     * Tested by TP3StatisticsGuiTestingAutomation entry and snapshot scenarios.
     * @param target application stage
     * @param account authenticated account
     * @param lessonId selected destination lesson key
     */
    public static void displayStatistics(Stage target, User account, long lessonId) {
        var current = guiTools.GUISingleRoleDispatch.requireRoleForView(target, account, guiTools.RoleNames.CURATOR);
        if (current == null) return;
        stage = target; user = current; destination = lessonId;
        sources = new ListView<>(); sources.setId("statistics-sources"); sources.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        sources.setPlaceholder(new Label("No experiences. Add valid Contributor experiences first."));
        sources.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ExperienceRecord record, boolean empty) {
                super.updateItem(record, empty);
                setText(empty || record == null ? null : "Lesson " + record.getLessonId() + " | Experience " + record.getId()
                        + " | elapsed " + record.getElapsedMinutes() + " minutes\n" + record.getTeamEfforts());
            }
        });
        summaries = new ListView<>(); summaries.setId("statistics-summaries"); summaries.setPlaceholder(new Label("No saved snapshots for this lesson."));
        sources.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<ExperienceRecord>) change -> ControllerCuratorStatistics.invalidatePreview());
        summaries.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> ControllerCuratorStatistics.read());
        previewText = area("statistics-preview"); savedText = area("statistics-saved");
        Button refresh = new Button("Refresh Sources"), preview = new Button("Preview Statistics"); save = new Button("Save Reviewed Snapshot");
        Button share = new Button("Share Summary"), hide = new Button("Hide Summary"), reload = new Button("Refresh Summaries"), back = new Button("Return to Curator Review");
        refresh.setOnAction(e -> ControllerCuratorStatistics.refreshSources()); preview.setOnAction(e -> ControllerCuratorStatistics.preview());
        save.setOnAction(e -> ControllerCuratorStatistics.save()); share.setOnAction(e -> ControllerCuratorStatistics.visibility(true));
        hide.setOnAction(e -> ControllerCuratorStatistics.visibility(false)); reload.setOnAction(e -> ControllerCuratorStatistics.refreshSummaries());
        back.setOnAction(e -> guiCuratorLessons.ViewCuratorLessons.displayCuratorLessons(stage, user));
        VBox left = new VBox(8, new Label("Sources: select at least two attempts (Ctrl/Cmd or Shift)"), sources,
                new HBox(8, refresh, preview), previewText, save);
        VBox right = new VBox(8, new Label("Saved historical summaries"), summaries, new HBox(8, share, hide, reload), savedText);
        left.setPrefWidth(540); right.setPrefWidth(540); HBox.setHgrow(left, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(sources, Priority.ALWAYS); VBox.setVgrow(previewText, Priority.ALWAYS);
        VBox.setVgrow(summaries, Priority.ALWAYS); VBox.setVgrow(savedText, Priority.ALWAYS);
        HBox content = new HBox(16, left, right); VBox.setVgrow(content, Priority.ALWAYS);
        status = new Label(); status.setWrapText(true); status.setId("statistics-status");
        Label title = new Label("Statistics for destination lesson " + destination); guiTools.ASUTheme.stylePageTitle(title);
        VBox root = new VBox(10, title, content, status, back); root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 1150, 740); guiTools.ASUTheme.apply(scene); stage.setScene(scene); stage.setTitle("TP3: Curator Statistics");
        ControllerCuratorStatistics.invalidatePreview(); ControllerCuratorStatistics.refreshSources(); ControllerCuratorStatistics.refreshSummaries(); stage.show();
    }
    private static TextArea area(String id) { TextArea text = new TextArea(); text.setEditable(false); text.setWrapText(true); text.setId(id); return text; }
}
