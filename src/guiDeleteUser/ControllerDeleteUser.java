package guiDeleteUser;

import database.Database;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

/*******
 * <p> Title: ControllerDeleteUser Class. </p>
 *
 * <p> Description: Actions for deleting another user's account after the administrator
 * explicitly confirms the selected username.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-19 Initial version
 */
public class ControllerDeleteUser {

	// Reference for the database used by this page
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	/**
	 * Default constructor is not used.
	 */
	public ControllerDeleteUser() {
	}

	/**********
	 * <p> Method: performDeleteUser() </p>
	 *
	 * <p> Description: Validate the selected account, ask for an explicit Yes response,
	 * and delete the account only after the administrator confirms.</p>
	 */
	protected static void performDeleteUser() {
		String username = ViewDeleteUser.combobox_SelectUser.getValue();
		if (username == null) {
			showError("Select a user to delete.");
			return;
		}
		if (username.equals(ViewDeleteUser.theUser.getUserName())) {
			showError("You cannot delete your own account.");
			return;
		}

		Alert confirmation = new Alert(AlertType.CONFIRMATION,
				"Delete the account for " + username + "? This user will no longer be able to log in.",
				ButtonType.YES, ButtonType.NO);
		confirmation.setTitle("Confirm User Deletion");
		confirmation.setHeaderText("Are you sure?");
		if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

		if (!theDatabase.deleteUser(username, ViewDeleteUser.theUser.getUserName())) {
			showError("The account could not be deleted. Refresh the list and try again.");
			ViewDeleteUser.refreshUserList();
			return;
		}

		ViewDeleteUser.refreshUserList();
		ViewDeleteUser.alertSuccess.setContentText("The account for " + username + " was deleted.");
		ViewDeleteUser.alertSuccess.showAndWait();
	}

	private static void showError(String message) {
		ViewDeleteUser.alertError.setContentText(message);
		ViewDeleteUser.alertError.showAndWait();
	}

	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Return to the administrator's home page.</p>
	 */
	protected static void performReturn() {
		guiAdminHome.ViewAdminHome.displayAdminHome(ViewDeleteUser.theStage, ViewDeleteUser.theUser);
	}

	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Log out and return to the login page.</p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewDeleteUser.theStage);
	}

	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Exit the application.</p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}
}
