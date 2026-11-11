package lessonManagement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import database.Database;
import entityClasses.ExperienceRecord;
import entityClasses.LessonsLearned;
import entityClasses.TeamMemberEffort;

/*******
 * <p> Title: ExperienceLockPolicyTestingAutomation Class. </p>
 *
 * <p> Description: Executes the automated tests for the HW3 risk-reduction prototype. The cases
 * cover every rule documented in Which Aspect and Why: unlocked edits, field-lock isolation,
 * complete-record locks, deletion protection, helpful feedback, repeatability, immutable state,
 * and the integration between the policy and {@link LessonService}.</p>
 *
 * <p>The tests use a small in-memory fake of the Foundations database API. It stores only the
 * lesson and experience needed by each service test, so no H2 file or existing account data is
 * changed. Every numbered test prints its expected behavior, each actual check, and the final
 * pass or fail totals.</p>
 *
 * <p>Requirement traceability:</p>
 * <ul>
 * <li><b>PR-1:</b> Tests 1 and 11 verify unlocked edits.</li>
 * <li><b>PR-2:</b> Tests 2 and 11 verify that each field lock protects its own field.</li>
 * <li><b>PR-3:</b> Tests 3 and 11 verify that unrelated unlocked fields remain editable.</li>
 * <li><b>PR-4:</b> Test 4 verifies that a complete-record lock blocks every edit.</li>
 * <li><b>PR-5:</b> Tests 5 and 12 verify deletion of a fully unlocked record.</li>
 * <li><b>PR-6:</b> Tests 6 and 12 verify complete-record deletion protection.</li>
 * <li><b>PR-7:</b> Tests 7 and 12 verify deletion protection for every field lock.</li>
 * <li><b>PR-8:</b> Test 8 verifies readable approval, rejection, and corrective messages.</li>
 * <li><b>PR-9:</b> Tests 9 and 10 verify immutable input and repeatable decisions.</li>
 * </ul>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-11-10 Initial HW3 prototype tests
 */
public class ExperienceLockPolicyTestingAutomation {

	private static final String OWNER = "ContributorA";
	private static final long LESSON_ID = 101;
	private static final long EXPERIENCE_ID = 201;

	private static int testsPassed;
	private static int testsFailed;
	private static int checksPassed;
	private static int checksFailed;
	private static int currentChecks;
	private static int currentFailures;

	/**********
	 * Prevent construction because the testing automation contains only static cases and shared
	 * reporting counters.
	 */
	private ExperienceLockPolicyTestingAutomation() {
	}

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run every prototype test and exit with an error when any numbered test or
	 * detailed check fails.</p>
	 *
	 * @param args command-line arguments are not used
	 */
	public static void main(String[] args) {
		System.out.println("HW3 EXPERIENCE LOCK POLICY TESTING AUTOMATION");
		System.out.println("Every test reports expected and actual behavior.\n");

		runTest(1, "Allow every unlocked field edit",
				"Record editing and all four field edits are approved.",
				ExperienceLockPolicyTestingAutomation::testUnlockedFieldEdits);
		runTest(2, "Block each locked field",
				"Each supported field lock rejects an edit to that same field.",
				ExperienceLockPolicyTestingAutomation::testEachFieldLock);
		runTest(3, "Allow unrelated unlocked fields",
				"A field lock does not block edits to the other three fields.",
				ExperienceLockPolicyTestingAutomation::testUnrelatedFieldEdits);
		runTest(4, "Block all edits with a record lock",
				"The complete-record lock rejects record editing and every field edit.",
				ExperienceLockPolicyTestingAutomation::testCompleteRecordEditLock);
		runTest(5, "Allow deletion when fully unlocked",
				"A record with no complete or field lock may be deleted.",
				ExperienceLockPolicyTestingAutomation::testUnlockedDeletion);
		runTest(6, "Block deletion with a record lock",
				"A complete-record lock rejects deletion.",
				ExperienceLockPolicyTestingAutomation::testRecordLockedDeletion);
		runTest(7, "Block deletion with any field lock",
				"Every single field lock and a multiple-lock state reject deletion.",
				ExperienceLockPolicyTestingAutomation::testFieldLockedDeletion);
		runTest(8, "Return helpful and safe messages",
				"Approvals explain success; rejections identify the lock or corrective action.",
				ExperienceLockPolicyTestingAutomation::testDecisionMessages);
		runTest(9, "Keep lock state immutable",
				"Construction, returned sets, and policy evaluation cannot alter the snapshot.",
				ExperienceLockPolicyTestingAutomation::testImmutableLockState);
		runTest(10, "Map flags and repeat decisions consistently",
				"Every persistent flag maps correctly and repeated calls return the same result.",
				ExperienceLockPolicyTestingAutomation::testFlagMappingAndRepeatability);
		runTest(11, "Enforce edit decisions through LessonService",
				"The service saves allowed edits and leaves protected records unchanged.",
				ExperienceLockPolicyTestingAutomation::testLessonServiceEditIntegration);
		runTest(12, "Enforce deletion decisions through LessonService",
				"The service deletes only fully unlocked records and preserves locked records.",
				ExperienceLockPolicyTestingAutomation::testLessonServiceDeleteIntegration);

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
			throw new AssertionError("HW3 experience lock policy testing failed.");
	}

