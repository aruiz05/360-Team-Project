package lessonManagement;

import database.Database;
import entityClasses.*;
import guiTools.RoleNames;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/*******
 * <p> Title: StatisticsService Class. </p>
 * <p> Description: Current-role statistics review, immutable snapshot persistence and explicit
 * sharing. Preview/save lock source parents and destination in ascending order, then reload
 * inputs. A reviewed fingerprint prevents a stale preview from saving changed evidence.
 * Contributor queries return only shared aggregates for an owned destination. All public
 * methods are validated by TP3StatisticsTest and TP3StatisticsGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Persistent statistics and visibility
 */
public class StatisticsService {
    /** Shared persistent connection; protected operations use the existing transaction wrapper. */
    private final Database database;
    /** @param database shared application or isolated test database; tested by statistics fixtures */
    public StatisticsService(Database database) { this.database = database; }

    /**
     * Curator-only source browser across lessons. Tested by previewAndSave and permissions.
     * @param actor authenticated Curator
     * @return current source attempts with IDs, relationships and units
     */
    public OperationResult<List<ExperienceRecord>> listSources(String actor) {
        return curator(actor, () -> {
            List<ExperienceRecord> sources = new ArrayList<>();
            // The database returns parents ordered by ID; follow the same order as all saves.
            for (var lesson : database.getAllLessons()) if (database.lockLesson(lesson.getId()))
                sources.addAll(database.getExperiencesForLesson(lesson.getId()));
            return OperationResult.success(List.copyOf(sources), "Source experiences loaded.");
        });
    }
    /**
     * Preview without writing; duplicate IDs count once. Tested by previewAndSave and invalidSelections.
     * @param actor authenticated Curator
     * @param destination positive destination lesson ID
     * @param sourceIds selected experience IDs, possibly spanning lessons
     * @return accepted inputs and current aggregates for review, or a corrective error
     */
    public OperationResult<StatisticsPreview> preview(String actor, String destination, List<Long> sourceIds) {
        return curator(actor, () -> prepare(DiscoveryAccess.id(destination), sourceIds));
    }
    /**
     * Save reviewed inputs as one hidden snapshot; no client-supplied calculated value is trusted.
     * Tested by previewAndSave, stalePreview, rollback, and persistenceAcrossProcesses.
     * @param actor authenticated Curator
     * @param request reviewed destination, source IDs and fingerprints
     * @return committed historical summary, or an error requiring renewed preview/review
     */
    public OperationResult<StatisticsSummary> save(String actor, StatisticsRequest request) {
        return curator(actor, () -> {
            if (request == null || request.getReviewedFingerprints() == null)
                return OperationResult.failure("Preview and review the selected evidence before saving.");
            var prepared = prepare(request.getDestinationLessonId(), request.getSourceExperienceIds());
            if (!prepared.isSuccessful()) return OperationResult.failure(prepared.getMessage());
            StatisticsPreview preview = prepared.getValue();
            if (!preview.toRequest().getReviewedFingerprints().equals(request.getReviewedFingerprints()))
                return OperationResult.failure("Source inputs changed since preview. Preview again and review the new values before saving.");
            var summary = new StatisticsSummary(0, preview.getDestinationLessonId(), Instant.now(), false,
                    preview.getValues(), preview.getSources(), false);
            long id = database.insertStatisticsSummary(summary);
            return OperationResult.success(database.getStatisticsSummary(id), "Statistics snapshot saved hidden. Share it explicitly when ready.");
        });
    }
    /**
     * Read saved snapshots without recalculating their stored values. Tested by sourceChanges.
     * @param actor authenticated Curator
     * @param destination destination lesson ID
     * @return immutable snapshots with current live-input staleness indicators
     */
    public OperationResult<List<StatisticsSummary>> listSummaries(String actor, String destination) {
        return curator(actor, () -> {
            long id = DiscoveryAccess.id(destination);
            if (id < 0 || !database.lockLesson(id)) return DiscoveryAccess.missing();
            List<StatisticsSummary> result = new ArrayList<>();
            for (var summary : database.getStatisticsSummaries(id)) result.add(withStaleness(summary));
            return OperationResult.success(List.copyOf(result), "Saved snapshots loaded.");
        });
    }
    /**
     * Reload a selected snapshot. Tested by sourceChanges and invalidSelections.
     * @param actor authenticated Curator
     * @param summaryId positive summary key
     * @return saved evidence and staleness, or an error
     */
    public OperationResult<StatisticsSummary> readSummary(String actor, String summaryId) {
        return curator(actor, () -> {
            var summary = selectedSummary(summaryId);
            return summary == null ? OperationResult.failure("Select an existing saved summary and refresh.")
                    : OperationResult.success(withStaleness(summary), "Saved snapshot loaded.");
        });
    }
    /**
     * Sharing changes only the persisted flag, never calculations or historical evidence.
     * Tested by visibilityAndPrivacy, permissions, rollback and persistenceAcrossProcesses.
     * @param actor authenticated Curator
     * @param summaryId selected summary key
     * @param visible explicit sharing choice
     * @return saved summary or a corrective error
     */
    public OperationResult<StatisticsSummary> setVisibility(String actor, String summaryId, boolean visible) {
        return curator(actor, () -> {
            var summary = selectedSummary(summaryId);
            if (summary == null || !database.updateSummaryVisibility(summary.getId(), visible))
                return OperationResult.failure("Select an existing saved summary and refresh.");
            return OperationResult.success(withStaleness(database.getStatisticsSummary(summary.getId())),
                    visible ? "Summary shared with the destination lesson's Contributor." : "Summary hidden from Contributors.");
        });
    }
    /**
     * Return only shared aggregate values; foreign raw inputs and source IDs are excluded.
     * Tested by visibilityAndPrivacy, permissions and persistenceAcrossProcesses.
     * @param actor authenticated Contributor
     * @param destination owned destination lesson key
     * @return shared aggregate projections or an ownership/access error
     */
    public OperationResult<List<SharedStatistics>> sharedSummaries(String actor, String destination) {
        return DiscoveryAccess.authorized(database, actor, RoleNames.CONTRIBUTOR, () -> {
            long id = DiscoveryAccess.id(destination);
            if (id < 0 || !database.lockLesson(id)) return DiscoveryAccess.missing();
            var lesson = database.getLesson(id);
            if (!lesson.getOwner().equals(actor)) return OperationResult.failure("You can only read shared statistics for your own lessons.");
            return OperationResult.success(List.copyOf(database.getSharedStatistics(id)), "Shared statistics loaded.");
        });
    }
    private OperationResult<StatisticsPreview> prepare(long destination, List<Long> ids) throws SQLException {
        if (destination <= 0) return DiscoveryAccess.missing();
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0))
            return OperationResult.failure("Select valid existing experience IDs before previewing.");
        TreeSet<Long> unique = new TreeSet<>(ids);
        if (unique.size() < 2) return OperationResult.failure("Select at least two distinct experience records.");
        TreeSet<Long> parents = new TreeSet<>(); parents.add(destination);
        for (long id : unique) {
            var source = database.getExperience(id);
            if (source == null) return OperationResult.failure("A selected experience no longer exists. Refresh the source list.");
            parents.add(source.getLessonId());
        }
        for (long parent : parents) if (!database.lockLesson(parent))
            return OperationResult.failure("A source or destination lesson no longer exists. Refresh the selection.");
        List<StatisticsSourceSnapshot> sources = new ArrayList<>();
        for (long id : unique) {
            var current = database.getExperience(id);
            if (current == null || !parents.contains(current.getLessonId()))
                return OperationResult.failure("A selected source changed or disappeared. Refresh and preview again.");
            var captured = capture(current);
            if (!captured.isSuccessful()) return OperationResult.failure(captured.getMessage());
            sources.add(captured.getValue());
        }
        var calculated = StatisticsCalculator.calculate(sources);
        return calculated.isSuccessful() ? OperationResult.success(new StatisticsPreview(destination,
                calculated.getValue(), sources), "Preview calculated. Review before saving.") : OperationResult.failure(calculated.getMessage());
    }
    private OperationResult<StatisticsSourceSnapshot> capture(ExperienceRecord record) {
        if (!Double.isFinite(record.getElapsedMinutes()) || record.getElapsedMinutes() < 0
                || record.getTeamEfforts() == null || record.getTeamEfforts().isEmpty())
            return OperationResult.failure("Every source needs finite nonnegative elapsed time and a valid nonempty effort list.");
        BigDecimal effort = BigDecimal.ZERO;
        for (var member : record.getTeamEfforts()) {
            if (member == null || member.getMemberName() == null || member.getMemberName().isBlank()
                    || !Double.isFinite(member.getEffortMinutes()) || member.getEffortMinutes() < 0)
                return OperationResult.failure("Correct missing member names and invalid effort values before calculating statistics.");
            effort = effort.add(BigDecimal.valueOf(member.getEffortMinutes()));
        }
        double total = effort.doubleValue();
        if (!Double.isFinite(total)) return OperationResult.failure("The total effort is too large. Correct the source values before calculating.");
        return OperationResult.success(new StatisticsSourceSnapshot(0, 0, record.getLessonId(), record.getId(), record.getId(),
                record.getElapsedMinutes(), total, fingerprint(record)), "Source captured.");
    }
    private String fingerprint(ExperienceRecord record) {
        // Length prefixes avoid ambiguous delimiters in member names; order is significant.
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            try (var output = new java.io.DataOutputStream(bytes)) {
                output.writeLong(record.getLessonId()); output.writeLong(record.getId()); output.writeDouble(record.getElapsedMinutes());
                output.writeInt(record.getTeamEfforts().size());
                for (var member : record.getTeamEfforts()) {
                    byte[] name = member.getMemberName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    output.writeInt(name.length); output.write(name); output.writeDouble(member.getEffortMinutes());
                }
            }
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Required snapshot fingerprint support is unavailable.", e);
        }
    }
    private StatisticsSummary selectedSummary(String text) throws SQLException {
        long id = DiscoveryAccess.id(text);
        if (id < 0) return null;
        var summary = database.getStatisticsSummary(id);
        if (summary == null || !database.lockLesson(summary.getDestinationLessonId())) return null;
        return database.getStatisticsSummary(id);
    }
    private StatisticsSummary withStaleness(StatisticsSummary summary) throws SQLException {
        boolean stale = false;
        // This is an advisory comparison with live inputs, never a rewrite of historical results.
        for (var source : summary.getSources()) {
            var live = source.getLiveExperienceId() == null ? null : database.getExperience(source.getLiveExperienceId());
            var current = live == null ? null : capture(live);
            if (current == null || !current.isSuccessful()
                    || !source.getSourceFingerprint().equals(current.getValue().getSourceFingerprint())) stale = true;
        }
        return new StatisticsSummary(summary.getId(), summary.getDestinationLessonId(), summary.getCreatedAt(),
                summary.isVisibleToContributor(), summary.getValues(), summary.getSources(), stale);
    }
    private <T> OperationResult<T> curator(String actor, Database.TransactionWork<OperationResult<T>> work) {
        return DiscoveryAccess.authorized(database, actor, RoleNames.CURATOR, work);
    }
}
