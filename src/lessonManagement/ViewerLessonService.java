package lessonManagement;

import database.Database;
import entityClasses.ViewerLesson;
import guiTools.RoleNames;
import java.util.List;

/*******
 * <p> Title: ViewerLessonService Class. </p>
 * <p> Description: Current-role, read-only discovery across all Contributors' lesson cores.
 * Search uses case-insensitive AND words across core text and saved Curator terms. No raw
 * experiences or mutation route is exposed. Tested by TP3DiscoveryTest and discovery GUI tests.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Viewer discovery
 */
public class ViewerLessonService {
    /** Shared database coordinated through its existing transaction boundary. */
    private final Database database;
    /**
     * Tested by TP3DiscoveryTest setup and unavailableStorage.
     * @param database shared application or isolated persistent test database
     */
    public ViewerLessonService(Database database) { this.database = database; }
    /**
     * Tested by TP3DiscoveryTest.discoveryAndPrivacy and collectionSizes.
     * @param actor authenticated Viewer from application context
     * @return all current core projections or an access error
     */
    public OperationResult<List<ViewerLesson>> listLessons(String actor) { return searchLessons(actor, ""); }
    /**
     * Tested by TP3DiscoveryTest.andSearch, refreshedSearch and revokedRoles.
     * @param actor authenticated Viewer
     * @param query whitespace-separated AND words; blank restores all
     * @return fresh immutable core subset or an access error
     */
    public OperationResult<List<ViewerLesson>> searchLessons(String actor, String query) {
        return DiscoveryAccess.authorized(database, actor, RoleNames.VIEWER, () ->
                OperationResult.success(DiscoveryAccess.search(database, query).stream()
                        .map(ViewerLesson::new).toList(), "Lessons loaded."));
    }
    /**
     * Tested by TP3DiscoveryTest.discoveryAndPrivacy, invalidSelections and deletedSelections.
     * @param actor authenticated Viewer
     * @param lessonId positive selected ID
     * @return current core only, or a helpful access/selection error
     */
    public OperationResult<ViewerLesson> readLesson(String actor, String lessonId) {
        return DiscoveryAccess.authorized(database, actor, RoleNames.VIEWER, () -> {
            long id = DiscoveryAccess.id(lessonId);
            if (id < 0) return DiscoveryAccess.missing();
            var lesson = database.getLesson(id);
            return lesson == null ? DiscoveryAccess.missing()
                    : OperationResult.success(new ViewerLesson(lesson), "Lesson loaded.");
        });
    }
}