	/**********
	 * <p><b>Test 1; requirement PR-1.</b> Evaluates record editing and each supported field with
	 * every lock false.</p>
	 */
	protected static void testUnlockedFieldEdits() {
		ExperienceLockState state = state(false);
		checkAllowed(ExperienceLockPolicy.evaluateRecordEdit(state), true,
				"Unlocked record edit");
		for (ExperienceField field : ExperienceField.values())
			checkAllowed(ExperienceLockPolicy.evaluateFieldEdit(state, field), true,
					"Unlocked " + field.getDisplayName() + " edit");
	}

	/**********
	 * <p><b>Test 2; requirement PR-2.</b> Locks each supported field independently and verifies
	 * that its matching edit is rejected with a field-specific message.</p>
	 */
	protected static void testEachFieldLock() {
		for (ExperienceField field : ExperienceField.values()) {
			ExperienceLockState state = state(false, field);
			LockDecision decision = ExperienceLockPolicy.evaluateFieldEdit(state, field);
			checkAllowed(decision, false, "Locked " + field.getDisplayName() + " edit");
			checkContains(decision.getMessage(), field.getDisplayName(),
					"Message names " + field.getDisplayName());
		}
	}

	/**********
	 * <p><b>Test 3; requirement PR-3.</b> For every individual lock, evaluates edits to all other
	 * fields so a protected field does not incorrectly disable the whole record.</p>
	 */
	protected static void testUnrelatedFieldEdits() {
		for (ExperienceField locked : ExperienceField.values()) {
			ExperienceLockState state = state(false, locked);
			for (ExperienceField requested : ExperienceField.values()) {
				if (requested != locked)
					checkAllowed(ExperienceLockPolicy.evaluateFieldEdit(state, requested), true,
							locked.getDisplayName() + " lock with " +
							requested.getDisplayName() + " edit");
			}
		}
	}

	/**********
	 * <p><b>Test 4; requirement PR-4.</b> Applies the complete-record lock and verifies that it
	 * overrides every individual field state.</p>
	 */
	protected static void testCompleteRecordEditLock() {
		ExperienceLockState state = state(true);
		checkAllowed(ExperienceLockPolicy.evaluateRecordEdit(state), false,
				"Complete-record edit");
		for (ExperienceField field : ExperienceField.values())
			checkAllowed(ExperienceLockPolicy.evaluateFieldEdit(state, field), false,
					"Complete-record lock with " + field.getDisplayName() + " edit");
	}

	/**********
	 * <p><b>Test 5; requirement PR-5.</b> Verifies the positive deletion path with no active
	 * record or field lock.</p>
	 */
	protected static void testUnlockedDeletion() {
		checkAllowed(ExperienceLockPolicy.evaluateDelete(state(false)), true,
				"Fully unlocked deletion");
	}

	/**********
	 * <p><b>Test 6; requirement PR-6.</b> Verifies that a complete-record lock protects the entire
	 * experience from deletion.</p>
	 */
	protected static void testRecordLockedDeletion() {
		LockDecision decision = ExperienceLockPolicy.evaluateDelete(state(true));
		checkAllowed(decision, false, "Complete-record locked deletion");
		checkContains(decision.getMessage(), "record is locked",
				"Complete-record deletion message identifies the lock");
	}

	/**********
	 * <p><b>Test 7; requirement PR-7.</b> Verifies each supported field lock independently and a
	 * state containing every field lock.</p>
	 */
	protected static void testFieldLockedDeletion() {
		for (ExperienceField field : ExperienceField.values())
			checkAllowed(ExperienceLockPolicy.evaluateDelete(state(false, field)), false,
					field.getDisplayName() + " locked deletion");
		checkAllowed(ExperienceLockPolicy.evaluateDelete(
				state(false, ExperienceField.values())), false, "Multiple field-locked deletion");
	}

