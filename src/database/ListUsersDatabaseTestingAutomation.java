package database;

import java.util.List;

import entityClasses.User;

/*******
 * <p> Title: ListUsersDatabaseTestingAutomation Class. </p>
 *
 * <p> Description: Automated database tests for listing all user accounts and their required
 * account information.  An in-memory database is used so the application's normal database is
 * not changed.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-21 Initial version
 */
public class ListUsersDatabaseTestingAutomation {

	private static int passed = 0;
	private static int failed = 0;

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run the automated list-users database tests.</p>
	 *
	 * @param args command-line arguments are not used
	 * @throws Exception when the test database cannot be initialized
	 */
	public static void main(String[] args) throws Exception {
		Database database = new Database("jdbc:h2:mem:ListUsersTest;DB_CLOSE_DELAY=-1");
		database.connectToDatabase();

		User admin = new User("AdminAccount", "Admin1!", "Ada", "M", "Admin", "Ada",
				"ada@example.com", true, false, true);
		User member = new User("MemberAccount", "Member1!", "Ben", "", "Member", "Ben",
				"ben@example.com", false, true, false);
		database.register(member);
		database.register(admin);

		List<User> users = database.getAllUsers();
		test("All accounts are returned", users != null && users.size() == 2);
		test("Accounts are sorted by username",
				users != null && "AdminAccount".equals(users.get(0).getUserName()) &&
				"MemberAccount".equals(users.get(1).getUserName()));

		User listedAdmin = users.get(0);
		test("Username is returned", "AdminAccount".equals(listedAdmin.getUserName()));
		test("First name is returned", "Ada".equals(listedAdmin.getFirstName()));
		test("Middle name is returned", "M".equals(listedAdmin.getMiddleName()));
		test("Last name is returned", "Admin".equals(listedAdmin.getLastName()));
		test("Preferred name is returned", "Ada".equals(listedAdmin.getPreferredFirstName()));
		test("Email address is returned", "ada@example.com".equals(listedAdmin.getEmailAddress()));
		test("Admin role is returned", listedAdmin.getAdminRole());
		test("Unassigned Role1 is returned", !listedAdmin.getNewRole1());
		test("Assigned Role2 is returned", listedAdmin.getNewRole2());

		User listedMember = users.get(1);
		test("Assigned Role1 is returned", listedMember.getNewRole1());
		test("Unassigned roles are returned",
				!listedMember.getAdminRole() && !listedMember.getNewRole2());

		database.closeConnection();
		System.out.println("List users database tests passed: " + passed);
		System.out.println("List users database tests failed: " + failed);
		if (failed > 0) throw new AssertionError("List users database testing failed.");
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
