package database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import entityClasses.ExperienceRecord;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import entityClasses.TeamMemberEffort;
import lessonManagement.EffortInput;
import lessonManagement.ExperienceDraft;
import lessonManagement.LessonDraft;
import lessonManagement.LessonService;
import lessonManagement.OperationResult;

/*******
 * <p> Title: TP2BoundaryCoverageTestingAutomation Class. </p>
 *
 * <p> Description: Implements the boundary-value and coverage-oriented tests designed in
 * {@code TP2 Test Designs.pdf} for {@link Database} and {@link LessonService}. The suite focuses
 * on SQL-shaped input, contributor isolation, transaction rollback, relationship integrity,
 * persistence, identifier and numeric boundaries, locks, collection boundaries, and safe error
 * messages.</p>
 *
 * <p>Every numbered case uses a unique file-backed H2 database created in the operating system's
 * temporary directory. The production URL {@code jdbc:h2:~/FoundationDatabase} is never used.
 * Test files are deleted after each case, including when an assertion fails.</p>
 *
 * <p>Requirement traceability:</p>
 * <ul>
 * <li><b>DB-R1:</b> Test 1 verifies that SQL-shaped values are stored only as data.</li>
 * <li><b>DB-R2 and LS-R3:</b> Test 2 verifies owner isolation and unchanged foreign data.</li>
 * <li><b>DB-R3:</b> Tests 3 and 4 verify create and update rollback.</li>
 * <li><b>DB-R4:</b> Tests 5 and 12 verify relationships, cascades, and lock queries.</li>
 * <li><b>DB-R5:</b> Test 6 verifies create, update, and delete across reopened connections.</li>
 * <li><b>DB-R6 and LS-R6:</b> Test 11 verifies isolated configuration and safe failures.</li>
 * <li><b>LS-R1 and LS-R2:</b> Test 7 verifies ID partitions and missing records.</li>
 * <li><b>LS-R4:</b> Test 9 verifies complete-record and field-lock enforcement.</li>
 * <li><b>LS-R5:</b> Test 8 verifies text, time, effort, and collection boundaries.</li>
 * <li><b>LS-R7:</b> Test 10 verifies empty, one-item, many-item, and keyword subsets.</li>
 * </ul>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-11-10 Initial HW3 Task 2 implementation
 */
public class TP2BoundaryCoverageTestingAutomation {

	private static final String OWNER_A = "ContributorA";
	private static final String OWNER_B = "ContributorB";
	private static final String H2_USER = "sa";
	private static final String H2_PASSWORD = "";

	private static int testsPassed;
	private static int testsFailed;
	private static int checksPassed;
	private static int checksFailed;
	private static int currentChecks;
	private static int currentFailures;

	/**********
	 * Prevent construction because the automation uses only static test cases and counters.
	 */
	private TP2BoundaryCoverageTestingAutomation() {
	}

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run all Task 2 cases, print expected and actual evidence, and return a
	 * nonzero process status if any test fails.</p>
	 *
	 * @param args command-line arguments are not used
	 */
	public static void main(String[] args) {
		System.out.println("HW3 TASK 2 BOUNDARY AND COVERAGE TESTING");
		System.out.println("Selected classes: database.Database and lessonManagement.LessonService\n");

		runTest(1, "Treat SQL-shaped input as ordinary data",
				"Hostile-looking text is stored exactly and cannot alter tables or unrelated rows.",
				TP2BoundaryCoverageTestingAutomation::testSqlShapedInput);
		runTest(2, "Preserve contributor ownership boundaries",
				"Lists, reads, updates, and deletes never disclose or change another owner's data.",
				TP2BoundaryCoverageTestingAutomation::testOwnershipIsolation);
		runTest(3, "Roll back a failed experience creation",
				"A failure after one effort insert leaves no partial experience or effort rows.",
				TP2BoundaryCoverageTestingAutomation::testCreateRollback);
		runTest(4, "Roll back a failed experience update",
				"A failed effort replacement restores the original content and effort rows.",
				TP2BoundaryCoverageTestingAutomation::testUpdateRollback);
		runTest(5, "Preserve foreign keys and cascade only selected children",
				"Deleting one lesson removes only its experiences and efforts.",
				TP2BoundaryCoverageTestingAutomation::testCascadeIntegrity);
		runTest(6, "Persist accepted changes and deletions across reopen",
				"Created and updated values survive reopen, while deleted values remain absent.",
				TP2BoundaryCoverageTestingAutomation::testPersistenceAcrossReopen);
		runTest(7, "Cover lesson and experience ID boundaries",
				"Blank, nonpositive, malformed, overflow, valid, and missing IDs return correct results.",
				TP2BoundaryCoverageTestingAutomation::testIdentifierBoundaries);
		runTest(8, "Cover text, time, effort, and collection boundaries",
				"Valid boundaries persist exactly and invalid partitions leave counts unchanged.",
				TP2BoundaryCoverageTestingAutomation::testInputBoundaries);
		runTest(9, "Enforce complete-record and field locks",
				"Protected changes and deletes fail unchanged while unrelated unlocked changes succeed.",
				TP2BoundaryCoverageTestingAutomation::testLockBoundaries);
		runTest(10, "Cover empty, one-item, many-item, and filtered lists",
				"Contributor-only full lists and keyword subsets contain exactly the expected IDs.",
				TP2BoundaryCoverageTestingAutomation::testListAndFilterBoundaries);
		runTest(11, "Return safe errors from an unavailable database",
				"Failures provide corrective messages without SQL, credentials, paths, or stack traces.",
				TP2BoundaryCoverageTestingAutomation::testSafeFailureMessages);
		runTest(12, "Detect every persisted experience lock",
				"No record and unlocked records return false; every lock state returns true.",
				TP2BoundaryCoverageTestingAutomation::testLockedExperienceQuery);

		System.out.println("============================================================");
		System.out.println("NUMBERED TEST TOTALS");
		System.out.println("Passed: " + testsPassed);
		System.out.println("Failed: " + testsFailed);
		System.out.println("Total:  " + (testsPassed + testsFailed));
		System.out.println("DETAILED CHECK TOTALS");
		System.out.println("Passed: " + checksPassed);
		System.out.println("Failed: " + checksFailed);
		System.out.println("Total:  " + (checksPassed + checksFailed));

		if (testsFailed > 0 || checksFailed > 0)
			throw new AssertionError("HW3 Task 2 boundary and coverage testing failed.");
	}

