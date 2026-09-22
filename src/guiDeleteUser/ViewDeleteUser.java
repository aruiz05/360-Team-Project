package guiDeleteUser;

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
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewDeleteUser Class. </p>
 *
 * <p> Description: The Java/FX-based page used by an administrator to remove another user's
 * account after an explicit confirmation.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-19 Initial version
 */
public class ViewDeleteUser {

	/*-*******************************************************************************************

	Attributes

	*/

	private static double width = applicationMain.FoundationsMain.WINDOW_WIDTH;
	private static double height = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	// GUI Area 1: Page title and current administrator
	private static Label label_PageTitle = new Label("Delete a User");
	private static Label label_UserDetails = new Label();
	private static Line line_Separator1 = new Line(20, 95, width-20, 95);

	// GUI Area 2: Select an account to remove
	private static Label label_SelectUser = new Label("Select a user:");
	protected static ComboBox<String> combobox_SelectUser = new ComboBox<String>();
	private static Label label_Description = new Label(
			"Select an account to remove. Your own account cannot be deleted.");
	protected static Button button_DeleteUser = new Button("Delete Selected User");
	protected static Alert alertError = new Alert(AlertType.ERROR);
	protected static Alert alertSuccess = new Alert(AlertType.INFORMATION);

	private static Line line_Separator2 = new Line(20, 515, width-20, 515);

	// GUI Area 3: Navigation buttons
	private static Button button_Return = new Button("Return");
	private static Button button_Logout = new Button("Logout");
	private static Button button_Quit = new Button("Quit");

	private static ViewDeleteUser theView;
	private static Database theDatabase = applicationMain.FoundationsMain.database;
	protected static Stage theStage;
	protected static User theUser;
	private static Pane theRootPane;
	private static Scene theDeleteUserScene;

	/**********
	 * <p> Method: displayDeleteUser(Stage ps, User user) </p>
	 *
	 * <p> Description: Display this page with an up-to-date list of accounts.</p>
	 *
	 * @param ps specifies the JavaFX Stage used by this page
	 * @param user specifies the administrator using this page
	 */
	public static void displayDeleteUser(Stage ps, User user) {
		theStage = ps;
		theUser = user;
		if (theView == null) theView = new ViewDeleteUser();

		label_UserDetails.setText("User: " + theUser.getUserName());
		refreshUserList();

		theStage.setTitle("CSE 360 Foundation Code: Delete a User");
		theStage.setScene(theDeleteUserScene);
		theStage.show();
	}

	/**********
	 * <p> Method: ViewDeleteUser() </p>
	 *
	 * <p> Description: Initialize the widgets and their actions once.</p>
	 */
	private ViewDeleteUser() {
		theRootPane = new Pane();
		theDeleteUserScene = new Scene(theRootPane, width, height);
		guiTools.ASUTheme.apply(theDeleteUserScene);

		setupLabelUI(label_PageTitle, 28, width, Pos.CENTER, 0, 5);
		guiTools.ASUTheme.stylePageTitle(label_PageTitle);
		guiTools.ASUTheme.styleDanger(button_DeleteUser);
		setupLabelUI(label_UserDetails, 20, width, Pos.BASELINE_LEFT, 20, 55);
		setupLabelUI(label_SelectUser, 20, 220, Pos.BASELINE_RIGHT, 20, 125);
		combobox_SelectUser.setLayoutX(260);
		combobox_SelectUser.setLayoutY(120);
		combobox_SelectUser.setPrefWidth(300);
		combobox_SelectUser.setPromptText("Select a user");
		setupLabelUI(label_Description, 17, width-40, Pos.BASELINE_LEFT, 40, 185);

		setupButtonUI(button_DeleteUser, 300, 260, 255);
		button_DeleteUser.setOnAction((_) -> ControllerDeleteUser.performDeleteUser());

		alertError.setTitle("Delete User Error");
		alertError.setHeaderText(null);
		alertSuccess.setTitle("User Deleted");
		alertSuccess.setHeaderText(null);

		setupButtonUI(button_Return, 210, 20, 540);
		button_Return.setOnAction((_) -> ControllerDeleteUser.performReturn());
		setupButtonUI(button_Logout, 210, 295, 540);
		button_Logout.setOnAction((_) -> ControllerDeleteUser.performLogout());
		setupButtonUI(button_Quit, 210, 570, 540);
		button_Quit.setOnAction((_) -> ControllerDeleteUser.performQuit());

		theRootPane.getChildren().addAll(label_PageTitle, label_UserDetails,
				line_Separator1, label_SelectUser, combobox_SelectUser, label_Description,
				button_DeleteUser, line_Separator2, button_Return, button_Logout, button_Quit);
	}

	/**********
	 * <p> Method: refreshUserList() </p>
	 *
	 * <p> Description: Reload accounts and exclude the administrator's own account.</p>
	 */
	protected static void refreshUserList() {
		List<String> userList = theDatabase.getUserList();
		if (userList == null) {
			combobox_SelectUser.setItems(FXCollections.observableArrayList());
			button_DeleteUser.setDisable(true);
			alertError.setContentText("The user list could not be loaded.");
			alertError.showAndWait();
			return;
		}
		userList.remove("<Select a User>");
		userList.remove(theUser.getUserName());
		combobox_SelectUser.setItems(FXCollections.observableArrayList(userList));
		combobox_SelectUser.getSelectionModel().clearSelection();
		button_DeleteUser.setDisable(userList.isEmpty());
	}

	// Helper methods used to place widgets consistently
	private static void setupLabelUI(Label label, double size, double widgetWidth,
			Pos position, double x, double y) {
		label.setFont(Font.font("Arial", size));
		label.setMinWidth(widgetWidth);
		label.setAlignment(position);
		label.setLayoutX(x);
		label.setLayoutY(y);
	}

	private static void setupButtonUI(Button button, double widgetWidth, double x, double y) {
		button.setFont(Font.font("Dialog", 18));
		button.setMinWidth(widgetWidth);
		button.setAlignment(Pos.CENTER);
		button.setLayoutX(x);
		button.setLayoutY(y);
	}
}
