package guiLessons;

import lessonManagement.ExperienceDraft;
import lessonManagement.LessonDraft;
import lessonManagement.LessonService;
import lessonManagement.OperationResult;
import entityClasses.ExperienceRecord;
import entityClasses.LessonLearned;
import entityClasses.LessonsCollection;

import java.util.List;

/*******
 * <p> Title: ModelLessons Class. </p>
 *
 * <p> Description: Connects the contributor lesson views to the shared lesson service.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ModelLessons {

	private static final LessonService SERVICE =
			new LessonService(applicationMain.FoundationsMain.database);

	private ModelLessons() {
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param filter specifies optional title text
	 * @return the current contributor-only collection or subset
	 */
	protected static OperationResult<LessonsCollection> list(String owner, String filter) {
		return SERVICE.filterLessons(owner, filter);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param draft specifies raw core lesson values
	 * @return the created lesson or a helpful error
	 */
	protected static OperationResult<LessonLearned> create(String owner, LessonDraft draft) {
		return SERVICE.createLesson(owner, draft);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the selected lesson
	 * @return the contributor-visible lesson or a helpful error
	 */
	protected static OperationResult<LessonLearned> read(String owner, long id) {
		return SERVICE.readLesson(Long.toString(id), owner);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the selected lesson
	 * @param draft specifies proposed replacement values
	 * @return the updated lesson or a helpful error
	 */
	protected static OperationResult<LessonLearned> update(String owner, long id,
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

	/**
	 * @param owner specifies the signed-in contributor
	 * @param lessonId specifies the parent lesson
	 * @return current experience records or a helpful error
	 */
	protected static OperationResult<List<ExperienceRecord>> listExperiences(String owner,
			long lessonId) {
		return SERVICE.listExperiences(Long.toString(lessonId), owner);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param lessonId specifies the parent lesson
	 * @param draft specifies raw experience values
	 * @return the created experience or a helpful error
	 */
	protected static OperationResult<ExperienceRecord> createExperience(String owner,
			long lessonId, ExperienceDraft draft) {
		return SERVICE.createExperience(Long.toString(lessonId), owner, draft);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the experience to read
	 * @return the selected experience or a helpful error
	 */
	protected static OperationResult<ExperienceRecord> readExperience(String owner, long id) {
		return SERVICE.readExperience(Long.toString(id), owner);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the experience to update
	 * @param draft specifies proposed replacement values
	 * @return the updated experience or a helpful error
	 */
	protected static OperationResult<ExperienceRecord> updateExperience(String owner, long id,
			ExperienceDraft draft) {
		return SERVICE.updateExperience(Long.toString(id), owner, draft);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param id specifies the experience to delete
	 * @return a successful deletion result or a helpful error
	 */
	protected static OperationResult<Boolean> deleteExperience(String owner, long id) {
		return SERVICE.deleteExperience(Long.toString(id), owner);
	}
}
