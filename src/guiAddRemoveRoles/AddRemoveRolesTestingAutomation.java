package guiAddRemoveRoles;

/*******
 * <p> Title: AddRemoveRolesTestingAutomation Class. </p>
 *
 * <p> Description: Automated tests for the role-removal safeguards used by the controller.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-21 Initial version
 */
public class AddRemoveRolesTestingAutomation {

	private static int passed = 0;
	private static int failed = 0;

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run the automated role-removal safeguard tests.</p>
	 *
	 * @param args command-line arguments are not used
	 */
	public static void main(String[] args) {
		test("Current Admin role removal is detected",
				RoleAssignmentValidator.isRemovingOwnAdminRole("AdminOne", "AdminOne", "Admin"));
		test("Another account is not treated as the current Admin",
				!RoleAssignmentValidator.isRemovingOwnAdminRole("AdminTwo", "AdminOne", "Admin"));
		test("Another role can be removed from the current Admin",
				!RoleAssignmentValidator.isRemovingOwnAdminRole("AdminOne", "AdminOne", "Role1"));
		test("Null usernames are handled safely",
				!RoleAssignmentValidator.isRemovingOwnAdminRole(null, "AdminOne", "Admin"));
		test("One Admin role is the final role",
				RoleAssignmentValidator.isLastAssignedRole(true, false, false));
		test("One Role1 role is the final role",
				RoleAssignmentValidator.isLastAssignedRole(false, true, false));
		test("Multiple assigned roles are not the final role",
				!RoleAssignmentValidator.isLastAssignedRole(true, true, false));

		System.out.println("Add remove role controller tests passed: " + passed);
		System.out.println("Add remove role controller tests failed: " + failed);
		if (failed > 0) throw new AssertionError("Add remove role controller testing failed.");
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
