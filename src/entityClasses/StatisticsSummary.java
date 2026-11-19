package entityClasses;

/*******
 * <p> Title: StatisticsSummary Class. </p>
 * <p> Description: Immutable saved historical summary with Curator-only provenance and live staleness indicator. All public elements are validated by
 * TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics and visibility
 */
public class StatisticsSummary {
    /** Persistent summary key. */
    private final long id;
    /** Owning destination lesson key. */
    private final long destinationLessonId;
    /** Creation time in UTC. */
    private final java.time.Instant createdAt;
    /** Explicit sharing choice; new summaries start hidden. */
    private final boolean visibleToContributor;
    /** Saved aggregate values, never silently recalculated. */
    private final StatisticsValues values;
    /** Historical source inputs. */
    private final java.util.List<StatisticsSourceSnapshot> sources;
    /** Whether any live input changed or disappeared. */
    private final boolean stale;
    /**
     * Construct immutable statistics data. Tested by TP3StatisticsTest.
     * @param id Persistent summary key.
     * @param destinationLessonId Owning destination lesson key.
     * @param createdAt Creation time in UTC.
     * @param visibleToContributor Explicit sharing choice; new summaries start hidden.
     * @param values Saved aggregate values, never silently recalculated.
     * @param sources Historical source inputs.
     * @param stale Whether any live input changed or disappeared.
     */
    public StatisticsSummary(long id, long destinationLessonId, java.time.Instant createdAt, boolean visibleToContributor, StatisticsValues values, java.util.List<StatisticsSourceSnapshot> sources, boolean stale) {
        this.id = id;
        this.destinationLessonId = destinationLessonId;
        this.createdAt = createdAt;
        this.visibleToContributor = visibleToContributor;
        this.values = values;
        this.sources = sources == null ? null : java.util.List.copyOf(sources);
        this.stale = stale;
    }
    /** @return Persistent summary key. Validated by TP3StatisticsTest. */
    public long getId() { return id; }
    /** @return Owning destination lesson key. Validated by TP3StatisticsTest. */
    public long getDestinationLessonId() { return destinationLessonId; }
    /** @return Creation time in UTC. Validated by TP3StatisticsTest. */
    public java.time.Instant getCreatedAt() { return createdAt; }
    /** @return Explicit sharing choice; new summaries start hidden. Validated by TP3StatisticsTest. */
    public boolean isVisibleToContributor() { return visibleToContributor; }
    /** @return Saved aggregate values, never silently recalculated. Validated by TP3StatisticsTest. */
    public StatisticsValues getValues() { return values; }
    /** @return Historical source inputs. Validated by TP3StatisticsTest. */
    public java.util.List<StatisticsSourceSnapshot> getSources() { return sources; }
    /** @return Whether any live input changed or disappeared. Validated by TP3StatisticsTest. */
    public boolean isStale() { return stale; }
    /** @return summary list label; tested by TP3StatisticsGuiTestingAutomation */
    @Override public String toString() { return "Summary " + id + " | " + createdAt + " | "
            + (visibleToContributor ? "Shared" : "Hidden") + (stale ? " | Earlier source data" : ""); }
}
