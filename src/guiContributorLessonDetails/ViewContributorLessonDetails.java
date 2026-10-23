package guiContributorLessonDetails;

import java.util.List;
import java.util.stream.Collectors;

import entityClasses.ExperienceRecord;
import entityClasses.LessonsLearned;
import entityClasses.TeamMemberEffort;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewContributorLessonDetails Class. </p>
 *
 * <p> Description: The JavaFX View for one contributor-owned lesson and its experience records.
 * It shows current core content, the owner, clear lock status for each core field, and the time
 * and effort information from later attempts to use the lesson. Separate information added by
 * another role is intentionally absent from this View.</p>
 *
 * <p>Action availability follows the persisted lock state. A whole-lesson lock prevents adding
 * or changing experience information, while a field-level experience lock disables deletion but
 * still permits editing any unlocked experience fields.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Separated lesson details into its own MVC package
 */
public class ViewContributorLessonDetails {

	/** Foundation window width retained for consistent navigation and layout. */
	private static final double WIDTH = applicationMain.FoundationsMain.WINDOW_WIDTH;

	/** Foundation window height retained for consistent navigation and layout. */
	private static final double HEIGHT = applicationMain.FoundationsMain.WINDOW_HEIGHT;

	/** Identifies the selected-lesson detail page. */
	private static final Label labelPageTitle = new Label("Lesson Details");

	/** Displays the stable lesson ID and owner used by relationship and privacy checks. */
	private static final Label labelIdentity = new Label();

	/** Displays whether the title is editable or locked. */
	private static final Label labelTitleLock = new Label();

	/** Displays whether the problem or situation is editable or locked. */
	private static final Label labelProblemLock = new Label();

	/** Displays whether the lesson text is editable or locked. */
	private static final Label labelLessonLock = new Label();

	/** Displays experience counts, successes, and helpful correction messages. */
	private static final Label labelStatus = new Label();

	/** Read-only current title, including any permitted Curator edit. */
	private static final TextArea textTitle = displayArea(1);

	/** Read-only current problem or situation, including any permitted Curator edit. */
	private static final TextArea textProblem = displayArea(2);

	/** Read-only current lesson text, including any permitted Curator edit. */
	private static final TextArea textLesson = displayArea(2);

	/** Displays only the signed-in Contributor's related experience records. */
	protected static final TableView<ExperienceRecord> tableExperiences =
			new TableView<ExperienceRecord>();

	/** Opens an editor for a new experience record under the selected lesson. */
	private static final Button buttonAdd = new Button("Add Experience");

	/** Opens the selected experience while disabling its persisted locked fields. */
	private static final Button buttonEdit = new Button("Edit Experience");

	/** Requests confirmed deletion when the selected experience has no locks. */
	private static final Button buttonDelete = new Button("Delete Experience");

	/** Returns to the current Contributor's lesson collection. */
	private static final Button buttonBack = new Button("Back to Lessons");

	/** Single reusable detail View following the Foundation shared-stage pattern. */
	private static ViewContributorLessonDetails theView;

	/** Shared stage used to return to the collection package. */
	protected static Stage theStage;

	/** Signed-in user whose username is passed to every ownership check. */
	protected static User theUser;

	/** Current persistent lesson whose core and experience information is displayed. */
	protected static LessonsLearned currentLesson;

	/** Scene retained so returning to this window preserves the Foundation navigation pattern. */
	private static Scene scene;

	/**********
	 * @param stage specifies the application stage
	 * @param user specifies the signed-in contributor
	 * @param lesson specifies the selected contributor-owned lesson
	 */
	public static void displayLessonDetails(Stage stage, User user, LessonsLearned lesson) {
		theStage = stage;
		theUser = user;
		currentLesson = lesson;
		if (theView == null) theView = new ViewContributorLessonDetails();
		ControllerContributorLessonDetails.performRefresh();
		stage.setTitle("TP2: Lesson Details");
		stage.setScene(scene);
		stage.show();
	}

