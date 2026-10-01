package entityClasses;

/*******
 * <p> Title: TeamMemberEffort Class. </p>
 *
 * <p> Description: Stores one team member's effort for an experience record.  Effort is stored
 * in person-minutes so the unit is clear and consistent.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class TeamMemberEffort {

	private final String memberName;
	private final double effortMinutes;

	/**********
	 * @param memberName specifies the team member
	 * @param effortMinutes specifies that member's effort in person-minutes
	 */
	public TeamMemberEffort(String memberName, double effortMinutes) {
		this.memberName = memberName;
		this.effortMinutes = effortMinutes;
	}

	/** @return the team member's name */
	public String getMemberName() { return memberName; }

	/** @return effort in person-minutes */
	public double getEffortMinutes() { return effortMinutes; }

	@Override
	public String toString() {
		return memberName + ": " + format(effortMinutes) + " person-minutes";
	}

	private static String format(double value) {
		if (value == Math.rint(value)) return Long.toString((long)value);
		return Double.toString(value);
	}
}
