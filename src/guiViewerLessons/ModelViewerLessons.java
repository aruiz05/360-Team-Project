package guiViewerLessons;

import lessonManagement.ViewerLessonService;
import lessonManagement.OperationResult;
import entityClasses.ViewerLesson;
import java.util.List;

/*******
 * <p> Title: ModelViewerLessons Class. </p>
 * <p> Description: Connects read-only controls to current-role discovery. All methods are
 * validated by TP3DiscoveryGuiTestingAutomation's search, details, and return scenarios.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Viewer discovery
 */
public class ModelViewerLessons {
    /** Shared service retains the existing application database. */
    private static final ViewerLessonService SERVICE = new ViewerLessonService(applicationMain.FoundationsMain.database);
    private ModelViewerLessons() { }
    /**
     * @param actor authenticated Viewer
     * @param query current keywords
     * @return fresh core subset or an error; tested by discovery GUI search scenarios
     */
    protected static OperationResult<List<ViewerLesson>> search(String actor, String query) {
        return SERVICE.searchLessons(actor, query);
    }
    /**
     * @param actor authenticated Viewer
     * @param id selection
     * @return latest core or an error; tested by discovery GUI detail scenarios
     */
    protected static OperationResult<ViewerLesson> read(String actor, String id) { return SERVICE.readLesson(actor, id); }
}
