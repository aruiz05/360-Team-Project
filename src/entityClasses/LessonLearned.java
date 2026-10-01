package entityClasses;

/*******
 * <p> Title: LessonLearned Class. </p>
 *
 * <p> Description: Represents the contributor-visible core information for one lesson learned.
 * The database owns the unique identifier and stores lock information with the content so a
 * contributor sees the same restrictions after the application is restarted.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class LessonLearned {

	private final long id;
	private final String owner;
	private final String title;
	private final String problemSituation;
	private final String lessonLearned;
	private final boolean lessonLocked;
	private final boolean titleLocked;
	private final boolean problemSituationLocked;
	private final boolean lessonLearnedLocked;

	/**********
	 * <p> Method: LessonLearned(...) </p>
	 *
	 * <p> Description: Construct a complete lesson read from the database.  A zero identifier is
	 * used only before a new lesson has been inserted.</p>
	 *
	 * @param id specifies the database identifier
	 * @param owner specifies the contributor who owns the lesson
	 * @param title specifies the short lesson title
	 * @param problemSituation specifies the problem or situation
	 * @param lessonLearned specifies what the contributor learned
	 * @param lessonLocked specifies whether the complete lesson is locked
	 * @param titleLocked specifies whether the title is locked
	 * @param problemSituationLocked specifies whether the problem field is locked
	 * @param lessonLearnedLocked specifies whether the lesson text is locked
	 */
	public LessonLearned(long id, String owner, String title, String problemSituation,
			String lessonLearned, boolean lessonLocked, boolean titleLocked,
			boolean problemSituationLocked, boolean lessonLearnedLocked) {
		this.id = id;
		this.owner = owner;
		this.title = title;
		this.problemSituation = problemSituation;
		this.lessonLearned = lessonLearned;
		this.lessonLocked = lessonLocked;
		this.titleLocked = titleLocked;
		this.problemSituationLocked = problemSituationLocked;
		this.lessonLearnedLocked = lessonLearnedLocked;
	}

	/** @return the unique lesson identifier */
	public long getId() { return id; }

	/** @return the username of the contributor who owns the lesson */
	public String getOwner() { return owner; }

	/** @return the contributor-visible lesson title */
	public String getTitle() { return title; }

	/** @return the contributor-visible problem or situation */
	public String getProblemSituation() { return problemSituation; }

	/** @return the contributor-visible lesson text */
	public String getLessonLearned() { return lessonLearned; }

	/** @return true when the complete lesson is locked */
	public boolean isLessonLocked() { return lessonLocked; }

	/** @return true when the title cannot be changed */
	public boolean isTitleLocked() { return titleLocked; }

	/** @return true when the problem or situation cannot be changed */
	public boolean isProblemSituationLocked() { return problemSituationLocked; }

	/** @return true when the lesson text cannot be changed */
	public boolean isLessonLearnedLocked() { return lessonLearnedLocked; }

	/**********
	 * <p> Method: hasAnyLockedField() </p>
	 *
	 * <p> Description: Determine whether deletion must be rejected because the lesson or one of
	 * its core fields is locked.</p>
	 *
	 * @return true when any applicable lock is active
	 */
	public boolean hasAnyLockedField() {
		return lessonLocked || titleLocked || problemSituationLocked || lessonLearnedLocked;
	}

	@Override
	public String toString() {
		return id + " - " + title;
	}
}