	/**********
	 * <p><b>Test 8; requirement PR-8.</b> Checks positive, protected, missing-state, and
	 * missing-field messages for readable and safe feedback.</p>
	 */
	protected static void testDecisionMessages() {
		LockDecision approved = ExperienceLockPolicy.evaluateDelete(state(false));
		checkContains(approved.getMessage(), "may be deleted", "Approval explains next action");

		LockDecision fieldDenied = ExperienceLockPolicy.evaluateFieldEdit(
				state(false, ExperienceField.ELAPSED_TIME), ExperienceField.ELAPSED_TIME);
		checkContains(fieldDenied.getMessage(), "elapsed time",
				"Field rejection identifies the protected value");
		checkContains(fieldDenied.getMessage(), "cannot be edited",
				"Field rejection explains the restriction");

		LockDecision missingState = ExperienceLockPolicy.evaluateDelete(null);
		checkAllowed(missingState, false, "Missing lock state fails closed");
		checkContains(missingState.getMessage(), "Refresh", "Missing-state message is corrective");

		LockDecision missingField = ExperienceLockPolicy.evaluateFieldEdit(state(false), null);
		checkAllowed(missingField, false, "Missing field is rejected");
		checkContains(missingField.getMessage(), "Select", "Missing-field message is corrective");

		String combined = approved.getMessage() + fieldDenied.getMessage() +
				missingState.getMessage() + missingField.getMessage();
		check(!combined.toLowerCase().contains("password") &&
				!combined.toLowerCase().contains("jdbc") &&
				!combined.toLowerCase().contains("select *"),
				"Messages do not expose credentials or raw database details",
				"No sensitive terms", combined);
	}

	/**********
	 * <p><b>Test 9; requirement PR-9.</b> Changes the caller's source set, attempts to change the
	 * returned set, and evaluates the policy while comparing the original snapshot.</p>
	 */
	protected static void testImmutableLockState() {
		EnumSet<ExperienceField> source = EnumSet.of(ExperienceField.WHAT_WAS_DONE);
		ExperienceLockState state = new ExperienceLockState(false, source);
		source.clear();
		check(state.isFieldLocked(ExperienceField.WHAT_WAS_DONE),
				"Constructor made a defensive copy", "Locked", "Locked");

		boolean unmodifiable = false;
		try {
			state.getLockedFields().clear();
		} catch (UnsupportedOperationException e) {
			unmodifiable = true;
		}
		check(unmodifiable, "Returned field set is unmodifiable",
				"UnsupportedOperationException", unmodifiable ?
				"UnsupportedOperationException" : "No exception");

		Set<ExperienceField> before = state.getLockedFields();
		ExperienceLockPolicy.evaluateFieldEdit(state, ExperienceField.WHAT_WAS_DONE);
		ExperienceLockPolicy.evaluateFieldEdit(state, ExperienceField.HOW_IT_WAS_DONE);
		ExperienceLockPolicy.evaluateDelete(state);
		check(before.equals(state.getLockedFields()) && !state.isRecordLocked(),
				"Policy evaluation leaves lock state unchanged", before.toString(),
				state.getLockedFields().toString());
	}

	/**********
	 * <p><b>Test 10; requirements PR-8 and PR-9.</b> Verifies the mapping for every stored flag,
	 * constructor boundaries, and identical results across repeated evaluations.</p>
	 */
	protected static void testFlagMappingAndRepeatability() {
		ExperienceLockState mapped = ExperienceLockState.fromFlags(
				true, true, false, true, false);
		check(mapped.isRecordLocked(), "Complete-record flag maps correctly", "true",
				String.valueOf(mapped.isRecordLocked()));
		check(mapped.getLockedFields().equals(EnumSet.of(
				ExperienceField.WHAT_WAS_DONE, ExperienceField.ELAPSED_TIME)),
				"Field flags map to the expected set",
				"[WHAT_WAS_DONE, ELAPSED_TIME]", mapped.getLockedFields().toString());

		LockDecision first = ExperienceLockPolicy.evaluateFieldEdit(
				mapped, ExperienceField.HOW_IT_WAS_DONE);
		boolean repeated = true;
		for (int count = 0; count < 100; count++) {
			LockDecision next = ExperienceLockPolicy.evaluateFieldEdit(
					mapped, ExperienceField.HOW_IT_WAS_DONE);
			if (first.isAllowed() != next.isAllowed() ||
					!first.getMessage().equals(next.getMessage())) repeated = false;
		}
		check(repeated, "One hundred evaluations are repeatable",
				"Identical decisions", repeated ? "Identical decisions" : "Different decision");

		Set<ExperienceField> containsNull = new HashSet<ExperienceField>();
		containsNull.add(null);
		checkThrowsIllegalArgument(() -> new ExperienceLockState(false, null),
				"Null field set is rejected");
		checkThrowsIllegalArgument(() -> new ExperienceLockState(false, containsNull),
				"Null field entry is rejected");
		checkThrowsIllegalArgument(() -> LockDecision.allow("   "),
				"Blank decision message is rejected");
	}

