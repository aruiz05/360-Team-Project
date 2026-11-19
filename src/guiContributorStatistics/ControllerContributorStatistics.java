package guiContributorStatistics;

/*******
 * <p> Title: ControllerContributorStatistics Class. </p>
 * <p> Description: Refreshes owned shared aggregates and clears previously shown values after
 * an access failure. Tested by TP3StatisticsGuiTestingAutomation sharing and role removal.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Shared statistics MVC
 */
public class ControllerContributorStatistics {
    private ControllerContributorStatistics() { }
    /** Reauthorize and reload safe values; tested by statistics GUI shared refresh scenarios. */
    protected static void refresh() {
        ViewContributorStatistics.values.clear();
        if (ViewContributorStatistics.lesson == null) { ViewContributorStatistics.status.setText("Select an owned lesson."); return; }
        var result = ModelContributorStatistics.read(ViewContributorStatistics.user.getUserName(), ViewContributorStatistics.lesson.getId());
        if (result.isSuccessful()) ViewContributorStatistics.values.setText(result.getValue().isEmpty() ? "No statistics have been shared for this lesson."
                : String.join("\n\n", result.getValue().stream().map(Object::toString).toList()));
        ViewContributorStatistics.status.setText(result.getMessage());
    }
}
