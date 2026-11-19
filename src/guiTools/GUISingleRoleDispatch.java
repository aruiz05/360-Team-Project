package guiTools;

import javafx.scene.Scene;
import javafx.stage.Stage;
import entityClasses.User;


/*******
 * <p> Title: GUISingleRoleDispatch Class. </p>
 * 
 * <p> Description: The class dispatches the execution to the appropriate role's home
 * page when the user has only one role.  This is not actually a GUI page... it just dispatches
 * to an actual GUI page for the specified role.
 * 
 * WHen a user has more than one role, a different
 * class, GUIMultipleRoleHomePage, asks the user which of their roles do they want to use,
 * and then it dispatches the user to that role's home page.</p>
 * 
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 * 
 * @author Lynn Robert Carter
 * 
 * @version 1.00		2025-04-20 Initial version
 *  
 */

public class GUISingleRoleDispatch {
	
	/**********************************************************************************************

	Attributes
	
	**********************************************************************************************/
	
	// These are the application values required by the user interface

	
	public Scene theViewStudentHomeScene;


	
	/**********************************************************************************************

	Constructors
	
	**********************************************************************************************/

	
	/**********
	 * <p> Method: GUISingleRoleDispatch() </p>
	 * 
	 * <p> Description: This method initializes all the elements of the graphical user interface. 
	 * This method determines the location, size, font, color, and change and event handlers for 
	 * each GUI object. </p>
	 * 
	 */
	public GUISingleRoleDispatch() {
	}

	
	/**********
	 * <p> Method: doSingleRoleDispatch(Stage ps, Pane theRoot, Database database, User user) </p>
	 * 
	 * <p> Description: This method is called after a GUI page has already been established and
	 * it is being display with potentially new contents for the various GUI elements. </p>
	 * 
	 * @param ps specifies the JavaFX Stage to be used for this GUI and it's methods
	 *
	 * @param user specifies the User for this GUI and it's methods
	 * 
	 */
	public static void doSingleRoleDispatch(Stage ps, User user) {
        lessonManagement.OperationResult<String> role = new roleManagement.RoleAccessService(
                applicationMain.FoundationsMain.database).singleRole(user == null ? null : user.getUserName());
        if (!role.isSuccessful()) {
            showAccessError(ps, role.getMessage());
            return;
        }
        dispatchRole(ps, user, role.getValue());
    }

    /**********
     * Dispatch only after checking the selected role against current persistent assignments.
     * Tested by TP3RoleNavigationTestingAutomation and TP3FoundationsTest dispatch cases.
     * @param stage specifies the application stage
     * @param user specifies the authenticated account snapshot
     * @param role specifies the selected stored or displayed role
     */
    public static void dispatchRole(Stage stage, User user, String role) {
        User current = requireRoleForView(stage, user, role);
        if (current == null) return;
        switch (RoleNames.toDisplayRole(role)) {
        case RoleNames.ADMIN -> guiAdminHome.ViewAdminHome.displayAdminHome(stage, current);
        case RoleNames.CONTRIBUTOR -> guiRole1.ViewRole1Home.displayRole1Home(stage, current);
        case RoleNames.VIEWER -> guiViewer.ViewViewerHome.displayViewerHome(stage, current);
        case RoleNames.CURATOR -> guiCurator.ViewCuratorHome.displayCuratorHome(stage, current);
        default -> showAccessError(stage, "Select a supported role.");
        }
    }

    /**********
     * Recheck direct view entry as well as normal login dispatch. Role decisions remain in the
     * non-GUI RoleAccessService and are also used by business operations.
     * Tested by TP3RoleNavigationTestingAutomation and TP3FoundationsTest.
     * @param stage specifies the application stage
     * @param user specifies the authenticated actor
     * @param role specifies the required role
     * @return the refreshed account, or null when access is rejected
     */
    public static User requireRoleForView(Stage stage, User user, String role) {
        lessonManagement.OperationResult<User> access = new roleManagement.RoleAccessService(
                applicationMain.FoundationsMain.database).requireRole(
                        user == null ? null : user.getUserName(), role);
        if (!access.isSuccessful()) {
            showAccessError(stage, access.getMessage());
            return null;
        }
        User current = access.getValue();
        // Retain the authenticated user's profile workflow without exposing passwords in role queries.
        current.setPassword(user.getPassword());
        return current;
    }

    private static void showAccessError(Stage stage, String message) {
        javafx.scene.control.Alert error = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        error.setTitle("Role Access");
        error.setHeaderText(null);
        error.setContentText(message);
        error.showAndWait();
        guiUserLogin.ViewUserLogin.displayUserLogin(stage);
    }
}
