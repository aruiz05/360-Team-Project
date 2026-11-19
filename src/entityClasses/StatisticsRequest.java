package entityClasses;

/*******
 * <p> Title: StatisticsRequest Class. </p>
 * <p> Description: Reviewed source selection and fingerprints; calculation values are always recomputed on save. All public elements are validated by
 * TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics and visibility
 */
public class StatisticsRequest {
    /** Destination lesson key. */
    private final long destinationLessonId;
    /** Selected experience keys; duplicates are counted once. */
    private final java.util.List<Long> sourceExperienceIds;
    /** Fingerprint for every distinct reviewed source. */
    private final java.util.Map<Long, String> reviewedFingerprints;
    /**
     * Construct immutable statistics data. Tested by TP3StatisticsTest.
     * @param destinationLessonId Destination lesson key.
     * @param sourceExperienceIds Selected experience keys; duplicates are counted once.
     * @param reviewedFingerprints Fingerprint for every distinct reviewed source.
     */
    public StatisticsRequest(long destinationLessonId, java.util.List<Long> sourceExperienceIds, java.util.Map<Long, String> reviewedFingerprints) {
        this.destinationLessonId = destinationLessonId;
        this.sourceExperienceIds = sourceExperienceIds == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(sourceExperienceIds));
        this.reviewedFingerprints = reviewedFingerprints == null ? null : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(reviewedFingerprints));
    }
    /** @return Destination lesson key. Validated by TP3StatisticsTest. */
    public long getDestinationLessonId() { return destinationLessonId; }
    /** @return Selected experience keys; duplicates are counted once. Validated by TP3StatisticsTest. */
    public java.util.List<Long> getSourceExperienceIds() { return sourceExperienceIds; }
    /** @return Fingerprint for every distinct reviewed source. Validated by TP3StatisticsTest. */
    public java.util.Map<Long, String> getReviewedFingerprints() { return reviewedFingerprints; }
}
