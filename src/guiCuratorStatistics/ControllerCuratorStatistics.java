package guiCuratorStatistics;

import entityClasses.StatisticsPreview;
import entityClasses.StatisticsSummary;

/*******
 * <p> Title: ControllerCuratorStatistics Class. </p>
 * <p> Description: Keeps reviewed inputs distinct from current selection and saved snapshots.
 * Selection changes invalidate preview; a failed stale save requires preview again. Every
 * action is tested by TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Statistics MVC
 */
public class ControllerCuratorStatistics {
    /** Accepted unsaved preview; cleared after selection changes, save, or failed save. */
    protected static StatisticsPreview reviewed;
    private ControllerCuratorStatistics() { }
    private static String actor() { return ViewCuratorStatistics.user.getUserName(); }
    private static String destination() { return Long.toString(ViewCuratorStatistics.destination); }
    /** Clear stale review state; tested by GUI selection invalidation and stale-save scenarios. */
    protected static void invalidatePreview() {
        reviewed = null;
        if (ViewCuratorStatistics.save != null) ViewCuratorStatistics.save.setDisable(true);
        if (ViewCuratorStatistics.previewText != null) ViewCuratorStatistics.previewText.clear();
    }
    /** Reload selectable evidence; tested by GUI source selection and removed-role scenarios. */
    protected static void refreshSources() {
        invalidatePreview(); var result = ModelCuratorStatistics.listSources(actor());
        ViewCuratorStatistics.sources.getItems().clear();
        if (result.isSuccessful()) ViewCuratorStatistics.sources.getItems().setAll(result.getValue());
        ViewCuratorStatistics.status.setText(result.getMessage());
    }
    /** Prepare current inputs for review without writing; tested by GUI preview scenarios. */
    protected static void preview() {
        invalidatePreview();
        var ids = ViewCuratorStatistics.sources.getSelectionModel().getSelectedItems().stream().map(x -> x.getId()).toList();
        var result = ModelCuratorStatistics.preview(actor(), destination(), ids);
        if (result.isSuccessful()) {
            reviewed = result.getValue(); StringBuilder text = new StringBuilder(reviewed.getValues().toString());
            for (var source : reviewed.getSources()) text.append("\nSource lesson ").append(source.getOriginalLessonId())
                    .append(", experience ").append(source.getOriginalExperienceId()).append(": ").append(source.getElapsedMinutes())
                    .append(" minutes; ").append(source.getEffortTotalMinutes()).append(" person-minutes");
            ViewCuratorStatistics.previewText.setText(text.toString()); ViewCuratorStatistics.save.setDisable(false);
        }
        ViewCuratorStatistics.status.setText(result.getMessage());
    }
    /** Explicitly save accepted current evidence hidden; tested by GUI save and stale rejection. */
    protected static void save() {
        var result = ModelCuratorStatistics.save(actor(), reviewed == null ? null : reviewed.toRequest());
        invalidatePreview();
        if (result.isSuccessful()) {
            refreshSummaries();
            ViewCuratorStatistics.summaries.getItems().stream().filter(x -> x.getId() == result.getValue().getId()).findFirst()
                    .ifPresent(x -> ViewCuratorStatistics.summaries.getSelectionModel().select(x));
        }
        ViewCuratorStatistics.status.setText(result.getMessage());
    }
    /** Reload saved immutable snapshots and live staleness notices; tested by GUI saved review. */
    protected static void refreshSummaries() {
        var selected = ViewCuratorStatistics.summaries.getSelectionModel().getSelectedItem();
        var result = ModelCuratorStatistics.listSummaries(actor(), destination());
        ViewCuratorStatistics.summaries.getItems().clear(); ViewCuratorStatistics.savedText.clear();
        if (result.isSuccessful()) {
            ViewCuratorStatistics.summaries.getItems().setAll(result.getValue());
            if (selected != null) result.getValue().stream().filter(x -> x.getId() == selected.getId()).findFirst()
                    .ifPresent(x -> ViewCuratorStatistics.summaries.getSelectionModel().select(x));
        }
        ViewCuratorStatistics.status.setText(result.getMessage());
    }
    /** Reauthorize current summary selection; tested by GUI saved review and staleness. */
    protected static void read() {
        var selected = ViewCuratorStatistics.summaries.getSelectionModel().getSelectedItem(); ViewCuratorStatistics.savedText.clear();
        if (selected == null) return;
        var result = ModelCuratorStatistics.readSummary(actor(), Long.toString(selected.getId()));
        if (result.isSuccessful()) ViewCuratorStatistics.savedText.setText(describe(result.getValue()));
        ViewCuratorStatistics.status.setText(result.getMessage());
    }
    /**
     * Tested by GUI share/hide and revoked-role scenarios.
     * @param visible true to share only aggregates with the destination owner
     */
    protected static void visibility(boolean visible) {
        var selected = ViewCuratorStatistics.summaries.getSelectionModel().getSelectedItem();
        var result = ModelCuratorStatistics.setVisibility(actor(), selected == null ? "" : Long.toString(selected.getId()), visible);
        if (result.isSuccessful()) refreshSummaries(); ViewCuratorStatistics.status.setText(result.getMessage());
    }
    private static String describe(StatisticsSummary summary) {
        StringBuilder text = new StringBuilder(summary.toString()).append("\n").append(summary.getValues());
        if (summary.isStale()) text.append("\nEarlier source data: live inputs changed or disappeared. Saved values remain historical. Preview a new selection to recalculate.");
        for (var source : summary.getSources()) text.append("\nOriginal lesson ").append(source.getOriginalLessonId())
                .append(", experience ").append(source.getOriginalExperienceId()).append(": ").append(source.getElapsedMinutes())
                .append(" minutes; ").append(source.getEffortTotalMinutes()).append(" person-minutes");
        return text.toString();
    }
}
