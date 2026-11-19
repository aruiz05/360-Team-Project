package guiContributorStatistics;

import entityClasses.User;
import entityClasses.LessonsLearned;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewContributorStatistics Class. </p>
 * <p> Description: Read-only shared aggregate values for an owned lesson. No foreign source
 * evidence or hidden snapshot is supplied to this view. Validated by TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Shared statistics MVC
 */
public class ViewContributorStatistics {
    protected static User user;
    protected static LessonsLearned lesson;
    protected static TextArea values;
    protected static Label status;
    private ViewContributorStatistics() { }
    /**
     * Tested by statistics GUI entry, sharing, hiding and ownership checks.
     * @param stage application stage
     * @param account authenticated snapshot
     * @param selected owned destination lesson
     */
    public static void displayStatistics(Stage stage, User account, LessonsLearned selected) {
        var current = guiTools.GUISingleRoleDispatch.requireRoleForView(stage, account, guiTools.RoleNames.CONTRIBUTOR);
        if (current == null) return;
        user = current; lesson = selected; values = new TextArea(); values.setEditable(false); values.setWrapText(true); values.setId("shared-statistics-values");
        status = new Label(); status.setWrapText(true); status.setId("shared-statistics-status");
        Button refresh = new Button("Refresh Shared Statistics"), back = new Button("Return to Lesson Details");
        refresh.setOnAction(e -> ControllerContributorStatistics.refresh());
        back.setOnAction(e -> guiContributorLessonDetails.ViewContributorLessonDetails.displayLessonDetails(stage, user, lesson));
        Label title = new Label("Shared Statistics"); guiTools.ASUTheme.stylePageTitle(title);
        VBox root = new VBox(10, title, values, status, new HBox(10, refresh, back)); root.setPadding(new Insets(20)); VBox.setVgrow(values, Priority.ALWAYS);
        Scene scene = new Scene(root, 800, 600); guiTools.ASUTheme.apply(scene); stage.setScene(scene); stage.setTitle("TP3: Shared Statistics");
        ControllerContributorStatistics.refresh(); stage.show();
    }
}
