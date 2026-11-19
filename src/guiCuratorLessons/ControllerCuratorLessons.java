package guiCuratorLessons;

import java.util.Arrays;
import java.util.List;
import lessonManagement.LessonDraft;
import lessonManagement.LessonLockState;
import lessonManagement.OperationResult;

/*******
 * <p> Title: ControllerCuratorLessons Class. </p>
 * <p> Description: Submits explicitly reviewed drafts, retains rejected edits, and refreshes
 * full/subset collections by stable ID after successful saves. Reload Selected discards drafts.
 * Every action is tested by TP3DiscoveryGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Curator core operations
 */
public class ControllerCuratorLessons {
    /** Avoid selection listeners during list replacement; never select another record silently. */
    protected static boolean refreshing;
    private ControllerCuratorLessons() { }
    private static String actor() { return ViewCuratorLessons.user.getUserName(); }
    private static String id() {
        var selected = ViewCuratorLessons.lessons.getSelectionModel().getSelectedItem();
        return selected == null ? "" : Long.toString(selected.getId());
    }
    /** Refresh from storage and preserve only the same matching ID; tested by discovery GUI filters. */
    protected static void search() {
        String selected = id(); var result = ModelCuratorLessons.search(actor(), ViewCuratorLessons.query.getText());
        refreshing = true;
        try {
            ViewCuratorLessons.lessons.getItems().clear();
            if (result.isSuccessful()) {
                ViewCuratorLessons.lessons.getItems().setAll(result.getValue());
                result.getValue().stream().filter(x -> Long.toString(x.getId()).equals(selected)).findFirst()
                        .ifPresent(x -> ViewCuratorLessons.lessons.getSelectionModel().select(x));
            }
        } finally { refreshing = false; }
        read(); ViewCuratorLessons.status.setText(result.isSuccessful()
                ? result.getValue().size() + " lesson(s). Select one to review." : result.getMessage());
    }
    /** Clear private controls after losing selection/access; tested by discovery GUI revoked-role checks. */
    protected static void clear() {
        ViewCuratorLessons.title.clear(); ViewCuratorLessons.problem.clear(); ViewCuratorLessons.text.clear();
        ViewCuratorLessons.terms.clear(); ViewCuratorLessons.experiences.clear();
        ViewCuratorLessons.wholeLock.setSelected(false); ViewCuratorLessons.titleLock.setSelected(false);
        ViewCuratorLessons.problemLock.setSelected(false); ViewCuratorLessons.textLock.setSelected(false);
        ViewCuratorLessons.identity.setText("Select a lesson to review."); ViewCuratorLessons.tabs.setDisable(true);
    }
    /** Reload current core and evidence; tested by discovery GUI review and Reload Selected checks. */
    protected static void read() {
        String selected = id(); clear(); if (selected.isEmpty()) return;
        var core = ModelCuratorLessons.read(actor(), selected); var terms = ModelCuratorLessons.terms(actor(), selected);
        var attempts = ModelCuratorLessons.experiences(actor(), selected);
        if (!core.isSuccessful() || !terms.isSuccessful() || !attempts.isSuccessful()) {
            ViewCuratorLessons.status.setText(!core.isSuccessful() ? core.getMessage() : !terms.isSuccessful() ? terms.getMessage() : attempts.getMessage()); return;
        }
        var lesson = core.getValue(); ViewCuratorLessons.tabs.setDisable(false);
        ViewCuratorLessons.identity.setText("Lesson ID: " + lesson.getId() + " | Owner: " + lesson.getOwner());
        ViewCuratorLessons.title.setText(lesson.getTitle()); ViewCuratorLessons.problem.setText(lesson.getProblemSituation()); ViewCuratorLessons.text.setText(lesson.getLessonLearned());
        ViewCuratorLessons.terms.setText(String.join("\n", terms.getValue().stream().map(x -> x.getTerm()).toList()));
        ViewCuratorLessons.wholeLock.setSelected(lesson.isLessonLocked()); ViewCuratorLessons.titleLock.setSelected(lesson.isTitleLocked());
        ViewCuratorLessons.problemLock.setSelected(lesson.isProblemSituationLocked()); ViewCuratorLessons.textLock.setSelected(lesson.isLessonLearnedLocked());
        StringBuilder detail = new StringBuilder();
        for (var experience : attempts.getValue()) {
            detail.append("Experience ID: ").append(experience.getId()).append(" | Lesson ID: ").append(experience.getLessonId())
                .append(" | Owner: ").append(experience.getOwner()).append("\nWhat: ").append(experience.getWhatWasDone())
                .append("\nHow: ").append(experience.getHowItWasDone()).append("\nElapsed minutes: ").append(experience.getElapsedMinutes())
                .append("\nLocks - record: ").append(experience.isExperienceLocked()).append(", what: ").append(experience.isWhatWasDoneLocked())
                .append(", how: ").append(experience.isHowItWasDoneLocked()).append(", elapsed: ").append(experience.isElapsedTimeLocked())
                .append(", effort: ").append(experience.isTeamEffortLocked()).append("\n");
            for (var effort : experience.getTeamEfforts()) detail.append(effort.getMemberName()).append(": ").append(effort.getEffortMinutes()).append(" person-minutes\n");
            detail.append("\n");
        }
        ViewCuratorLessons.experiences.setText(detail.isEmpty() ? "No experiences for this lesson." : detail.toString());
        ViewCuratorLessons.status.setText(core.getMessage());
    }
    /** Submit reviewed core; tested by discovery GUI valid save and rejected draft retention. */
    protected static void refine() {
        complete(ModelCuratorLessons.refine(actor(), id(), new LessonDraft(ViewCuratorLessons.title.getText(), ViewCuratorLessons.problem.getText(), ViewCuratorLessons.text.getText())));
    }
    /**
     * Tested by discovery GUI normalization, invalid entries, and clearing a filtered result.
     * @param clear true only for explicit Clear Terms
     */
    protected static void saveTerms(boolean clear) {
        List<String> terms = clear ? List.of() : Arrays.asList(ViewCuratorLessons.terms.getText().split("\\R", -1));
        complete(ModelCuratorLessons.saveTerms(actor(), id(), terms));
    }
    /** Submit all independent flags; tested by discovery GUI protection and Contributor enforcement. */
    protected static void locks() {
        complete(ModelCuratorLessons.locks(actor(), id(), new LessonLockState(ViewCuratorLessons.wholeLock.isSelected(),
                ViewCuratorLessons.titleLock.isSelected(), ViewCuratorLessons.problemLock.isSelected(), ViewCuratorLessons.textLock.isSelected())));
    }
    /** Open statistics for the stable selected destination; tested by TP3StatisticsGuiTestingAutomation. */
    protected static void statistics() {
        String selected = id();
        if (selected.isEmpty()) { ViewCuratorLessons.status.setText("Select a destination lesson first."); return; }
        guiCuratorStatistics.ViewCuratorStatistics.displayStatistics(ViewCuratorLessons.stage, ViewCuratorLessons.user, Long.parseLong(selected));
    }
    /** Open Curator AI with the current unsaved core; tested by TP3AiGuiTestingAutomation. */
    protected static void ai() {
        if (id().isEmpty()) { ViewCuratorLessons.status.setText("Select a lesson for AI assistance."); return; }
        guiAiAssistance.ViewAiAssistance.displayAssistance(ViewCuratorLessons.stage, ViewCuratorLessons.user,
                guiTools.RoleNames.CURATOR, Long.parseLong(id()), new LessonDraft(ViewCuratorLessons.title.getText(),
                ViewCuratorLessons.problem.getText(), ViewCuratorLessons.text.getText()));
    }
    private static void complete(OperationResult<?> result) {
        if (result.isSuccessful()) search(); ViewCuratorLessons.status.setText(result.getMessage());
    }
}
