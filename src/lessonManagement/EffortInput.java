package lessonManagement;

/*******
 * <p> Title: EffortInput Class. </p>
 *
 * <p> Description: Holds one team member name and raw effort input before validation.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class EffortInput {

	private final String memberName;
	private final String effortMinutes;

	/**
	 * @param memberName specifies the team member
	 * @param effortMinutes specifies raw person-minute input
	 */
	public EffortInput(String memberName, String effortMinutes) {
		this.memberName = memberName;
		this.effortMinutes = effortMinutes;
	}

	/** @return the raw team member name */
	public String getMemberName() { return memberName; }

	/** @return the raw effort value in person-minutes */
	public String getEffortMinutes() { return effortMinutes; }
}
