package entityClasses;

/*******
 * <p> Title: StatisticsSourceSnapshot Class. </p>
 * <p> Description: Historical calculation inputs survive later changes or deletion of live sources. All public elements are validated by
 * TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics and visibility
 */
public class StatisticsSourceSnapshot {
    /** Snapshot database key; zero before saving. */
    private final long id;
    /** Parent summary key; zero during preview. */
    private final long summaryId;
    /** Original source lesson ID retained as provenance. */
    private final long originalLessonId;
    /** Original distinct source experience ID. */
    private final long originalExperienceId;
    /** Nullable live experience reference; deletion retains historical inputs. */
    private final Long liveExperienceId;
    /** Captured elapsed minutes. */
    private final double elapsedMinutes;
    /** Captured total member effort in person-minutes. */
    private final double effortTotalMinutes;
    /** Fingerprint of relevant ordered source inputs. */
    private final String sourceFingerprint;
    /**
     * Construct immutable statistics data. Tested by TP3StatisticsTest.
     * @param id Snapshot database key; zero before saving.
     * @param summaryId Parent summary key; zero during preview.
     * @param originalLessonId Original source lesson ID retained as provenance.
     * @param originalExperienceId Original distinct source experience ID.
     * @param liveExperienceId Nullable live experience reference; deletion retains historical inputs.
     * @param elapsedMinutes Captured elapsed minutes.
     * @param effortTotalMinutes Captured total member effort in person-minutes.
     * @param sourceFingerprint Fingerprint of relevant ordered source inputs.
     */
    public StatisticsSourceSnapshot(long id, long summaryId, long originalLessonId, long originalExperienceId, Long liveExperienceId, double elapsedMinutes, double effortTotalMinutes, String sourceFingerprint) {
        this.id = id;
        this.summaryId = summaryId;
        this.originalLessonId = originalLessonId;
        this.originalExperienceId = originalExperienceId;
        this.liveExperienceId = liveExperienceId;
        this.elapsedMinutes = elapsedMinutes;
        this.effortTotalMinutes = effortTotalMinutes;
        this.sourceFingerprint = sourceFingerprint;
    }
    /** @return Snapshot database key; zero before saving. Validated by TP3StatisticsTest. */
    public long getId() { return id; }
    /** @return Parent summary key; zero during preview. Validated by TP3StatisticsTest. */
    public long getSummaryId() { return summaryId; }
    /** @return Original source lesson ID retained as provenance. Validated by TP3StatisticsTest. */
    public long getOriginalLessonId() { return originalLessonId; }
    /** @return Original distinct source experience ID. Validated by TP3StatisticsTest. */
    public long getOriginalExperienceId() { return originalExperienceId; }
    /** @return Nullable live experience reference; deletion retains historical inputs. Validated by TP3StatisticsTest. */
    public Long getLiveExperienceId() { return liveExperienceId; }
    /** @return Captured elapsed minutes. Validated by TP3StatisticsTest. */
    public double getElapsedMinutes() { return elapsedMinutes; }
    /** @return Captured total member effort in person-minutes. Validated by TP3StatisticsTest. */
    public double getEffortTotalMinutes() { return effortTotalMinutes; }
    /** @return Fingerprint of relevant ordered source inputs. Validated by TP3StatisticsTest. */
    public String getSourceFingerprint() { return sourceFingerprint; }
}
