package guiLessons;

import java.util.Optional;

import entityClasses.LessonLearned;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lessonManagement.LessonDraft;
import lessonManagement.OperationResult;

/*******
 * <p> Title: ControllerLessons Class. </p>
 *
 * <p> Description: Handles contributor actions on the lesson collection page.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ControllerLessons {

	private ControllerLessons() {
	}

	/** Refresh the full contributor list or the currently requested title subset. */
	protected static void performRefresh() {
		OperationResult<entityClasses.LessonsCollection> result = ModelLessons.list(
				ViewLessons.theUser.getUserName(), ViewLessons.fieldFilter.getText());
		if (result.isSuccessful()) ViewLessons.setLessons(result.getValue(), result.getMessage());
		else ViewLessons.showError(result.getMessage());
	}

	/** Open the lesson editor and save a valid new contributor-owned lesson. */
	protected static void performCreate() {
		Optional<LessonDraft> draft = LessonEditorDialog.show(null);
		if (draft.isEmpty()) return;
		OperationResult<LessonLearned> result = ModelLessons.create(
				ViewLessons.theUser.getUserName(), draft.get());
		if (!result.isSuccessful()) {
			ViewLessons.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewLessons.showInformation(result.getMessage());
	}

	/** Open the selected contributor-owned lesson and its experience records. */
	protected static void performView() {
		LessonLearned selected = selected();
		if (selected == null) return;
		OperationResult<LessonLearned> result = ModelLessons.read(
				ViewLessons.theUser.getUserName(), selected.getId());
		if (!result.isSuccessful()) {
			ViewLessons.showError(result.getMessage());
			performRefresh();
			return;
		}
		ViewLessonDetails.displayLessonDetails(ViewLessons.theStage, ViewLessons.theUser,
				result.getValue());
	}

	/** Open the editor for the selected lesson and submit its unlocked values. */
	protected static void performEdit() {
		LessonLearned selected = selected();
		if (selected == null) return;
		OperationResult<LessonLearned> current = ModelLessons.read(
				ViewLessons.theUser.getUserName(), selected.getId());
		if (!current.isSuccessful()) {
			ViewLessons.showError(current.getMessage());
			performRefresh();
			return;
		}
		Optional<LessonDraft> draft = LessonEditorDialog.show(current.getValue());
		if (draft.isEmpty()) return;
		OperationResult<LessonLearned> result = ModelLessons.update(
				ViewLessons.theUser.getUserName(), selected.getId(), draft.get());
		if (!result.isSuccessful()) {
			ViewLessons.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewLessons.showInformation(result.getMessage());
	}

	/** Confirm and request deletion of the selected unlocked lesson. */
	protected static void performDelete() {
		LessonLearned selected = selected();
		if (selected == null) return;
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
				"Delete lesson " + selected.getId() + " and all of its experience records?",
				ButtonType.YES, ButtonType.NO);
		confirmation.setTitle("Confirm Lesson Deletion");
		confirmation.setHeaderText("Are you sure?");
		if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

		OperationResult<Boolean> result = ModelLessons.delete(
				ViewLessons.theUser.getUserName(), selected.getId());
		if (!result.isSuccessful()) {
			ViewLessons.showError(result.getMessage());
			return;
		}
		performRefresh();
		ViewLessons.showInformation(result.getMessage());
	}

	/** Return to the contributor home page. */
	protected static void performReturn() {
		guiRole1.ViewRole1Home.displayRole1Home(ViewLessons.theStage, ViewLessons.theUser);
	}

	/** Log out and return to the existing Foundations login page. */
	protected static void performLogout() {
		guiUserLogin.ViewUserLogin.displayUserLogin(ViewLessons.theStage);
	}

	/** Close the shared database connection and end the application. */
	protected static void performQuit() {
		applicationMain.FoundationsMain.database.closeConnection();
		System.exit(0);
	}

	private static LessonLearned selected() {
		LessonLearned lesson = ViewLessons.getSelectedLesson();
		if (lesson == null) ViewLessons.showError("Select a lesson.");
		return lesson;
	}
}
