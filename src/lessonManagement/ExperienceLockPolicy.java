package lessonManagement;

/*******
 * <p> Title: ExperienceLockPolicy Class. </p>
 *
 * <p> Description: Makes the reusable contributor edit and deletion decisions for the HW3
 * risk-reduction prototype. Ownership and ordinary input validation remain the responsibility of
 * {@link LessonService}; this class answers only whether the latest lock state permits an
 * otherwise authorized operation.</p>
 *
 * <p>The policy intentionally contains no database or JavaFX code and never changes the supplied
 * lock state. This keeps the high-risk rule testable in isolation and prevents rejected requests
 * from partially changing experience content or lock values.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-11-10 Initial HW3 risk-reduction prototype
 */
public final class ExperienceLockPolicy {

	private ExperienceLockPolicy() {
	}

	/**********
	 * <p> Method: evaluateRecordEdit(ExperienceLockState lockState) </p>
	 *
	 * <p> Description: Determine whether an edit request may enter field-level evaluation. A
	 * complete-record lock has priority over every individual field decision.</p>
	 *
	 * @param lockState specifies the latest persistent lock snapshot
	 * @return approval when record-level editing is available, otherwise a helpful rejection
	 */
	public static LockDecision evaluateRecordEdit(ExperienceLockState lockState) {
		LockDecision stateCheck = validateState(lockState);
		if (stateCheck != null) return stateCheck;
		if (lockState.isRecordLocked())
			return LockDecision.deny(
					"This experience record is locked and cannot be edited.");
		return LockDecision.allow("This experience record may be edited.");
	}

	/**********
	 * <p> Method: evaluateFieldEdit(ExperienceLockState lockState, ExperienceField field) </p>
	 *
	 * <p> Description: Determine whether a contributor may change one selected field. A complete
	 * record lock blocks every field. Otherwise, only the selected field's lock is considered, so
	 * a lock on one field does not incorrectly block a different unlocked field.</p>
	 *
	 * @param lockState specifies the latest persistent lock snapshot
	 * @param field specifies the field the contributor wants to change
	 * @return a structured approval or rejection with a helpful message
	 */
	public static LockDecision evaluateFieldEdit(ExperienceLockState lockState,
			ExperienceField field) {
		LockDecision stateCheck = validateState(lockState);
		if (stateCheck != null) return stateCheck;
		if (field == null)
			return LockDecision.deny("Select the experience field you want to edit.");
		if (lockState.isRecordLocked())
			return LockDecision.deny(
					"This experience record is locked and cannot be edited.");
		if (lockState.isFieldLocked(field))
			return LockDecision.deny("The " + field.getDisplayName() +
					" field is locked and cannot be edited.");
		return LockDecision.allow("The " + field.getDisplayName() + " field may be edited.");
	}

	/**********
	 * <p> Method: evaluateDelete(ExperienceLockState lockState) </p>
	 *
	 * <p> Description: Determine whether a contributor may delete an experience. Deletion would
	 * remove every field, so either a complete-record lock or any individual field lock must block
	 * the operation.</p>
	 *
	 * @param lockState specifies the latest persistent lock snapshot
	 * @return approval only when the record and all supported fields are unlocked
	 */
	public static LockDecision evaluateDelete(ExperienceLockState lockState) {
		LockDecision stateCheck = validateState(lockState);
		if (stateCheck != null) return stateCheck;
		if (lockState.isRecordLocked())
			return LockDecision.deny(
					"This experience record is locked and cannot be deleted.");
		// A field lock protects its value from removal as well as from an ordinary edit.
		if (lockState.hasAnyFieldLock())
			return LockDecision.deny(
					"This experience contains locked information and cannot be deleted.");
		return LockDecision.allow("This experience record may be deleted.");
	}

	/**********
	 * The policy rejects missing lock information instead of assuming that the record is
	 * unlocked. This fail-closed behavior prevents stale or incomplete callers from bypassing a
	 * protection decision.
	 *
	 * @param lockState specifies the lock snapshot supplied by the caller
	 * @return a rejection for missing state, or null when evaluation may continue
	 */
	private static LockDecision validateState(ExperienceLockState lockState) {
		if (lockState == null)
			return LockDecision.deny(
					"Lock information is unavailable. Refresh the experience and try again.");
		return null;
	}
}
