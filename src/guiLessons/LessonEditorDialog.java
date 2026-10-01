package guiLessons;

import java.util.Optional;

import entityClasses.LessonLearned;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lessonManagement.LessonDraft;

/*******
 * <p> Title: LessonEditorDialog Class. </p>
 *
 * <p> Description: Collects core lesson information and visibly disables fields whose persisted
 * lock state does not allow contributor edits.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class LessonEditorDialog {

	private LessonEditorDialog() {
	}

	/**********
	 * @param existing specifies the lesson being edited, or null when creating
	 * @return the proposed values when Save is selected
	 */
	public static Optional<LessonDraft> show(LessonLearned existing) {
		Dialog<LessonDraft> dialog = new Dialog<LessonDraft>();
		dialog.setTitle(existing == null ? "Create Lesson" : "Edit Lesson");
		dialog.setHeaderText(existing == null ? "Enter the lesson's required information." :
				"Locked fields are shown but cannot be changed.");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

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
		dialog.getDialogPane().setContent(grid);

		dialog.setResultConverter(button -> button == ButtonType.OK ?
				new LessonDraft(title.getText(), problem.getText(), lesson.getText()) : null);
		return dialog.showAndWait();
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
