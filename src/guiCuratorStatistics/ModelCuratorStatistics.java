package guiCuratorStatistics;

import entityClasses.*;
import lessonManagement.StatisticsService;
import lessonManagement.OperationResult;
import java.util.List;

/*******
 * <p> Title: ModelCuratorStatistics Class. </p>
 * <p> Description: Connects statistics controls to current-role persistent business operations.
 * All model methods are exercised by TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Statistics MVC
 */
public class ModelCuratorStatistics {
    /** Existing shared connection and the same business rules tested without JavaFX. */
    private static final StatisticsService SERVICE = new StatisticsService(applicationMain.FoundationsMain.database);
    private ModelCuratorStatistics() { }
    /**
     * Tested by TP3StatisticsGuiTestingAutomation.
     * @param actor authenticated Curator
     * @return authorized current result or a corrective error
     */
    protected static OperationResult<List<ExperienceRecord>> listSources(String actor) { return SERVICE.listSources(actor); }
    /**
     * Tested by TP3StatisticsGuiTestingAutomation.
     * @param actor authenticated Curator
     * @param destination destination lesson ID
     * @param sourceIds selected source IDs
     * @return authorized current result or a corrective error
     */
    protected static OperationResult<StatisticsPreview> preview(String actor, String destination, List<Long> sourceIds) { return SERVICE.preview(actor, destination, sourceIds); }
    /**
     * Tested by TP3StatisticsGuiTestingAutomation.
     * @param actor authenticated Curator
     * @param request reviewed evidence request
     * @return authorized current result or a corrective error
     */
    protected static OperationResult<StatisticsSummary> save(String actor, StatisticsRequest request) { return SERVICE.save(actor, request); }
    /**
     * Tested by TP3StatisticsGuiTestingAutomation.
     * @param actor authenticated Curator
     * @param destination destination lesson ID
     * @return authorized current result or a corrective error
     */
    protected static OperationResult<List<StatisticsSummary>> listSummaries(String actor, String destination) { return SERVICE.listSummaries(actor, destination); }
    /**
     * Tested by TP3StatisticsGuiTestingAutomation.
     * @param actor authenticated Curator
     * @param summaryId selected saved summary
     * @return authorized current result or a corrective error
     */
    protected static OperationResult<StatisticsSummary> readSummary(String actor, String summaryId) { return SERVICE.readSummary(actor, summaryId); }
    /**
     * Tested by TP3StatisticsGuiTestingAutomation.
     * @param actor authenticated Curator
     * @param summaryId selected saved summary
     * @param visible explicit sharing decision
     * @return authorized current result or a corrective error
     */
    protected static OperationResult<StatisticsSummary> setVisibility(String actor, String summaryId, boolean visible) { return SERVICE.setVisibility(actor, summaryId, visible); }
}
