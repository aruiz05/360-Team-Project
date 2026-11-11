package lessonManagement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/*******
 * <p> Title: ExperienceLockState Class. </p>
 *
 * <p> Description: Provides an immutable snapshot of the complete-record and field-level locks
 * that protect one experience record. The snapshot separates permission evaluation from the
 * persistent entity so the policy can inspect locks without changing saved experience data.</p>
 *
 * <p>The complete-record flag comes from {@code ExperienceRecord.experienceLocked}. The field
 * set represents the four persistent field locks already stored by TP2. Later staff workflows can
 * create this value after reloading the latest database record, then pass it to
 * {@link ExperienceLockPolicy}.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-11-10 Initial HW3 risk-reduction prototype
 */
public final class ExperienceLockState {

	/** True when the complete experience record is protected from edits and deletion. */
	private final boolean recordLocked;

	/** Independent field locks copied into an EnumSet owned only by this object. */
	private final EnumSet<ExperienceField> lockedFields;

	/**********
	 * <p> Method: ExperienceLockState(boolean recordLocked,
	 * Set&lt;ExperienceField&gt; lockedFields) </p>
	 *
	 * <p> Description: Construct an immutable lock snapshot. A defensive copy prevents a caller
	 * from changing the decision after the snapshot is created.</p>
	 *
	 * @param recordLocked specifies whether the complete experience is locked
	 * @param lockedFields specifies the individually locked experience fields
	 * @throws IllegalArgumentException when the field set is null or contains null
	 */
	public ExperienceLockState(boolean recordLocked, Set<ExperienceField> lockedFields) {
		if (lockedFields == null)
			throw new IllegalArgumentException("Locked fields are required.");
		for (ExperienceField field : lockedFields) {
			if (field == null)
				throw new IllegalArgumentException("Locked fields cannot contain null.");
		}
		this.recordLocked = recordLocked;
		this.lockedFields = lockedFields.isEmpty()
				? EnumSet.noneOf(ExperienceField.class)
				: EnumSet.copyOf(lockedFields);
	}

	/**********
	 * <p> Method: fromFlags(...) </p>
	 *
	 * <p> Description: Translate the lock flags already stored by {@code ExperienceRecord} into
	 * the policy snapshot. This mapping is centralized so every caller treats each persistent flag
	 * consistently.</p>
	 *
	 * @param recordLocked specifies the complete-record lock
	 * @param whatWasDoneLocked specifies the what-was-done field lock
	 * @param howItWasDoneLocked specifies the how-it-was-done field lock
	 * @param elapsedTimeLocked specifies the elapsed-time field lock
	 * @param teamEffortLocked specifies the team-effort field lock
	 * @return an immutable snapshot containing the supplied lock values
	 */
	public static ExperienceLockState fromFlags(boolean recordLocked,
			boolean whatWasDoneLocked, boolean howItWasDoneLocked,
			boolean elapsedTimeLocked, boolean teamEffortLocked) {
		EnumSet<ExperienceField> fields = EnumSet.noneOf(ExperienceField.class);
		if (whatWasDoneLocked) fields.add(ExperienceField.WHAT_WAS_DONE);
		if (howItWasDoneLocked) fields.add(ExperienceField.HOW_IT_WAS_DONE);
		if (elapsedTimeLocked) fields.add(ExperienceField.ELAPSED_TIME);
		if (teamEffortLocked) fields.add(ExperienceField.TEAM_MEMBER_EFFORT);
		return new ExperienceLockState(recordLocked, fields);
	}

	/**
	 * Determine whether the complete record is protected.
	 *
	 * @return true when the complete experience record is locked
	 */
	public boolean isRecordLocked() {
		return recordLocked;
	}

	/**
	 * Determine whether one supported field is protected.
	 *
	 * @param field specifies the field to inspect
	 * @return true only when the supplied field has its own lock
	 */
	public boolean isFieldLocked(ExperienceField field) {
		return field != null && lockedFields.contains(field);
	}

	/**
	 * Determine whether deletion must preserve at least one protected field.
	 *
	 * @return true when one or more individual fields are locked
	 */
	public boolean hasAnyFieldLock() {
		return !lockedFields.isEmpty();
	}

	/**
	 * Return the individual locks without exposing the internal mutable EnumSet.
	 *
	 * @return an unmodifiable defensive copy of the individually locked fields
	 */
	public Set<ExperienceField> getLockedFields() {
		return Collections.unmodifiableSet(EnumSet.copyOf(lockedFields));
	}
}
