package lessonManagement;

/*******
 * <p> Title: ExperienceField Enumeration. </p>
 *
 * <p> Description: Identifies each contributor-editable field in an experience record that can
 * be protected by a field-level lock. Keeping the supported values in one enumeration prevents
 * controllers and later staff tools from using inconsistent field names.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-11-10 Initial HW3 risk-reduction prototype
 */
public enum ExperienceField {

	/** Description of the work that was attempted. */
	WHAT_WAS_DONE("what was done"),

	/** Description of the method used to perform the work. */
	HOW_IT_WAS_DONE("how it was done"),

	/** Elapsed time measured in minutes. */
	ELAPSED_TIME("elapsed time"),

	/** Ordered team-member effort values measured in person-minutes. */
	TEAM_MEMBER_EFFORT("team member effort");

	/** Short user-facing name used in approval and rejection messages. */
	private final String displayName;

	private ExperienceField(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Return the readable name used by user-facing lock messages.
	 *
	 * @return the user-facing field name
	 */
	public String getDisplayName() {
		return displayName;
	}
}
