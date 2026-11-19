package guiCurator;

import entityClasses.User;
import javafx.stage.Stage;

/*******
 * <p> Title: ModelCuratorHome Class. </p>
 * <p> Description: Connects Curator entry to current persistent role authorization.
 * Tested by TP3RoleNavigationTestingAutomation and TP3FoundationsTest dispatch cases.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-18 Phase 3 role access foundation
 */
public class ModelCuratorHome {
    private ModelCuratorHome() { }

    /**
     * @param stage specifies the application stage
     * @param user specifies the authenticated account snapshot
     * @return the currently authorized account, or null after showing a corrective error
     */
    protected static User authorize(Stage stage, User user) {
        return guiTools.GUISingleRoleDispatch.requireRoleForView(stage, user,
                guiTools.RoleNames.CURATOR);
    }
}
