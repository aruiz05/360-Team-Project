package database;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import entityClasses.ExperienceRecord;
import entityClasses.LessonLearned;
import entityClasses.LessonsCollection;
import entityClasses.TeamMemberEffort;
import lessonManagement.EffortInput;
import lessonManagement.ExperienceDraft;
import lessonManagement.LessonDraft;
import lessonManagement.LessonService;
import lessonManagement.OperationResult;

/*******
 * <p> Title: HW2TestingAutomation Class. </p>
 *
 * <p> Description: Executes the 26 numbered HW2 test cases.  Each case prints a readable name,
 * expected behavior, actual result, and pass or fail status.  Test cases use isolated databases,
 * and Tests 23 through 26 launch separate Java processes against file-backed databases.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class HW2TestingAutomation {

	private static final String OWNER_A = "ContributorA";
	private static final String OWNER_B = "ContributorB";
	private static final LessonDraft SAMPLE_LESSON = new LessonDraft(
			"Fixing JavaFX setup", "The app would not start",
			"Check the JavaFX path in the run configuration.");

	private static int testsPassed;
	private static int testsFailed;
	private static int checksPassed;
	private static int checksFailed;
	private static int currentChecks;

	/**
	 * @param args command-line arguments are not used
	 */
	public static void main(String[] args) {
		System.out.println("HW2 LESSONS LEARNED TESTING AUTOMATION");
		System.out.println("Each numbered test may contain multiple required input checks.\n");

		runTest(1, "Create a valid lesson",
				"A saved lesson has a unique ID, owner, matching fields, and appears in A's list.",
				HW2TestingAutomation::testCreateValidLesson);
		runTest(2, "Reject missing core information",
				"All missing, empty, and spaces-only core-field variants are rejected unchanged.",
				HW2TestingAutomation::testRejectMissingCoreInformation);
		runTest(3, "Keep similar lessons separate",
				"Lessons sharing a title retain different IDs and lesson text.",
				HW2TestingAutomation::testKeepSimilarLessonsSeparate);
		runTest(4, "Read a saved lesson",
				"The selected lesson and its related experience values match saved data.",
				HW2TestingAutomation::testReadSavedLesson);
		runTest(5, "Show an empty list",
				"Empty full and filtered collections report No lessons found.",
				HW2TestingAutomation::testShowEmptyList);
		runTest(6, "Show one and many lessons",
				"One-item, 250-item, and filtered collections retain every expected ID.",
				HW2TestingAutomation::testShowOneAndManyLessons);
		runTest(7, "Keep contributor content private",
				"A sees only A's records and cannot open B's lesson.",
				HW2TestingAutomation::testKeepContributorContentPrivate);
		runTest(8, "Show edited content and lock status",
				"Curator-edited core text and locks are visible; separate role data is hidden.",
				HW2TestingAutomation::testShowEditedContentAndLockStatus);
		runTest(9, "Reject invalid or missing lesson IDs",
				"Empty, malformed, and missing lesson IDs produce the required errors.",
				HW2TestingAutomation::testRejectInvalidLessonIds);
		runTest(10, "Update valid core information",
				"All core fields update while the ID and owner remain unchanged.",
				HW2TestingAutomation::testUpdateValidCoreInformation);
		runTest(11, "Reject invalid edits",
				"Every invalid core edit is rejected and the original row is unchanged.",
				HW2TestingAutomation::testRejectInvalidEdits);
		runTest(12, "Reject locked edits",
				"Locked changes fail while an unrelated unlocked field can change.",
				HW2TestingAutomation::testRejectLockedEdits);
		runTest(13, "Reject unavailable update targets",
				"Ownership and invalid-ID update attempts fail without changes.",
				HW2TestingAutomation::testRejectUnavailableUpdateTargets);
		runTest(14, "Create and read experience records",
				"Two complete attempts remain separate and linked to the correct lesson.",
				HW2TestingAutomation::testCreateAndReadExperiences);
		runTest(15, "Update an experience record",
				"Valid, zero, and decimal updates persist without changing another attempt.",
				HW2TestingAutomation::testUpdateExperience);
		runTest(16, "Delete experience records",
				"Selected attempts are deleted while their parent lesson remains.",
				HW2TestingAutomation::testDeleteExperiences);
		runTest(17, "Reject invalid experience input",
				"Every required create and update variant fails with helpful errors unchanged.",
				HW2TestingAutomation::testRejectInvalidExperienceInput);
		runTest(18, "Reject unavailable experience targets",
				"Missing, foreign, locked, and invalid-ID experience operations are rejected.",
				HW2TestingAutomation::testRejectUnavailableExperienceTargets);
		runTest(19, "Delete an unlocked lesson",
				"The lesson and children disappear while unrelated records remain.",
				HW2TestingAutomation::testDeleteUnlockedLesson);
		runTest(20, "Reject deletion of locked lessons",
				"Whole-lesson and field locks preserve the lesson and related records.",
				HW2TestingAutomation::testRejectLockedLessonDeletion);
		runTest(21, "Reject unavailable deletion targets",
				"Ownership and invalid-ID deletion attempts remove nothing.",
				HW2TestingAutomation::testRejectUnavailableDeletionTargets);
		runTest(22, "Refresh full lists and subsets",
				"Create, update, and delete are reflected without stale or duplicate rows.",
				HW2TestingAutomation::testRefreshCollections);
		runTest(23, "Keep created data after restart",
				"A separate process reads the created lesson, experience, IDs, and relationship.",
				() -> testPersistence("23", "create", "verify-created"));
		runTest(24, "Keep updates after restart",
				"A separate process reads updated values and rebuilt collections.",
				() -> testPersistence("24", "create", "update", "verify-updated"));
		runTest(25, "Keep deletions after restart",
				"Deleted lessons and experiences remain absent in a separate process.",
				() -> testPersistence("25", "create", "delete", "verify-deleted"));
		runTest(26, "Keep restrictions and rejected data unchanged",
				"Owner, locks, original values, and restriction messages survive restart.",
				() -> testPersistence("26", "restrict", "verify-restricted"));

		System.out.println("============================================================");
		System.out.println("NUMBERED TEST TOTALS");
		System.out.println("Passed: " + testsPassed);
		System.out.println("Failed: " + testsFailed);
		System.out.println("Total:  " + (testsPassed + testsFailed));
		System.out.println("VARIANT CHECK TOTALS");
		System.out.println("Passed: " + checksPassed);
		System.out.println("Failed: " + checksFailed);
		System.out.println("Total:  " + (checksPassed + checksFailed));

		if (testsFailed > 0) System.exit(1);
	}

	private static void testCreateValidLesson() throws Exception {
		try (TestContext context = context("test1")) {
			LessonLearned created = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			check(created.getId() > 0, "Database assigned a positive unique ID.");
			check(OWNER_A.equals(created.getOwner()), "Owner A was saved.");
			checkLessonFields(created, SAMPLE_LESSON);
			LessonLearned read = success(context.service.readLesson(id(created), OWNER_A));
			check(read.getId() == created.getId(), "Read returned the saved ID.");
			check(success(context.service.listLessons(OWNER_A)).findById(created.getId()) != null,
					"A's list contains the saved lesson.");
		}
	}

	private static void testRejectMissingCoreInformation() throws Exception {
		try (TestContext context = context("test2")) {
			String[] invalid = {null, "", "   "};
			for (String value : invalid) {
				assertFailure(context.service.createLesson(OWNER_A,
						new LessonDraft(value, "Problem", "Lesson")),
						"Enter a lesson title.");
				assertFailure(context.service.createLesson(OWNER_A,
						new LessonDraft("Title", value, "Lesson")),
						"Enter the problem or situation.");
				assertFailure(context.service.createLesson(OWNER_A,
						new LessonDraft("Title", "Problem", value)),
						"Enter the lesson learned.");
			}
			check(context.database.getLessonCount() == 0,
					"Rejected creates left the database empty.");
		}
	}

	private static void testKeepSimilarLessonsSeparate() throws Exception {
		try (TestContext context = context("test3")) {
			LessonLearned first = createLesson(context, OWNER_A, "Same title", "First problem",
					"First lesson text");
			LessonLearned second = createLesson(context, OWNER_A, "Same title", "Second problem",
					"Second lesson text");
			check(first.getId() != second.getId(), "Duplicate titles received separate IDs.");
			check("First lesson text".equals(success(context.service.readLesson(
					id(first), OWNER_A)).getLessonLearned()), "First ID opens the first record.");
			check("Second lesson text".equals(success(context.service.readLesson(
					id(second), OWNER_A)).getLessonLearned()), "Second ID opens the second record.");
			check(success(context.service.listLessons(OWNER_A)).size() == 2,
					"Both similar lessons remain in the collection.");
		}
	}

	private static void testReadSavedLesson() throws Exception {
		try (TestContext context = context("test4")) {
			LessonLearned lesson = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			ExperienceDraft draft = experience("Changed the module path", "Used Eclipse settings",
					"30", effort("Adan", "20"), effort("Mahin", "10"));
			ExperienceRecord saved = success(context.service.createExperience(
					id(lesson), OWNER_A, draft));
			LessonLearned read = success(context.service.readLesson(id(lesson), OWNER_A));
			checkLessonFields(read, SAMPLE_LESSON);
			List<ExperienceRecord> records = success(context.service.listExperiences(
					id(lesson), OWNER_A));
			check(records.size() == 1, "The lesson shows one related experience.");
			check(records.get(0).getId() == saved.getId(), "The related experience ID matches.");
			checkExperience(records.get(0), lesson.getId(), "Changed the module path",
					"Used Eclipse settings", 30, new String[] {"Adan", "Mahin"},
					new double[] {20, 10});
		}
	}

	private static void testShowEmptyList() throws Exception {
		try (TestContext context = context("test5")) {
			OperationResult<LessonsCollection> empty = context.service.listLessons(OWNER_A);
			check(success(empty).isEmpty(), "Contributor with no records receives an empty list.");
			check("No lessons found.".equals(empty.getMessage()), "Empty list message is helpful.");
			createLesson(context, OWNER_A, "Database indexing", "Slow query", "Add an index");
			OperationResult<LessonsCollection> noMatch = context.service.filterLessons(
					OWNER_A, "JavaFX");
			check(success(noMatch).isEmpty(), "No-match filter returns an empty subset.");
			check("No lessons found.".equals(noMatch.getMessage()),
					"No-match subset message is helpful.");
		}
	}

	private static void testShowOneAndManyLessons() throws Exception {
		try (TestContext context = context("test6")) {
			LessonLearned only = createLesson(context, OWNER_A, "Solo marker",
					"One-record problem", "One-record lesson");
			LessonsCollection one = success(context.service.listLessons(OWNER_A));
			check(one.size() == 1 && one.findById(only.getId()) != null,
					"One-item collection preserves its ID.");

			List<Long> expectedIds = new ArrayList<Long>();
			expectedIds.add(only.getId());
			for (int index = 1; index < 250; index++) {
				String marker = index % 25 == 0 ? " selected" : "";
				LessonLearned created = createLesson(context, OWNER_A,
						"Lesson " + index + marker, "Problem " + index, "Learning " + index);
				expectedIds.add(created.getId());
			}
			LessonsCollection many = success(context.service.listLessons(OWNER_A));
			check(many.size() == 250, "Collection holds all 250 prepared lessons.");
			for (long id : expectedIds)
				check(many.findById(id) != null, "Prepared lesson ID " + id + " remains present.");
			LessonsCollection oneItem = success(context.service.filterLessons(OWNER_A,
					"Solo marker"));
			check(oneItem.size() == 1 && oneItem.findById(only.getId()) != null,
					"A one-item subset contains the expected ID.");
			LessonsCollection multiple = success(context.service.filterLessons(OWNER_A,
					"selected"));
			check(multiple.size() == 9, "Multiple-item subset contains all nine matches.");
			check(many.getLessons() instanceof List<?>,
					"Collection uses a dynamically sized List rather than a fixed entry array.");
		}
	}

	private static void testKeepContributorContentPrivate() throws Exception {
		try (TestContext context = context("test7")) {
			LessonLearned aOne = createLesson(context, OWNER_A, "A Java lesson", "A problem", "A1");
			LessonLearned aTwo = createLesson(context, OWNER_A, "A database lesson", "A problem", "A2");
			LessonLearned b = createLesson(context, OWNER_B, "B Java lesson", "B problem", "B1");
			LessonsCollection full = success(context.service.listLessons(OWNER_A));
			check(full.size() == 2 && full.findById(aOne.getId()) != null &&
					full.findById(aTwo.getId()) != null && full.findById(b.getId()) == null,
					"A's full list includes only A's two lessons.");
			LessonsCollection subset = success(context.service.filterLessons(OWNER_A, "Java"));
			check(subset.size() == 1 && subset.findById(aOne.getId()) != null,
					"A's subset does not expose B's matching title.");
			assertFailure(context.service.readLesson(id(b), OWNER_A),
					"You can only view your own lessons.");
		}
	}

	private static void testShowEditedContentAndLockStatus() throws Exception {
		try (TestContext context = context("test8")) {
			LessonLearned original = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			check(context.database.updateLessonCoreForTesting(original.getId(),
					"Curator approved title", "Curator clarified problem",
					"Curator clarified lesson"), "Curator edit was seeded.");
			check(context.database.setLessonLocksForTesting(original.getId(), false, true,
					false, false), "Title lock was seeded.");
			check(context.database.setHiddenRoleInformationForTesting(original.getId(),
					"Internal curator note"), "Separate role information was seeded.");
			LessonLearned visible = success(context.service.readLesson(id(original), OWNER_A));
			check("Curator approved title".equals(visible.getTitle()) &&
					"Curator clarified problem".equals(visible.getProblemSituation()) &&
					"Curator clarified lesson".equals(visible.getLessonLearned()),
					"Contributor sees curator-edited core content.");
			check(visible.isTitleLocked(), "Contributor-visible record marks the title as locked.");
			boolean exposesHiddenField = Arrays.stream(LessonLearned.class.getMethods())
					.anyMatch(method -> method.getName().toLowerCase().contains("hiddenrole"));
			check(!exposesHiddenField, "Contributor lesson API does not expose other-role data.");
			check("Internal curator note".equals(context.database
					.getHiddenRoleInformationForTesting(original.getId())),
					"Hidden role data remains stored separately.");
		}
	}

	private static void testRejectInvalidLessonIds() throws Exception {
		try (TestContext context = context("test9")) {
			assertFailure(context.service.readLesson("", OWNER_A), "Select a lesson.");
			assertFailure(context.service.readLesson("abc", OWNER_A),
					"Lesson ID must be a valid number.");
			assertFailure(context.service.readLesson("999999", OWNER_A),
					"Lesson not found. Refresh the list.");
			check(context.database.getLessonCount() == 0, "Invalid reads did not create data.");
		}
	}

	private static void testUpdateValidCoreInformation() throws Exception {
		try (TestContext context = context("test10")) {
			LessonLearned original = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			LessonDraft changed = new LessonDraft("Diagnosing JavaFX startup",
					"The application failed after an SDK update",
					"Verify both module path and VM arguments.");
			LessonLearned updated = success(context.service.updateLesson(id(original), OWNER_A,
					changed));
			check(updated.getId() == original.getId(), "Update retained the lesson ID.");
			check(updated.getOwner().equals(original.getOwner()), "Update retained the owner.");
			checkLessonFields(updated, changed);
			checkLessonFields(success(context.service.readLesson(id(original), OWNER_A)), changed);
			LessonsCollection subset = success(context.service.filterLessons(OWNER_A, "Diagnosing"));
			check(subset.size() == 1 && subset.findById(original.getId()) != null,
					"Refreshed subset contains the updated lesson.");
		}
	}

	private static void testRejectInvalidEdits() throws Exception {
		try (TestContext context = context("test11")) {
			LessonLearned original = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			String[] invalid = {null, "", "   "};
			for (String value : invalid) {
				assertFailure(context.service.updateLesson(id(original), OWNER_A,
						new LessonDraft(value, SAMPLE_LESSON.getProblemSituation(),
								SAMPLE_LESSON.getLessonLearned())), "Enter a lesson title.");
				assertLessonUnchanged(context, original);
				assertFailure(context.service.updateLesson(id(original), OWNER_A,
						new LessonDraft(SAMPLE_LESSON.getTitle(), value,
								SAMPLE_LESSON.getLessonLearned())),
						"Enter the problem or situation.");
				assertLessonUnchanged(context, original);
				assertFailure(context.service.updateLesson(id(original), OWNER_A,
						new LessonDraft(SAMPLE_LESSON.getTitle(),
								SAMPLE_LESSON.getProblemSituation(), value)),
						"Enter the lesson learned.");
				assertLessonUnchanged(context, original);
			}
			check(context.database.getLessonCount() == 1, "Invalid edits created no extra row.");
		}
	}

	private static void testRejectLockedEdits() throws Exception {
		try (TestContext context = context("test12")) {
			LessonLearned fieldLocked = createLesson(context, OWNER_A, "Locked title",
					"Editable problem", "Editable lesson");
			check(context.database.setLessonLocksForTesting(fieldLocked.getId(), false, true,
					false, false), "Field lock was seeded.");
			assertFailure(context.service.updateLesson(id(fieldLocked), OWNER_A,
					new LessonDraft("Changed title", "Editable problem", "Editable lesson")),
					"This field is locked and cannot be edited.");
			LessonLearned permitted = success(context.service.updateLesson(id(fieldLocked), OWNER_A,
					new LessonDraft("Locked title", "Improved unlocked problem", "Editable lesson")));
			check("Locked title".equals(permitted.getTitle()) &&
					"Improved unlocked problem".equals(permitted.getProblemSituation()),
					"Unlocked field changed without changing the locked title.");

			LessonLearned wholeLocked = createLesson(context, OWNER_A, "Whole lock",
					"Whole problem", "Whole lesson");
			check(context.database.setLessonLocksForTesting(wholeLocked.getId(), true, false,
					false, false), "Whole-lesson lock was seeded.");
			assertFailure(context.service.updateLesson(id(wholeLocked), OWNER_A,
					new LessonDraft("Changed", "Whole problem", "Whole lesson")),
					"This lesson is locked and cannot be edited.");
		}
	}

	private static void testRejectUnavailableUpdateTargets() throws Exception {
		try (TestContext context = context("test13")) {
			LessonLearned b = createLesson(context, OWNER_B, "B title", "B problem", "B lesson");
			LessonDraft change = new LessonDraft("Changed", "Changed", "Changed");
			assertFailure(context.service.updateLesson(id(b), OWNER_A, change),
					"You can only edit your own lessons.");
			assertFailure(context.service.updateLesson("", OWNER_A, change), "Select a lesson.");
			assertFailure(context.service.updateLesson("abc", OWNER_A, change),
					"Lesson ID must be a valid number.");
			assertFailure(context.service.updateLesson("999999", OWNER_A, change),
					"Lesson not found. Refresh the list.");
			LessonLearned retained = context.database.getLesson(b.getId());
			check("B title".equals(retained.getTitle()), "Rejected updates preserved B's record.");
			check(context.database.getLessonCount() == 1, "Rejected updates changed no counts.");
		}
	}

	private static void testCreateAndReadExperiences() throws Exception {
		try (TestContext context = context("test14")) {
			LessonLearned lesson = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			ExperienceRecord first = success(context.service.createExperience(id(lesson), OWNER_A,
					experience("Updated dependencies", "Used Eclipse Build Path", "30",
							effort("Adan", "20"), effort("Aditi", "10"))));
			ExperienceRecord second = success(context.service.createExperience(id(lesson), OWNER_A,
					experience("Retested launch", "Used a clean workspace", "12.5",
							effort("Adan", "12.5"))));
			check(first.getId() != second.getId(), "Two attempts received separate IDs.");
			checkExperience(success(context.service.readExperience(id(first), OWNER_A)),
					lesson.getId(), "Updated dependencies", "Used Eclipse Build Path", 30,
					new String[] {"Adan", "Aditi"}, new double[] {20, 10});
			checkExperience(success(context.service.readExperience(id(second), OWNER_A)),
					lesson.getId(), "Retested launch", "Used a clean workspace", 12.5,
					new String[] {"Adan"}, new double[] {12.5});
			check(success(context.service.listExperiences(id(lesson), OWNER_A)).size() == 2,
					"Both experiences are listed under the parent lesson.");
		}
	}

	private static void testUpdateExperience() throws Exception {
		try (TestContext context = context("test15")) {
			LessonLearned lesson = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			ExperienceRecord first = createExperience(context, lesson, "First attempt", "First method",
					"10", effort("Adan", "10"));
			ExperienceRecord second = createExperience(context, lesson, "Second attempt", "Second method",
					"20", effort("Aditi", "20"));
			ExperienceRecord updated = success(context.service.updateExperience(id(first), OWNER_A,
					experience("Changed attempt", "Changed method", "0", effort("Adan", "0"))));
			check(updated.getId() == first.getId() && updated.getLessonId() == lesson.getId(),
					"Update retained experience ID and parent.");
			check(Double.compare(updated.getElapsedMinutes(), 0) == 0 &&
					Double.compare(updated.getTeamEfforts().get(0).getEffortMinutes(), 0) == 0,
					"Zero time and effort are accepted.");
			ExperienceRecord decimal = success(context.service.updateExperience(id(first), OWNER_A,
					experience("Decimal attempt", "Decimal method", "12.75",
							effort("Adan", "7.25"), effort("Mahin", "5.5"))));
			check(Double.compare(decimal.getElapsedMinutes(), 12.75) == 0 &&
					Double.compare(decimal.getTeamEfforts().get(0).getEffortMinutes(), 7.25) == 0,
					"Finite nonnegative decimal values are accepted.");
			ExperienceRecord unchanged = success(context.service.readExperience(id(second), OWNER_A));
			check("Second attempt".equals(unchanged.getWhatWasDone()) &&
					Double.compare(unchanged.getElapsedMinutes(), 20) == 0,
					"Updating one attempt leaves the other unchanged.");
		}
	}

	private static void testDeleteExperiences() throws Exception {
		try (TestContext context = context("test16")) {
			LessonLearned lesson = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			ExperienceRecord first = createExperience(context, lesson, "First", "Method 1", "10",
					effort("Adan", "10"));
			ExperienceRecord second = createExperience(context, lesson, "Second", "Method 2", "20",
					effort("Aditi", "20"));
			success(context.service.deleteExperience(id(first), OWNER_A));
			List<ExperienceRecord> one = success(context.service.listExperiences(id(lesson), OWNER_A));
			check(one.size() == 1 && one.get(0).getId() == second.getId(),
					"Only the selected first attempt was removed.");
			success(context.service.deleteExperience(id(second), OWNER_A));
			check(success(context.service.listExperiences(id(lesson), OWNER_A)).isEmpty(),
					"Deleting the last attempt leaves an empty experience list.");
			check(context.service.readLesson(id(lesson), OWNER_A).isSuccessful(),
					"Parent lesson remains after all experience records are deleted.");
		}
	}

	private static void testRejectInvalidExperienceInput() throws Exception {
		try (TestContext context = context("test17")) {
			LessonLearned lesson = success(context.service.createLesson(OWNER_A, SAMPLE_LESSON));
			ExperienceRecord saved = createExperience(context, lesson, "Original what", "Original how",
					"10", effort("Adan", "10"));
			String[] textInvalid = {null, "", "   "};
			for (String value : textInvalid) {
				verifyInvalidExperienceCreate(context, lesson,
						experience(value, "How", "1", effort("Adan", "1")),
						"Describe what was done.");
				verifyInvalidExperienceUpdate(context, saved,
						experience(value, "How", "1", effort("Adan", "1")),
						"Describe what was done.");
				verifyInvalidExperienceCreate(context, lesson,
						experience("What", value, "1", effort("Adan", "1")),
						"Describe how it was done.");
				verifyInvalidExperienceUpdate(context, saved,
						experience("What", value, "1", effort("Adan", "1")),
						"Describe how it was done.");
				verifyInvalidExperienceCreate(context, lesson,
						experience("What", "How", "1", effort(value, "1")),
						"Enter a team member name.");
				verifyInvalidExperienceUpdate(context, saved,
						experience("What", "How", "1", effort(value, "1")),
						"Enter a team member name.");
			}

			String[] numberInvalid = {null, "abc", "-1", "NaN", "Infinity"};
			for (String value : numberInvalid) {
				verifyInvalidExperienceCreate(context, lesson,
						experience("What", "How", value, effort("Adan", "1")),
						"Time spent must be a finite number zero or greater.");
				verifyInvalidExperienceUpdate(context, saved,
						experience("What", "How", value, effort("Adan", "1")),
						"Time spent must be a finite number zero or greater.");
				verifyInvalidExperienceCreate(context, lesson,
						experience("What", "How", "1", effort("Adan", value)),
						"Effort must be a finite number zero or greater.");
				verifyInvalidExperienceUpdate(context, saved,
						experience("What", "How", "1", effort("Adan", value)),
						"Effort must be a finite number zero or greater.");
			}
			check(context.database.getExperienceCount() == 1,
					"All rejected creates and updates left one original record.");
		}
	}

	private static void testRejectUnavailableExperienceTargets() throws Exception {
		try (TestContext context = context("test18")) {
			ExperienceDraft valid = experience("What", "How", "5", effort("Adan", "5"));
			assertFailure(context.service.createExperience("999999", OWNER_A, valid),
					"Lesson not found.");

			LessonLearned bLesson = createLesson(context, OWNER_B, "B lesson", "B problem", "B lesson");
			ExperienceRecord bRecord = createExperience(context, bLesson, "B what", "B how", "6",
					effort("B", "6"));
			assertFailure(context.service.updateExperience(id(bRecord), OWNER_A, valid),
					"You can only manage your own experience records.");

			LessonLearned aLesson = createLesson(context, OWNER_A, "A lesson", "A problem", "A lesson");
			ExperienceRecord lockedField = createExperience(context, aLesson, "Locked what", "How", "7",
					effort("Adan", "7"));
			check(context.database.setExperienceLocksForTesting(lockedField.getId(), false, true,
					false, false, false), "Experience field lock was seeded.");
			assertFailure(context.service.updateExperience(id(lockedField), OWNER_A,
					experience("Changed what", "How", "7", effort("Adan", "7"))),
					"This field is locked.");

			ExperienceRecord lessonLockedRecord = createExperience(context, aLesson,
					"Delete test", "Method", "8", effort("Adan", "8"));
			check(context.database.setLessonLocksForTesting(aLesson.getId(), true, false,
					false, false), "Parent lesson lock was seeded.");
			assertFailure(context.service.deleteExperience(id(lessonLockedRecord), OWNER_A),
					"This lesson is locked.");

			for (String operationId : new String[] {"", "abc", "999999"}) {
				String expected = operationId.isEmpty() ? "Select an experience record." :
						"abc".equals(operationId) ? "Experience ID must be a valid number." :
						"Experience record not found.";
				assertFailure(context.service.readExperience(operationId, OWNER_A), expected);
				assertFailure(context.service.updateExperience(operationId, OWNER_A, valid), expected);
				assertFailure(context.service.deleteExperience(operationId, OWNER_A), expected);
			}
			check(context.database.getExperienceCount() == 3,
					"Rejected target operations preserved all three experience records.");
		}
	}

	private static void testDeleteUnlockedLesson() throws Exception {
		try (TestContext context = context("test19")) {
			LessonLearned removed = createLesson(context, OWNER_A, "Delete marker lesson",
					"Temporary problem", "Temporary lesson");
			createExperience(context, removed, "Attempt 1", "Method 1", "4", effort("Adan", "4"));
			createExperience(context, removed, "Attempt 2", "Method 2", "5", effort("Adan", "5"));
			LessonLearned retained = createLesson(context, OWNER_A, "Retained lesson",
					"Permanent problem", "Permanent lesson");
			success(context.service.deleteLesson(id(removed), OWNER_A));
			assertFailure(context.service.readLesson(id(removed), OWNER_A),
					"Lesson not found. Refresh the list.");
			LessonsCollection full = success(context.service.listLessons(OWNER_A));
			check(full.size() == 1 && full.findById(retained.getId()) != null &&
					full.findById(removed.getId()) == null,
					"Full list removed only the selected lesson.");
			check(success(context.service.filterLessons(OWNER_A, "Delete marker")).isEmpty(),
					"Filtered subset no longer contains the deleted lesson.");
			check(context.database.getExperiencesForLesson(removed.getId()).isEmpty(),
					"Related experience rows were deleted by the database.");
			check(context.database.getExperienceCount() == 0,
					"No deleted child experience remains.");
		}
	}

	private static void testRejectLockedLessonDeletion() throws Exception {
		try (TestContext context = context("test20")) {
			LessonLearned whole = createLesson(context, OWNER_A, "Whole lock", "Problem", "Lesson");
			ExperienceRecord wholeChild = createExperience(context, whole, "What", "How", "1",
					effort("Adan", "1"));
			context.database.setLessonLocksForTesting(whole.getId(), true, false, false, false);
			assertFailure(context.service.deleteLesson(id(whole), OWNER_A),
					"This lesson or one of its fields is locked and cannot be deleted.");

			LessonLearned field = createLesson(context, OWNER_A, "Field lock", "Problem", "Lesson");
			ExperienceRecord fieldChild = createExperience(context, field, "What", "How", "2",
					effort("Adan", "2"));
			context.database.setLessonLocksForTesting(field.getId(), false, false, true, false);
			assertFailure(context.service.deleteLesson(id(field), OWNER_A),
					"This lesson or one of its fields is locked and cannot be deleted.");
			check(context.database.getLesson(whole.getId()) != null &&
					context.database.getExperience(wholeChild.getId()) != null &&
					context.database.getLesson(field.getId()) != null &&
					context.database.getExperience(fieldChild.getId()) != null,
					"Both locked lessons and their related records remain.");
		}
	}

	private static void testRejectUnavailableDeletionTargets() throws Exception {
		try (TestContext context = context("test21")) {
			LessonLearned b = createLesson(context, OWNER_B, "B title", "B problem", "B lesson");
			assertFailure(context.service.deleteLesson(id(b), OWNER_A),
					"You can only delete your own lessons.");
			assertFailure(context.service.deleteLesson("", OWNER_A), "Select a lesson.");
			assertFailure(context.service.deleteLesson("abc", OWNER_A),
					"Lesson ID must be a valid number.");
			assertFailure(context.service.deleteLesson("999999", OWNER_A),
					"Lesson not found. Refresh the list.");
			check(context.database.getLessonCount() == 1 &&
					context.database.getLesson(b.getId()) != null,
					"Rejected deletions preserved the only lesson.");
		}
	}

	private static void testRefreshCollections() throws Exception {
		try (TestContext context = context("test22")) {
			LessonLearned unrelated = createLesson(context, OWNER_A, "Unrelated database lesson",
					"Problem", "Lesson");
			check(success(context.service.filterLessons(OWNER_A, "JavaFX marker")).isEmpty(),
					"Initial selected subset is empty.");
			LessonLearned matching = createLesson(context, OWNER_A, "JavaFX marker lesson",
					"Problem", "Lesson");
			LessonsCollection afterCreate = success(context.service.filterLessons(OWNER_A,
					"JavaFX marker"));
			check(afterCreate.size() == 1 && afterCreate.findById(matching.getId()) != null,
					"Created matching lesson appears once in rebuilt subset.");
			success(context.service.updateLesson(id(matching), OWNER_A,
					new LessonDraft("Renamed lesson", "Problem", "Lesson")));
			check(success(context.service.filterLessons(OWNER_A, "JavaFX marker")).isEmpty(),
					"Edited lesson leaves the no-longer-matching subset.");
			LessonsCollection fullAfterEdit = success(context.service.listLessons(OWNER_A));
			check(fullAfterEdit.size() == 2 &&
					"Renamed lesson".equals(fullAfterEdit.findById(matching.getId()).getTitle()),
					"Full list contains one current copy after the edit.");
			success(context.service.deleteLesson(id(matching), OWNER_A));
			LessonsCollection finalFull = success(context.service.listLessons(OWNER_A));
			check(finalFull.size() == 1 && finalFull.findById(unrelated.getId()) != null &&
					finalFull.findById(matching.getId()) == null,
					"Final full list retains only the unrelated current record.");
			check(success(context.service.filterLessons(OWNER_A, "JavaFX marker")).isEmpty(),
					"Final subset remains empty without stale entries.");
		}
	}

	private static void verifyInvalidExperienceCreate(TestContext context, LessonLearned lesson,
			ExperienceDraft draft, String expectedMessage) throws Exception {
		int before = context.database.getExperienceCount();
		assertFailure(context.service.createExperience(id(lesson), OWNER_A, draft), expectedMessage);
		check(context.database.getExperienceCount() == before,
				"Rejected experience create left the record count unchanged.");
	}

	private static void verifyInvalidExperienceUpdate(TestContext context, ExperienceRecord saved,
			ExperienceDraft draft, String expectedMessage) throws Exception {
		assertFailure(context.service.updateExperience(id(saved), OWNER_A, draft), expectedMessage);
		ExperienceRecord retained = context.database.getExperience(saved.getId());
		check("Original what".equals(retained.getWhatWasDone()) &&
				"Original how".equals(retained.getHowItWasDone()) &&
				Double.compare(retained.getElapsedMinutes(), 10) == 0 &&
				retained.getTeamEfforts().size() == 1 &&
				"Adan".equals(retained.getTeamEfforts().get(0).getMemberName()) &&
				Double.compare(retained.getTeamEfforts().get(0).getEffortMinutes(), 10) == 0,
				"Rejected experience update preserved the original values.");
	}

	private static void testPersistence(String testNumber, String... phases) throws Exception {
		Path directory = Files.createTempDirectory("hw2-persistence-" + testNumber + "-");
		Path databasePath = directory.resolve("lessonDatabase");
		Path statePath = directory.resolve("state.properties");
		try {
			for (String phase : phases) {
				ProcessResult result = runPersistenceProcess(phase, databasePath, statePath);
				check(result.exitCode == 0,
						"Separate process phase " + phase + " exited successfully. Output: " +
						result.output.trim());
				check(result.output.contains("Process phase passed: " + phase),
						"Separate process reported a passed " + phase + " phase.");
			}
		} finally {
			deleteTree(directory);
		}
	}

	private static ProcessResult runPersistenceProcess(String phase, Path databasePath,
			Path statePath) throws IOException, InterruptedException, URISyntaxException {
		Path java = Path.of(System.getProperty("java.home"), "bin", "java");
		Path codeLocation = Path.of(HW2PersistenceProcess.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		String classPath = codeLocation + System.getProperty("path.separator") +
				System.getProperty("java.class.path");
		Process process = new ProcessBuilder(java.toString(), "-cp", classPath,
				HW2PersistenceProcess.class.getName(), phase, databasePath.toString(),
				statePath.toString()).redirectErrorStream(true).start();
		String output = new String(process.getInputStream().readAllBytes());
		int exitCode = process.waitFor();
		return new ProcessResult(exitCode, output);
	}

	private static void deleteTree(Path directory) {
		if (!Files.exists(directory)) return;
		try (var paths = Files.walk(directory)) {
			paths.sorted((first, second) -> second.compareTo(first)).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					path.toFile().deleteOnExit();
				}
			});
		} catch (IOException e) {
			directory.toFile().deleteOnExit();
		}
	}

	private static TestContext context(String name) throws Exception {
		String url = "jdbc:h2:mem:" + name + "_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
		Database database = new Database(url);
		database.connectToDatabase();
		return new TestContext(database);
	}

	private static LessonLearned createLesson(TestContext context, String owner, String title,
			String problem, String lessonText) {
		return success(context.service.createLesson(owner,
				new LessonDraft(title, problem, lessonText)));
	}

	private static ExperienceRecord createExperience(TestContext context, LessonLearned lesson,
			String what, String how, String elapsed, EffortInput... efforts) {
		return success(context.service.createExperience(id(lesson), lesson.getOwner(),
				experience(what, how, elapsed, efforts)));
	}

	private static ExperienceDraft experience(String what, String how, String elapsed,
			EffortInput... efforts) {
		return new ExperienceDraft(what, how, elapsed,
				efforts == null ? null : Arrays.asList(efforts));
	}

	private static EffortInput effort(String member, String value) {
		return new EffortInput(member, value);
	}

	private static String id(LessonLearned lesson) {
		return Long.toString(lesson.getId());
	}

	private static String id(ExperienceRecord experience) {
		return Long.toString(experience.getId());
	}

	private static <T> T success(OperationResult<T> result) {
		if (!result.isSuccessful()) throw new AssertionError(
				"Expected success but received: " + result.getMessage());
		return result.getValue();
	}

	private static void assertFailure(OperationResult<?> result, String expectedMessage) {
		check(!result.isSuccessful(), "Operation was rejected.");
		check(expectedMessage.equals(result.getMessage()),
				"Message matched: " + expectedMessage + " Actual: " + result.getMessage());
	}

	private static void assertLessonUnchanged(TestContext context, LessonLearned expected)
			throws Exception {
		LessonLearned actual = context.database.getLesson(expected.getId());
		check(actual != null && actual.getOwner().equals(expected.getOwner()) &&
				actual.getTitle().equals(expected.getTitle()) &&
				actual.getProblemSituation().equals(expected.getProblemSituation()) &&
				actual.getLessonLearned().equals(expected.getLessonLearned()),
				"Rejected lesson edit preserved all original values.");
	}

	private static void checkLessonFields(LessonLearned lesson, LessonDraft expected) {
		check(expected.getTitle().equals(lesson.getTitle()), "Lesson title matches.");
		check(expected.getProblemSituation().equals(lesson.getProblemSituation()),
				"Problem or situation matches.");
		check(expected.getLessonLearned().equals(lesson.getLessonLearned()),
				"Lesson learned text matches.");
	}

	private static void checkExperience(ExperienceRecord record, long lessonId, String what,
			String how, double elapsed, String[] names, double[] efforts) {
		check(record.getLessonId() == lessonId, "Experience parent lesson matches.");
		check(what.equals(record.getWhatWasDone()), "What-was-done text matches.");
		check(how.equals(record.getHowItWasDone()), "How-it-was-done text matches.");
		check(Double.compare(elapsed, record.getElapsedMinutes()) == 0,
				"Elapsed minutes match.");
		check(record.getTeamEfforts().size() == names.length,
				"Team effort entry count matches.");
		for (int index = 0; index < names.length; index++) {
			TeamMemberEffort effort = record.getTeamEfforts().get(index);
			check(names[index].equals(effort.getMemberName()),
					"Team member name " + index + " matches.");
			check(Double.compare(efforts[index], effort.getEffortMinutes()) == 0,
					"Team member effort " + index + " matches.");
		}
	}

	private static void check(boolean condition, String description) {
		if (!condition) {
			checksFailed++;
			throw new AssertionError(description);
		}
		checksPassed++;
		currentChecks++;
	}

	private static void runTest(int number, String name, String expected, TestAction action) {
		System.out.println("------------------------------------------------------------");
		System.out.println("Test " + number + ": " + name);
		System.out.println("Expected: " + expected);
		currentChecks = 0;
		try {
			action.run();
			testsPassed++;
			System.out.println("Actual: All " + currentChecks + " required checks passed.");
			System.out.println("Result: PASS\n");
		} catch (Throwable failure) {
			testsFailed++;
			System.out.println("Actual: " + failure.getClass().getSimpleName() + ": " +
					failure.getMessage());
			System.out.println("Result: FAIL\n");
			failure.printStackTrace(System.out);
		}
	}

	@FunctionalInterface
	private interface TestAction {
		void run() throws Exception;
	}

	private static class TestContext implements AutoCloseable {
		private final Database database;
		private final LessonService service;

		private TestContext(Database database) {
			this.database = database;
			service = new LessonService(database);
		}

		@Override
		public void close() {
			database.closeConnection();
		}
	}

	private static class ProcessResult {
		private final int exitCode;
		private final String output;

		private ProcessResult(int exitCode, String output) {
			this.exitCode = exitCode;
			this.output = output;
		}
	}
}
