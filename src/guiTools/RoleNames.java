package guiTools;

/*******
 * <p> Title: RoleNames Class. </p>
 *
 * <p> Description: Defines the role names shown by the TP2 user interface while preserving the
 * Foundation database's existing role identifiers. The Foundation field {@code newRole1} and
 * stored invitation value {@code Role1} remain unchanged so existing TP1 account data continues
 * to work, but users see the Phase Two role name {@code Contributor}.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-22 Added the Phase Two Contributor display name
 */
public final class RoleNames {

	/** Role name already used by both the database and the interface. */
	public static final String ADMIN = "Admin";

	/** Phase Two name displayed for the role that authors lessons learned. */
	public static final String CONTRIBUTOR = "Contributor";

	/** Foundation identifier retained in the database for backward compatibility. */
	public static final String STORED_CONTRIBUTOR = "Role1";

	/** Unimplemented Foundation placeholder retained for future phases. */
	public static final String ROLE_2 = "Role2";

	private RoleNames() {
	}

	/**
	 * @param role specifies a displayed or stored role name
	 * @return true when the value identifies the Phase Two Contributor role
	 */
	public static boolean isContributor(String role) {
		return CONTRIBUTOR.equals(role) || STORED_CONTRIBUTOR.equals(role);
	}

	/**
	 * @param displayRole specifies a role selected in the interface
	 * @return the compatible value to persist in the Foundation database
	 */
	public static String toStoredRole(String displayRole) {
		return CONTRIBUTOR.equals(displayRole) ? STORED_CONTRIBUTOR : displayRole;
	}

	/**
	 * @param storedRole specifies a role read from the Foundation database
	 * @return the Phase Two name to display to a user
	 */
	public static String toDisplayRole(String storedRole) {
		return STORED_CONTRIBUTOR.equals(storedRole) ? CONTRIBUTOR : storedRole;
	}
}
