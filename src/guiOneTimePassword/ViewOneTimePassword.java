package guiOneTimePassword;

import java.util.List;

import database.Database;
import entityClasses.User;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewOneTimePassword Class. </p>
 *
 * <p> Description: The Java/FX-based page that allows an administrator to establish a one-time
 * password for an existing user.  Password requirements are updated while the administrator
 * types, and no credential is displayed after it has been saved.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-18 Initial version
 */
public class ViewOneTimePassword {

	/*-*******************************************************************************************

	Attributes

	*/

	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	// GUI Area 1: Page title and current administrator
	protected static Label label_PageTitle = new Label("Set a One-Time Password");
	protected static Label label_UserDetails = new Label();
	private static Line line_Separator1 = new Line(20, 95, width-20, 95);

	// GUI Area 2: User selection and password input
	private static Label label_SelectUser = new Label("Select a user:");
	protected static ComboBox<String> combobox_SelectUser = new ComboBox<String>();
	private static Label label_OneTimePassword = new Label("One-time password:");
	protected static PasswordField text_OneTimePassword = new PasswordField();
	private static Label label_ConfirmOneTimePassword = new Label("Confirm password:");
	protected static PasswordField text_ConfirmOneTimePassword = new PasswordField();
	protected static Label label_PasswordRequirements = new Label();
	protected static Button button_SetOneTimePassword = new Button("Set One-Time Password");
	protected static Alert alertError = new Alert(AlertType.ERROR);
	protected static Alert alertSuccess = new Alert(AlertType.INFORMATION);

	private static Line line_Separator2 = new Line(20, 515, width-20, 515);

	// GUI Area 3: Navigation buttons
	private static Button button_Return = new Button("Return");
	private static Button button_Logout = new Button("Logout");
	private static Button button_Quit = new Button("Quit");

	private static ViewOneTimePassword theView;
	private static Database theDatabase = applicationMain.FoundationsMain.database;
	protected static Stage theStage;
	protected static User theUser;
	private static Pane theRootPane;
	private static Scene theOneTimePasswordScene;

	/**********
	 * <p> Method: displayOneTimePassword(Stage ps, User user) </p>
	 *
	 * <p> Description: Display the page and refresh the account list every time it is opened.</p>
	 *
	 * @param ps specifies the JavaFX Stage used by this page
	 *
	 * @param user specifies the administrator using this page
	 */
	public static void displayOneTimePassword(Stage ps, User user) {
		theStage = ps;
		theUser = user;
		if (theView == null) theView = new ViewOneTimePassword();

		label_UserDetails.setText("User: " + theUser.getUserName());
		refreshUserList();
		text_OneTimePassword.clear();
		text_ConfirmOneTimePassword.clear();
		ControllerOneTimePassword.updatePasswordFeedback();

		theStage.setTitle("CSE 360 Foundation Code: Set a One-Time Password");
		theStage.setScene(theOneTimePasswordScene);
		theStage.show();
	}

	/**********
	 * <p> Method: ViewOneTimePassword() </p>
	 *
	 * <p> Description: Initialize all static aspects of the One-Time Password Page.</p>
	 */
	private ViewOneTimePassword() {
		theRootPane = new Pane();
		theOneTimePasswordScene = new Scene(theRootPane, width, height);

		setupLabelUI(label_PageTitle, "Arial", 28, width, Pos.CENTER, 0, 5);
		setupLabelUI(label_UserDetails, "Arial", 20, width, Pos.BASELINE_LEFT, 20, 55);

		setupLabelUI(label_SelectUser, "Arial", 18, 220, Pos.BASELINE_RIGHT, 20, 125);
		setupComboBoxUI(combobox_SelectUser, "Dialog", 16, 300, 260, 120);
		combobox_SelectUser.setOnAction((_) ->
				ControllerOneTimePassword.updatePasswordFeedback());

		setupLabelUI(label_OneTimePassword, "Arial", 18, 220, Pos.BASELINE_RIGHT, 20, 180);
		setupTextUI(text_OneTimePassword, "Arial", 18, 300, 260, 175);
		text_OneTimePassword.setPromptText("Enter one-time password");

		setupLabelUI(label_ConfirmOneTimePassword, "Arial", 18, 220, Pos.BASELINE_RIGHT,
				20, 235);
		setupTextUI(text_ConfirmOneTimePassword, "Arial", 18, 300, 260, 230);
		text_ConfirmOneTimePassword.setPromptText("Confirm one-time password");

		setupLabelUI(label_PasswordRequirements, "Arial", 15, 460, Pos.BASELINE_LEFT,
				260, 285);
		label_PasswordRequirements.setWrapText(true);

		setupButtonUI(button_SetOneTimePassword, "Dialog", 18, 300, Pos.CENTER, 260, 455);
		button_SetOneTimePassword.setOnAction((_) ->
				ControllerOneTimePassword.performSetOneTimePassword());

		text_OneTimePassword.textProperty().addListener((_, _, _) ->
				ControllerOneTimePassword.updatePasswordFeedback());
		text_ConfirmOneTimePassword.textProperty().addListener((_, _, _) ->
				ControllerOneTimePassword.updatePasswordFeedback());

		alertError.setTitle("One-Time Password Error");
		alertError.setHeaderText(null);
		alertSuccess.setTitle("One-Time Password Established");
		alertSuccess.setHeaderText(null);

		setupButtonUI(button_Return, "Dialog", 18, 210, Pos.CENTER, 20, 540);
		button_Return.setOnAction((_) -> ControllerOneTimePassword.performReturn());
		setupButtonUI(button_Logout, "Dialog", 18, 210, Pos.CENTER, 295, 540);
		button_Logout.setOnAction((_) -> ControllerOneTimePassword.performLogout());
		setupButtonUI(button_Quit, "Dialog", 18, 210, Pos.CENTER, 570, 540);
		button_Quit.setOnAction((_) -> ControllerOneTimePassword.performQuit());

		theRootPane.getChildren().addAll(label_PageTitle, label_UserDetails, line_Separator1,
				label_SelectUser, combobox_SelectUser, label_OneTimePassword,
				text_OneTimePassword, label_ConfirmOneTimePassword,
				text_ConfirmOneTimePassword, label_PasswordRequirements,
				button_SetOneTimePassword, line_Separator2, button_Return, button_Logout,
				button_Quit);
	}

	/**********
	 * <p> Method: refreshUserList() </p>
	 *
	 * <p> Description: Refresh the controlled account list from the database.</p>
	 */
	protected static void refreshUserList() {
		List<String> userList = theDatabase.getUserList();
		combobox_SelectUser.setItems(FXCollections.observableArrayList(userList));
		combobox_SelectUser.getSelectionModel().select(0);
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

	private static void setupComboBoxUI(ComboBox<String> combobox, String font, double size,
			double widgetWidth, double x, double y) {
		combobox.setStyle("-fx-font: " + size + " " + font + ";");
		combobox.setMinWidth(widgetWidth);
		combobox.setLayoutX(x);
		combobox.setLayoutY(y);
	}
}
