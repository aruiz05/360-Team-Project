package database;

/*******
 * <p> Title: ContributorManualTesting Class. </p>
 *
 * <p> Description: Prints the repeatable manual JavaFX tests used with
 * {@link HW2TestingAutomation}. The automated class validates persistence and business rules
 * directly; these manual cases confirm that the TP2 Views and Controllers display those same
 * results, retain invalid input for correction, and enable only permitted actions.</p>
 *
 * <p>This guide never reports a test as passed automatically. A reviewer must perform each
 * numbered procedure in {@code applicationMain.TP2}, compare the observed GUI behavior with the
 * printed expected result, and record the actual result for the testing evidence.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-22 Initial TP2 manual GUI test guide
 */
public final class ContributorManualTesting {

	private ContributorManualTesting() {
	}

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Print all six manual Contributor GUI procedures and their expected
	 * results. The output is a test guide, not a claim that a person completed the tests.</p>
	 *
	 * @param args command-line arguments are not used
	 */
	public static void main(String[] args) {
		System.out.println("TP2 CONTRIBUTOR MANUAL GUI TEST GUIDE");
		System.out.println("Launch applicationMain.TP2 and record PASS or FAIL after each case.\n");
		printNavigationAndOwnershipTest();
		printLessonCreateValidationTest();
		printListFilterAndSelectionTest();
		printLessonEditAndDeleteTest();
		printExperienceCrudTest();
		printRestartPersistenceTest();
	}

	/**
	 * <p><b>Manual Test M1; requirements C2 and C3.</b> Sign in as a Contributor, open
	 * {@code Manage My Lessons}, and repeat with a second Contributor account. Expected: the role
	 * and page titles say Contributor and each account sees only lessons it owns.</p>
	 */
	protected static void printNavigationAndOwnershipTest() {
		printCase("M1", "Contributor navigation and ownership",
				"Sign in as Contributor A; open Manage My Lessons; repeat as Contributor B.",
				"Contributor pages open and each account lists only its own lessons.");
	}

	/**
	 * <p><b>Manual Test M2; requirements C1 and C7.</b> Select Create, enter spaces in each
	 * required field in turn, and select Save before entering valid values. Expected: the dialog
	 * remains open with entered data and a helpful field-specific message; valid data saves once.</p>
	 */
	protected static void printLessonCreateValidationTest() {
		printCase("M2", "Lesson creation and correction messages",
				"Open Create; try blank title, problem, and lesson values; then enter valid values.",
				"Invalid input stays in the dialog with a helpful message; valid input saves once.");
	}

	/**
	 * <p><b>Manual Test M3; requirements C2 and C6.</b> Create multiple lessons, apply one or more
	 * title keywords in a different order, clear the filter, and select a row. Expected: matching
	 * subsets contain no duplicates, Show All restores the full list, and View, Edit, and Delete
	 * remain disabled until a row is selected.</p>
	 */
	protected static void printListFilterAndSelectionTest() {
		printCase("M3", "Lists, keyword subsets, and selection",
				"Create several lessons; filter with multiple title keywords; clear; select a row.",
				"Correct subsets and full list appear; action buttons require a selected lesson.");
	}

	/**
	 * <p><b>Manual Test M4; requirements C1 and C4.</b> Edit an unlocked lesson, cancel a second
	 * edit, and confirm and cancel lesson deletion. Use test-seeded lock data when demonstrating
	 * locks. Expected: saves refresh the list, Cancel changes nothing, confirmation is required,
	 * and locked fields are labeled and cannot be changed or cascade-deleted.</p>
	 */
	protected static void printLessonEditAndDeleteTest() {
		printCase("M4", "Lesson update, deletion, and locks",
				"Edit and save; edit and cancel; cancel then confirm Delete; inspect seeded locks.",
				"Only confirmed valid actions persist and every locked field remains protected.");
	}

	/**
	 * <p><b>Manual Test M5; requirements C4, C5, and C7.</b> View a lesson, add two experience
	 * records, try negative and nonnumeric time or effort, edit one record, and delete one after
	 * first cancelling. Expected: units and lock labels are visible, invalid values remain for
	 * correction, and only the selected confirmed record changes.</p>
	 */
	protected static void printExperienceCrudTest() {
		printCase("M5", "Experience information CRUD and validation",
				"Add, read, edit, and delete attempts; try invalid time, effort, and member values.",
				"Minutes and person-minutes are clear; errors help; selected records change correctly.");
	}

	/**
	 * <p><b>Manual Test M6; requirement C8.</b> Save and delete test data, close the entire TP2
	 * application, relaunch it, and sign in again. Expected: created and updated data remains,
	 * deleted data stays absent, and existing TP1 accounts and Admin functions still work.</p>
	 */
	protected static void printRestartPersistenceTest() {
		printCase("M6", "Application restart persistence and TP1 regression",
				"Save and delete lesson data; quit TP2; relaunch; sign in; check Admin functions.",
				"Saved changes survive, deletions remain, and TP1 account behavior still works.");
	}

	private static void printCase(String id, String name, String procedure, String expected) {
		System.out.println("------------------------------------------------------------");
		System.out.println("Manual Test " + id + ": " + name);
		System.out.println("Procedure: " + procedure);
		System.out.println("Expected:  " + expected);
		System.out.println("Actual:    Record observations during execution.");
		System.out.println("Result:    Mark PASS or FAIL after comparing actual and expected.\n");
	}
}
