package lessonManagement;

/*******
 * <p> Title: LessonLockState Class. </p>
 * <p> Description: Immutable independent whole-lesson and core-field protections. Validated by TP3DiscoveryTest.independentLocks and wholeLessonProtection.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 discovery and Curator operations
 */
public class LessonLockState {
    /** Whole-record protection is independent of each field's persisted flag. */
    private final boolean lessonLocked, titleLocked, problemSituationLocked, lessonLearnedLocked;
    /**
     * Tested by TP3DiscoveryTest.independentLocks.
     * @param lessonLocked whole lesson protection
     * @param titleLocked title protection
     * @param problemSituationLocked problem protection
     * @param lessonLearnedLocked lesson text protection
     */
    public LessonLockState(boolean lessonLocked, boolean titleLocked,
            boolean problemSituationLocked, boolean lessonLearnedLocked) {
        this.lessonLocked = lessonLocked; this.titleLocked = titleLocked;
        this.problemSituationLocked = problemSituationLocked; this.lessonLearnedLocked = lessonLearnedLocked;
    }
    /** @return whole-record flag; tested by wholeLessonProtection */
    public boolean isLessonLocked() { return lessonLocked; }
    /** @return title flag; tested by independentLocks */
    public boolean isTitleLocked() { return titleLocked; }
    /** @return problem flag; tested by independentLocks */
    public boolean isProblemSituationLocked() { return problemSituationLocked; }
    /** @return lesson text flag; tested by independentLocks */
    public boolean isLessonLearnedLocked() { return lessonLearnedLocked; }
}
