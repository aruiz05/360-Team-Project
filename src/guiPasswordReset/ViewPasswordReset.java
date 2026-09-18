package guiPasswordReset;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewPasswordReset Class. </p>
 *
 * <p> Description: The Java/FX-based page that requires a user who entered a valid one-time
 * password to establish a new permanent password.  This page does not provide access to any role
 * home page, and it returns to login after the password is changed.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-18 Initial version
 */
public class ViewPasswordReset {

	/*-*******************************************************************************************

	Attributes

	*/

	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	// GUI Area 1: Page title and instructions
	private static Label label_PageTitle = new Label("Create a New Password");
	private static Label label_Instructions = new Label(
			"Your one-time password was accepted. Create a permanent password to continue.");
	private static Line line_Separator1 = new Line(20, 105, width-20, 105);

	// GUI Area 2: Permanent password input
	private static Label label_NewPassword = new Label("New password:");
	protected static PasswordField text_NewPassword = new PasswordField();
	private static Label label_ConfirmPassword = new Label("Confirm password:");
	protected static PasswordField text_ConfirmPassword = new PasswordField();
	protected static Label label_PasswordRequirements = new Label();
	protected static Button button_ResetPassword = new Button("Save New Password");
	protected static Alert alertError = new Alert(AlertType.ERROR);
	protected static Alert alertSuccess = new Alert(AlertType.INFORMATION);

	private static Line line_Separator2 = new Line(20, 510, width-20, 510);

	// GUI Area 3: The only available action before the reset is complete
	private static Button button_Quit = new Button("Quit");

	private static ViewPasswordReset theView;
	protected static Stage theStage;
	protected static String theUsername;
	private static Pane theRootPane;
	private static Scene thePasswordResetScene;

	/**********
	 * <p> Method: displayPasswordReset(Stage ps, String username) </p>
	 *
	 * <p> Description: Display a clean forced-reset page for the account that used a valid one-time
	 * password.</p>
	 *
	 * @param ps specifies the JavaFX Stage used by this page
	 *
	 * @param username specifies the account completing the password reset
	 */
	public static void displayPasswordReset(Stage ps, String username) {
		theStage = ps;
		theUsername = username;
		if (theView == null) theView = new ViewPasswordReset();

		text_NewPassword.clear();
		text_ConfirmPassword.clear();
		ControllerPasswordReset.updatePasswordFeedback();

		theStage.setTitle("CSE 360 Foundation Code: Create a New Password");
		theStage.setScene(thePasswordResetScene);
		theStage.show();
	}

	/**********
	 * <p> Method: ViewPasswordReset() </p>
	 *
	 * <p> Description: Initialize all static aspects of the forced Password Reset Page.</p>
	 */
	private ViewPasswordReset() {
		theRootPane = new Pane();
		thePasswordResetScene = new Scene(theRootPane, width, height);

		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_Instructions, "Arial", 17, width-40, Pos.CENTER, 20, 55);
		label_Instructions.setWrapText(true);

		setupLabelUI(label_NewPassword, "Arial", 18, 220, Pos.BASELINE_RIGHT, 20, 145);
		setupTextUI(text_NewPassword, "Arial", 18, 300, 260, 140);
		text_NewPassword.setPromptText("Enter new password");

		setupLabelUI(label_ConfirmPassword, "Arial", 18, 220, Pos.BASELINE_RIGHT, 20, 200);
		setupTextUI(text_ConfirmPassword, "Arial", 18, 300, 260, 195);
		text_ConfirmPassword.setPromptText("Confirm new password");

		setupLabelUI(label_PasswordRequirements, "Arial", 15, 460, Pos.BASELINE_LEFT,
				260, 250);
		label_PasswordRequirements.setWrapText(true);

		setupButtonUI(button_ResetPassword, "Dialog", 18, 300, Pos.CENTER, 260, 450);
		button_ResetPassword.setOnAction((_) ->
				ControllerPasswordReset.performPasswordReset());

		text_NewPassword.textProperty().addListener((_, _, _) ->
				ControllerPasswordReset.updatePasswordFeedback());
		text_ConfirmPassword.textProperty().addListener((_, _, _) ->
				ControllerPasswordReset.updatePasswordFeedback());

		alertError.setTitle("Password Reset Error");
		alertError.setHeaderText(null);
		alertSuccess.setTitle("Password Reset Complete");
		alertSuccess.setHeaderText(null);

		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 295, 535);
		button_Quit.setOnAction((_) -> ControllerPasswordReset.performQuit());

		theRootPane.getChildren().addAll(label_PageTitle, label_Instructions, line_Separator1,
				label_NewPassword, text_NewPassword, label_ConfirmPassword,
				text_ConfirmPassword, label_PasswordRequirements, button_ResetPassword,
				line_Separator2, button_Quit);
	}

	// Helper methods to consistently establish the JavaFX widgets
	private static void setupLabelUI(Label label, String font, double size, double widgetWidth,
			Pos position, double x, double y) {
		label.setFont(Font.font(font, size));
		label.setMinWidth(widgetWidth);
		label.setAlignment(position);
		label.setLayoutX(x);
		label.setLayoutY(y);
	}

	private static void setupTextUI(PasswordField field, String font, double size,
			double widgetWidth, double x, double y) {
		field.setFont(Font.font(font, size));
		field.setMinWidth(widgetWidth);
		field.setMaxWidth(widgetWidth);
		field.setLayoutX(x);
		field.setLayoutY(y);
	}

	private static void setupButtonUI(Button button, String font, double size,
			double widgetWidth, Pos position, double x, double y) {
		button.setFont(Font.font(font, size));
		button.setMinWidth(widgetWidth);
		button.setAlignment(position);
		button.setLayoutX(x);
		button.setLayoutY(y);
	}
}