	private ViewContributorLessonDetails() {
		Pane root = new Pane();
		scene = new Scene(root, WIDTH, HEIGHT);
		guiTools.ASUTheme.apply(scene);
		setupLabel(labelPageTitle, 28, WIDTH, Pos.CENTER, 0, 5);
		guiTools.ASUTheme.stylePageTitle(labelPageTitle);
		setupLabel(labelIdentity, 15, 500, Pos.BASELINE_LEFT, 20, 52);
		setupLabel(labelStatus, 14, 260, Pos.BASELINE_RIGHT, 520, 52);

		Label labelTitle = new Label("Title");
		setupLabel(labelTitle, 14, 120, Pos.BASELINE_LEFT, 20, 82);
		textTitle.setLayoutX(130);
		textTitle.setLayoutY(76);
		textTitle.setPrefSize(500, 35);
		setupLabel(labelTitleLock, 13, 135, Pos.BASELINE_LEFT, 640, 82);

		Label labelProblem = new Label("Problem or situation");
		setupLabel(labelProblem, 14, 120, Pos.BASELINE_LEFT, 20, 125);
		textProblem.setLayoutX(130);
		textProblem.setLayoutY(118);
		textProblem.setPrefSize(500, 60);
		setupLabel(labelProblemLock, 13, 135, Pos.BASELINE_LEFT, 640, 125);

		Label labelLesson = new Label("Lesson learned");
		setupLabel(labelLesson, 14, 120, Pos.BASELINE_LEFT, 20, 190);
		textLesson.setLayoutX(130);
		textLesson.setLayoutY(184);
		textLesson.setPrefSize(500, 60);
		setupLabel(labelLessonLock, 13, 135, Pos.BASELINE_LEFT, 640, 190);

		TableColumn<ExperienceRecord, Number> id =
				new TableColumn<ExperienceRecord, Number>("ID");
		id.setCellValueFactory(data -> new ReadOnlyLongWrapper(data.getValue().getId()));
		id.setPrefWidth(55);
		TableColumn<ExperienceRecord, String> what =
				new TableColumn<ExperienceRecord, String>("What Was Done");
		what.setCellValueFactory(data ->
				new ReadOnlyStringWrapper(data.getValue().getWhatWasDone()));
		what.setPrefWidth(190);
		TableColumn<ExperienceRecord, String> how =
				new TableColumn<ExperienceRecord, String>("How It Was Done");
		how.setCellValueFactory(data ->
				new ReadOnlyStringWrapper(data.getValue().getHowItWasDone()));
		how.setPrefWidth(190);
		TableColumn<ExperienceRecord, String> elapsed =
				new TableColumn<ExperienceRecord, String>("Time (minutes)");
		elapsed.setCellValueFactory(data -> new ReadOnlyStringWrapper(
				format(data.getValue().getElapsedMinutes())));
		elapsed.setPrefWidth(110);
		TableColumn<ExperienceRecord, String> efforts =
				new TableColumn<ExperienceRecord, String>("Effort (person-minutes)");
		efforts.setCellValueFactory(data -> new ReadOnlyStringWrapper(
				formatEfforts(data.getValue())));
		efforts.setPrefWidth(215);
		tableExperiences.getColumns().add(id);
		tableExperiences.getColumns().add(what);
		tableExperiences.getColumns().add(how);
		tableExperiences.getColumns().add(elapsed);
		tableExperiences.getColumns().add(efforts);
		tableExperiences.setLayoutX(20);
		tableExperiences.setLayoutY(260);
		tableExperiences.setPrefSize(WIDTH - 40, 220);
		tableExperiences.setPlaceholder(new Label("No experience records found."));
		tableExperiences.getSelectionModel().selectedItemProperty().addListener(
				(_, _, _) -> updateActionButtons());

		setupButton(buttonAdd, 190, 20, 492);
		setupButton(buttonEdit, 190, 220, 492);
		setupButton(buttonDelete, 190, 420, 492);
		setupButton(buttonBack, 170, 610, 540);
		guiTools.ASUTheme.styleDanger(buttonDelete);
		buttonEdit.setTooltip(new Tooltip(
				"Edit the unlocked fields of the selected experience record."));
		buttonDelete.setTooltip(new Tooltip(
				"Locked experience records or fields cannot be deleted."));
		buttonAdd.setOnAction((_) -> ControllerContributorLessonDetails.performAdd());
		buttonEdit.setOnAction((_) -> ControllerContributorLessonDetails.performEdit());
		buttonDelete.setOnAction((_) -> ControllerContributorLessonDetails.performDelete());
		buttonBack.setOnAction((_) -> ControllerContributorLessonDetails.performBack());

		root.getChildren().addAll(labelPageTitle, labelIdentity, labelStatus,
				labelTitle, labelProblem, labelLesson, textTitle, textProblem, textLesson,
				labelTitleLock, labelProblemLock,
				labelLessonLock, tableExperiences, buttonAdd, buttonEdit, buttonDelete,
				buttonBack);
		updateActionButtons();
	}

