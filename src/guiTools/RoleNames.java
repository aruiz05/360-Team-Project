package guiTools;

import java.util.ArrayList;
import java.util.List;
import entityClasses.User;

/*******
 * <p> Title: RoleNames Class. </p>
 *
 * <p> Description: Defines the role names shown by the TP2 user interface while preserving the
 * Foundation database's existing role identifiers. The Foundation field {@code newRole1} and
 * stored invitation value {@code Role1} remain unchanged so existing TP1 account data continues
 * to work, but users see {@code Contributor} and {@code Viewer}. Curator has a separate assignment.
 * Tested by TP3FoundationsTest.roleNamesAndAccounts.</p>
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

	/** Legacy Viewer identifier retained so TP1 accounts and invitations remain readable. */
	public static final String ROLE_2 = "Role2";

	/** Phase 3 role for reading lessons. */
	public static final String VIEWER = "Viewer";

	/** Phase 3 role for Curator operations. */
	public static final String CURATOR = "Curator";

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
		if (CONTRIBUTOR.equals(displayRole)) return STORED_CONTRIBUTOR;
		return VIEWER.equals(displayRole) ? ROLE_2 : displayRole;
	}

	/**
	 * @param storedRole specifies a role read from the Foundation database
	 * @return the Phase Two name to display to a user
	 */
	public static String toDisplayRole(String storedRole) {
		if (STORED_CONTRIBUTOR.equals(storedRole)) return CONTRIBUTOR;
		return ROLE_2.equals(storedRole) ? VIEWER : storedRole;
	}
	/**
     * Return meaningful role names in the same order used by home dispatch and account lists.
     * Tested by TP3FoundationsTest.roleNamesAndAccounts and dispatchUsesPersistedRoles.
     * @param user specifies a current account snapshot
     * @return a new list of assigned display names, or an empty list for no account
     */
    public static List<String> assignedRoles(User user) {
        List<String> roles = new ArrayList<String>();
        if (user == null) return roles;
        if (user.getAdminRole()) roles.add(ADMIN);
        if (user.getNewRole1()) roles.add(CONTRIBUTOR);
        if (user.getNewRole2()) roles.add(VIEWER);
        if (user.getCuratorRole()) roles.add(CURATOR);
        return roles;
    }

    /**
     * @param role specifies a stored or displayed role
     * @return whether the role belongs to the supported set
     * Tested by TP3FoundationsTest.roleNamesAndAccounts.
     */
    public static boolean isSupported(String role) {
        return ADMIN.equals(role) || isContributor(role) || VIEWER.equals(role)
                || ROLE_2.equals(role) || CURATOR.equals(role);
    }
}
