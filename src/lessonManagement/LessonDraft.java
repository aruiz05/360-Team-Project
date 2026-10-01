package lessonManagement;

/*******
 * <p> Title: LessonDraft Class. </p>
 *
 * <p> Description: Holds raw lesson form values until validation succeeds.  Keeping raw values
 * separate from saved entities prevents rejected edits from changing stored data.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class LessonDraft {

	private final String title;
	private final String problemSituation;
	private final String lessonLearned;

	/**
	 * @param title specifies the proposed title
	 * @param problemSituation specifies the proposed problem or situation
	 * @param lessonLearned specifies the proposed lesson text
	 */
	public LessonDraft(String title, String problemSituation, String lessonLearned) {
		this.title = title;
		this.problemSituation = problemSituation;
		this.lessonLearned = lessonLearned;
	}

	/** @return the raw proposed title */
	public String getTitle() { return title; }

	/** @return the raw proposed problem or situation */
	public String getProblemSituation() { return problemSituation; }

	/** @return the raw proposed lesson text */
	public String getLessonLearned() { return lessonLearned; }
}
