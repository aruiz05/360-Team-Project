package entityClasses;

/*******
 * <p> Title: ViewerLesson Class. </p>
 * <p> Description: Read-only lesson core projection. Owners, raw experiences, and private role data are excluded. Validated by TP3DiscoveryTest.discoveryAndPrivacy and TP3DiscoveryGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 discovery and Curator operations
 */
public class ViewerLesson {
    /** Stable database key used for fresh detail selection. */
    private final long id;
    /** The three reusable core text fields visible to Viewers. */
    private final String title, problemSituation, lessonLearned;
    /**
     * Project only permitted core values. Tested by TP3DiscoveryTest.discoveryAndPrivacy.
     * @param lesson current persistent lesson
     */
    public ViewerLesson(LessonsLearned lesson) {
        id = lesson.getId(); title = lesson.getTitle();
        problemSituation = lesson.getProblemSituation(); lessonLearned = lesson.getLessonLearned();
    }
    /** @return unique lesson ID; tested by discoveryAndPrivacy */
    public long getId() { return id; }
    /** @return current title; tested by discoveryAndPrivacy */
    public String getTitle() { return title; }
    /** @return current problem; tested by discoveryAndPrivacy */
    public String getProblemSituation() { return problemSituation; }
    /** @return current lesson text; tested by discoveryAndPrivacy */
    public String getLessonLearned() { return lessonLearned; }
    /** @return ID and title for lists; tested by TP3DiscoveryGuiTestingAutomation */
    @Override public String toString() { return id + " - " + title; }
}
