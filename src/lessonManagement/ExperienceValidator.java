package lessonManagement;

import java.util.ArrayList;
import java.util.List;

import entityClasses.TeamMemberEffort;

/*******
 * <p> Title: ExperienceValidator Class. </p>
 *
 * <p> Description: Validates all required experience values and converts valid time and effort
 * text to finite nonnegative numbers with consistent units.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class ExperienceValidator {

	private ExperienceValidator() {
	}

	/**********
	 * @param draft specifies the raw experience values
	 * @return a successful normalized draft or a helpful validation error
	 */
	public static OperationResult<ValidatedExperience> validate(ExperienceDraft draft) {
		if (draft == null || blank(draft.getWhatWasDone()))
			return OperationResult.failure("Describe what was done.");
		if (blank(draft.getHowItWasDone()))
			return OperationResult.failure("Describe how it was done.");

		Double elapsed = parseNonnegative(draft.getElapsedMinutes());
		if (elapsed == null)
			return OperationResult.failure(
					"Time spent must be a finite number zero or greater.");

		if (draft.getTeamEfforts() == null || draft.getTeamEfforts().isEmpty())
			return OperationResult.failure("Enter a team member name.");

		List<TeamMemberEffort> efforts = new ArrayList<TeamMemberEffort>();
		for (EffortInput effort : draft.getTeamEfforts()) {
			if (effort == null || blank(effort.getMemberName()))
				return OperationResult.failure("Enter a team member name.");
			Double value = parseNonnegative(effort.getEffortMinutes());
			if (value == null)
				return OperationResult.failure(
						"Effort must be a finite number zero or greater.");
			efforts.add(new TeamMemberEffort(effort.getMemberName().trim(), value));
		}

		ValidatedExperience value = new ValidatedExperience(
				draft.getWhatWasDone().trim(), draft.getHowItWasDone().trim(), elapsed, efforts);
		return OperationResult.success(value, "Experience information is valid.");
	}

	private static Double parseNonnegative(String input) {
		if (blank(input)) return null;
		try {
			double value = Double.parseDouble(input.trim());
			if (!Double.isFinite(value) || value < 0) return null;
			return value;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	/*******
	 * <p> Title: ValidatedExperience Class. </p>
	 *
	 * <p> Description: Contains normalized values that are safe to pass to the database.</p>
	 */
	public static class ValidatedExperience {
		private final String whatWasDone;
		private final String howItWasDone;
		private final double elapsedMinutes;
		private final List<TeamMemberEffort> teamEfforts;

		private ValidatedExperience(String whatWasDone, String howItWasDone,
				double elapsedMinutes, List<TeamMemberEffort> teamEfforts) {
			this.whatWasDone = whatWasDone;
			this.howItWasDone = howItWasDone;
			this.elapsedMinutes = elapsedMinutes;
			this.teamEfforts = teamEfforts;
		}

		/** @return normalized description of what was done */
		public String getWhatWasDone() { return whatWasDone; }

		/** @return normalized description of how it was done */
		public String getHowItWasDone() { return howItWasDone; }

		/** @return validated elapsed time in minutes */
		public double getElapsedMinutes() { return elapsedMinutes; }

		/** @return validated effort entries in person-minutes */
		public List<TeamMemberEffort> getTeamEfforts() { return teamEfforts; }
	}
}
