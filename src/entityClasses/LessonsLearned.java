package entityClasses;

import java.sql.SQLException;

import database.Database;
import lessonManagement.LessonDraft;
import lessonManagement.LessonValidator;
import lessonManagement.OperationResult;

/*******
 * <p> Title: LessonsLearned Class. </p>
 *
 * <p> Description: Represents the contributor-visible core information for one lesson learned
 * and provides its persistent Create, Read, Update, and Delete operations. The database owns the
 * unique identifier and stores ownership and lock information with the content. Keeping every
 * write in this class ensures validation, ownership, and lock rules are applied before saved data
 * changes.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Added the TP2 persistent CRUD operations
 */
public class LessonsLearned {

	private final long id;
	private final String owner;
	private final String title;
	private final String problemSituation;
	private final String lessonLearned;
	private final boolean lessonLocked;
	private final boolean titleLocked;
	private final boolean problemSituationLocked;
	private final boolean lessonLearnedLocked;

	/**********
	 * <p> Method: LessonsLearned(...) </p>
	 *
	 * <p> Description: Construct a complete lesson read from the database.  A zero identifier is
	 * used only before a new lesson has been inserted.</p>
	 *
	 * @param id specifies the database identifier
	 * @param owner specifies the contributor who owns the lesson
	 * @param title specifies the short lesson title
	 * @param problemSituation specifies the problem or situation
	 * @param lessonLearned specifies what the contributor learned
	 * @param lessonLocked specifies whether the complete lesson is locked
	 * @param titleLocked specifies whether the title is locked
	 * @param problemSituationLocked specifies whether the problem field is locked
	 * @param lessonLearnedLocked specifies whether the lesson text is locked
	 */
	public LessonsLearned(long id, String owner, String title, String problemSituation,
			String lessonLearned, boolean lessonLocked, boolean titleLocked,
			boolean problemSituationLocked, boolean lessonLearnedLocked) {
		this.id = id;
		this.owner = owner;
		this.title = title;
		this.problemSituation = problemSituation;
		this.lessonLearned = lessonLearned;
		this.lessonLocked = lessonLocked;
		this.titleLocked = titleLocked;
		this.problemSituationLocked = problemSituationLocked;
		this.lessonLearnedLocked = lessonLearnedLocked;
	}

	/**********
	 * <p> Method: create(Database database, String owner, LessonDraft draft) </p>
	 *
	 * <p> Description: Validate and persist a new contributor-owned lesson. New lessons begin
	 * unlocked because TP2 does not provide Curator lock controls.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param owner specifies the signed-in contributor
	 * @param draft specifies the proposed lesson information
	 * @return the saved lesson or a helpful validation or database error
	 */
	public static OperationResult<LessonsLearned> create(Database database, String owner,
			LessonDraft draft) {
		if (blank(owner)) return OperationResult.failure(
				"Sign in as a contributor before creating a lesson.");
		String validation = LessonValidator.validate(draft);
		if (!validation.isEmpty()) return OperationResult.failure(validation);

		LessonsLearned proposed = new LessonsLearned(0, owner.trim(), draft.getTitle().trim(),
				draft.getProblemSituation().trim(), draft.getLessonLearned().trim(),
				false, false, false, false);
		try {
			long createdId = database.createLesson(proposed);
			return OperationResult.success(database.getLesson(createdId), "Lesson created.");
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be saved. Try again.");
		}
	}

	/**********
	 * <p> Method: read(Database database, long id, String contributor) </p>
	 *
	 * <p> Description: Read one persisted lesson while preventing a contributor from viewing
	 * another contributor's submission.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param id specifies the database lesson identifier
	 * @param contributor specifies the signed-in contributor
	 * @return the contributor-visible lesson or a helpful error
	 */
	public static OperationResult<LessonsLearned> read(Database database, long id,
			String contributor) {
		try {
			LessonsLearned lesson = database.getLesson(id);
			if (lesson == null)
				return OperationResult.failure("Lesson not found. Refresh the list.");
			if (!lesson.getOwner().equals(contributor))
				return OperationResult.failure("You can only view your own lessons.");
			return OperationResult.success(lesson, "Lesson loaded.");
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be loaded. Try again.");
		}
	}

