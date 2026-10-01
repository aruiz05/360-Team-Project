package database;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import entityClasses.ExperienceRecord;
import entityClasses.LessonLearned;
import lessonManagement.EffortInput;
import lessonManagement.ExperienceDraft;
import lessonManagement.LessonDraft;
import lessonManagement.LessonService;
import lessonManagement.OperationResult;

/*******
 * <p> Title: HW2PersistenceProcess Class. </p>
 *
 * <p> Description: Performs one persistence-test phase and then exits.  The main HW2 testing
 * automation launches this class as separate Java processes so persistence is not simulated by
 * merely reopening a database connection in one process.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class HW2PersistenceProcess {

	private static final String OWNER_A = "ContributorA";
	private static final String OWNER_B = "ContributorB";

	/**********
	 * @param args requires phase, file-backed database path, and state-file path
	 * @throws Exception when a persistence check fails
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 3) throw new IllegalArgumentException(
				"Expected phase, database path, and state path.");
		String phase = args[0];
		Database database = new Database("jdbc:h2:file:" + args[1]);
		database.connectToDatabase();
		LessonService service = new LessonService(database);
		Path statePath = Path.of(args[2]);
		Properties state = Files.exists(statePath) ? load(statePath) : new Properties();
		try {
			switch (phase) {
			case "create" -> create(service, state, statePath);
			case "verify-created" -> verifyCreated(service, state);
			case "update" -> update(service, state);
			case "verify-updated" -> verifyUpdated(service, state);
			case "delete" -> delete(service, state, statePath);
			case "verify-deleted" -> verifyDeleted(service, state);
			case "restrict" -> restrict(database, service, state, statePath);
			case "verify-restricted" -> verifyRestricted(service, state);
			default -> throw new IllegalArgumentException("Unknown persistence phase: " + phase);
			}
			System.out.println("Process phase passed: " + phase);
		} finally {
			database.closeConnection();
		}
	}

	private static void create(LessonService service, Properties state, Path statePath)
			throws Exception {
		LessonLearned lesson = value(service.createLesson(OWNER_A, new LessonDraft(
				"Fixing JavaFX setup", "The app would not start",
				"Check the JavaFX path in the run configuration.")));
		ExperienceRecord experience = value(service.createExperience(Long.toString(lesson.getId()),
				OWNER_A, experience("Updated the module path", "Used Eclipse settings", "30",
						new EffortInput("Adan", "30"))));
		state.setProperty("lessonId", Long.toString(lesson.getId()));
		state.setProperty("experienceId", Long.toString(experience.getId()));
		store(statePath, state);
	}

	private static void verifyCreated(LessonService service, Properties state) {
		LessonLearned lesson = value(service.readLesson(state.getProperty("lessonId"), OWNER_A));
		check("Fixing JavaFX setup".equals(lesson.getTitle()), "Created lesson title changed.");
		ExperienceRecord experience = value(service.readExperience(
				state.getProperty("experienceId"), OWNER_A));
		check(experience.getLessonId() == lesson.getId(), "Experience parent changed.");
		check(value(service.listLessons(OWNER_A)).size() == 1,
				"Created lesson was not rebuilt in the collection.");
	}

	private static void update(LessonService service, Properties state) {
		long lessonId = Long.parseLong(state.getProperty("lessonId"));
		long experienceId = Long.parseLong(state.getProperty("experienceId"));
		value(service.updateLesson(Long.toString(lessonId), OWNER_A, new LessonDraft(
				"Fixing JavaFX module paths", "The app failed after setup",
				"Check both the build path and run configuration.")));
		value(service.updateExperience(Long.toString(experienceId), OWNER_A,
				experience("Retested the module path", "Used a clean Eclipse launch", "45.5",
						new EffortInput("Adan", "25.5"), new EffortInput("Mahin", "20"))));
	}

	private static void verifyUpdated(LessonService service, Properties state) {
		LessonLearned lesson = value(service.readLesson(state.getProperty("lessonId"), OWNER_A));
		check("Fixing JavaFX module paths".equals(lesson.getTitle()),
				"Updated lesson title did not persist.");
		ExperienceRecord experience = value(service.readExperience(
				state.getProperty("experienceId"), OWNER_A));
		check(Double.compare(experience.getElapsedMinutes(), 45.5) == 0,
				"Updated elapsed time did not persist.");
		check(experience.getTeamEfforts().size() == 2,
				"Updated effort list did not persist.");
		check(value(service.filterLessons(OWNER_A, "module paths")).size() == 1,
				"Updated lesson is missing from its current subset.");
	}

	private static void delete(LessonService service, Properties state, Path statePath)
			throws Exception {
		long retainedLessonId = Long.parseLong(state.getProperty("lessonId"));
		long deletedExperienceId = Long.parseLong(state.getProperty("experienceId"));
		value(service.deleteExperience(Long.toString(deletedExperienceId), OWNER_A));

		LessonLearned removedLesson = value(service.createLesson(OWNER_A, new LessonDraft(
				"Remove this lesson", "Temporary test record", "Deleted data must stay deleted.")));
		ExperienceRecord removedExperience = value(service.createExperience(
				Long.toString(removedLesson.getId()), OWNER_A,
				experience("Prepared deletion", "Created related data", "5",
						new EffortInput("Adan", "5"))));
		value(service.deleteLesson(Long.toString(removedLesson.getId()), OWNER_A));
		state.setProperty("retainedLessonId", Long.toString(retainedLessonId));
		state.setProperty("removedLessonId", Long.toString(removedLesson.getId()));
		state.setProperty("removedExperienceId", Long.toString(removedExperience.getId()));
		store(statePath, state);
	}

	private static void verifyDeleted(LessonService service, Properties state) {
		LessonLearned retained = value(service.readLesson(
				state.getProperty("retainedLessonId"), OWNER_A));
		check(value(service.listExperiences(Long.toString(retained.getId()), OWNER_A)).isEmpty(),
				"Deleted experience reappeared.");
		check(!service.readLesson(state.getProperty("removedLessonId"), OWNER_A).isSuccessful(),
				"Deleted lesson reappeared.");
		check(!service.readExperience(state.getProperty("removedExperienceId"), OWNER_A)
				.isSuccessful(), "Experience from a deleted lesson reappeared.");
	}

	private static void restrict(Database database, LessonService service, Properties state,
			Path statePath) throws Exception {
		LessonLearned lesson = value(service.createLesson(OWNER_A, new LessonDraft(
				"Locked lesson", "Protect the original problem", "Keep the approved wording.")));
		check(database.setLessonLocksForTesting(lesson.getId(), false, true, false, false),
				"Could not seed the title lock.");
		OperationResult<LessonLearned> rejectedEdit = service.updateLesson(
				Long.toString(lesson.getId()), OWNER_A,
				new LessonDraft("Changed locked title", lesson.getProblemSituation(),
						lesson.getLessonLearned()));
		check(!rejectedEdit.isSuccessful(), "Locked edit unexpectedly succeeded.");
		OperationResult<Boolean> rejectedDelete = service.deleteLesson(
				Long.toString(lesson.getId()), OWNER_A);
		check(!rejectedDelete.isSuccessful(), "Locked deletion unexpectedly succeeded.");
		state.setProperty("restrictedLessonId", Long.toString(lesson.getId()));
		store(statePath, state);
	}

	private static void verifyRestricted(LessonService service, Properties state) {
		LessonLearned lesson = value(service.readLesson(
				state.getProperty("restrictedLessonId"), OWNER_A));
		check("Locked lesson".equals(lesson.getTitle()), "Rejected title edit was saved.");
		check(lesson.isTitleLocked(), "Title lock did not persist.");
		OperationResult<LessonLearned> denied = service.readLesson(
				state.getProperty("restrictedLessonId"), OWNER_B);
		check(!denied.isSuccessful() &&
				"You can only view your own lessons.".equals(denied.getMessage()),
				"Ownership restriction changed after restart.");
	}

	private static ExperienceDraft experience(String what, String how, String elapsed,
			EffortInput... efforts) {
		return new ExperienceDraft(what, how, elapsed, List.of(efforts));
	}

	private static <T> T value(OperationResult<T> result) {
		if (!result.isSuccessful()) throw new AssertionError(result.getMessage());
		return result.getValue();
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static Properties load(Path path) throws Exception {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(path)) {
			properties.load(input);
		}
		return properties;
	}

	private static void store(Path path, Properties properties) throws Exception {
		try (OutputStream output = Files.newOutputStream(path)) {
			properties.store(output, "HW2 persistence test identifiers");
		}
	}
}