	/** @param lesson specifies the current contributor-visible lesson and lock state */
	protected static void setLesson(LessonsLearned lesson) {
		currentLesson = lesson;
		labelIdentity.setText("Lesson ID: " + lesson.getId() + "   Owner: " + lesson.getOwner());
		textTitle.setText(lesson.getTitle());
		textProblem.setText(lesson.getProblemSituation());
		textLesson.setText(lesson.getLessonLearned());
		labelTitleLock.setText(lockText(lesson.isLessonLocked() || lesson.isTitleLocked()));
		labelProblemLock.setText(lockText(
				lesson.isLessonLocked() || lesson.isProblemSituationLocked()));
		labelLessonLock.setText(lockText(
				lesson.isLessonLocked() || lesson.isLessonLearnedLocked()));
		updateActionButtons();
	}

	/**
	 * @param experiences specifies the current records for the selected lesson
	 * @param message describes the list result
	 */
	protected static void setExperiences(List<ExperienceRecord> experiences, String message) {
		tableExperiences.setItems(FXCollections.observableArrayList(experiences));
		labelStatus.setText(message);
		updateActionButtons();
	}

	/** @return the experience currently selected in the table, or null when none is selected */
	protected static ExperienceRecord getSelectedExperience() {
		return tableExperiences.getSelectionModel().getSelectedItem();
	}

	/** @param message explains the detail operation that failed */
	protected static void showError(String message) {
		labelStatus.setText(message);
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Experience Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	/** @param message describes the completed detail operation */
	protected static void showInformation(String message) {
		labelStatus.setText(message);
	}

	/**********
	 * <p> Method: updateActionButtons() </p>
	 *
	 * <p> Description: Keep the experience actions consistent with the currently displayed
	 * lesson, selected record, and their lock states.</p>
	 */
	private static void updateActionButtons() {
		boolean lessonUnavailable = currentLesson == null || currentLesson.isLessonLocked();
		ExperienceRecord selected = getSelectedExperience();
		buttonAdd.setDisable(lessonUnavailable);
		buttonEdit.setDisable(lessonUnavailable || selected == null ||
				selected.isExperienceLocked());
		buttonDelete.setDisable(lessonUnavailable || selected == null ||
				selected.hasAnyLockedField());
	}

	private static TextArea displayArea(int rows) {
		TextArea area = new TextArea();
		area.setEditable(false);
		area.setWrapText(true);
		area.setPrefRowCount(rows);
		return area;
	}

	private static String formatEfforts(ExperienceRecord record) {
		return record.getTeamEfforts().stream().map(TeamMemberEffort::toString)
				.collect(Collectors.joining("; "));
	}

	private static String format(double value) {
		return value == Math.rint(value) ? Long.toString((long)value) : Double.toString(value);
	}

	private static String lockText(boolean locked) {
		return locked ? "Locked" : "Editable";
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
		button.setFont(Font.font("Dialog", 14));
		button.setMinWidth(buttonWidth);
		button.setLayoutX(x);
		button.setLayoutY(y);
	}
}
