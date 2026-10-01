package lessonManagement;

/*******
 * <p> Title: LessonValidator Class. </p>
 *
 * <p> Description: Validates the three required lesson fields before the database is changed.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class LessonValidator {

	private LessonValidator() {
	}

	/**********
	 * @param draft specifies the proposed lesson values
	 * @return an empty string when valid, otherwise a helpful correction message
	 */
	public static String validate(LessonDraft draft) {
		if (draft == null || blank(draft.getTitle())) return "Enter a lesson title.";
		if (blank(draft.getProblemSituation())) return "Enter the problem or situation.";
		if (blank(draft.getLessonLearned())) return "Enter the lesson learned.";
		return "";
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
