package guiContributorLessonDetails;

import java.util.Optional;

import entityClasses.ExperienceRecord;
import entityClasses.LessonsLearned;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lessonManagement.ExperienceDraft;
import lessonManagement.OperationResult;

/*******
 * <p> Title: ControllerContributorLessonDetails Class. </p>
 *
 * <p> Description: The Controller for the Contributor Lesson Details window. It converts button
 * actions into Model requests for experience information and displays safe, action-specific
 * results. The Controller never performs SQL or decides ownership and lock permissions.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Separated lesson details into its own MVC package
 */
public class ControllerContributorLessonDetails {

	private ControllerContributorLessonDetails() {
	}

	/** Reload the selected lesson and its current experience records from the database. */
	protected static void performRefresh() {
		String owner = ViewContributorLessonDetails.theUser.getUserName();
		OperationResult<LessonsLearned> lesson = ModelContributorLessonDetails.readLesson(owner,
				ViewContributorLessonDetails.currentLesson.getId());
		if (!lesson.isSuccessful()) {
			ViewContributorLessonDetails.showError(lesson.getMessage());
			performBack();
			return;
		}
		ViewContributorLessonDetails.setLesson(lesson.getValue());
		OperationResult<java.util.List<ExperienceRecord>> experiences =
				ModelContributorLessonDetails.listExperiences(owner, lesson.getValue().getId());
		if (experiences.isSuccessful())
			ViewContributorLessonDetails.setExperiences(
					experiences.getValue(), experiences.getMessage());
		else ViewContributorLessonDetails.showError(experiences.getMessage());
	}

	/** Open an empty experience editor and save valid values under the current lesson. */
	protected static void performAdd() {
		Optional<ExperienceDraft> draft = ExperienceEditorDialog.show(null);
		if (draft.isEmpty()) return;
		OperationResult<ExperienceRecord> result = ModelContributorLessonDetails.createExperience(
				ViewContributorLessonDetails.theUser.getUserName(),
				ViewContributorLessonDetails.currentLesson.getId(), draft.get());
		if (!result.isSuccessful()) {
			ViewContributorLessonDetails.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewContributorLessonDetails.showInformation(result.getMessage());
	}

	/** Open the selected experience and submit changes to its unlocked fields. */
	protected static void performEdit() {
		ExperienceRecord selected = selected();
		if (selected == null) return;
		OperationResult<ExperienceRecord> current = ModelContributorLessonDetails.readExperience(
				ViewContributorLessonDetails.theUser.getUserName(), selected.getId());
		if (!current.isSuccessful()) {
			ViewContributorLessonDetails.showError(current.getMessage());
			performRefresh();
			return;
		}
		Optional<ExperienceDraft> draft = ExperienceEditorDialog.show(current.getValue());
		if (draft.isEmpty()) return;
		OperationResult<ExperienceRecord> result = ModelContributorLessonDetails.updateExperience(
				ViewContributorLessonDetails.theUser.getUserName(), selected.getId(), draft.get());
		if (!result.isSuccessful()) {
			ViewContributorLessonDetails.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewContributorLessonDetails.showInformation(result.getMessage());
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
		OperationResult<Boolean> result = ModelContributorLessonDetails.deleteExperience(
				ViewContributorLessonDetails.theUser.getUserName(), selected.getId());
		if (!result.isSuccessful()) {
			ViewContributorLessonDetails.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewContributorLessonDetails.showInformation(result.getMessage());
	}

	/** Return to the contributor's current lesson collection. */
	protected static void performBack() {
		guiContributorLessons.ViewContributorLessons.displayLessons(
				ViewContributorLessonDetails.theStage, ViewContributorLessonDetails.theUser);
	}

	private static ExperienceRecord selected() {
		ExperienceRecord record = ViewContributorLessonDetails.getSelectedExperience();
		if (record == null)
			ViewContributorLessonDetails.showError("Select an experience record.");
		return record;
	}
}