	/**********
	 * <p><b>Test 11; requirements PR-1 through PR-4 and PR-9.</b> Exercises the real service path
	 * with a fake database, checking saved data and update counts after allowed and denied edits.</p>
	 */
	protected static void testLessonServiceEditIntegration() {
		FakeDatabase unlocked = databaseWithExperience(false, false, false, false, false);
		OperationResult<ExperienceRecord> allowed = new LessonService(unlocked).updateExperience(
				String.valueOf(EXPERIENCE_ID), OWNER,
				draft("Updated work", "Original method", "30", "20"));
		check(allowed.isSuccessful() && unlocked.updateCalls == 1 &&
				"Updated work".equals(unlocked.experience.getWhatWasDone()),
				"Unlocked service edit is persisted", "Successful update and one database write",
				resultDescription(allowed, unlocked.updateCalls));

		FakeDatabase protectedField = databaseWithExperience(false, true, false, false, false);
		ExperienceRecord protectedBaseline = protectedField.experience;
		OperationResult<ExperienceRecord> denied = new LessonService(protectedField).updateExperience(
				String.valueOf(EXPERIENCE_ID), OWNER,
				draft("Blocked work", "Original method", "30", "20"));
		check(!denied.isSuccessful() && protectedField.updateCalls == 0 &&
				protectedField.experience == protectedBaseline,
				"Locked service edit leaves data unchanged",
				"Rejected update and zero database writes", resultDescription(denied,
						protectedField.updateCalls));

		FakeDatabase unrelatedLock = databaseWithExperience(false, false, false, true, false);
		OperationResult<ExperienceRecord> unrelated = new LessonService(unrelatedLock).updateExperience(
				String.valueOf(EXPERIENCE_ID), OWNER,
				draft("Allowed work", "Original method", "30", "20"));
		check(unrelated.isSuccessful() && unrelatedLock.updateCalls == 1,
				"Unrelated elapsed-time lock does not block text edit",
				"Successful update and one database write", resultDescription(unrelated,
						unrelatedLock.updateCalls));

		FakeDatabase recordLock = databaseWithExperience(true, false, false, false, false);
		OperationResult<ExperienceRecord> recordDenied = new LessonService(recordLock).updateExperience(
				String.valueOf(EXPERIENCE_ID), OWNER,
				draft("Blocked work", "Original method", "30", "20"));
		check(!recordDenied.isSuccessful() && recordLock.updateCalls == 0,
				"Complete-record lock blocks service edit",
				"Rejected update and zero database writes", resultDescription(recordDenied,
						recordLock.updateCalls));
	}

	/**********
	 * <p><b>Test 12; requirements PR-5 through PR-7 and PR-9.</b> Exercises the real service
	 * deletion path and verifies that database deletion occurs only for a fully unlocked record.</p>
	 */
	protected static void testLessonServiceDeleteIntegration() {
		FakeDatabase unlocked = databaseWithExperience(false, false, false, false, false);
		OperationResult<Boolean> allowed = new LessonService(unlocked).deleteExperience(
				String.valueOf(EXPERIENCE_ID), OWNER);
		check(allowed.isSuccessful() && unlocked.deleteCalls == 1 && unlocked.experience == null,
				"Unlocked service deletion removes the record",
				"Successful deletion and one database delete", resultDescription(allowed,
						unlocked.deleteCalls));

		FakeDatabase recordLock = databaseWithExperience(true, false, false, false, false);
		ExperienceRecord recordBaseline = recordLock.experience;
		OperationResult<Boolean> recordDenied = new LessonService(recordLock).deleteExperience(
				String.valueOf(EXPERIENCE_ID), OWNER);
		check(!recordDenied.isSuccessful() && recordLock.deleteCalls == 0 &&
				recordLock.experience == recordBaseline,
				"Complete-record lock preserves the service record",
				"Rejected deletion and zero database deletes", resultDescription(recordDenied,
						recordLock.deleteCalls));

		for (ExperienceField field : ExperienceField.values()) {
			boolean what = field == ExperienceField.WHAT_WAS_DONE;
			boolean how = field == ExperienceField.HOW_IT_WAS_DONE;
			boolean elapsed = field == ExperienceField.ELAPSED_TIME;
			boolean effort = field == ExperienceField.TEAM_MEMBER_EFFORT;
			FakeDatabase fieldLock = databaseWithExperience(false, what, how, elapsed, effort);
			ExperienceRecord baseline = fieldLock.experience;
			OperationResult<Boolean> denied = new LessonService(fieldLock).deleteExperience(
					String.valueOf(EXPERIENCE_ID), OWNER);
			check(!denied.isSuccessful() && fieldLock.deleteCalls == 0 &&
					fieldLock.experience == baseline,
					field.getDisplayName() + " lock preserves the service record",
					"Rejected deletion and zero database deletes",
					resultDescription(denied, fieldLock.deleteCalls));
		}
	}

