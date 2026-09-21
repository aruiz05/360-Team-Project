package database;

import java.nio.file.Files;
import java.nio.file.Path;

import entityClasses.User;

/*******
 * <p> Title: DeleteUserDatabaseTestingAutomation Class. </p>
 *
 * <p> Description: Test account removal, login denial, and Admin safeguards with an
 * in-memory database.  The application's normal database is not modified.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-19 Initial version
 */
public class DeleteUserDatabaseTestingAutomation {

	private static int passed = 0;
	private static int failed = 0;

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run the automated delete-user database tests.</p>
	 *
	 * @param args command-line arguments are not used
	 * @throws Exception when the test database cannot be initialized
	 */
	public static void main(String[] args) throws Exception {
		Database database = new Database("jdbc:h2:mem:DeleteUserTest;DB_CLOSE_DELAY=-1");
		database.connectToDatabase();

		User admin = new User("AdminOne", "Admin1!", "First", "", "Admin", "First",
				"admin1@example.com", true, false, false);
		User secondAdmin = new User("AdminTwo", "Admin2!", "Second", "", "Admin", "Second",
				"admin2@example.com", true, false, false);
		User member = new User("TeamMember", "Member1!", "Team", "", "Member", "Team",
				"member@example.com", false, true, false);
		database.register(admin);
		database.register(secondAdmin);
		database.register(member);
		test("Three users were registered", database.getNumberOfUsers() == 3);

		test("Self deletion is rejected", !database.deleteUser("AdminOne", "AdminOne"));
		test("Self deletion leaves the Admin account active", database.loginAdmin(admin));
		test("A non-Admin cannot delete an account",
				!database.deleteUser("AdminTwo", "TeamMember"));
		test("Unknown acting Admin is rejected",
				!database.deleteUser("AdminTwo", "MissingAdmin"));
		test("Unknown target is rejected", !database.deleteUser("MissingUser", "AdminOne"));
		test("Null and empty selections are rejected",
				!database.deleteUser(null, "AdminOne") &&
				!database.deleteUser("", "AdminOne") &&
				!database.deleteUser("TeamMember", null));
		test("Invalid requests do not remove accounts", database.getNumberOfUsers() == 3);

		test("Admin deletes another user", database.deleteUser("TeamMember", "AdminOne"));
		test("Deleted user cannot log in", !database.loginRole1(member));
		test("Deleted user is absent from account list",
				!database.getUserList().contains("TeamMember"));
		test("User count reflects deletion", database.getNumberOfUsers() == 2);
		test("Repeated deletion does not report success",
				!database.deleteUser("TeamMember", "AdminOne"));

		test("Another Admin can remove an Admin account",
				database.deleteUser("AdminTwo", "AdminOne"));
		test("Removed Admin cannot log in", !database.loginAdmin(secondAdmin));
		test("The acting Admin retains access", database.loginAdmin(admin));
		test("At least one Admin remains", database.getNumberOfUsers() == 1);

		database.closeConnection();

		// Verify that the deletion also persists in a newly opened database connection.
		Path directory = Files.createTempDirectory("delete-user-test-");
		String databasePath = directory.resolve("accounts").toString();
		Database firstConnection = new Database("jdbc:h2:file:" + databasePath);
		firstConnection.connectToDatabase();
		firstConnection.register(admin);
		firstConnection.register(member);
		test("User is deleted from the file database",
				firstConnection.deleteUser("TeamMember", "AdminOne"));
		firstConnection.closeConnection();

		Database secondConnection = new Database("jdbc:h2:file:" + databasePath);
		secondConnection.connectToDatabase();
		test("Deletion survives a new database connection",
				secondConnection.getNumberOfUsers() == 1 &&
				!secondConnection.doesUserExist("TeamMember"));
		test("Deleted user cannot log in after reopening the database",
				!secondConnection.loginRole1(member));
		secondConnection.closeConnection();
		Files.deleteIfExists(directory.resolve("accounts.mv.db"));
		Files.deleteIfExists(directory);

		System.out.println("Delete user database tests passed: " + passed);
		System.out.println("Delete user database tests failed: " + failed);
		if (failed > 0) throw new AssertionError("Delete user database testing failed.");
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
