package lessonManagement;

import java.sql.SQLException;
import java.util.List;

import database.Database;
import entityClasses.ExperienceRecord;
import entityClasses.LessonLearned;
import entityClasses.LessonsCollection;
import entityClasses.TeamMemberEffort;
import lessonManagement.ExperienceValidator.ValidatedExperience;

/*******
 * <p> Title: LessonService Class. </p>
 *
 * <p> Description: Coordinates lesson and experience CRUD.  It validates complete requests and
 * checks ownership and lock state before calling the Database, which ensures a rejected request
 * leaves saved data unchanged.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class LessonService {

	private final Database database;

	/**
	 * @param database specifies the Foundations database used for persistence
	 */
	public LessonService(Database database) {
		this.database = database;
	}

	/**********
	 * <p> Method: createLesson(String owner, LessonDraft draft) </p>
	 *
	 * @param owner specifies the signed-in contributor
	 * @param draft specifies the proposed core information
	 * @return the created lesson or a helpful error
	 */
	public OperationResult<LessonLearned> createLesson(String owner, LessonDraft draft) {
		if (blank(owner)) return OperationResult.failure(
				"Sign in as a contributor before creating a lesson.");
		String validation = LessonValidator.validate(draft);
		if (!validation.isEmpty()) return OperationResult.failure(validation);

		LessonLearned proposed = new LessonLearned(0, owner.trim(), draft.getTitle().trim(),
				draft.getProblemSituation().trim(), draft.getLessonLearned().trim(),
				false, false, false, false);
		try {
			long id = database.createLesson(proposed);
			return OperationResult.success(database.getLesson(id), "Lesson created.");
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be saved. Try again.");
		}
	}

	/**********
	 * <p> Method: readLesson(String idInput, String contributor) </p>
	 *
	 * @param idInput specifies the selected lesson identifier
	 * @param contributor specifies the signed-in contributor
	 * @return the contributor-visible lesson or a helpful error
	 */
	public OperationResult<LessonLearned> readLesson(String idInput, String contributor) {
		OperationResult<Long> parsed = parseLessonId(idInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			LessonLearned lesson = database.getLesson(parsed.getValue());
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
	 * <p> Method: listLessons(String contributor) </p>
	 *
	 * @param contributor specifies the signed-in contributor
	 * @return only that contributor's lessons
	 */
	public OperationResult<LessonsCollection> listLessons(String contributor) {
		if (blank(contributor))
			return OperationResult.failure("Sign in as a contributor to view lessons.");
		try {
			LessonsCollection lessons = new LessonsCollection(
					database.getLessonsByOwner(contributor));
			String message = lessons.isEmpty() ? "No lessons found." :
					lessons.size() + " lesson(s) found.";
			return OperationResult.success(lessons, message);
		} catch (SQLException e) {
			return OperationResult.failure("The lesson list could not be loaded. Try again.");
		}
	}

	/**********
	 * <p> Method: filterLessons(String contributor, String titleText) </p>
	 *
	 * @param contributor specifies the signed-in contributor
	 * @param titleText specifies the simple title filter
	 * @return the matching contributor-only subset
	 */
	public OperationResult<LessonsCollection> filterLessons(String contributor, String titleText) {
		OperationResult<LessonsCollection> full = listLessons(contributor);
		if (!full.isSuccessful()) return full;
		LessonsCollection subset = full.getValue().filterByTitle(titleText);
		String message = subset.isEmpty() ? "No lessons found." :
				subset.size() + " lesson(s) found.";
		return OperationResult.success(subset, message);
	}

	/**********
	 * <p> Method: updateLesson(String idInput, String contributor, LessonDraft draft) </p>
	 *
	 * @param idInput specifies the selected lesson identifier
	 * @param contributor specifies the signed-in contributor
	 * @param draft specifies the proposed replacement values
	 * @return the updated lesson or a helpful error
	 */
	public OperationResult<LessonLearned> updateLesson(String idInput, String contributor,
			LessonDraft draft) {
		OperationResult<Long> parsed = parseLessonId(idInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			LessonLearned existing = database.getLesson(parsed.getValue());
			if (existing == null)
				return OperationResult.failure("Lesson not found. Refresh the list.");
			if (!existing.getOwner().equals(contributor))
				return OperationResult.failure("You can only edit your own lessons.");
			if (existing.isLessonLocked())
				return OperationResult.failure("This lesson is locked and cannot be edited.");

			String validation = LessonValidator.validate(draft);
			if (!validation.isEmpty()) return OperationResult.failure(validation);

			String title = draft.getTitle().trim();
			String problem = draft.getProblemSituation().trim();
			String lessonText = draft.getLessonLearned().trim();
			if (existing.isTitleLocked() && !existing.getTitle().equals(title))
				return OperationResult.failure("This field is locked and cannot be edited.");
			if (existing.isProblemSituationLocked() &&
					!existing.getProblemSituation().equals(problem))
				return OperationResult.failure("This field is locked and cannot be edited.");
			if (existing.isLessonLearnedLocked() &&
					!existing.getLessonLearned().equals(lessonText))
				return OperationResult.failure("This field is locked and cannot be edited.");

			LessonLearned updated = new LessonLearned(existing.getId(), existing.getOwner(),
					title, problem, lessonText, existing.isLessonLocked(),
					existing.isTitleLocked(), existing.isProblemSituationLocked(),
					existing.isLessonLearnedLocked());
			if (!database.updateLesson(updated))
				return OperationResult.failure("Lesson not found. Refresh the list.");
			return OperationResult.success(database.getLesson(updated.getId()), "Lesson updated.");
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be updated. Try again.");
		}
	}

	/**********
	 * <p> Method: deleteLesson(String idInput, String contributor) </p>
	 *
	 * @param idInput specifies the lesson identifier
	 * @param contributor specifies the signed-in contributor
	 * @return a successful result or a helpful error
	 */
	public OperationResult<Boolean> deleteLesson(String idInput, String contributor) {
		OperationResult<Long> parsed = parseLessonId(idInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			LessonLearned existing = database.getLesson(parsed.getValue());
			if (existing == null)
				return OperationResult.failure("Lesson not found. Refresh the list.");
			if (!existing.getOwner().equals(contributor))
				return OperationResult.failure("You can only delete your own lessons.");
			if (existing.hasAnyLockedField())
				return OperationResult.failure(
						"This lesson or one of its fields is locked and cannot be deleted.");
			if (!database.deleteLesson(existing.getId()))
				return OperationResult.failure("Lesson not found. Refresh the list.");
			return OperationResult.success(Boolean.TRUE, "Lesson deleted.");
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be deleted. Try again.");
		}
	}

	/**********
	 * <p> Method: createExperience(String lessonIdInput, String contributor,
	 * ExperienceDraft draft) </p>
	 *
	 * @param lessonIdInput specifies the parent lesson
	 * @param contributor specifies the signed-in contributor
	 * @param draft specifies raw experience information
	 * @return the created experience or a helpful error
	 */
	public OperationResult<ExperienceRecord> createExperience(String lessonIdInput,
			String contributor, ExperienceDraft draft) {
		OperationResult<Long> parsed = parseLessonId(lessonIdInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			LessonLearned lesson = database.getLesson(parsed.getValue());
			if (lesson == null) return OperationResult.failure("Lesson not found.");
			if (!lesson.getOwner().equals(contributor))
				return OperationResult.failure(
						"You can only manage your own experience records.");
			if (lesson.isLessonLocked())
				return OperationResult.failure("This lesson is locked.");

			OperationResult<ValidatedExperience> validation = ExperienceValidator.validate(draft);
			if (!validation.isSuccessful()) return OperationResult.failure(validation.getMessage());
			ValidatedExperience value = validation.getValue();
			ExperienceRecord proposed = new ExperienceRecord(0, lesson.getId(), contributor,
					value.getWhatWasDone(), value.getHowItWasDone(), value.getElapsedMinutes(),
					value.getTeamEfforts(), false, false, false, false, false);
			long id = database.createExperience(proposed);
			return OperationResult.success(database.getExperience(id), "Experience added.");
		} catch (SQLException e) {
			return OperationResult.failure("The experience could not be saved. Try again.");
		}
	}

	/**********
	 * <p> Method: readExperience(String idInput, String contributor) </p>
	 *
	 * @param idInput specifies the selected experience identifier
	 * @param contributor specifies the signed-in contributor
	 * @return the experience or a helpful error
	 */
	public OperationResult<ExperienceRecord> readExperience(String idInput, String contributor) {
		OperationResult<Long> parsed = parseExperienceId(idInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			ExperienceRecord experience = database.getExperience(parsed.getValue());
			if (experience == null)
				return OperationResult.failure("Experience record not found.");
			LessonLearned lesson = database.getLesson(experience.getLessonId());
			if (lesson == null) return OperationResult.failure("Lesson not found.");
			if (!experience.getOwner().equals(contributor) ||
					!lesson.getOwner().equals(contributor))
				return OperationResult.failure(
						"You can only manage your own experience records.");
			return OperationResult.success(experience, "Experience loaded.");
		} catch (SQLException e) {
			return OperationResult.failure("The experience could not be loaded. Try again.");
		}
	}

	/**********
	 * <p> Method: listExperiences(String lessonIdInput, String contributor) </p>
	 *
	 * @param lessonIdInput specifies the parent lesson
	 * @param contributor specifies the signed-in contributor
	 * @return all contributor-visible experience records for the lesson
	 */
	public OperationResult<List<ExperienceRecord>> listExperiences(String lessonIdInput,
			String contributor) {
		OperationResult<Long> parsed = parseLessonId(lessonIdInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			LessonLearned lesson = database.getLesson(parsed.getValue());
			if (lesson == null) return OperationResult.failure("Lesson not found.");
			if (!lesson.getOwner().equals(contributor))
				return OperationResult.failure(
						"You can only manage your own experience records.");
			List<ExperienceRecord> experiences = database.getExperiencesForLesson(lesson.getId());
			String message = experiences.isEmpty() ? "No experience records found." :
					experiences.size() + " experience record(s) found.";
			return OperationResult.success(experiences, message);
		} catch (SQLException e) {
			return OperationResult.failure("Experience records could not be loaded. Try again.");
		}
	}

	/**********
	 * <p> Method: updateExperience(String idInput, String contributor,
	 * ExperienceDraft draft) </p>
	 *
	 * @param idInput specifies the experience identifier
	 * @param contributor specifies the signed-in contributor
	 * @param draft specifies proposed replacement values
	 * @return the updated experience or a helpful error
	 */
	public OperationResult<ExperienceRecord> updateExperience(String idInput,
			String contributor, ExperienceDraft draft) {
		OperationResult<Long> parsed = parseExperienceId(idInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			ExperienceRecord existing = database.getExperience(parsed.getValue());
			if (existing == null)
				return OperationResult.failure("Experience record not found.");
			LessonLearned lesson = database.getLesson(existing.getLessonId());
			if (lesson == null) return OperationResult.failure("Lesson not found.");
			if (!existing.getOwner().equals(contributor) ||
					!lesson.getOwner().equals(contributor))
				return OperationResult.failure(
						"You can only manage your own experience records.");
			if (lesson.isLessonLocked())
				return OperationResult.failure("This lesson is locked.");
			if (existing.isExperienceLocked())
				return OperationResult.failure("This experience record is locked.");

			OperationResult<ValidatedExperience> validation = ExperienceValidator.validate(draft);
			if (!validation.isSuccessful()) return OperationResult.failure(validation.getMessage());
			ValidatedExperience value = validation.getValue();
			if (existing.isWhatWasDoneLocked() &&
					!existing.getWhatWasDone().equals(value.getWhatWasDone()))
				return OperationResult.failure("This field is locked.");
			if (existing.isHowItWasDoneLocked() &&
					!existing.getHowItWasDone().equals(value.getHowItWasDone()))
				return OperationResult.failure("This field is locked.");
			if (existing.isElapsedTimeLocked() &&
					Double.compare(existing.getElapsedMinutes(), value.getElapsedMinutes()) != 0)
				return OperationResult.failure("This field is locked.");
			if (existing.isTeamEffortLocked() &&
					!sameEfforts(existing.getTeamEfforts(), value.getTeamEfforts()))
				return OperationResult.failure("This field is locked.");

			ExperienceRecord updated = new ExperienceRecord(existing.getId(),
					existing.getLessonId(), existing.getOwner(), value.getWhatWasDone(),
					value.getHowItWasDone(), value.getElapsedMinutes(), value.getTeamEfforts(),
					existing.isExperienceLocked(), existing.isWhatWasDoneLocked(),
					existing.isHowItWasDoneLocked(), existing.isElapsedTimeLocked(),
					existing.isTeamEffortLocked());
			if (!database.updateExperience(updated))
				return OperationResult.failure("Experience record not found.");
			return OperationResult.success(database.getExperience(updated.getId()),
					"Experience updated.");
		} catch (SQLException e) {
			return OperationResult.failure("The experience could not be updated. Try again.");
		}
	}

	/**********
	 * <p> Method: deleteExperience(String idInput, String contributor) </p>
	 *
	 * @param idInput specifies the experience identifier
	 * @param contributor specifies the signed-in contributor
	 * @return a successful result or a helpful error
	 */
	public OperationResult<Boolean> deleteExperience(String idInput, String contributor) {
		OperationResult<Long> parsed = parseExperienceId(idInput);
		if (!parsed.isSuccessful()) return OperationResult.failure(parsed.getMessage());
		try {
			ExperienceRecord existing = database.getExperience(parsed.getValue());
			if (existing == null)
				return OperationResult.failure("Experience record not found.");
			LessonLearned lesson = database.getLesson(existing.getLessonId());
			if (lesson == null) return OperationResult.failure("Lesson not found.");
			if (!existing.getOwner().equals(contributor) ||
					!lesson.getOwner().equals(contributor))
				return OperationResult.failure(
						"You can only manage your own experience records.");
			if (lesson.isLessonLocked())
				return OperationResult.failure("This lesson is locked.");
			if (existing.hasAnyLockedField())
				return OperationResult.failure("This field is locked.");
			if (!database.deleteExperience(existing.getId()))
				return OperationResult.failure("Experience record not found.");
			return OperationResult.success(Boolean.TRUE, "Experience deleted.");
		} catch (SQLException e) {
			return OperationResult.failure("The experience could not be deleted. Try again.");
		}
	}

	private OperationResult<Long> parseLessonId(String input) {
		if (blank(input)) return OperationResult.failure("Select a lesson.");
		try {
			long value = Long.parseLong(input.trim());
			if (value <= 0) throw new NumberFormatException();
			return OperationResult.success(value, "Lesson ID is valid.");
		} catch (NumberFormatException e) {
			return OperationResult.failure("Lesson ID must be a valid number.");
		}
	}

	private OperationResult<Long> parseExperienceId(String input) {
		if (blank(input)) return OperationResult.failure("Select an experience record.");
		try {
			long value = Long.parseLong(input.trim());
			if (value <= 0) throw new NumberFormatException();
			return OperationResult.success(value, "Experience ID is valid.");
		} catch (NumberFormatException e) {
			return OperationResult.failure("Experience ID must be a valid number.");
		}
	}

	private boolean sameEfforts(List<TeamMemberEffort> first, List<TeamMemberEffort> second) {
		if (first.size() != second.size()) return false;
		for (int index = 0; index < first.size(); index++) {
			TeamMemberEffort a = first.get(index);
			TeamMemberEffort b = second.get(index);
			if (!a.getMemberName().equals(b.getMemberName()) ||
					Double.compare(a.getEffortMinutes(), b.getEffortMinutes()) != 0)
				return false;
		}
		return true;
	}

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
