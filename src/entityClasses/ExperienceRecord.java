package entityClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*******
 * <p> Title: ExperienceRecord Class. </p>
 *
 * <p> Description: Represents one later attempt to apply a lesson.  It records what was done,
 * how it was done, elapsed time in minutes, and each team member's effort in person-minutes.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ExperienceRecord {

	private final long id;
	private final long lessonId;
	private final String owner;
	private final String whatWasDone;
	private final String howItWasDone;
	private final double elapsedMinutes;
	private final List<TeamMemberEffort> teamEfforts;
	private final boolean experienceLocked;
	private final boolean whatWasDoneLocked;
	private final boolean howItWasDoneLocked;
	private final boolean elapsedTimeLocked;
	private final boolean teamEffortLocked;

	/**********
	 * @param id specifies the database identifier
	 * @param lessonId specifies the parent lesson identifier
	 * @param owner specifies the contributor who owns this record
	 * @param whatWasDone specifies what the team attempted
	 * @param howItWasDone specifies how the work was performed
	 * @param elapsedMinutes specifies elapsed time in minutes
	 * @param teamEfforts specifies team effort in person-minutes
	 * @param experienceLocked specifies whether the complete record is locked
	 * @param whatWasDoneLocked specifies whether the what field is locked
	 * @param howItWasDoneLocked specifies whether the how field is locked
	 * @param elapsedTimeLocked specifies whether elapsed time is locked
	 * @param teamEffortLocked specifies whether the team effort list is locked
	 */
	public ExperienceRecord(long id, long lessonId, String owner, String whatWasDone,
			String howItWasDone, double elapsedMinutes, List<TeamMemberEffort> teamEfforts,
			boolean experienceLocked, boolean whatWasDoneLocked,
			boolean howItWasDoneLocked, boolean elapsedTimeLocked, boolean teamEffortLocked) {
		this.id = id;
		this.lessonId = lessonId;
		this.owner = owner;
		this.whatWasDone = whatWasDone;
		this.howItWasDone = howItWasDone;
		this.elapsedMinutes = elapsedMinutes;
		this.teamEfforts = Collections.unmodifiableList(new ArrayList<TeamMemberEffort>(teamEfforts));
		this.experienceLocked = experienceLocked;
		this.whatWasDoneLocked = whatWasDoneLocked;
		this.howItWasDoneLocked = howItWasDoneLocked;
		this.elapsedTimeLocked = elapsedTimeLocked;
		this.teamEffortLocked = teamEffortLocked;
	}

	/** @return the unique experience identifier */
	public long getId() { return id; }

	/** @return the parent lesson identifier */
	public long getLessonId() { return lessonId; }

	/** @return the contributor who owns this experience record */
	public String getOwner() { return owner; }

	/** @return the description of what was done */
	public String getWhatWasDone() { return whatWasDone; }

	/** @return the description of how it was done */
	public String getHowItWasDone() { return howItWasDone; }

	/** @return elapsed time in minutes */
	public double getElapsedMinutes() { return elapsedMinutes; }

	/** @return an unmodifiable list of team-member effort values */
	public List<TeamMemberEffort> getTeamEfforts() { return teamEfforts; }

	/** @return true when the complete experience is locked */
	public boolean isExperienceLocked() { return experienceLocked; }

	/** @return true when the what field is locked */
	public boolean isWhatWasDoneLocked() { return whatWasDoneLocked; }

	/** @return true when the how field is locked */
	public boolean isHowItWasDoneLocked() { return howItWasDoneLocked; }

	/** @return true when elapsed time is locked */
	public boolean isElapsedTimeLocked() { return elapsedTimeLocked; }

	/** @return true when the effort list is locked */
	public boolean isTeamEffortLocked() { return teamEffortLocked; }

	/** @return true when the record or one of its fields is locked */
	public boolean hasAnyLockedField() {
		return experienceLocked || whatWasDoneLocked || howItWasDoneLocked ||
				elapsedTimeLocked || teamEffortLocked;
	}
}