	/**********
	 * <p><b>Test 1; DB-R1.</b> Performs CRUD using apostrophes, comments, and SQL statement-shaped
	 * text, then verifies exact values and the continued presence of an unrelated row.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testSqlShapedInput() throws Exception {
		try (TestContext context = context("sql_input")) {
			LessonsLearned normal = createLesson(context, OWNER_A, "Normal lesson");
			String hostileOwner = "owner' OR '1'='1";
			LessonDraft hostile = new LessonDraft("'); DROP TABLE lessons; --",
					"Problem ' OR '1'='1", "Lesson; DELETE FROM lessons; --");
			LessonsLearned created = success(context.service.createLesson(hostileOwner, hostile));
			LessonsLearned read = context.database.getLesson(created.getId());
			check(hostileOwner.equals(read.getOwner()), "Hostile-looking owner is stored exactly",
					hostileOwner, read.getOwner());
			check(hostile.getTitle().equals(read.getTitle()), "SQL-shaped title is stored exactly",
					hostile.getTitle(), read.getTitle());
			check(hostile.getProblemSituation().equals(read.getProblemSituation()),
					"SQL-shaped problem is stored exactly", hostile.getProblemSituation(),
					read.getProblemSituation());
			check(context.database.getLessonCount() == 2 &&
					context.database.getLesson(normal.getId()) != null,
					"Unrelated lesson and table remain intact", "2 lessons", context.database.getLessonCount() +
					" lessons");

			ExperienceDraft experience = experience("What'); DROP TABLE experienceRecords; --",
					"How -- comment", "15", effort("member' OR '1'='1", "15"));
			ExperienceRecord saved = success(context.service.createExperience(
					id(created), hostileOwner, experience));
			ExperienceRecord readExperience = context.database.getExperience(saved.getId());
			check(experience.getWhatWasDone().equals(readExperience.getWhatWasDone()),
					"SQL-shaped experience text is stored exactly", experience.getWhatWasDone(),
					readExperience.getWhatWasDone());
			check("member' OR '1'='1".equals(
					readExperience.getTeamEfforts().get(0).getMemberName()),
					"SQL-shaped member name is stored exactly", "member' OR '1'='1",
					readExperience.getTeamEfforts().get(0).getMemberName());

			LessonDraft updated = new LessonDraft("Updated ' --", "Still data; SELECT *",
					"No SQL command executed");
			check(context.service.updateLesson(id(created), hostileOwner, updated).isSuccessful(),
					"SQL-shaped update succeeds as data", "Successful", "Successful");
			check(context.service.deleteLesson(id(created), hostileOwner).isSuccessful(),
					"Selected hostile-looking record can be deleted", "Successful", "Successful");
			check(context.database.getLessonCount() == 1 &&
					context.database.getLesson(normal.getId()) != null,
					"Delete leaves unrelated row and schema intact", "1 unrelated lesson",
					context.database.getLessonCount() + " lesson(s)");
		}
	}

	/**********
	 * <p><b>Test 2; DB-R2 and LS-R3.</b> Seeds two owners and both compound experience-owner
	 * mismatch combinations. Rejected operations are compared with their original records.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testOwnershipIsolation() throws Exception {
		try (TestContext context = context("ownership")) {
			LessonsLearned aLesson = createLesson(context, OWNER_A, "Owner A lesson");
			LessonsLearned bFirst = createLesson(context, OWNER_B, "Owner B first");
			createLesson(context, OWNER_B, "Owner B second");
			check(success(context.service.listLessons(OWNER_A)).size() == 1,
					"Owner A receives one own lesson", "1", String.valueOf(
						success(context.service.listLessons(OWNER_A)).size()));
			check(success(context.service.listLessons(OWNER_B)).size() == 2,
					"Owner B receives both own lessons", "2", String.valueOf(
						success(context.service.listLessons(OWNER_B)).size()));
			check(success(context.service.listLessons("ContributorC")).isEmpty(),
					"Owner with zero rows receives an empty list", "Empty", "Empty");

			assertFailure(context.service.readLesson(id(aLesson), OWNER_B),
					"You can only view your own lessons.");
			assertFailure(context.service.updateLesson(id(aLesson), OWNER_B,
					new LessonDraft("Blocked", "Blocked", "Blocked")),
					"You can only edit your own lessons.");
			assertFailure(context.service.deleteLesson(id(aLesson), OWNER_B),
					"You can only delete your own lessons.");
			check("Owner A lesson".equals(context.database.getLesson(aLesson.getId()).getTitle()),
					"Rejected foreign operations leave Owner A unchanged", "Owner A lesson",
					context.database.getLesson(aLesson.getId()).getTitle());

			ExperienceRecord experienceOwnedByB = directExperience(context, aLesson.getId(), OWNER_B,
					"B experience under A lesson");
			assertFailure(context.service.readExperience(id(experienceOwnedByB), OWNER_A),
					"You can only manage your own experience records.");
			ExperienceRecord experienceOwnedByA = directExperience(context, bFirst.getId(), OWNER_A,
					"A experience under B lesson");
			assertFailure(context.service.readExperience(id(experienceOwnedByA), OWNER_A),
					"You can only manage your own experience records.");
			check(context.database.getExperienceCount() == 2,
					"Rejected compound ownership checks preserve both records", "2", String.valueOf(
						context.database.getExperienceCount()));
		}
	}

	/**********
	 * <p><b>Test 3; DB-R3.</b> Adds a temporary database constraint that permits the first effort
	 * and rejects the second, proving the surrounding transaction removes the partially inserted
	 * parent and child rows.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testCreateRollback() throws Exception {
		try (TestContext context = context("create_rollback")) {
			LessonsLearned lesson = createLesson(context, OWNER_A, "Atomic creation");
			addEffortLimit(context.url);
			ExperienceDraft failing = experience("Attempt", "Method", "30",
					effort("First", "50"), effort("Second", "150"));
			assertFailure(context.service.createExperience(id(lesson), OWNER_A, failing),
					"The experience could not be saved. Try again.");
			check(context.database.getExperienceCount() == 0,
					"Failed create rolls back the experience row", "0", String.valueOf(
						context.database.getExperienceCount()));
			check(queryCount(context.url, "SELECT COUNT(*) FROM experienceEfforts") == 0,
					"Failed create rolls back the first effort row", "0", String.valueOf(
						queryCount(context.url, "SELECT COUNT(*) FROM experienceEfforts")));

			dropEffortLimit(context.url);
			ExperienceRecord saved = success(context.service.createExperience(id(lesson), OWNER_A,
					experience("Retry", "Method", "30", effort("First", "50"))));
			check(saved != null && context.database.getExperienceCount() == 1,
					"Database remains usable after rollback", "1 saved experience",
					context.database.getExperienceCount() + " saved experience(s)");
		}
	}

	/**********
	 * <p><b>Test 4; DB-R3.</b> Forces the second replacement effort to fail and verifies rollback
	 * restores both the original core values and original effort rows.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testUpdateRollback() throws Exception {
		try (TestContext context = context("update_rollback")) {
			LessonsLearned lesson = createLesson(context, OWNER_A, "Atomic update");
			ExperienceRecord original = success(context.service.createExperience(id(lesson), OWNER_A,
					experience("Original what", "Original how", "30",
						effort("First", "20"), effort("Second", "30"))));
			addEffortLimit(context.url);
			assertFailure(context.service.updateExperience(id(original), OWNER_A,
					experience("Changed what", "Changed how", "60",
						effort("First", "50"), effort("Second", "150"))),
					"The experience could not be updated. Try again.");
			ExperienceRecord reloaded = context.database.getExperience(original.getId());
			check("Original what".equals(reloaded.getWhatWasDone()) &&
					"Original how".equals(reloaded.getHowItWasDone()) &&
					Double.compare(30, reloaded.getElapsedMinutes()) == 0,
					"Failed update restores original core values",
					"Original what, Original how, 30.0", reloaded.getWhatWasDone() + ", " +
						reloaded.getHowItWasDone() + ", " + reloaded.getElapsedMinutes());
			check(effortsMatch(reloaded, new String[] {"First", "Second"},
					new double[] {20, 30}), "Failed update restores original effort rows",
					"First=20.0, Second=30.0", effortDescription(reloaded));

			dropEffortLimit(context.url);
			OperationResult<ExperienceRecord> retry = context.service.updateExperience(id(original),
					OWNER_A, experience("Retry what", "Retry how", "60", effort("First", "40")));
			check(retry.isSuccessful(), "Database remains usable after update rollback",
					"Successful", resultDescription(retry));
		}
	}

	/**********
	 * <p><b>Test 5; DB-R4.</b> Covers lessons with zero, one, and multiple children and confirms
	 * that cascading deletion affects only the selected relationship tree.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testCascadeIntegrity() throws Exception {
		try (TestContext context = context("cascade")) {
			LessonsLearned noChild = createLesson(context, OWNER_A, "No child");
			LessonsLearned oneChild = createLesson(context, OWNER_A, "One child");
			LessonsLearned manyChildren = createLesson(context, OWNER_A, "Many children");
			LessonsLearned unrelated = createLesson(context, OWNER_B, "Unrelated");
			createExperience(context, oneChild, OWNER_A, "One");
			createExperience(context, manyChildren, OWNER_A, "Many one");
			createExperience(context, manyChildren, OWNER_A, "Many two");
			ExperienceRecord unrelatedExperience = createExperience(context, unrelated, OWNER_B,
					"Unrelated experience");
			check(context.database.deleteLesson(manyChildren.getId()),
					"Existing lesson with many children is deleted", "true", "true");
			check(context.database.getExperiencesForLesson(manyChildren.getId()).isEmpty(),
					"Selected child experiences are cascaded", "Empty", "Empty");
			check(context.database.getExperience(unrelatedExperience.getId()) != null,
					"Unrelated experience remains", "Present", "Present");
			check(context.database.deleteLesson(noChild.getId()),
					"Lesson with no children can be deleted", "true", "true");
			check(!context.database.deleteLesson(Long.MAX_VALUE),
					"Missing parent delete returns false", "false", "false");
			check(context.database.getLesson(oneChild.getId()) != null &&
					context.database.getLesson(unrelated.getId()) != null,
					"Other parent records remain", "Present", "Present");
			check(queryCount(context.url, "SELECT COUNT(*) FROM experienceEfforts") == 2,
					"Only effort rows for remaining experiences remain", "2", String.valueOf(
						queryCount(context.url, "SELECT COUNT(*) FROM experienceEfforts")));
		}
	}

	/**********
	 * <p><b>Test 6; DB-R5.</b> Creates and updates data, reopens a separate connection, verifies
	 * exact values, deletes the records, and reopens again to prove absence.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testPersistenceAcrossReopen() throws Exception {
		try (TestContext context = context("persistence")) {
			String longTitle = repeat('T', 500);
			LessonsLearned lesson = success(context.service.createLesson(OWNER_A,
					new LessonDraft(longTitle, "Persistent problem", "Persistent lesson")));
			ExperienceRecord experience = success(context.service.createExperience(id(lesson), OWNER_A,
					experience("Persistent what", "Persistent how", "0",
						effort("Adan", Double.toString(Double.MAX_VALUE)))));
			success(context.service.updateLesson(id(lesson), OWNER_A,
					new LessonDraft(longTitle, "Updated problem", "Updated lesson")));
			context.reopen();
			LessonsLearned reopenedLesson = context.database.getLesson(lesson.getId());
			ExperienceRecord reopenedExperience = context.database.getExperience(experience.getId());
			check(reopenedLesson != null && "Updated problem".equals(
					reopenedLesson.getProblemSituation()), "Lesson update survives reopen",
					"Updated problem", reopenedLesson == null ? "Missing" :
						reopenedLesson.getProblemSituation());
			check(reopenedExperience != null && Double.compare(0,
					reopenedExperience.getElapsedMinutes()) == 0,
					"Zero elapsed time survives reopen", "0.0", reopenedExperience == null ?
						"Missing" : String.valueOf(reopenedExperience.getElapsedMinutes()));
			check(reopenedExperience != null && Double.compare(Double.MAX_VALUE,
					reopenedExperience.getTeamEfforts().get(0).getEffortMinutes()) == 0,
					"Maximum finite effort survives reopen", Double.toString(Double.MAX_VALUE),
					reopenedExperience == null ? "Missing" : String.valueOf(
						reopenedExperience.getTeamEfforts().get(0).getEffortMinutes()));

			success(context.service.deleteExperience(id(experience), OWNER_A));
			success(context.service.deleteLesson(id(lesson), OWNER_A));
			context.reopen();
			check(context.database.getLesson(lesson.getId()) == null &&
					context.database.getExperience(experience.getId()) == null,
					"Deleted records remain absent after a second reopen", "Both absent",
					"Lesson=" + context.database.getLesson(lesson.getId()) + ", experience=" +
						context.database.getExperience(experience.getId()));
		}
	}

	/**********
	 * <p><b>Test 7; LS-R1 and LS-R2.</b> Exercises blank, below-minimum, boundary, malformed,
	 * overflow, existing, and missing identifier partitions through public service methods.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testIdentifierBoundaries() throws Exception {
		try (TestContext context = context("identifiers")) {
			LessonsLearned lesson = createLesson(context, OWNER_A, "ID lesson");
			ExperienceRecord experience = createExperience(context, lesson, OWNER_A, "ID experience");
			LessonDraft lessonDraft = new LessonDraft("Valid", "Valid", "Valid");
			ExperienceDraft experienceDraft = experience("Valid", "Valid", "1",
					effort("Adan", "1"));

			for (String input : new String[] {null, "", "   "}) {
				assertFailure(context.service.readLesson(input, OWNER_A), "Select a lesson.");
				assertFailure(context.service.createExperience(input, OWNER_A, experienceDraft),
						"Select a lesson.");
				assertFailure(context.service.readExperience(input, OWNER_A),
						"Select an experience record.");
			}
			for (String input : new String[] {"-1", "0", "1.0", "abc",
					"9223372036854775808"}) {
				assertFailure(context.service.readLesson(input, OWNER_A),
						"Lesson ID must be a valid number.");
				assertFailure(context.service.updateExperience(input, OWNER_A, experienceDraft),
						"Experience ID must be a valid number.");
			}
			check(context.service.readLesson(id(lesson), OWNER_A).isSuccessful(),
					"Smallest generated lesson ID is accepted", "Successful", "Successful");
			check(context.service.readExperience(id(experience), OWNER_A).isSuccessful(),
					"Smallest generated experience ID is accepted", "Successful", "Successful");
			long missing = Math.max(lesson.getId(), experience.getId()) + 1;
			assertFailure(context.service.readLesson(String.valueOf(missing), OWNER_A),
					"Lesson not found. Refresh the list.");
			assertFailure(context.service.updateLesson(String.valueOf(Long.MAX_VALUE), OWNER_A,
					lessonDraft), "Lesson not found. Refresh the list.");
			assertFailure(context.service.deleteExperience(String.valueOf(Long.MAX_VALUE), OWNER_A),
					"Experience record not found.");
			check(context.database.getLessonCount() == 1 &&
					context.database.getExperienceCount() == 1,
					"Rejected ID partitions leave storage unchanged", "1 lesson and 1 experience",
					context.database.getLessonCount() + " lesson(s), " +
						context.database.getExperienceCount() + " experience(s)");
		}
	}

	/**********
	 * <p><b>Test 8; LS-R5 and database boundaries.</b> Covers 1, 500, and 501-character titles;
	 * required text partitions; finite numeric boundaries; and missing effort collections.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testInputBoundaries() throws Exception {
		try (TestContext context = context("input_boundaries")) {
			LessonsLearned oneCharacter = success(context.service.createLesson(OWNER_A,
					new LessonDraft("A", "Problem", "Lesson")));
			LessonsLearned fiveHundred = success(context.service.createLesson(OWNER_A,
					new LessonDraft(repeat('B', 500), "Problem", "Lesson")));
			check(oneCharacter.getTitle().length() == 1 && fiveHundred.getTitle().length() == 500,
					"Minimum and database title boundary are accepted", "1 and 500", oneCharacter.getTitle().length() +
					" and " + fiveHundred.getTitle().length());
			int lessonCount = context.database.getLessonCount();
			assertFailure(context.service.createLesson(OWNER_A,
					new LessonDraft(repeat('C', 501), "Problem", "Lesson")),
					"The lesson could not be saved. Try again.");
			check(context.database.getLessonCount() == lessonCount,
					"501-character title leaves lesson count unchanged", String.valueOf(lessonCount),
					String.valueOf(context.database.getLessonCount()));

			for (String value : new String[] {null, "", "   "}) {
				assertFailure(context.service.createLesson(OWNER_A,
						new LessonDraft(value, "Problem", "Lesson")), "Enter a lesson title.");
				assertFailure(context.service.createLesson(OWNER_A,
						new LessonDraft("Title", value, "Lesson")),
						"Enter the problem or situation.");
				assertFailure(context.service.createLesson(OWNER_A,
						new LessonDraft("Title", "Problem", value)), "Enter the lesson learned.");
			}

			LessonsLearned lesson = createLesson(context, OWNER_A, "Experience boundaries");
			for (String value : new String[] {"0", "0.001", "60", Double.toString(Double.MAX_VALUE)}) {
				ExperienceRecord saved = success(context.service.createExperience(id(lesson), OWNER_A,
						experience("What", "How", value, effort("Adan", value))));
				check(Double.isFinite(saved.getElapsedMinutes()) && saved.getElapsedMinutes() >= 0,
						"Finite nonnegative value is accepted: " + value,
						value, String.valueOf(saved.getElapsedMinutes()));
			}
			int validExperienceCount = context.database.getExperienceCount();
			for (String value : new String[] {null, "", "   ", "-1", "-0.001", "NaN",
					"Infinity", "-Infinity", "one"}) {
				assertFailure(context.service.createExperience(id(lesson), OWNER_A,
						experience("What", "How", value, effort("Adan", "1"))),
						"Time spent must be a finite number zero or greater.");
				assertFailure(context.service.createExperience(id(lesson), OWNER_A,
						experience("What", "How", "1", effort("Adan", value))),
						"Effort must be a finite number zero or greater.");
			}
			assertFailure(context.service.createExperience(id(lesson), OWNER_A,
					new ExperienceDraft("What", "How", "1", null)), "Enter a team member name.");
			assertFailure(context.service.createExperience(id(lesson), OWNER_A,
					new ExperienceDraft("What", "How", "1", new ArrayList<EffortInput>())),
					"Enter a team member name.");
			List<EffortInput> nullItem = new ArrayList<EffortInput>();
			nullItem.add(null);
			assertFailure(context.service.createExperience(id(lesson), OWNER_A,
					new ExperienceDraft("What", "How", "1", nullItem)),
					"Enter a team member name.");
			check(context.database.getExperienceCount() == validExperienceCount,
					"All invalid numeric and effort partitions leave counts unchanged",
					String.valueOf(validExperienceCount), String.valueOf(
						context.database.getExperienceCount()));
		}
	}

	/**********
	 * <p><b>Test 9; LS-R4.</b> Exercises every experience field lock, unchanged locked values,
	 * unrelated unlocked edits, complete-record locks, and deletion protection.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testLockBoundaries() throws Exception {
		try (TestContext context = context("locks")) {
			LessonsLearned lesson = createLesson(context, OWNER_A, "Lock lesson");
			ExperienceRecord experience = success(context.service.createExperience(id(lesson), OWNER_A,
					experience("Original what", "Original how", "30", effort("Adan", "20"))));

			boolean[][] fieldLocks = {
					{true, false, false, false}, {false, true, false, false},
					{false, false, true, false}, {false, false, false, true}};
			ExperienceDraft[] changedDrafts = {
					experience("Changed what", "Original how", "30", effort("Adan", "20")),
					experience("Original what", "Changed how", "30", effort("Adan", "20")),
					experience("Original what", "Original how", "31", effort("Adan", "20")),
					experience("Original what", "Original how", "30", effort("Adan", "21"))};
			for (int index = 0; index < fieldLocks.length; index++) {
				boolean[] locks = fieldLocks[index];
				check(context.database.setExperienceLocksForTesting(experience.getId(), false,
						locks[0], locks[1], locks[2], locks[3]),
						"Field lock variant " + index + " is seeded", "true", "true");
				ExperienceRecord before = context.database.getExperience(experience.getId());
				OperationResult<ExperienceRecord> result = context.service.updateExperience(
						id(experience), OWNER_A, changedDrafts[index]);
				check(!result.isSuccessful() && recordsEqual(before,
						context.database.getExperience(experience.getId())),
						"Locked field variant " + index + " rejects change unchanged",
						"Rejected and unchanged", resultDescription(result));
				check(!context.service.deleteExperience(id(experience), OWNER_A).isSuccessful(),
						"Locked field variant " + index + " blocks deletion", "Rejected", "Rejected");
			}

			check(context.database.setExperienceLocksForTesting(experience.getId(), false,
					false, false, true, false), "Unrelated field lock is seeded", "true", "true");
			OperationResult<ExperienceRecord> unrelated = context.service.updateExperience(
					id(experience), OWNER_A,
					experience("Allowed what", "Original how", "30", effort("Adan", "20")));
			check(unrelated.isSuccessful(), "Unrelated unlocked field can change", "Successful",
					resultDescription(unrelated));

			check(context.database.setExperienceLocksForTesting(experience.getId(), true,
					false, false, false, false), "Complete record lock is seeded", "true", "true");
			ExperienceRecord beforeRecordLock = context.database.getExperience(experience.getId());
			check(!context.service.updateExperience(id(experience), OWNER_A,
					experience("Blocked", "Original how", "30", effort("Adan", "20"))).isSuccessful() &&
					recordsEqual(beforeRecordLock, context.database.getExperience(experience.getId())),
					"Complete record lock blocks edit unchanged", "Rejected and unchanged",
					"Rejected and unchanged");
			check(!context.service.deleteExperience(id(experience), OWNER_A).isSuccessful(),
					"Complete record lock blocks deletion", "Rejected", "Rejected");
		}
	}

	/**********
	 * <p><b>Test 10; LS-R7.</b> Exercises zero, one, and many contributor-only lessons with blank,
	 * exact, partial, case-insensitive, repeated, multiple-keyword, and no-match filters.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testListAndFilterBoundaries() throws Exception {
		try (TestContext context = context("lists")) {
			check(success(context.service.listLessons(OWNER_A)).isEmpty(),
					"Empty contributor list is supported", "Empty", "Empty");
			LessonsLearned first = createLesson(context, OWNER_A, "JavaFX Security Setup");
			check(success(context.service.listLessons(OWNER_A)).size() == 1,
					"One-item contributor list is supported", "1", "1");
			for (int index = 0; index < 75; index++) {
				String title = index % 3 == 0 ? "JavaFX Security Lesson " + index :
						index % 3 == 1 ? "Database Integrity Lesson " + index :
						"Networking Lesson " + index;
				createLesson(context, OWNER_A, title);
			}
			for (int index = 0; index < 10; index++)
				createLesson(context, OWNER_B, "JavaFX Security Foreign " + index);
			LessonsLearnedList full = success(context.service.listLessons(OWNER_A));
			check(full.size() == 76, "Many-item list has no fixed practical limit", "76",
					String.valueOf(full.size()));
			check(full.getLessons().stream().allMatch(item -> OWNER_A.equals(item.getOwner())),
					"Many-item list contains only the requested owner", "All Owner A", "All Owner A");
			check(success(context.service.filterLessons(OWNER_A, "")).size() == 76,
					"Blank filter returns the complete list", "76", String.valueOf(
						success(context.service.filterLessons(OWNER_A, "")).size()));
			check(success(context.service.filterLessons(OWNER_A, "javafx security")).size() == 26,
					"Multiple keywords match regardless of case", "26", String.valueOf(
						success(context.service.filterLessons(OWNER_A, "javafx security")).size()));
			check(success(context.service.filterLessons(OWNER_A, "SECURITY security")).size() == 26,
					"Repeated keywords are normalized", "26", String.valueOf(
						success(context.service.filterLessons(OWNER_A, "SECURITY security")).size()));
			check(success(context.service.filterLessons(OWNER_A, "no-match-value")).isEmpty(),
					"No-match filter returns an empty subset", "Empty", "Empty");
			check(full.findById(first.getId()) != null,
					"First generated ID remains selectable in the large list", "Present", "Present");
		}
	}

	/**********
	 * <p><b>Test 11; DB-R6 and LS-R6.</b> Closes an isolated database before operations and checks
	 * that service results do not expose the underlying URL, SQL text, credentials, or stack trace.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testSafeFailureMessages() throws Exception {
		try (TestContext context = context("safe_failures")) {
			LessonsLearned lesson = createLesson(context, OWNER_A, "Safe failure lesson");
			ExperienceRecord experience = createExperience(context, lesson, OWNER_A, "Safe failure");
			context.database.closeConnection();
			List<OperationResult<?>> results = new ArrayList<OperationResult<?>>();
			results.add(context.service.listLessons(OWNER_A));
			results.add(context.service.readLesson(id(lesson), OWNER_A));
			results.add(context.service.readExperience(id(experience), OWNER_A));
			results.add(context.service.listExperiences(id(lesson), OWNER_A));
			for (OperationResult<?> result : results) {
				check(!result.isSuccessful(), "Unavailable database returns failure", "Failure",
						resultDescription(result));
				check(isSafeMessage(result.getMessage(), context.url),
						"Failure message hides SQL, credentials, paths, and stack traces",
						"Safe user-facing message", result.getMessage());
			}
			context.reopen();
			check(context.database.getLesson(lesson.getId()) != null &&
					context.database.getExperience(experience.getId()) != null,
					"Failed reads leave persistent data unchanged", "Lesson and experience present",
					"Lesson and experience present");
		}
	}

	/**********
	 * <p><b>Test 12; DB-R4 and lock coverage.</b> Covers no child, unlocked child, each individual
	 * lock, and multiple-lock conditions in {@link Database#hasLockedExperienceForLesson(long)}.</p>
	 *
	 * @throws Exception when the isolated database cannot be used
	 */
	protected static void testLockedExperienceQuery() throws Exception {
		try (TestContext context = context("lock_query")) {
			LessonsLearned lesson = createLesson(context, OWNER_A, "Lock query lesson");
			check(!context.database.hasLockedExperienceForLesson(lesson.getId()),
					"Lesson with no experiences reports no lock", "false", "false");
			ExperienceRecord experience = createExperience(context, lesson, OWNER_A, "Lock query");
			check(!context.database.hasLockedExperienceForLesson(lesson.getId()),
					"Unlocked experience reports no lock", "false", "false");
			boolean[][] variants = {
					{true, false, false, false, false},
					{false, true, false, false, false},
					{false, false, true, false, false},
					{false, false, false, true, false},
					{false, false, false, false, true},
					{true, true, true, true, true}};
			for (int index = 0; index < variants.length; index++) {
				boolean[] locks = variants[index];
				context.database.setExperienceLocksForTesting(experience.getId(), locks[0], locks[1],
						locks[2], locks[3], locks[4]);
				check(context.database.hasLockedExperienceForLesson(lesson.getId()),
						"Lock query recognizes variant " + index, "true", "true");
			}
		}
	}

