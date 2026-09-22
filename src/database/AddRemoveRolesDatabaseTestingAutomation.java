package database;

import entityClasses.User;

/*******
 * <p> Title: AddRemoveRolesDatabaseTestingAutomation Class. </p>
 *
 * <p> Description: Automated tests for adding and removing account roles.  An in-memory database
 * is used so the application's normal database is not changed.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-21 Initial version
 */
public class AddRemoveRolesDatabaseTestingAutomation {

	private static int passed = 0;
	private static int failed = 0;

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run the automated add-and-remove-role database tests.</p>
	 *
	 * @param args command-line arguments are not used
	 * @throws Exception when the test database cannot be initialized
	 */
	public static void main(String[] args) throws Exception {
		Database database = new Database("jdbc:h2:mem:AddRemoveRolesTest;DB_CLOSE_DELAY=-1");
		database.connectToDatabase();

		User user = new User("RoleUser", "Current1!", "Role", "", "User", "Role",
				"role@example.com", false, true, false);
		database.register(user);

		test("Starting role is loaded", database.getUserAccountDetails("RoleUser") &&
				!database.getCurrentAdminRole() && database.getCurrentNewRole1() &&
				!database.getCurrentNewRole2());
		test("Admin role can be added", database.updateUserRole("RoleUser", "Admin", "true"));
		test("Added Admin role is saved", database.getUserAccountDetails("RoleUser") &&
				database.getCurrentAdminRole());
		test("Role2 can be added", database.updateUserRole("RoleUser", "Role2", "true"));
		test("Added Role2 is saved", database.getUserAccountDetails("RoleUser") &&
				database.getCurrentNewRole2());
		test("Role1 can be removed", database.updateUserRole("RoleUser", "Role1", "false"));
		test("Removed Role1 stays removed", database.getUserAccountDetails("RoleUser") &&
				!database.getCurrentNewRole1());
		test("Admin can be removed while another role remains",
				database.updateUserRole("RoleUser", "Admin", "false"));
		test("Removed Admin role stays removed", database.getUserAccountDetails("RoleUser") &&
				!database.getCurrentAdminRole() && database.getCurrentNewRole2());
		test("Final role cannot be removed",
				!database.updateUserRole("RoleUser", "Role2", "false"));
		test("Final role remains assigned", database.getUserAccountDetails("RoleUser") &&
				database.getCurrentNewRole2());
		test("Unknown account is rejected",
				!database.updateUserRole("MissingUser", "Admin", "true"));
		test("Unknown role is rejected",
				!database.updateUserRole("RoleUser", "Unknown", "true"));
		test("Invalid role value is rejected",
				!database.updateUserRole("RoleUser", "Admin", "yes"));
		test("Null and empty input is rejected",
				!database.updateUserRole(null, "Admin", "true") &&
				!database.updateUserRole("", "Admin", "true") &&
				!database.updateUserRole("RoleUser", null, "true") &&
				!database.updateUserRole("RoleUser", "Admin", null));

		database.closeConnection();
		System.out.println("Add remove role database tests passed: " + passed);
		System.out.println("Add remove role database tests failed: " + failed);
		if (failed > 0) throw new AssertionError("Add remove role database testing failed.");
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
