package database;

import java.sql.SQLException;
import entityClasses.User;

/*******
 * <p> Title: ContributorTestAccounts Class. </p>
 * <p> Description: Establishes authenticated-role fixtures for the inherited lesson tests.
 * TP3 now checks persistent Contributor assignments, so a username alone is no longer sufficient.
 * Used and validated by HW2TestingAutomation, HW2PersistenceProcess, and
 * TP2BoundaryCoverageTestingAutomation. It never opens the production database.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-18 Phase 3 role-aware regression fixtures
 */
final class ContributorTestAccounts {
    private ContributorTestAccounts() { }

    /**
     * @param database specifies an isolated test database
     * @throws SQLException when fixture accounts cannot be persisted
     */
    static void register(Database database) throws SQLException {
        for (String username : new String[] { "ContributorA", "ContributorB" }) {
            if (!database.doesUserExist(username)) database.register(new User(username, "Test1!",
                    "Course", "", "Contributor", "Course", "course@example.test",
                    false, true, false));
        }
    }
}
