package guiAddRemoveRoles;

/*******
 * <p> Title: RoleAssignmentValidator Class. </p>
 *
 * <p> Description: Provides validation rules used before an Admin removes an assigned role.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-21 Initial version
 */
public class RoleAssignmentValidator {

	/**
	 * Default constructor is not used.
	 */
	private RoleAssignmentValidator() {
	}

	// Determine whether the requested change would remove the logged-in Admin's Admin role
	protected static boolean isRemovingOwnAdminRole(String selectedUsername,
			String currentUsername, String role) {
		return selectedUsername != null && selectedUsername.equals(currentUsername) &&
				"Admin".equals(role);
	}

	// Determine whether the selected account has exactly one assigned role
	protected static boolean isLastAssignedRole(boolean admin, boolean role1, boolean role2) {
		int roleCount = 0;
		if (admin) roleCount++;
		if (role1) roleCount++;
		if (role2) roleCount++;
		return roleCount <= 1;
	}
}