	private static TestContext context(String name) throws Exception {
		return new TestContext(name);
	}

	private static LessonsLearned createLesson(TestContext context, String owner, String title) {
		return success(context.service.createLesson(owner,
				new LessonDraft(title, "Problem for " + title, "Lesson from " + title)));
	}

	private static ExperienceRecord createExperience(TestContext context, LessonsLearned lesson,
			String owner, String what) {
		return success(context.service.createExperience(id(lesson), owner,
				experience(what, "Method for " + what, "30", effort("Adan", "20"))));
	}

	private static ExperienceRecord directExperience(TestContext context, long lessonId,
			String owner, String what) throws SQLException {
		List<TeamMemberEffort> efforts = new ArrayList<TeamMemberEffort>();
		efforts.add(new TeamMemberEffort("Adan", 10));
		long id = context.database.createExperience(new ExperienceRecord(0, lessonId, owner,
				what, "Direct method", 10, efforts, false, false, false, false, false));
		return context.database.getExperience(id);
	}

	private static ExperienceDraft experience(String what, String how, String elapsed,
			EffortInput... efforts) {
		List<EffortInput> values = new ArrayList<EffortInput>();
		if (efforts != null) {
			for (EffortInput effort : efforts) values.add(effort);
		}
		return new ExperienceDraft(what, how, elapsed, values);
	}

