package guiCuratorLessons;

import entityClasses.ExperienceRecord;
import entityClasses.LessonsLearned;
import entityClasses.SearchTerm;
import java.util.List;
import lessonManagement.CuratorLessonService;
import lessonManagement.LessonDraft;
import lessonManagement.LessonLockState;
import lessonManagement.OperationResult;

/*******
 * <p> Title: ModelCuratorLessons Class. </p>
 * <p> Description: Connects review controls to authorized persistent operations. All methods
 * are exercised by TP3DiscoveryGuiTestingAutomation. Raw JDBC stays outside the GUI.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 Curator operations
 */
public class ModelCuratorLessons {
    /** Shared business service retains the application's existing database connection. */
    private static final CuratorLessonService SERVICE = new CuratorLessonService(applicationMain.FoundationsMain.database);
    private ModelCuratorLessons() { }
    /**
     * @param actor authenticated Curator
     * @param query search words
     * @return current results; tested by discovery GUI search
     */
    protected static OperationResult<List<LessonsLearned>> search(String actor, String query) { return SERVICE.searchLessons(actor, query); }
    /**
     * @param actor authenticated Curator
     * @param id parent selection
     * @return current core; tested by discovery GUI review
     */
    protected static OperationResult<LessonsLearned> read(String actor, String id) { return SERVICE.readLesson(actor, id); }
    /**
     * @param actor authenticated Curator
     * @param id parent selection
     * @return related experiences; tested by discovery GUI review
     */
    protected static OperationResult<List<ExperienceRecord>> experiences(String actor, String id) { return SERVICE.readExperiences(actor, id); }
    /**
     * @param actor authenticated Curator
     * @param id parent selection
     * @return saved terms; tested by discovery GUI terms
     */
    protected static OperationResult<List<SearchTerm>> terms(String actor, String id) { return SERVICE.readTerms(actor, id); }
    /**
     * @param actor authenticated Curator
     * @param id parent selection
     * @param terms reviewed replacement
     * @return saved terms or error; tested by discovery GUI terms
     */
    protected static OperationResult<List<SearchTerm>> saveTerms(String actor, String id, List<String> terms) { return SERVICE.saveTerms(actor, id, terms); }
    /**
     * @param actor authenticated Curator
     * @param id parent selection
     * @param draft reviewed core
     * @return saved core or error; tested by discovery GUI refinement
     */
    protected static OperationResult<LessonsLearned> refine(String actor, String id, LessonDraft draft) { return SERVICE.refineLesson(actor, id, draft); }
    /**
     * @param actor authenticated Curator
     * @param id parent selection
     * @param locks reviewed protections
     * @return saved lesson or error; tested by discovery GUI protections
     */
    protected static OperationResult<LessonsLearned> locks(String actor, String id, LessonLockState locks) { return SERVICE.setLessonLocks(actor, id, locks); }
}