	/**********
	 * <p> Method: update(Database database, String contributor, LessonDraft draft) </p>
	 *
	 * <p> Description: Reload the current persisted state, verify ownership and locks, validate
	 * the proposed values, and then update the lesson. Reloading first prevents a stale screen
	 * from bypassing a lock added after the lesson was displayed.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param contributor specifies the signed-in contributor
	 * @param draft specifies the proposed replacement values
	 * @return the updated lesson or a helpful error
	 */
	public OperationResult<LessonsLearned> update(Database database, String contributor,
			LessonDraft draft) {
		try {
			LessonsLearned current = database.getLesson(id);
			if (current == null)
				return OperationResult.failure("Lesson not found. Refresh the list.");
			if (!current.getOwner().equals(contributor))
				return OperationResult.failure("You can only edit your own lessons.");
			if (current.isLessonLocked())
				return OperationResult.failure("This lesson is locked and cannot be edited.");

			String validation = LessonValidator.validate(draft);
			if (!validation.isEmpty()) return OperationResult.failure(validation);

			String updatedTitle = draft.getTitle().trim();
			String updatedProblem = draft.getProblemSituation().trim();
			String updatedLesson = draft.getLessonLearned().trim();
			if (current.isTitleLocked() && !current.getTitle().equals(updatedTitle))
				return OperationResult.failure("This field is locked and cannot be edited.");
			if (current.isProblemSituationLocked() &&
					!current.getProblemSituation().equals(updatedProblem))
				return OperationResult.failure("This field is locked and cannot be edited.");
			if (current.isLessonLearnedLocked() &&
					!current.getLessonLearned().equals(updatedLesson))
				return OperationResult.failure("This field is locked and cannot be edited.");

			LessonsLearned replacement = new LessonsLearned(current.getId(), current.getOwner(),
					updatedTitle, updatedProblem, updatedLesson, current.isLessonLocked(),
					current.isTitleLocked(), current.isProblemSituationLocked(),
					current.isLessonLearnedLocked());
			if (!database.updateLesson(replacement))
				return OperationResult.failure("Lesson not found. Refresh the list.");
			return OperationResult.success(database.getLesson(id), "Lesson updated.");
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be updated. Try again.");
		}
	}

	/**********
	 * <p> Method: delete(Database database, String contributor) </p>
	 *
	 * <p> Description: Delete an owned lesson only when neither the complete lesson nor any core
	 * field is locked. Related experience and effort records are removed by database cascading.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param contributor specifies the signed-in contributor
	 * @return a successful result or a helpful error
	 */
	public OperationResult<Boolean> delete(Database database, String contributor) {
		try {
			LessonsLearned current = database.getLesson(id);
			if (current == null)
				return OperationResult.failure("Lesson not found. Refresh the list.");
			if (!current.getOwner().equals(contributor))
				return OperationResult.failure("You can only delete your own lessons.");
			if (current.hasAnyLockedField())
				return OperationResult.failure(
						"This lesson or one of its fields is locked and cannot be deleted.");
			if (!database.deleteLesson(id))
				return OperationResult.failure("Lesson not found. Refresh the list.");
			return OperationResult.success(Boolean.TRUE, "Lesson deleted.");
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be deleted. Try again.");
		}
	}

	/** @return the unique lesson identifier */
	public long getId() { return id; }

	/** @return the username of the contributor who owns the lesson */
	public String getOwner() { return owner; }

	/** @return the contributor-visible lesson title */
	public String getTitle() { return title; }

	/** @return the contributor-visible problem or situation */
	public String getProblemSituation() { return problemSituation; }

	/** @return the contributor-visible lesson text */
	public String getLessonLearned() { return lessonLearned; }

	/** @return true when the complete lesson is locked */
	public boolean isLessonLocked() { return lessonLocked; }

	/** @return true when the title cannot be changed */
	public boolean isTitleLocked() { return titleLocked; }

	/** @return true when the problem or situation cannot be changed */
	public boolean isProblemSituationLocked() { return problemSituationLocked; }

	/** @return true when the lesson text cannot be changed */
	public boolean isLessonLearnedLocked() { return lessonLearnedLocked; }

	/**********
	 * <p> Method: hasAnyLockedField() </p>
	 *
	 * <p> Description: Determine whether deletion must be rejected because the lesson or one of
	 * its core fields is locked.</p>
	 *
	 * @return true when any applicable lock is active
	 */
	public boolean hasAnyLockedField() {
		return lessonLocked || titleLocked || problemSituationLocked || lessonLearnedLocked;
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	@Override
	public String toString() {
		return id + " - " + title;
	}
}
