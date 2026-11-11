package lessonManagement;

/*******
 * <p> Title: LockDecision Class. </p>
 *
 * <p> Description: Returns the result of an experience-lock policy check. The allowed value is
 * suitable for controller logic, while the required message explains the result to the user.
 * Returning both values prevents JavaFX controllers from recreating or weakening lock rules.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-11-10 Initial HW3 risk-reduction prototype
 */
public final class LockDecision {

	/** True when the requested contributor operation may continue. */
	private final boolean allowed;

	/** Safe user-facing explanation of the approval or rejection. */
	private final String message;

	private LockDecision(boolean allowed, String message) {
		if (message == null || message.isBlank())
			throw new IllegalArgumentException("A lock decision message is required.");
		this.allowed = allowed;
		this.message = message;
	}

	/**
	 * Create a decision that permits the requested operation.
	 *
	 * @param message explains why the request may continue
	 * @return an approved decision
	 */
	public static LockDecision allow(String message) {
		return new LockDecision(true, message);
	}

	/**
	 * Create a decision that rejects the requested operation.
	 *
	 * @param message explains what is protected or how the user can proceed
	 * @return a rejected decision
	 */
	public static LockDecision deny(String message) {
		return new LockDecision(false, message);
	}

	/**
	 * Determine whether the caller may continue the requested operation.
	 *
	 * @return true when the requested operation may continue
	 */
	public boolean isAllowed() {
		return allowed;
	}

	/**
	 * Return the explanation that can be displayed by the JavaFX caller.
	 *
	 * @return the safe user-facing explanation
	 */
	public String getMessage() {
		return message;
	}
}
