package lessonManagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*******
 * <p> Title: ExperienceDraft Class. </p>
 *
 * <p> Description: Holds raw experience form values until the complete record has passed
 * validation.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ExperienceDraft {

	private final String whatWasDone;
	private final String howItWasDone;
	private final String elapsedMinutes;
	private final List<EffortInput> teamEfforts;

	/**
	 * @param whatWasDone specifies the raw description of the attempt
	 * @param howItWasDone specifies the raw method description
	 * @param elapsedMinutes specifies raw elapsed minutes
	 * @param teamEfforts specifies raw team-member effort entries
	 */
	public ExperienceDraft(String whatWasDone, String howItWasDone, String elapsedMinutes,
			List<EffortInput> teamEfforts) {
		this.whatWasDone = whatWasDone;
		this.howItWasDone = howItWasDone;
		this.elapsedMinutes = elapsedMinutes;
		this.teamEfforts = teamEfforts == null ? null :
				Collections.unmodifiableList(new ArrayList<EffortInput>(teamEfforts));
	}

	/** @return the raw description of what was done */
	public String getWhatWasDone() { return whatWasDone; }

	/** @return the raw description of how it was done */
	public String getHowItWasDone() { return howItWasDone; }

	/** @return the raw elapsed-minute input */
	public String getElapsedMinutes() { return elapsedMinutes; }

	/** @return raw team effort entries, or null when none were supplied */
	public List<EffortInput> getTeamEfforts() { return teamEfforts; }
}
