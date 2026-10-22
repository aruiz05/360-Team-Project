package guiContributorLessonDetails;

import java.util.List;

import entityClasses.ExperienceRecord;
import entityClasses.LessonsLearned;
import lessonManagement.ExperienceDraft;
import lessonManagement.LessonService;
import lessonManagement.OperationResult;

/*******
 * <p> Title: ModelContributorLessonDetails Class. </p>
 *
 * <p> Description: The Model for the Contributor Lesson Details window. It supplies the current
 * contributor-visible lesson and the Create, Read, Update, and Delete operations for experience
 * records linked to that lesson.</p>
 *
 * <p>All calls delegate to the non-GUI LessonService. That service rechecks the lesson owner,
 * record owner, lock state, required descriptions, elapsed minutes, team-member names, and
 * person-minute effort before the shared Database package is allowed to change persistent data.
 * Keeping those rules below the JavaFX layer lets the automated tests exercise the same behavior
 * shown by this window.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-22 Initial TP2 Contributor details model
 */
public class ModelContributorLessonDetails {

	// One shared service gives every detail operation the same persistent database and rules.
	private static final LessonService SERVICE =
			new LessonService(applicationMain.FoundationsMain.database);

	private ModelContributorLessonDetails() {
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param lessonId specifies the selected lesson
	 * @return the contributor-visible lesson or a helpful error
	 */
	protected static OperationResult<LessonsLearned> readLesson(String owner, long lessonId) {
		return SERVICE.readLesson(Long.toString(lessonId), owner);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param lessonId specifies the parent lesson
	 * @return the current related experience records or a helpful error
	 */
	protected static OperationResult<List<ExperienceRecord>> listExperiences(String owner,
			long lessonId) {
		return SERVICE.listExperiences(Long.toString(lessonId), owner);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param lessonId specifies the parent lesson
	 * @param draft specifies raw experience values entered by the contributor
	 * @return the created experience record or a helpful error
	 */
	protected static OperationResult<ExperienceRecord> createExperience(String owner,
			long lessonId, ExperienceDraft draft) {
		return SERVICE.createExperience(Long.toString(lessonId), owner, draft);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param experienceId specifies the selected experience record
	 * @return the contributor-visible experience record or a helpful error
	 */
	protected static OperationResult<ExperienceRecord> readExperience(String owner,
			long experienceId) {
		return SERVICE.readExperience(Long.toString(experienceId), owner);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param experienceId specifies the record to update
	 * @param draft specifies proposed replacement values
	 * @return the updated record or a helpful error
	 */
	protected static OperationResult<ExperienceRecord> updateExperience(String owner,
			long experienceId, ExperienceDraft draft) {
		return SERVICE.updateExperience(Long.toString(experienceId), owner, draft);
	}

	/**
	 * @param owner specifies the signed-in contributor
	 * @param experienceId specifies the record to delete
	 * @return a successful deletion result or a helpful error
	 */
	protected static OperationResult<Boolean> deleteExperience(String owner, long experienceId) {
		return SERVICE.deleteExperience(Long.toString(experienceId), owner);
	}
}
