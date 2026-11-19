package lessonManagement;

import entityClasses.StatisticsSourceSnapshot;
import entityClasses.StatisticsValues;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.List;

/*******
 * <p> Title: StatisticsCalculator Class. </p>
 * <p> Description: Pure count, mean, median, minimum and maximum calculation. Decimal
 * accumulation avoids overflow of intermediate sums when the final mean is representable.
 * Validated by TP3StatisticsTest.calculationBoundaries and invalidNumbers.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics
 */
public class StatisticsCalculator {
    private StatisticsCalculator() { }
    /**
     * Reject invalid or duplicate attempts instead of silently dropping calculation inputs.
     * @param sources at least two distinct accepted source snapshots
     * @return aggregate values or a corrective error; tested by calculationBoundaries and invalidNumbers
     */
    public static OperationResult<StatisticsValues> calculate(List<StatisticsSourceSnapshot> sources) {
        if (sources == null || sources.size() < 2) return OperationResult.failure("Select at least two distinct experience records.");
        var seen = new HashSet<Long>();
        for (var source : sources) {
            if (source == null || source.getOriginalExperienceId() <= 0 || source.getOriginalLessonId() <= 0
                    || !seen.add(source.getOriginalExperienceId()) || !valid(source.getElapsedMinutes()) || !valid(source.getEffortTotalMinutes()))
                return OperationResult.failure("Use distinct existing attempts with finite, nonnegative elapsed and effort values.");
        }
        double[] elapsed = sources.stream().mapToDouble(StatisticsSourceSnapshot::getElapsedMinutes).sorted().toArray();
        double[] effort = sources.stream().mapToDouble(StatisticsSourceSnapshot::getEffortTotalMinutes).sorted().toArray();
        double[] timeStats = series(elapsed), effortStats = series(effort);
        return OperationResult.success(new StatisticsValues(sources.size(), timeStats[0], timeStats[1], timeStats[2], timeStats[3],
                effortStats[0], effortStats[1], effortStats[2], effortStats[3]), "Statistics calculated.");
    }
    private static boolean valid(double value) { return Double.isFinite(value) && value >= 0; }
    private static double[] series(double[] sorted) {
        BigDecimal total = BigDecimal.ZERO;
        for (double value : sorted) total = total.add(BigDecimal.valueOf(value));
        int n = sorted.length;
        double mean = total.divide(BigDecimal.valueOf(n), MathContext.DECIMAL128).doubleValue();
        double median = n % 2 == 1 ? sorted[n / 2] : BigDecimal.valueOf(sorted[n / 2 - 1])
                .add(BigDecimal.valueOf(sorted[n / 2])).divide(BigDecimal.valueOf(2)).doubleValue();
        return new double[] { mean, median, sorted[0], sorted[n - 1] };
    }
}
