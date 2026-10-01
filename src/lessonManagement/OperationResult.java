package lessonManagement;

/*******
 * <p> Title: OperationResult Class. </p>
 *
 * <p> Description: Carries either a successful value or a helpful user-facing error message.
 * Returning one result object keeps validation and database failures consistent between the
 * JavaFX controllers and automated tests.</p>
 *
 * @param <T> specifies the type returned by a successful operation
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 */
public class OperationResult<T> {

	private final boolean successful;
	private final String message;
	private final T value;

	private OperationResult(boolean successful, String message, T value) {
		this.successful = successful;
		this.message = message;
		this.value = value;
	}

	/**
	 * @param <T> specifies the successful value type
	 * @param value specifies the returned value
	 * @param message specifies the success message
	 * @return a successful result
	 */
	public static <T> OperationResult<T> success(T value, String message) {
		return new OperationResult<T>(true, message, value);
	}

	/**
	 * @param <T> specifies the expected value type
	 * @param message explains what failed and how to correct it
	 * @return a failed result with no value
	 */
	public static <T> OperationResult<T> failure(String message) {
		return new OperationResult<T>(false, message, null);
	}

	/** @return true when the operation succeeded */
	public boolean isSuccessful() { return successful; }

	/** @return the user-facing result message */
	public String getMessage() { return message; }

	/** @return the successful value, or null when the operation failed */
	public T getValue() { return value; }
}