	private static ExperienceLockState state(boolean recordLocked, ExperienceField... fields) {
		EnumSet<ExperienceField> set = EnumSet.noneOf(ExperienceField.class);
		if (fields != null) {
			for (ExperienceField field : fields) set.add(field);
		}
		return new ExperienceLockState(recordLocked, set);
	}

	private static FakeDatabase databaseWithExperience(boolean recordLocked, boolean whatLocked,
			boolean howLocked, boolean elapsedLocked, boolean effortLocked) {
		LessonsLearned lesson = new LessonsLearned(LESSON_ID, OWNER, "JavaFX setup",
				"The application did not start", "Verify the module path.",
				false, false, false, false);
		List<TeamMemberEffort> efforts = new ArrayList<TeamMemberEffort>();
		efforts.add(new TeamMemberEffort("Adan", 20));
		ExperienceRecord experience = new ExperienceRecord(EXPERIENCE_ID, LESSON_ID, OWNER,
				"Original work", "Original method", 30, efforts, recordLocked, whatLocked,
				howLocked, elapsedLocked, effortLocked);
		return new FakeDatabase(lesson, experience);
	}

	private static ExperienceDraft draft(String what, String how, String elapsed, String effort) {
		List<EffortInput> efforts = new ArrayList<EffortInput>();
		efforts.add(new EffortInput("Adan", effort));
		return new ExperienceDraft(what, how, elapsed, efforts);
	}

	private static String resultDescription(OperationResult<?> result, int databaseCalls) {
		return (result.isSuccessful() ? "Successful" : "Rejected") +
				"; message=\"" + result.getMessage() + "\"; database calls=" + databaseCalls;
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

	private static void checkAllowed(LockDecision decision, boolean expected, String description) {
		check(decision.isAllowed() == expected, description,
				String.valueOf(expected), String.valueOf(decision.isAllowed()) +
				"; message=\"" + decision.getMessage() + "\"");
	}

	private static void checkContains(String actual, String expectedText, String description) {
		check(actual != null && actual.toLowerCase().contains(expectedText.toLowerCase()),
				description, "Message contains \"" + expectedText + "\"", actual);
	}

	private static void checkThrowsIllegalArgument(Action action, String description) {
		boolean thrown = false;
		try {
			action.run();
		} catch (IllegalArgumentException e) {
			thrown = true;
		}
		check(thrown, description, "IllegalArgumentException",
				thrown ? "IllegalArgumentException" : "No exception");
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

	@FunctionalInterface
	private interface Action {
		void run();
	}

	/*******
	 * Minimal database test double used only to verify the production service integration. It
	 * stores the latest record and counts write attempts so rejected operations can prove that no
	 * persistence method was called.
	 */
	private static class FakeDatabase extends Database {
		private final LessonsLearned lesson;
		private ExperienceRecord experience;
		private int updateCalls;
		private int deleteCalls;

		private FakeDatabase(LessonsLearned lesson, ExperienceRecord experience) {
			this.lesson = lesson;
			this.experience = experience;
		}

		@Override
		public LessonsLearned getLesson(long id) throws SQLException {
			return lesson != null && lesson.getId() == id ? lesson : null;
		}

		@Override
		public ExperienceRecord getExperience(long id) throws SQLException {
			return experience != null && experience.getId() == id ? experience : null;
		}

		@Override
		public boolean updateExperience(ExperienceRecord replacement) throws SQLException {
			updateCalls++;
			if (experience == null || experience.getId() != replacement.getId()) return false;
			experience = replacement;
			return true;
		}

		@Override
		public boolean deleteExperience(long id) throws SQLException {
			deleteCalls++;
			if (experience == null || experience.getId() != id) return false;
			experience = null;
			return true;
		}
	}
}
