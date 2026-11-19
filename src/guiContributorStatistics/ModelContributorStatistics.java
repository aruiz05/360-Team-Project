package guiContributorStatistics;

import entityClasses.SharedStatistics;
import lessonManagement.OperationResult;
import lessonManagement.StatisticsService;
import java.util.List;

/*******
 * <p> Title: ModelContributorStatistics Class. </p>
 * <p> Description: Requests only authorized owned shared aggregates. Tested by
 * TP3StatisticsGuiTestingAutomation visibility and ownership scenarios.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Shared statistics MVC
 */
public class ModelContributorStatistics {
    /** Shared business service enforces current ownership and sharing before any data is returned. */
    private static final StatisticsService SERVICE = new StatisticsService(applicationMain.FoundationsMain.database);
    private ModelContributorStatistics() { }
    /**
     * @param actor authenticated Contributor
     * @param lessonId selected owned lesson
     * @return shared aggregates only; tested by statistics GUI share/hide scenarios
     */
    protected static OperationResult<List<SharedStatistics>> read(String actor, long lessonId) {
        return SERVICE.sharedSummaries(actor, Long.toString(lessonId));
    }
}
