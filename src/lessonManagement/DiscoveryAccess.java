package lessonManagement;

import database.Database;
import entityClasses.LessonsLearned;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import roleManagement.RoleAccessService;

/*******
 * <p> Title: DiscoveryAccess Class. </p>
 * <p> Description: Shared package-level role/transaction and fresh-search rules. Each search
 * begins with current storage instead of filtering a previous subset. Validated through public
 * services by TP3DiscoveryTest.andSearch, refreshedSearch, invalidSelections and revokedRoles.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 discovery
 */
final class DiscoveryAccess {
    private DiscoveryAccess() { }
    static <T> OperationResult<T> authorized(Database database, String actor, String role,
            Database.TransactionWork<OperationResult<T>> work) {
        try {
            return database.inTransaction(() -> {
                var access = new RoleAccessService(database).requireRole(actor, role);
                if (!access.isSuccessful()) return OperationResult.failure(access.getMessage());
                return work.execute();
            });
        } catch (SQLException e) {
            return OperationResult.failure("The lesson operation could not be completed. Refresh and try again.");
        }
    }
    static long id(String text) {
        try { long id = Long.parseLong(text == null ? "" : text.trim()); return id > 0 ? id : -1; }
        catch (NumberFormatException e) { return -1; }
    }
    static <T> OperationResult<T> missing() {
        return OperationResult.failure("Select an existing lesson with a positive numeric ID, then refresh and try again.");
    }
    static List<LessonsLearned> search(Database database, String query) throws SQLException {
        List<LessonsLearned> result = new ArrayList<>();
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        String[] words = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
        for (LessonsLearned lesson : database.getAllLessons()) {
            StringBuilder content = new StringBuilder(lesson.getTitle()).append('\n')
                    .append(lesson.getProblemSituation()).append('\n').append(lesson.getLessonLearned());
            for (var term : database.getSearchTerms(lesson.getId())) content.append('\n').append(term.getTerm());
            String searchable = content.toString().toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String word : words) if (!searchable.contains(word)) { matches = false; break; }
            if (matches) result.add(lesson);
        }
        return List.copyOf(result);
    }
}
