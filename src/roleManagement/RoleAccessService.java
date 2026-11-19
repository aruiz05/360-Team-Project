package roleManagement;

import java.sql.SQLException;
import java.util.List;
import database.Database;
import entityClasses.User;
import guiTools.RoleNames;
import lessonManagement.OperationResult;

/*******
 * <p> Title: RoleAccessService Class. </p>
 * <p> Description: Provides current persisted authorization for role entry and business logic.
 * It never trusts role flags copied into an old screen or changes the Foundations user cache.
 * Supports the assigned-role story. Tested by TP3FoundationsTest dispatch and authorization
 * cases and TP3RoleNavigationTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-18 Phase 3 role access foundation
 */
public class RoleAccessService {
    /** Shared persistent database; no additional connection or GUI dependency is introduced. */
    private final Database database;

    /** @param database specifies the application or isolated test database */
    public RoleAccessService(Database database) { this.database = database; }

    /**********
     * Require a current role for the authenticated username supplied by application context.
     * Tested by TP3FoundationsTest.dispatchUsesPersistedRoles and passwordResetBlocksRoleAccess.
     * @param username specifies the authenticated actor, not a form-entered owner
     * @param role specifies the required stored or displayed role
     * @return a refreshed non-secret account or a corrective access error
     */
    public OperationResult<User> requireRole(String username, String role) {
        if (!RoleNames.isSupported(role)) return OperationResult.failure("Select a supported role.");
        OperationResult<User> loaded = loadAccount(username);
        if (!loaded.isSuccessful()) return loaded;
        String displayed = RoleNames.toDisplayRole(role);
        if (!RoleNames.assignedRoles(loaded.getValue()).contains(displayed))
            return OperationResult.failure("Your account is not assigned the " + displayed
                    + " role. Select an assigned role or contact an administrator.");
        return loaded;
    }

    /**********
     * Read the available role choices without mutating cached account details.
     * Tested by TP3FoundationsTest.dispatchUsesPersistedRoles.
     * @param username specifies the authenticated account
     * @return the current roles in display order or an access error
     */
    public OperationResult<List<String>> rolesFor(String username) {
        OperationResult<User> loaded = loadAccount(username);
        if (!loaded.isSuccessful()) return OperationResult.failure(loaded.getMessage());
        return OperationResult.success(RoleNames.assignedRoles(loaded.getValue()), "Roles loaded.");
    }

    /**********
     * Determine whether direct entry is permitted rather than silently choosing from many roles.
     * Tested by TP3FoundationsTest.dispatchUsesPersistedRoles.
     * @param username specifies the authenticated account
     * @return its only role or an instruction to choose a role
     */
    public OperationResult<String> singleRole(String username) {
        OperationResult<List<String>> roles = rolesFor(username);
        if (!roles.isSuccessful()) return OperationResult.failure(roles.getMessage());
        if (roles.getValue().size() != 1)
            return OperationResult.failure("Select which assigned role you want to use.");
        return OperationResult.success(roles.getValue().getFirst(), "Role selected.");
    }

    private OperationResult<User> loadAccount(String username) {
        try {
            User user = database.findUserForRoleAccess(username);
            if (user == null) return OperationResult.failure(
                    "Sign in again and complete any required password replacement.");
            if (RoleNames.assignedRoles(user).isEmpty()) return OperationResult.failure(
                    "No role is assigned. Contact an administrator.");
            return OperationResult.success(user, "Account access verified.");
        } catch (SQLException e) {
            return OperationResult.failure("Account access could not be checked. Try again.");
        }
    }
}
