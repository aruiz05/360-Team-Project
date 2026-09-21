package database;

import entityClasses.User;

/*******
 * <p> Title: OneTimePasswordDatabaseTestingAutomation Class. </p>
 *
 * <p> Description: Automated database tests for the one-time password lifecycle.  An in-memory
 * test database is used so the application's normal database is not changed.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-18 Initial version
 */
public class OneTimePasswordDatabaseTestingAutomation {

	private static int passed = 0;
	private static int failed = 0;

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run the automated one-time-password database tests.</p>
	 *
	 * @param args command-line arguments are not used
	 *
	 * @throws Exception when the test database cannot be initialized
	 */
	public static void main(String[] args) throws Exception {
		Database database = new Database("jdbc:h2:mem:OneTimePasswordTest;DB_CLOSE_DELAY=-1");
		database.connectToDatabase();

		User user = new User("ResetUser", "Current1!", "Reset", "", "User", "Reset",
				"reset@example.com", true, true, true);
		database.register(user);

		test("Unknown account is rejected",
				!database.setOneTimePassword("MissingUser", "Temporary1!"));
		test("One-time password is saved",
				database.setOneTimePassword("ResetUser", "Temporary1!"));
		test("Account is marked for reset",
				database.isPasswordResetRequired("ResetUser"));
		test("Admin login is blocked while reset is required",
				!database.loginAdmin(user));
		test("Role 1 login is blocked while reset is required",
				!database.loginRole1(user));
		test("Role 2 login is blocked while reset is required",
				!database.loginRole2(user));
		test("Incorrect one-time password is rejected",
				!database.consumeOneTimePassword("ResetUser", "Incorrect1!"));
		test("Correct one-time password is accepted",
				database.consumeOneTimePassword("ResetUser", "Temporary1!"));
		test("One-time password cannot be reused",
				!database.consumeOneTimePassword("ResetUser", "Temporary1!"));
		test("Reset remains required after one-time password use",
				database.isPasswordResetRequired("ResetUser"));
		test("New permanent password is saved",
				database.completePasswordReset("ResetUser", "Changed1!"));
		test("Reset requirement is cleared",
				!database.isPasswordResetRequired("ResetUser"));

		User oldPassword = new User("ResetUser", "Current1!", "Reset", "", "User", "Reset",
				"reset@example.com", true, true, true);
		User newPassword = new User("ResetUser", "Changed1!", "Reset", "", "User", "Reset",
				"reset@example.com", true, true, true);
		test("Old permanent password is rejected for Admin",
				!database.loginAdmin(oldPassword));
		test("Old permanent password is rejected for Role 1",
				!database.loginRole1(oldPassword));
		test("Old permanent password is rejected for Role 2",
				!database.loginRole2(oldPassword));
		test("New permanent password is accepted for Admin",
				database.loginAdmin(newPassword));
		test("New permanent password is accepted for Role 1",
				database.loginRole1(newPassword));
		test("New permanent password is accepted for Role 2",
				database.loginRole2(newPassword));

		database.closeConnection();
		System.out.println("One-time password database tests passed: " + passed);
		System.out.println("One-time password database tests failed: " + failed);
		if (failed > 0) throw new AssertionError("One-time password database testing failed.");
	}

	// Run one test and update the summary counters
	private static void test(String name, boolean condition) {
		if (condition) passed++;
		else {
			failed++;
			System.out.println("FAILED: " + name);
		}
	}
}
