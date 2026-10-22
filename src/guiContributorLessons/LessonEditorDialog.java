package guiContributorLessons;

import java.util.Optional;

import entityClasses.LessonsLearned;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lessonManagement.LessonDraft;
import lessonManagement.LessonValidator;

/*******
 * <p> Title: LessonEditorDialog Class. </p>
 *
 * <p> Description: Collects the three required core lesson fields for the Contributor Lessons
 * window. Persisted locks are displayed beside their fields and disable only the values the
 * contributor is not permitted to change.</p>
 *
 * <p>The dialog calls the same non-GUI LessonValidator used by the persistent model before it
 * closes. This preserves the user's entered values while a helpful correction message is shown.
 * The Model performs the validation again before any database operation, so bypassing the dialog
 * cannot save invalid information.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Added in-dialog validation and Save/Cancel actions
 */
public class LessonEditorDialog {

	private LessonEditorDialog() {
	}

	/**********
	 * @param existing specifies the lesson being edited, or null when creating
	 * @return the proposed values when Save is selected
	 */
	public static Optional<LessonDraft> show(LessonsLearned existing) {
		Dialog<LessonDraft> dialog = new Dialog<LessonDraft>();
		dialog.setTitle(existing == null ? "Create Lesson" : "Edit Lesson");
		dialog.setHeaderText(existing == null ? "Enter the lesson's required information." :
				"Locked fields are shown but cannot be changed.");
		ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

		TextField title = new TextField(existing == null ? "" : existing.getTitle());
		TextArea problem = area(existing == null ? "" : existing.getProblemSituation());
		TextArea lesson = area(existing == null ? "" : existing.getLessonLearned());
		if (existing != null) {
			title.setDisable(existing.isLessonLocked() || existing.isTitleLocked());
			problem.setDisable(existing.isLessonLocked() || existing.isProblemSituationLocked());
			lesson.setDisable(existing.isLessonLocked() || existing.isLessonLearnedLocked());
		}

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(15));
		grid.add(new Label(label("Title", existing != null &&
				(existing.isLessonLocked() || existing.isTitleLocked()))), 0, 0);
		grid.add(title, 1, 0);
		grid.add(new Label(label("Problem or situation", existing != null &&
				(existing.isLessonLocked() || existing.isProblemSituationLocked()))), 0, 1);
		grid.add(problem, 1, 1);
		grid.add(new Label(label("Lesson learned", existing != null &&
				(existing.isLessonLocked() || existing.isLessonLearnedLocked()))), 0, 2);
		grid.add(lesson, 1, 2);
		Label validationMessage = new Label();
		validationMessage.setStyle("-fx-text-fill: #8C1D40; -fx-font-weight: bold;");
		validationMessage.setWrapText(true);
		grid.add(validationMessage, 0, 3, 2, 1);
		dialog.getDialogPane().setContent(grid);

		// Keep the dialog open when a required value is missing so the contributor can correct it.
		dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
			String message = LessonValidator.validate(draft(title, problem, lesson));
			if (!message.isEmpty()) {
				validationMessage.setText(message);
				event.consume();
			}
		});
		dialog.setResultConverter(button -> button == save ? draft(title, problem, lesson) : null);
		return dialog.showAndWait();
	}

	private static LessonDraft draft(TextField title, TextArea problem, TextArea lesson) {
		return new LessonDraft(title.getText(), problem.getText(), lesson.getText());
	}

	private static TextArea area(String text) {
		TextArea area = new TextArea(text);
		area.setPrefColumnCount(42);
		area.setPrefRowCount(4);
		area.setWrapText(true);
		return area;
	}

	private static String label(String name, boolean locked) {
		return locked ? name + " (locked)" : name;
	}
}
