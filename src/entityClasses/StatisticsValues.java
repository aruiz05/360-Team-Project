package entityClasses;

/*******
 * <p> Title: StatisticsValues Class. </p>
 * <p> Description: Aggregate values with elapsed minutes and effort person-minutes kept separate. All public elements are validated by
 * TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics and visibility
 */
public class StatisticsValues {
    /** Number of distinct selected attempts. */
    private final int attemptCount;
    /** Elapsed minutes. */
    private final double meanElapsedMinutes;
    /** Elapsed minutes. */
    private final double medianElapsedMinutes;
    /** Elapsed minutes. */
    private final double minElapsedMinutes;
    /** Elapsed minutes. */
    private final double maxElapsedMinutes;
    /** Effort person-minutes per attempt. */
    private final double meanEffortMinutes;
    /** Effort person-minutes per attempt. */
    private final double medianEffortMinutes;
    /** Effort person-minutes per attempt. */
    private final double minEffortMinutes;
    /** Effort person-minutes per attempt. */
    private final double maxEffortMinutes;
    /**
     * Construct immutable statistics data. Tested by TP3StatisticsTest.
     * @param attemptCount Number of distinct selected attempts.
     * @param meanElapsedMinutes Elapsed minutes.
     * @param medianElapsedMinutes Elapsed minutes.
     * @param minElapsedMinutes Elapsed minutes.
     * @param maxElapsedMinutes Elapsed minutes.
     * @param meanEffortMinutes Effort person-minutes per attempt.
     * @param medianEffortMinutes Effort person-minutes per attempt.
     * @param minEffortMinutes Effort person-minutes per attempt.
     * @param maxEffortMinutes Effort person-minutes per attempt.
     */
    public StatisticsValues(int attemptCount, double meanElapsedMinutes, double medianElapsedMinutes, double minElapsedMinutes, double maxElapsedMinutes, double meanEffortMinutes, double medianEffortMinutes, double minEffortMinutes, double maxEffortMinutes) {
        this.attemptCount = attemptCount;
        this.meanElapsedMinutes = meanElapsedMinutes;
        this.medianElapsedMinutes = medianElapsedMinutes;
        this.minElapsedMinutes = minElapsedMinutes;
        this.maxElapsedMinutes = maxElapsedMinutes;
        this.meanEffortMinutes = meanEffortMinutes;
        this.medianEffortMinutes = medianEffortMinutes;
        this.minEffortMinutes = minEffortMinutes;
        this.maxEffortMinutes = maxEffortMinutes;
    }
    /** @return Number of distinct selected attempts. Validated by TP3StatisticsTest. */
    public int getAttemptCount() { return attemptCount; }
    /** @return Elapsed minutes. Validated by TP3StatisticsTest. */
    public double getMeanElapsedMinutes() { return meanElapsedMinutes; }
    /** @return Elapsed minutes. Validated by TP3StatisticsTest. */
    public double getMedianElapsedMinutes() { return medianElapsedMinutes; }
    /** @return Elapsed minutes. Validated by TP3StatisticsTest. */
    public double getMinElapsedMinutes() { return minElapsedMinutes; }
    /** @return Elapsed minutes. Validated by TP3StatisticsTest. */
    public double getMaxElapsedMinutes() { return maxElapsedMinutes; }
    /** @return Effort person-minutes per attempt. Validated by TP3StatisticsTest. */
    public double getMeanEffortMinutes() { return meanEffortMinutes; }
    /** @return Effort person-minutes per attempt. Validated by TP3StatisticsTest. */
    public double getMedianEffortMinutes() { return medianEffortMinutes; }
    /** @return Effort person-minutes per attempt. Validated by TP3StatisticsTest. */
    public double getMinEffortMinutes() { return minEffortMinutes; }
    /** @return Effort person-minutes per attempt. Validated by TP3StatisticsTest. */
    public double getMaxEffortMinutes() { return maxEffortMinutes; }
    /** @return labeled aggregate values; tested by the statistics GUI suite */
    @Override public String toString() {
        return "Attempts: " + attemptCount + "\nElapsed minutes: mean=" + meanElapsedMinutes
                + ", median=" + medianElapsedMinutes + ", min=" + minElapsedMinutes + ", max=" + maxElapsedMinutes
                + "\nEffort person-minutes: mean=" + meanEffortMinutes + ", median=" + medianEffortMinutes
                + ", min=" + minEffortMinutes + ", max=" + maxEffortMinutes;
    }
}
