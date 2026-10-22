package guiContributorLessons;

import lessonManagement.LessonDraft;
import lessonManagement.LessonService;
import lessonManagement.OperationResult;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;

/*******
 * <p> Title: ModelContributorLessons Class. </p>
 *
 * <p> Description: The Model for the Contributor Lessons window. It provides only the
 * collection and core-lesson operations used by that window and delegates them to the shared,
 * non-GUI LessonService. The service in turn uses the persistent LessonsLearned and
 * LessonsLearnedList classes, which keeps database rules and ownership checks outside the View
 * and Controller.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Separated list and detail window models for TP2
 */
public class ModelContributorLessons {

	private static final LessonService SERVICE =
			new LessonService(applicationMain.FoundationsMain.database);

	private ModelContributorLessons() {
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param filter specifies optional title text
	 * @return the current contributor-only collection or subset
	 */
	protected static OperationResult<LessonsLearnedList> list(String owner, String filter) {
		return SERVICE.filterLessons(owner, filter);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param draft specifies raw core lesson values
	 * @return the created lesson or a helpful error
	 */
	protected static OperationResult<LessonsLearned> create(String owner, LessonDraft draft) {
		return SERVICE.createLesson(owner, draft);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the selected lesson
	 * @return the contributor-visible lesson or a helpful error
	 */
	protected static OperationResult<LessonsLearned> read(String owner, long id) {
		return SERVICE.readLesson(Long.toString(id), owner);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the selected lesson
	 * @param draft specifies proposed replacement values
	 * @return the updated lesson or a helpful error
	 */
	protected static OperationResult<LessonsLearned> update(String owner, long id,
			LessonDraft draft) {
		return SERVICE.updateLesson(Long.toString(id), owner, draft);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the lesson to delete
	 * @return a successful deletion result or a helpful error
	 */
	protected static OperationResult<Boolean> delete(String owner, long id) {
		return SERVICE.deleteLesson(Long.toString(id), owner);
	}

}
