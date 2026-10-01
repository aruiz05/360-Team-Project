package guiLessons;

import entityClasses.LessonLearned;
import entityClasses.LessonsCollection;
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
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewLessons Class. </p>
 *
 * <p> Description: Displays the signed-in contributor's lesson collection and a simple title
 * subset.  Every refresh replaces the table contents with current database results so additions,
 * edits, and deletions cannot leave stale entries.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ViewLessons {

	private static final double WIDTH = applicationMain.FoundationsMain.WINDOW_WIDTH;
	private static final double HEIGHT = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	private static final Label labelPageTitle = new Label("My Lessons Learned");
	private static final Label labelUser = new Label();
	private static final Label labelStatus = new Label();
	protected static final TextField fieldFilter = new TextField();
	protected static final TableView<LessonLearned> tableLessons =
			new TableView<LessonLearned>();
	private static final Button buttonFilter = new Button("Apply Filter");
	private static final Button buttonClear = new Button("Show All");
	private static final Button buttonCreate = new Button("Create");
	private static final Button buttonView = new Button("View");
	private static final Button buttonEdit = new Button("Edit");
	private static final Button buttonDelete = new Button("Delete");
	private static final Button buttonReturn = new Button("Return");
	private static final Button buttonLogout = new Button("Logout");
	private static final Button buttonQuit = new Button("Quit");

	private static ViewLessons theView;
	protected static Stage theStage;
	protected static User theUser;
	private static Scene scene;

	/**********
	 * @param stage specifies the application stage
	 * @param user specifies the signed-in contributor
	 */
	public static void displayLessons(Stage stage, User user) {
		theStage = stage;
		theUser = user;
		if (theView == null) theView = new ViewLessons();
		labelUser.setText("Contributor: " + user.getUserName());
		ControllerLessons.performRefresh();
		stage.setTitle("HW2: My Lessons Learned");
		stage.setScene(scene);
		stage.show();
	}

	private ViewLessons() {
		Pane root = new Pane();
		scene = new Scene(root, WIDTH, HEIGHT);
		guiTools.ASUTheme.apply(scene);

		setupLabel(labelPageTitle, 28, WIDTH, Pos.CENTER, 0, 5);
		guiTools.ASUTheme.stylePageTitle(labelPageTitle);
		setupLabel(labelUser, 17, 300, Pos.BASELINE_LEFT, 20, 55);
		setupLabel(labelStatus, 15, 430, Pos.BASELINE_LEFT, 350, 55);

		fieldFilter.setPromptText("Filter by title");
		fieldFilter.setLayoutX(20);
		fieldFilter.setLayoutY(87);
		fieldFilter.setPrefWidth(330);
		setupButton(buttonFilter, 125, 365, 84);
		setupButton(buttonClear, 125, 505, 84);

		TableColumn<LessonLearned, Number> id = new TableColumn<LessonLearned, Number>("ID");
		id.setCellValueFactory(data -> new ReadOnlyLongWrapper(data.getValue().getId()));
		id.setPrefWidth(70);
		TableColumn<LessonLearned, String> title =
				new TableColumn<LessonLearned, String>("Title");
		title.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitle()));
		title.setPrefWidth(260);
		TableColumn<LessonLearned, String> problem =
				new TableColumn<LessonLearned, String>("Problem or Situation");
		problem.setCellValueFactory(data ->
				new ReadOnlyStringWrapper(data.getValue().getProblemSituation()));
		problem.setPrefWidth(300);
		TableColumn<LessonLearned, String> locked =
				new TableColumn<LessonLearned, String>("Lock Status");
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
			if (event.getClickCount() == 2) ControllerLessons.performView();
		});

		setupButton(buttonCreate, 125, 20, 475);
		setupButton(buttonView, 125, 155, 475);
		setupButton(buttonEdit, 125, 290, 475);
		setupButton(buttonDelete, 125, 425, 475);
		guiTools.ASUTheme.styleDanger(buttonDelete);
		buttonCreate.setOnAction((_) -> ControllerLessons.performCreate());
		buttonView.setOnAction((_) -> ControllerLessons.performView());
		buttonEdit.setOnAction((_) -> ControllerLessons.performEdit());
		buttonDelete.setOnAction((_) -> ControllerLessons.performDelete());
		buttonFilter.setOnAction((_) -> ControllerLessons.performRefresh());
		buttonClear.setOnAction((_) -> {
			fieldFilter.clear();
			ControllerLessons.performRefresh();
		});

		setupButton(buttonReturn, 200, 20, 540);
		setupButton(buttonLogout, 200, 300, 540);
		setupButton(buttonQuit, 200, 580, 540);
		buttonReturn.setOnAction((_) -> ControllerLessons.performReturn());
		buttonLogout.setOnAction((_) -> ControllerLessons.performLogout());
		buttonQuit.setOnAction((_) -> ControllerLessons.performQuit());

		root.getChildren().addAll(labelPageTitle, labelUser, labelStatus, fieldFilter,
				buttonFilter, buttonClear, tableLessons, buttonCreate, buttonView, buttonEdit,
				buttonDelete, buttonReturn, buttonLogout, buttonQuit);
	}

	/**
	 * @param lessons specifies the full list or filtered subset to display
	 * @param message describes the collection result
	 */
	protected static void setLessons(LessonsCollection lessons, String message) {
		tableLessons.setItems(FXCollections.observableArrayList(lessons.getLessons()));
		labelStatus.setText(message);
	}

	/** @return the lesson currently selected in the table, or null when none is selected */
	protected static LessonLearned getSelectedLesson() {
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
