package guiContributorLessons;

import java.util.Optional;

import entityClasses.LessonsLearned;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lessonManagement.LessonDraft;
import lessonManagement.OperationResult;

/*******
 * <p> Title: ControllerContributorLessons Class. </p>
 *
 * <p> Description: The Controller for the Contributor Lessons window. It translates JavaFX
 * actions into Model requests, displays the Model's success or correction message, and changes
 * windows only after the requested operation succeeds. Validation and persistence are kept out
 * of this class so the same rules remain testable without JavaFX.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Separated the Contributor list into its own MVC package
 */
public class ControllerContributorLessons {

	private ControllerContributorLessons() {
	}

	/** Refresh the full contributor list or the currently requested title subset. */
	protected static void performRefresh() {
		OperationResult<entityClasses.LessonsLearnedList> result = ModelContributorLessons.list(
				ViewContributorLessons.theUser.getUserName(),
				ViewContributorLessons.fieldFilter.getText());
		if (result.isSuccessful())
			ViewContributorLessons.setLessons(result.getValue(), result.getMessage());
		else ViewContributorLessons.showError(result.getMessage());
	}

	/** Open the lesson editor and save a valid new contributor-owned lesson. */
	protected static void performCreate() {
		Optional<LessonDraft> draft = LessonEditorDialog.show(null);
		if (draft.isEmpty()) return;
		OperationResult<LessonsLearned> result = ModelContributorLessons.create(
				ViewContributorLessons.theUser.getUserName(), draft.get());
		if (!result.isSuccessful()) {
			ViewContributorLessons.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewContributorLessons.showInformation(result.getMessage());
	}

	/** Open the selected contributor-owned lesson and its experience records. */
	protected static void performView() {
		LessonsLearned selected = selected();
		if (selected == null) return;
		OperationResult<LessonsLearned> result = ModelContributorLessons.read(
				ViewContributorLessons.theUser.getUserName(), selected.getId());
		if (!result.isSuccessful()) {
			ViewContributorLessons.showError(result.getMessage());
			performRefresh();
			return;
		}
		guiContributorLessonDetails.ViewContributorLessonDetails.displayLessonDetails(
				ViewContributorLessons.theStage, ViewContributorLessons.theUser, result.getValue());
	}

	/** Open the editor for the selected lesson and submit its unlocked values. */
	protected static void performEdit() {
		LessonsLearned selected = selected();
		if (selected == null) return;
		OperationResult<LessonsLearned> current = ModelContributorLessons.read(
				ViewContributorLessons.theUser.getUserName(), selected.getId());
		if (!current.isSuccessful()) {
			ViewContributorLessons.showError(current.getMessage());
			performRefresh();
			return;
		}
		Optional<LessonDraft> draft = LessonEditorDialog.show(current.getValue());
		if (draft.isEmpty()) return;
		OperationResult<LessonsLearned> result = ModelContributorLessons.update(
				ViewContributorLessons.theUser.getUserName(), selected.getId(), draft.get());
		if (!result.isSuccessful()) {
			ViewContributorLessons.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewContributorLessons.showInformation(result.getMessage());
	}

	/** Confirm and request deletion of the selected unlocked lesson. */
	protected static void performDelete() {
		LessonsLearned selected = selected();
		if (selected == null) return;
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete lesson " + selected.getId() + " and all of its experience records?",
				ButtonType.YES, ButtonType.NO);
		confirmation.setTitle("Confirm Lesson Deletion");
		confirmation.setHeaderText("Are you sure?");
		if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

		OperationResult<Boolean> result = ModelContributorLessons.delete(
				ViewContributorLessons.theUser.getUserName(), selected.getId());
		if (!result.isSuccessful()) {
			ViewContributorLessons.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewContributorLessons.showInformation(result.getMessage());
	}

	/** Return to the contributor home page. */
	protected static void performReturn() {
		guiRole1.ViewRole1Home.displayRole1Home(
				ViewContributorLessons.theStage, ViewContributorLessons.theUser);
	}

	/** Log out and return to the existing Foundations login page. */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewContributorLessons.theStage);
	}

	/** Close the shared database connection and end the application. */
	protected static void performQuit() {
		applicationMain.FoundationsMain.database.closeConnection();
		System.exit(0);
	}

	private static LessonsLearned selected() {
		LessonsLearned lesson = ViewContributorLessons.getSelectedLesson();
		if (lesson == null) ViewContributorLessons.showError("Select a lesson.");
		return lesson;
	}
}
