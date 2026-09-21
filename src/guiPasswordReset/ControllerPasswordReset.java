package guiPasswordReset;

import database.Database;
import inputValidation.PasswordEvaluator;

/*******
 * <p> Title: ControllerPasswordReset Class. </p>
 *
 * <p> Description: The Java/FX-based forced Password Reset Page controller.  This class validates
 * a new permanent password and saves it before returning the user to the login page.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-18 Initial version
 */
public class ControllerPasswordReset {

	// Reference for the in-memory database so this package has access
	private static Database theDatabase = applicationMain.FoundationsMain.database;

	/**
	 * Default constructor is not used.
	 */
	public ControllerPasswordReset() {
	}

	/**********
	 * <p> Method: updatePasswordFeedback() </p>
	 *
	 * <p> Description: Update the dynamic requirements and enable the reset button only when the
	 * new password and confirmation are valid.</p>
	 */
	protected static void updatePasswordFeedback() {
		String password = ViewPasswordReset.text_NewPassword.getText();
		String confirmation = ViewPasswordReset.text_ConfirmPassword.getText();

		ViewPasswordReset.label_PasswordRequirements.setText(
				PasswordEvaluator.getRequirementText(password));
		boolean validPassword = PasswordEvaluator.evaluatePassword(password).length() == 0;
		boolean passwordsMatch = password.compareTo(confirmation) == 0;
		ViewPasswordReset.button_ResetPassword.setDisable(
				!validPassword || !passwordsMatch);
	}

	/**********
	 * <p> Method: performPasswordReset() </p>
	 *
	 * <p> Description: Validate the new permanent password and confirmation in the required order.
	 * A successful reset clears the reset-required state and requires a fresh login.</p>
	 */
	protected static void performPasswordReset() {
		String password = ViewPasswordReset.text_NewPassword.getText();
		String confirmation = ViewPasswordReset.text_ConfirmPassword.getText();

		// Every text field is checked for excessive length before any other processing
		if (password.length() > PasswordEvaluator.MAXIMUM_LENGTH ||
				confirmation.length() > PasswordEvaluator.MAXIMUM_LENGTH) {
			displayError("A password cannot exceed 64 characters.");
			return;
		}

		String passwordError = PasswordEvaluator.evaluatePassword(password);
		if (passwordError.length() > 0) {
			displayError(passwordError);
			return;
		}

		if (confirmation.length() == 0 || password.compareTo(confirmation) != 0) {
			displayError("The passwords do not match.");
			return;
		}

		if (!theDatabase.completePasswordReset(ViewPasswordReset.theUsername, password)) {
			displayError("The password could not be changed. Ask an administrator for a new " +
					"one-time password.");
			return;
		}

		// Remove the credentials from the page before the user returns to login
		ViewPasswordReset.text_NewPassword.clear();
		ViewPasswordReset.text_ConfirmPassword.clear();
		ViewPasswordReset.alertSuccess.setContentText(
				"Your password was changed. Log in again using the new password.");
		ViewPasswordReset.alertSuccess.showAndWait();
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewPasswordReset.theStage);
	}

	// Display a consistent error alert for this workflow
	private static void displayError(String message) {
		ViewPasswordReset.alertError.setContentText(message);
		ViewPasswordReset.alertError.showAndWait();
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
