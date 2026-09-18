package guiOneTimePassword;

import database.Database;
import inputValidation.PasswordEvaluator;

/*******
 * <p> Title: ControllerOneTimePassword Class. </p>
 *
 * <p> Description: The Java/FX-based One-Time Password Page controller.  This class validates
 * the selected user and the password fields before asking the database to establish a one-time
 * credential.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-18 Initial version
 */
public class ControllerOneTimePassword {

	// Reference for the in-memory database so this package has access
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	/**
	 * Default constructor is not used.
	 */
	public ControllerOneTimePassword() {
	}

	/**********
	 * <p> Method: updatePasswordFeedback() </p>
	 *
	 * <p> Description: Update the dynamic password requirements and enable the submit button only
	 * when the selected user and both password fields are valid.</p>
	 */
	protected static void updatePasswordFeedback() {
		String password = ViewOneTimePassword.text_OneTimePassword.getText();
		String confirmation = ViewOneTimePassword.text_ConfirmOneTimePassword.getText();
		String selectedUser = ViewOneTimePassword.combobox_SelectUser.getValue();

		ViewOneTimePassword.label_PasswordRequirements.setText(
				PasswordEvaluator.getRequirementText(password));
		boolean validUser = selectedUser != null &&
				selectedUser.compareTo("<Select a User>") != 0;
		boolean validPassword = PasswordEvaluator.evaluatePassword(password).length() == 0;
		boolean passwordsMatch = password.compareTo(confirmation) == 0;
		ViewOneTimePassword.button_SetOneTimePassword.setDisable(
				!validUser || !validPassword || !passwordsMatch);
	}

	/**********
	 * <p> Method: performSetOneTimePassword() </p>
	 *
	 * <p> Description: Validate the selected user, one-time password, and confirmation in the
	 * required order.  Only fully valid input is sent to the database.</p>
	 */
	protected static void performSetOneTimePassword() {
		String selectedUser = ViewOneTimePassword.combobox_SelectUser.getValue();
		String password = ViewOneTimePassword.text_OneTimePassword.getText();
		String confirmation = ViewOneTimePassword.text_ConfirmOneTimePassword.getText();

		// Every text field is checked for excessive length before any other processing
		if (password.length() > PasswordEvaluator.MAXIMUM_LENGTH ||
				confirmation.length() > PasswordEvaluator.MAXIMUM_LENGTH) {
			displayError("A one-time password cannot exceed 64 characters.");
			return;
		}

		if (selectedUser == null || selectedUser.compareTo("<Select a User>") == 0) {
			displayError("Select a user.");
			return;
		}

		// Recheck the controlled selection in case the account changed after the page was opened
		if (!theDatabase.doesUserExist(selectedUser)) {
			displayError("The selected user is no longer available.");
			ViewOneTimePassword.refreshUserList();
			return;
		}

		String passwordError = PasswordEvaluator.evaluatePassword(password);
		if (passwordError.length() > 0) {
			displayError(passwordError);
			return;
		}

		if (confirmation.length() == 0 || password.compareTo(confirmation) != 0) {
			displayError("The one-time passwords do not match.");
			return;
		}

		if (!theDatabase.setOneTimePassword(selectedUser, password)) {
			displayError("The one-time password could not be saved. Try again.");
			return;
		}

		// Clear the secrets before confirming success so they are not left on the screen
		ViewOneTimePassword.text_OneTimePassword.clear();
		ViewOneTimePassword.text_ConfirmOneTimePassword.clear();
		ViewOneTimePassword.alertSuccess.setContentText(
				"A one-time password was established for " + selectedUser + ".");
		ViewOneTimePassword.alertSuccess.showAndWait();
		updatePasswordFeedback();
	}

	// Display a consistent error alert for this workflow
	private static void displayError(String message) {
		ViewOneTimePassword.alertError.setContentText(message);
		ViewOneTimePassword.alertError.showAndWait();
	}

	/**********
	 * <p> Method: performReturn() </p>
	 *
	 * <p> Description: Return to the Admin Home Page.</p>
	 */
	protected static void performReturn() {
		guiAdminHome.ViewAdminHome.displayAdminHome(ViewOneTimePassword.theStage,
				ViewOneTimePassword.theUser);
	}

	/**********
	 * <p> Method: performLogout() </p>
	 *
	 * <p> Description: Log the administrator out and return to the login page.</p>
	 */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewOneTimePassword.theStage);
	}

	/**********
	 * <p> Method: performQuit() </p>
	 *
	 * <p> Description: Gracefully terminate the application.</p>
	 */
	protected static void performQuit() {
		System.exit(0);
	}
}
