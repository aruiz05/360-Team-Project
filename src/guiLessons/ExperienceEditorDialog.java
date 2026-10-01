package guiLessons;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import entityClasses.ExperienceRecord;
import entityClasses.TeamMemberEffort;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lessonManagement.EffortInput;
import lessonManagement.ExperienceDraft;

/*******
 * <p> Title: ExperienceEditorDialog Class. </p>
 *
 * <p> Description: Collects experience data using minutes and person-minutes.  Team effort uses
 * one line per member in the readable form {@code Name = minutes}.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ExperienceEditorDialog {

	private ExperienceEditorDialog() {
	}

	/**********
	 * @param existing specifies the record being edited, or null when creating
	 * @return raw experience values when Save is selected
	 */
	public static Optional<ExperienceDraft> show(ExperienceRecord existing) {
		Dialog<ExperienceDraft> dialog = new Dialog<ExperienceDraft>();
		dialog.setTitle(existing == null ? "Add Experience" : "Edit Experience");
		dialog.setHeaderText("Use minutes for elapsed time and person-minutes for effort.");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		TextArea what = area(existing == null ? "" : existing.getWhatWasDone(), 3);
		TextArea how = area(existing == null ? "" : existing.getHowItWasDone(), 3);
		TextField elapsed = new TextField(existing == null ? "" :
				format(existing.getElapsedMinutes()));
		TextArea efforts = area(existing == null ? "" : formatEfforts(existing), 4);
		if (existing != null) {
			what.setDisable(existing.isExperienceLocked() || existing.isWhatWasDoneLocked());
			how.setDisable(existing.isExperienceLocked() || existing.isHowItWasDoneLocked());
			elapsed.setDisable(existing.isExperienceLocked() || existing.isElapsedTimeLocked());
			efforts.setDisable(existing.isExperienceLocked() || existing.isTeamEffortLocked());
		}

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(15));
		grid.add(new Label("What was done" + locked(existing, 1)), 0, 0);
		grid.add(what, 1, 0);
		grid.add(new Label("How it was done" + locked(existing, 2)), 0, 1);
		grid.add(how, 1, 1);
		grid.add(new Label("Elapsed time (minutes)" + locked(existing, 3)), 0, 2);
		grid.add(elapsed, 1, 2);
		grid.add(new Label("Team effort\nName = person-minutes" + locked(existing, 4)), 0, 3);
		grid.add(efforts, 1, 3);
		dialog.getDialogPane().setContent(grid);

		dialog.setResultConverter(button -> button == ButtonType.OK ?
				new ExperienceDraft(what.getText(), how.getText(), elapsed.getText(),
						parseEfforts(efforts.getText())) : null);
		return dialog.showAndWait();
	}

	private static TextArea area(String text, int rows) {
		TextArea area = new TextArea(text);
		area.setPrefColumnCount(42);
		area.setPrefRowCount(rows);
		area.setWrapText(true);
		return area;
	}

	private static List<EffortInput> parseEfforts(String text) {
		List<EffortInput> values = new ArrayList<EffortInput>();
		if (text == null || text.isBlank()) return values;
		for (String line : text.split("\\R")) {
			if (line.isBlank()) continue;
			String[] parts = line.split("=", 2);
			if (parts.length == 2) values.add(new EffortInput(parts[0], parts[1]));
			else values.add(new EffortInput(line, ""));
		}
		return values;
	}

	private static String formatEfforts(ExperienceRecord record) {
		StringBuilder result = new StringBuilder();
		for (TeamMemberEffort effort : record.getTeamEfforts()) {
			if (result.length() > 0) result.append(System.lineSeparator());
			result.append(effort.getMemberName()).append(" = ")
					.append(format(effort.getEffortMinutes()));
		}
		return result.toString();
	}

	private static String format(double value) {
		return value == Math.rint(value) ? Long.toString((long)value) : Double.toString(value);
	}

	private static String locked(ExperienceRecord record, int field) {
		if (record == null) return "";
		boolean value = record.isExperienceLocked() ||
				(field == 1 && record.isWhatWasDoneLocked()) ||
				(field == 2 && record.isHowItWasDoneLocked()) ||
				(field == 3 && record.isElapsedTimeLocked()) ||
				(field == 4 && record.isTeamEffortLocked());
		return value ? " (locked)" : "";
	}
}
