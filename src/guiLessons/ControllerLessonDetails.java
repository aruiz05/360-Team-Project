package guiLessons;

import java.util.Optional;

import entityClasses.ExperienceRecord;
import entityClasses.LessonLearned;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lessonManagement.ExperienceDraft;
import lessonManagement.OperationResult;

/*******
 * <p> Title: ControllerLessonDetails Class. </p>
 *
 * <p> Description: Handles viewing and managing experience records for the selected lesson.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ControllerLessonDetails {

	private ControllerLessonDetails() {
	}

	/** Reload the selected lesson and its current experience records from the database. */
	protected static void performRefresh() {
		String owner = ViewLessonDetails.theUser.getUserName();
		OperationResult<LessonLearned> lesson = ModelLessons.read(owner,
				ViewLessonDetails.currentLesson.getId());
		if (!lesson.isSuccessful()) {
			ViewLessonDetails.showError(lesson.getMessage());
			performBack();
			return;
		}
		ViewLessonDetails.setLesson(lesson.getValue());
		OperationResult<java.util.List<ExperienceRecord>> experiences =
				ModelLessons.listExperiences(owner, lesson.getValue().getId());
		if (experiences.isSuccessful())
			ViewLessonDetails.setExperiences(experiences.getValue(), experiences.getMessage());
		else ViewLessonDetails.showError(experiences.getMessage());
	}

	/** Open an empty experience editor and save valid values under the current lesson. */
	protected static void performAdd() {
		Optional<ExperienceDraft> draft = ExperienceEditorDialog.show(null);
		if (draft.isEmpty()) return;
		OperationResult<ExperienceRecord> result = ModelLessons.createExperience(
				ViewLessonDetails.theUser.getUserName(), ViewLessonDetails.currentLesson.getId(),
				draft.get());
		if (!result.isSuccessful()) {
			ViewLessonDetails.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewLessonDetails.showInformation(result.getMessage());
	}

	/** Open the selected experience and submit changes to its unlocked fields. */
	protected static void performEdit() {
		ExperienceRecord selected = selected();
		if (selected == null) return;
		OperationResult<ExperienceRecord> current = ModelLessons.readExperience(
				ViewLessonDetails.theUser.getUserName(), selected.getId());
		if (!current.isSuccessful()) {
			ViewLessonDetails.showError(current.getMessage());
			performRefresh();
			return;
		}
		Optional<ExperienceDraft> draft = ExperienceEditorDialog.show(current.getValue());
		if (draft.isEmpty()) return;
		OperationResult<ExperienceRecord> result = ModelLessons.updateExperience(
				ViewLessonDetails.theUser.getUserName(), selected.getId(), draft.get());
		if (!result.isSuccessful()) {
			ViewLessonDetails.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewLessonDetails.showInformation(result.getMessage());
	}

	/** Confirm and request deletion of the selected unlocked experience record. */
	protected static void performDelete() {
		ExperienceRecord selected = selected();
		if (selected == null) return;
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete experience record " + selected.getId() + "?",
				ButtonType.YES, ButtonType.NO);
		confirmation.setTitle("Confirm Experience Deletion");
		confirmation.setHeaderText("Are you sure?");
		if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
		OperationResult<Boolean> result = ModelLessons.deleteExperience(
				ViewLessonDetails.theUser.getUserName(), selected.getId());
		if (!result.isSuccessful()) {
			ViewLessonDetails.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewLessonDetails.showInformation(result.getMessage());
	}

	/** Return to the contributor's current lesson collection. */
	protected static void performBack() {
		ViewLessons.displayLessons(ViewLessonDetails.theStage, ViewLessonDetails.theUser);
	}

	private static ExperienceRecord selected() {
		ExperienceRecord record = ViewLessonDetails.getSelectedExperience();
		if (record == null) ViewLessonDetails.showError("Select an experience record.");
		return record;
	}
}
