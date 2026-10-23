package guiContributorLessons;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import entityClasses.User;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewContributorLessons Class. </p>
 *
 * <p> Description: The JavaFX View for the Contributor Lessons window. It displays only the
 * signed-in contributor's lessons, supports title-keyword subsets, and provides the visible
 * controls needed to create, read, update, and delete a selected lesson. The owner restriction
 * is enforced again by the Model and persistent classes; the View never requests a system-wide
 * lesson list.</p>
 *
 * <p>The table selection controls which actions are available. Delete is disabled when the
 * complete lesson or any core field is locked, while Edit remains available when at least one
 * core field can still be changed. Every refresh replaces the table contents with current
 * persistent values so the user never sees a stale or duplicate row.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Separated the Contributor list into its own MVC package
 */
public class ViewContributorLessons {

	/** Foundation window width retained so the new page matches the existing application. */
	private static final double WIDTH = applicationMain.FoundationsMain.WINDOW_WIDTH;

	/** Foundation window height retained so navigation does not resize the shared stage. */
	private static final double HEIGHT = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	/** Identifies the page as the signed-in Contributor's lesson collection. */
	private static final Label labelPageTitle = new Label("My Lessons Learned");

	/** Shows which Contributor owns every record permitted in this View. */
	private static final Label labelUser = new Label();

	/** Displays list counts, successes, and helpful correction messages. */
	private static final Label labelStatus = new Label();

	/** Accepts optional title keywords used to create a Contributor-only subset. */
	protected static final TextField fieldFilter = new TextField();

	/** Displays the current full list or subset and supplies the selected lesson. */
	protected static final TableView<LessonsLearned> tableLessons =
			new TableView<LessonsLearned>();

	/** Requests a subset using the current title keywords. */
	private static final Button buttonFilter = new Button("Apply Filter");

	/** Clears all title keywords and restores the complete Contributor list. */
	private static final Button buttonClear = new Button("Show All");

	/** Opens an empty editor for a new lesson. */
	private static final Button buttonCreate = new Button("Create");

	/** Opens the selected lesson and its related experience information. */
	private static final Button buttonView = new Button("View");

	/** Opens the selected lesson while disabling its persisted locked fields. */
	private static final Button buttonEdit = new Button("Edit");

	/** Requests confirmed deletion when no lesson or related field is locked. */
	private static final Button buttonDelete = new Button("Delete");

	/** Returns to the Contributor home page without ending the session. */
	private static final Button buttonReturn = new Button("Return");

	/** Ends the current session and returns to the Foundation login page. */
	private static final Button buttonLogout = new Button("Logout");

	/** Closes the shared database connection and exits the application. */
	private static final Button buttonQuit = new Button("Quit");

	/** Single reusable View instance for the Foundation application's shared-stage pattern. */
	private static ViewContributorLessons theView;

	/** Shared Foundation stage used when this View navigates to another GUI package. */
	protected static Stage theStage;

	/** Signed-in user whose username is passed to every Model ownership check. */
	protected static User theUser;

	/** Scene retained so returning to the page reuses its controls and styling. */
	private static Scene scene;

	/**********
	 * @param stage specifies the application stage
	 * @param user specifies the signed-in contributor
	 */
	public static void displayLessons(Stage stage, User user) {
		theStage = stage;
		theUser = user;
		if (theView == null) theView = new ViewContributorLessons();
		labelUser.setText("Contributor: " + user.getUserName());
		ControllerContributorLessons.performRefresh();
		stage.setTitle("TP2: My Lessons Learned");
		stage.setScene(scene);
		stage.show();
	}

