package lessonManagement;

import database.Database;
import entityClasses.ExperienceRecord;
import entityClasses.LessonsLearned;
import entityClasses.SearchTerm;
import guiTools.RoleNames;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/*******
 * <p> Title: CuratorLessonService Class. </p>
 * <p> Description: Cross-Contributor review, reviewed manual core refinement, normalized
 * related terms, and four independent protections. Each operation requires the current stored
 * Curator role. Writes use the same parent lock as Contributor operations, preserving IDs,
 * owners, and related records. Tested by TP3DiscoveryTest and TP3DiscoveryGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Curator core operations
 */
public class CuratorLessonService {
    /** Existing shared database; no second application connection is introduced. */
    private final Database database;
    /**
     * Tested by TP3DiscoveryTest setup and unavailableStorage.
     * @param database shared application or isolated persistent test database
     */
    public CuratorLessonService(Database database) { this.database = database; }
    /**
     * Tested by discoveryAndPrivacy and collectionSizes.
     * @param actor authenticated Curator
     * @return all current lesson cores and ownership or an error
     */
    public OperationResult<List<LessonsLearned>> listLessons(String actor) { return searchLessons(actor, ""); }
    /**
     * Tested by andSearch, refreshedSearch and revokedRoles.
     * @param actor authenticated Curator
     * @param query optional AND words
     * @return a fresh immutable subset or an access error
     */
    public OperationResult<List<LessonsLearned>> searchLessons(String actor, String query) {
        return authorized(actor, () -> OperationResult.success(DiscoveryAccess.search(database, query), "Lessons loaded."));
    }
    /**
     * Tested by discoveryAndPrivacy, invalidSelections and deletedSelections.
     * @param actor authenticated Curator
     * @param lessonId selected ID
     * @return current lesson or a helpful error
     */
    public OperationResult<LessonsLearned> readLesson(String actor, String lessonId) {
        return authorized(actor, () -> {
            long id = DiscoveryAccess.id(lessonId);
            if (id < 0) return DiscoveryAccess.missing();
            var lesson = database.getLesson(id);
            return lesson == null ? DiscoveryAccess.missing() : OperationResult.success(lesson, "Lesson loaded.");
        });
    }
    /**
     * Tested by experienceReview and wrongRoles.
     * @param actor authenticated Curator
     * @param lessonId parent selection
     * @return related raw attempts, effort values and locks, for Curator review only
     */
    public OperationResult<List<ExperienceRecord>> readExperiences(String actor, String lessonId) {
        return authorized(actor, () -> {
            long id = DiscoveryAccess.id(lessonId);
            if (id < 0 || !database.lockLesson(id)) return DiscoveryAccess.missing();
            return OperationResult.success(List.copyOf(database.getExperiencesForLesson(id)), "Experiences loaded.");
        });
    }
    /**
     * Tested by termsNormalizationAndClear and invalidSelections.
     * @param actor authenticated Curator
     * @param lessonId parent selection
     * @return saved terms or an error
     */
    public OperationResult<List<SearchTerm>> readTerms(String actor, String lessonId) {
        return authorized(actor, () -> {
            long id = DiscoveryAccess.id(lessonId);
            if (id < 0 || !database.lockLesson(id)) return DiscoveryAccess.missing();
            return OperationResult.success(List.copyOf(database.getSearchTerms(id)), "Terms loaded.");
        });
    }
    /**
     * Atomically replace the reviewed set; an explicit empty list clears it.
     * Tested by termsNormalizationAndClear, invalidTerms, sqlFailureRollback and persistenceAcrossProcesses.
     * @param actor authenticated Curator
     * @param lessonId parent selection
     * @param proposed reviewed terms; null or any blank entry rejects the whole replacement
     * @return normalized unique saved terms or a corrective error with no saved changes
     */
    public OperationResult<List<SearchTerm>> saveTerms(String actor, String lessonId, List<String> proposed) {
        return authorized(actor, () -> {
            long id = DiscoveryAccess.id(lessonId);
            if (id < 0 || !database.lockLesson(id)) return DiscoveryAccess.missing();
            if (proposed == null) return OperationResult.failure("Provide a reviewed term list; use Clear Terms to remove all terms.");
            TreeSet<String> unique = new TreeSet<>();
            for (String text : proposed) {
                if (text == null || text.isBlank()) return OperationResult.failure(
                        "Remove blank term entries before saving; use Clear Terms to remove all terms.");
                String term = text.trim().toLowerCase(Locale.ROOT);
                if (term.length() > 500) return OperationResult.failure("Each term must be at most 500 characters.");
                unique.add(term);
            }
            List<SearchTerm> terms = unique.stream().map(term -> new SearchTerm(id, term)).toList();
            database.replaceSearchTerms(id, terms);
            return OperationResult.success(List.copyOf(database.getSearchTerms(id)), "Search terms saved.");
        });
    }
    /**
     * Refine protected core through an authorized Curator path without changing Contributor ownership.
     * Tested by refinementPreservesRelationships, invalidCore, serviceFailureRollback and persistenceAcrossProcesses.
     * @param actor authenticated Curator
     * @param lessonId parent selection
     * @param draft reviewed required core fields
     * @return current saved core or a corrective error
     */
    public OperationResult<LessonsLearned> refineLesson(String actor, String lessonId, LessonDraft draft) {
        return authorized(actor, () -> {
            long id = DiscoveryAccess.id(lessonId);
            if (id < 0 || !database.lockLesson(id)) return DiscoveryAccess.missing();
            String validation = LessonValidator.validate(draft);
            if (!validation.isEmpty()) return OperationResult.failure(validation);
            if (draft.getTitle().trim().length() > 500)
                return OperationResult.failure("Shorten the title to 500 characters or fewer.");
            var current = database.getLesson(id);
            var replacement = new LessonsLearned(id, current.getOwner(), draft.getTitle().trim(),
                    draft.getProblemSituation().trim(), draft.getLessonLearned().trim(), current.isLessonLocked(),
                    current.isTitleLocked(), current.isProblemSituationLocked(), current.isLessonLearnedLocked());
            if (!database.updateLesson(replacement)) return DiscoveryAccess.missing();
            return OperationResult.success(database.getLesson(id), "Reviewed lesson saved.");
        });
    }
    /**
     * Save all four flags atomically. Unlocking the whole record does not clear independent flags.
     * Tested by independentLocks, wholeLessonProtection, concurrentLock and serviceFailureRollback.
     * @param actor authenticated Curator
     * @param lessonId selected parent
     * @param locks reviewed whole-record and field decisions
     * @return current saved lesson or a corrective error
     */
    public OperationResult<LessonsLearned> setLessonLocks(String actor, String lessonId, LessonLockState locks) {
        return authorized(actor, () -> {
            long id = DiscoveryAccess.id(lessonId);
            if (id < 0 || !database.lockLesson(id)) return DiscoveryAccess.missing();
            if (locks == null) return OperationResult.failure("Select the complete lock settings before saving.");
            if (!database.updateLessonLocks(id, locks)) return DiscoveryAccess.missing();
            return OperationResult.success(database.getLesson(id), "Lesson protections saved.");
        });
    }
    private <T> OperationResult<T> authorized(String actor, Database.TransactionWork<OperationResult<T>> work) {
        return DiscoveryAccess.authorized(database, actor, RoleNames.CURATOR, work);
    }
}
