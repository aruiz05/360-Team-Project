package entityClasses;

/*******
 * <p> Title: StatisticsPreview Class. </p>
 * <p> Description: Unsaved current calculation and source evidence ready for explicit review. All public elements are validated by
 * TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics and visibility
 */
public class StatisticsPreview {
    /** Destination lesson key. */
    private final long destinationLessonId;
    /** Calculated aggregates. */
    private final StatisticsValues values;
    /** Immutable accepted source inputs. */
    private final java.util.List<StatisticsSourceSnapshot> sources;
    /**
     * Construct immutable statistics data. Tested by TP3StatisticsTest.
     * @param destinationLessonId Destination lesson key.
     * @param values Calculated aggregates.
     * @param sources Immutable accepted source inputs.
     */
    public StatisticsPreview(long destinationLessonId, StatisticsValues values, java.util.List<StatisticsSourceSnapshot> sources) {
        this.destinationLessonId = destinationLessonId;
        this.values = values;
        this.sources = sources == null ? null : java.util.List.copyOf(sources);
    }
    /** @return Destination lesson key. Validated by TP3StatisticsTest. */
    public long getDestinationLessonId() { return destinationLessonId; }
    /** @return Calculated aggregates. Validated by TP3StatisticsTest. */
    public StatisticsValues getValues() { return values; }
    /** @return Immutable accepted source inputs. Validated by TP3StatisticsTest. */
    public java.util.List<StatisticsSourceSnapshot> getSources() { return sources; }
    /** @return reviewed request; tested by TP3StatisticsTest.previewAndSave */
    public StatisticsRequest toRequest() {
        java.util.Map<Long, String> fingerprints = new java.util.LinkedHashMap<>();
        for (var source : sources) fingerprints.put(source.getOriginalExperienceId(), source.getSourceFingerprint());
        return new StatisticsRequest(destinationLessonId, java.util.List.copyOf(fingerprints.keySet()), fingerprints);
    }
}