	private ViewContributorLessons() {
		Pane root = new Pane();
		scene = new Scene(root, WIDTH, HEIGHT);
		guiTools.ASUTheme.apply(scene);

		setupLabel(labelPageTitle, 28, WIDTH, Pos.CENTER, 0, 5);
		guiTools.ASUTheme.stylePageTitle(labelPageTitle);
		setupLabel(labelUser, 17, 300, Pos.BASELINE_LEFT, 20, 55);
		setupLabel(labelStatus, 15, 430, Pos.BASELINE_LEFT, 350, 55);

		fieldFilter.setPromptText("Filter by one or more title keywords");
		fieldFilter.setLayoutX(20);
		fieldFilter.setLayoutY(87);
		fieldFilter.setPrefWidth(330);
		setupButton(buttonFilter, 125, 365, 84);
		setupButton(buttonClear, 125, 505, 84);

		TableColumn<LessonsLearned, Number> id = new TableColumn<LessonsLearned, Number>("ID");
		id.setCellValueFactory(data -> new ReadOnlyLongWrapper(data.getValue().getId()));
		id.setPrefWidth(70);
		TableColumn<LessonsLearned, String> title =
				new TableColumn<LessonsLearned, String>("Title");
		title.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitle()));
		title.setPrefWidth(260);
		TableColumn<LessonsLearned, String> problem =
				new TableColumn<LessonsLearned, String>("Problem or Situation");
		problem.setCellValueFactory(data ->
				new ReadOnlyStringWrapper(data.getValue().getProblemSituation()));
		problem.setPrefWidth(300);
		TableColumn<LessonsLearned, String> locked =
				new TableColumn<LessonsLearned, String>("Lock Status");
		locked.setCellValueFactory(data -> new ReadOnlyStringWrapper(
				data.getValue().hasAnyLockedField() ? "Locked field(s)" : "Editable"));
		locked.setPrefWidth(130);
		tableLessons.getColumns().add(id);
		tableLessons.getColumns().add(title);
		tableLessons.getColumns().add(problem);
		tableLessons.getColumns().add(locked);
		tableLessons.setLayoutX(20);
		tableLessons.setLayoutY(130);
		tableLessons.setPrefSize(WIDTH - 40, 330);
		tableLessons.setPlaceholder(new Label("No lessons found."));
		tableLessons.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) ControllerContributorLessons.performView();
		});
		tableLessons.getSelectionModel().selectedItemProperty().addListener(
				(_, _, _) -> updateActionButtons());

		setupButton(buttonCreate, 125, 20, 475);
		setupButton(buttonView, 125, 155, 475);
		setupButton(buttonEdit, 125, 290, 475);
		setupButton(buttonDelete, 125, 425, 475);
		guiTools.ASUTheme.styleDanger(buttonDelete);
		buttonView.setTooltip(new Tooltip("Open the selected lesson and its experience records."));
		buttonEdit.setTooltip(new Tooltip("Edit the unlocked fields of the selected lesson."));
		buttonDelete.setTooltip(new Tooltip(
				"Lessons with locked core or related information cannot be deleted."));
		buttonCreate.setOnAction((_) -> ControllerContributorLessons.performCreate());
		buttonView.setOnAction((_) -> ControllerContributorLessons.performView());
		buttonEdit.setOnAction((_) -> ControllerContributorLessons.performEdit());
		buttonDelete.setOnAction((_) -> ControllerContributorLessons.performDelete());
		buttonFilter.setOnAction((_) -> ControllerContributorLessons.performRefresh());
		buttonClear.setOnAction((_) -> {
			fieldFilter.clear();
			ControllerContributorLessons.performRefresh();
		});

		setupButton(buttonReturn, 200, 20, 540);
		setupButton(buttonLogout, 200, 300, 540);
		setupButton(buttonQuit, 200, 580, 540);
		buttonReturn.setOnAction((_) -> ControllerContributorLessons.performReturn());
		buttonLogout.setOnAction((_) -> ControllerContributorLessons.performLogout());
		buttonQuit.setOnAction((_) -> ControllerContributorLessons.performQuit());

		root.getChildren().addAll(labelPageTitle, labelUser, labelStatus, fieldFilter,
				buttonFilter, buttonClear, tableLessons, buttonCreate, buttonView, buttonEdit,
				buttonDelete, buttonReturn, buttonLogout, buttonQuit);
		updateActionButtons();
	}

	/**
	 * @param lessons specifies the full list or filtered subset to display
	 * @param message describes the collection result
	 */
	protected static void setLessons(LessonsLearnedList lessons, String message) {
		tableLessons.setItems(FXCollections.observableArrayList(lessons.getLessons()));
		labelStatus.setText(message);
		updateActionButtons();
	}

	/** @return the lesson currently selected in the table, or null when none is selected */
	protected static LessonsLearned getSelectedLesson() {
		return tableLessons.getSelectionModel().getSelectedItem();
	}

	/** @param message explains the collection operation that failed */
	protected static void showError(String message) {
		labelStatus.setText(message);
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Lesson Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	/** @param message describes the completed collection operation */
	protected static void showInformation(String message) {
		labelStatus.setText(message);
	}

	/**********
	 * <p> Method: updateActionButtons() </p>
	 *
	 * <p> Description: Reflect the selected lesson's persisted lock state in the controls. A
	 * field-level lock still permits editing other fields, but any lock prevents deletion as
	 * required by the Contributor stories.</p>
	 */
	private static void updateActionButtons() {
		LessonsLearned selected = getSelectedLesson();
		buttonView.setDisable(selected == null);
		buttonEdit.setDisable(selected == null || selected.isLessonLocked());
		buttonDelete.setDisable(selected == null || selected.hasAnyLockedField());
	}

	private static void setupLabel(Label label, double size, double labelWidth, Pos alignment,
			double x, double y) {
		label.setFont(Font.font("Arial", size));
		label.setMinWidth(labelWidth);
		label.setAlignment(alignment);
		label.setLayoutX(x);
		label.setLayoutY(y);
	}

	private static void setupButton(Button button, double buttonWidth, double x, double y) {
		button.setFont(Font.font("Dialog", 15));
		button.setMinWidth(buttonWidth);
		button.setLayoutX(x);
		button.setLayoutY(y);
	}
}
