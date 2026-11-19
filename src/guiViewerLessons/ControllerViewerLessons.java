package guiViewerLessons;

/*******
 * <p> Title: ControllerViewerLessons Class. </p>
 * <p> Description: Reloads fresh result subsets and current details by stable ID. Tested by
 * TP3DiscoveryGuiTestingAutomation search, return, removed-role, and stale selection scenarios.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Viewer discovery
 */
public class ControllerViewerLessons {
    private ControllerViewerLessons() { }
    /** Refresh from storage, preserving only the same matching ID; tested by discovery GUI search. */
    protected static void search() {
        var selected = ViewViewerLessons.lessons.getSelectionModel().getSelectedItem();
        var result = ModelViewerLessons.search(ViewViewerLessons.user.getUserName(), ViewViewerLessons.query.getText());
        ViewViewerLessons.lessons.getItems().clear(); ViewViewerLessons.details.clear();
        if (result.isSuccessful()) {
            ViewViewerLessons.lessons.getItems().setAll(result.getValue());
            if (selected != null) result.getValue().stream().filter(x -> x.getId() == selected.getId()).findFirst()
                    .ifPresent(x -> ViewViewerLessons.lessons.getSelectionModel().select(x));
        }
        ViewViewerLessons.status.setText(result.isSuccessful()
                ? result.getValue().size() + " lesson(s). Select one to read." : result.getMessage());
    }
    /** Reload current core rather than stale list text; tested by discovery GUI detail and deletion. */
    protected static void read() {
        var selected = ViewViewerLessons.lessons.getSelectionModel().getSelectedItem();
        var result = ModelViewerLessons.read(ViewViewerLessons.user.getUserName(),
                selected == null ? "" : Long.toString(selected.getId()));
        ViewViewerLessons.details.clear();
        if (result.isSuccessful()) {
            var lesson = result.getValue();
            ViewViewerLessons.details.setText("Lesson ID: " + lesson.getId() + "\nTitle: " + lesson.getTitle()
                    + "\n\nProblem or Situation\n" + lesson.getProblemSituation() + "\n\nLesson Learned\n" + lesson.getLessonLearned());
            ViewViewerLessons.showDetails(true);
        } else search();
        ViewViewerLessons.status.setText(result.getMessage());
    }
}