	private static EffortInput effort(String member, String minutes) {
		return new EffortInput(member, minutes);
	}

	private static String id(LessonsLearned lesson) {
		return String.valueOf(lesson.getId());
	}

	private static String id(ExperienceRecord experience) {
		return String.valueOf(experience.getId());
	}

	private static <T> T success(OperationResult<T> result) {
		if (!result.isSuccessful())
			throw new AssertionError("Expected success, but received: " + result.getMessage());
		return result.getValue();
	}

	private static void assertFailure(OperationResult<?> result, String expectedMessage) {
		check(!result.isSuccessful(), "Operation is rejected", "Failure", resultDescription(result));
		check(expectedMessage.equals(result.getMessage()), "Failure message matches",
				expectedMessage, result.getMessage());
	}

	private static void addEffortLimit(String url) throws SQLException {
		executeSql(url, "ALTER TABLE experienceEfforts ADD CONSTRAINT effort_limit " +
				"CHECK (effortMinutes <= 100)");
	}

	private static void dropEffortLimit(String url) throws SQLException {
		executeSql(url, "ALTER TABLE experienceEfforts DROP CONSTRAINT effort_limit");
	}

	private static void executeSql(String url, String sql) throws SQLException {
		try (Connection connection = DriverManager.getConnection(url, H2_USER, H2_PASSWORD);
				Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	private static int queryCount(String url, String sql) throws SQLException {
		try (Connection connection = DriverManager.getConnection(url, H2_USER, H2_PASSWORD);
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			return result.next() ? result.getInt(1) : 0;
		}
	}

	private static boolean effortsMatch(ExperienceRecord record, String[] names, double[] values) {
		if (record.getTeamEfforts().size() != names.length || names.length != values.length)
			return false;
		for (int index = 0; index < names.length; index++) {
			TeamMemberEffort effort = record.getTeamEfforts().get(index);
			if (!names[index].equals(effort.getMemberName()) ||
					Double.compare(values[index], effort.getEffortMinutes()) != 0) return false;
		}
		return true;
	}

	private static boolean recordsEqual(ExperienceRecord first, ExperienceRecord second) {
		return first != null && second != null && first.getId() == second.getId() &&
				first.getLessonId() == second.getLessonId() && first.getOwner().equals(second.getOwner()) &&
				first.getWhatWasDone().equals(second.getWhatWasDone()) &&
				first.getHowItWasDone().equals(second.getHowItWasDone()) &&
				Double.compare(first.getElapsedMinutes(), second.getElapsedMinutes()) == 0 &&
				first.isExperienceLocked() == second.isExperienceLocked() &&
				first.isWhatWasDoneLocked() == second.isWhatWasDoneLocked() &&
				first.isHowItWasDoneLocked() == second.isHowItWasDoneLocked() &&
				first.isElapsedTimeLocked() == second.isElapsedTimeLocked() &&
				first.isTeamEffortLocked() == second.isTeamEffortLocked() &&
				effortsEqual(first.getTeamEfforts(), second.getTeamEfforts());
	}

	private static boolean effortsEqual(List<TeamMemberEffort> first,
			List<TeamMemberEffort> second) {
		if (first.size() != second.size()) return false;
		for (int index = 0; index < first.size(); index++) {
			TeamMemberEffort a = first.get(index);
			TeamMemberEffort b = second.get(index);
			if (!a.getMemberName().equals(b.getMemberName()) ||
					Double.compare(a.getEffortMinutes(), b.getEffortMinutes()) != 0) return false;
		}
		return true;
	}

	private static String effortDescription(ExperienceRecord record) {
		StringBuilder result = new StringBuilder();
		for (TeamMemberEffort effort : record.getTeamEfforts()) {
			if (result.length() > 0) result.append(", ");
			result.append(effort.getMemberName()).append('=').append(effort.getEffortMinutes());
		}
		return result.toString();
	}

	private static boolean isSafeMessage(String message, String databaseUrl) {
		if (message == null || message.isBlank()) return false;
		String normalized = message.toLowerCase();
		return !normalized.contains("jdbc") && !normalized.contains("select ") &&
				!normalized.contains("insert ") && !normalized.contains("password") &&
				!normalized.contains("exception") && !message.contains(databaseUrl);
	}

	private static String resultDescription(OperationResult<?> result) {
		return (result.isSuccessful() ? "Successful" : "Failure") +
				"; message=\"" + result.getMessage() + "\"";
	}

	private static String repeat(char value, int count) {
		StringBuilder result = new StringBuilder(count);
		for (int index = 0; index < count; index++) result.append(value);
		return result.toString();
	}

	private static void runTest(int number, String name, String expected, TestCase test) {
		currentChecks = 0;
		currentFailures = 0;
		System.out.println("============================================================");
		System.out.println("Test " + number + ": " + name);
		System.out.println("Expected: " + expected);
		try {
			test.run();
		} catch (Throwable error) {
			currentFailures++;
			checksFailed++;
			System.out.println("  Actual: Unexpected " + error.getClass().getSimpleName() +
					": " + error.getMessage());
		}
		if (currentFailures == 0) {
			testsPassed++;
			System.out.println("Result: PASS (" + currentChecks + " checks)");
		} else {
			testsFailed++;
			System.out.println("Result: FAIL (" + currentFailures + " failed checks)");
		}
		System.out.println();
	}

	private static void check(boolean condition, String description, String expected,
			String actual) {
		currentChecks++;
		System.out.println("  Check: " + description);
		System.out.println("    Expected: " + expected);
		System.out.println("    Actual:   " + actual);
		if (condition) {
			checksPassed++;
			System.out.println("    PASS");
		} else {
			checksFailed++;
			currentFailures++;
			System.out.println("    FAIL");
		}
	}

	@FunctionalInterface
	private interface TestCase {
		void run() throws Exception;
	}

	/*******
	 * Owns one isolated file-backed H2 database and removes only that database's temporary
	 * directory. Reopen creates a separate Database object against the same persistent file.
	 */
	private static class TestContext implements AutoCloseable {
		private final Path directory;
		private final String url;
		private Database database;
		private LessonService service;

		private TestContext(String name) throws Exception {
			directory = Files.createTempDirectory("hw3_tp2_" + name + "_");
			String databasePath = directory.resolve("coverage_test").toAbsolutePath().toString();
			url = "jdbc:h2:file:" + databasePath + ";DB_CLOSE_DELAY=0";
			reopen();
		}

		private void reopen() throws SQLException {
			if (database != null) database.closeConnection();
			database = new Database(url);
			database.connectToDatabase();
			service = new LessonService(database);
		}

		@Override
		public void close() throws Exception {
			if (database != null) database.closeConnection();
			deleteRecursively(directory);
		}
	}

	private static void deleteRecursively(Path directory) throws IOException {
		if (directory == null || !Files.exists(directory)) return;
		try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new CleanupFailure(e);
				}
			});
		} catch (CleanupFailure failure) {
			throw failure.cause;
		}
	}

	private static class CleanupFailure extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final IOException cause;

		private CleanupFailure(IOException cause) {
			super(cause);
			this.cause = cause;
		}
	}
}
