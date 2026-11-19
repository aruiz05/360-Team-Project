package entityClasses;

/*******
 * <p> Title: SharedStatistics Class. </p>
 * <p> Description: Contributor-safe projection of shared aggregates with no source IDs or foreign raw data. All public elements are validated by
 * TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics and visibility
 */
public class SharedStatistics {
    /** Summary key. */
    private final long id;
    /** Snapshot creation time. */
    private final java.time.Instant createdAt;
    /** Shared aggregate values and units. */
    private final StatisticsValues values;
    /**
     * Construct immutable statistics data. Tested by TP3StatisticsTest.
     * @param id Summary key.
     * @param createdAt Snapshot creation time.
     * @param values Shared aggregate values and units.
     */
    public SharedStatistics(long id, java.time.Instant createdAt, StatisticsValues values) {
        this.id = id;
        this.createdAt = createdAt;
        this.values = values;
    }
    /** @return Summary key. Validated by TP3StatisticsTest. */
    public long getId() { return id; }
    /** @return Snapshot creation time. Validated by TP3StatisticsTest. */
    public java.time.Instant getCreatedAt() { return createdAt; }
    /** @return Shared aggregate values and units. Validated by TP3StatisticsTest. */
    public StatisticsValues getValues() { return values; }
    /** @return labeled shared aggregates; tested by TP3StatisticsGuiTestingAutomation */
    @Override public String toString() { return "Summary " + id + " | " + createdAt + "\n" + values; }
}
