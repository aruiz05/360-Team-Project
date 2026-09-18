package inputValidation;

/*******
 * <p> Title: PasswordEvaluator Class. </p>
 *
 * <p> Description: This class evaluates passwords using the five requirements defined by the
 * Password Evaluation Testbed and the maximum length selected by the team.  The methods are kept
 * separate from the GUI so every password-creation workflow can use the same rules.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-18 Initial version
 */
public class PasswordEvaluator {

	// Password limits agreed to by the team in the Input Validation document
	public static final int MINIMUM_LENGTH = 8;
	public static final int MAXIMUM_LENGTH = 64;

	// These are the special characters accepted by the provided password requirements
	private static final String SPECIAL_CHARACTERS =
			"~`!@#$%^&*()_-+={}[]|\\:;\"'<>,.?/";

	/**
	 * Default constructor is not used.
	 */
	public PasswordEvaluator() {
	}

	/**********
	 * <p> Method: evaluatePassword(String password) </p>
	 *
	 * <p> Description: Check the password maximum before applying the five password requirements
	 * and the supported-character rule.</p>
	 *
	 * @param password specifies the password to evaluate
	 *
	 * @return an empty string when the password is valid, otherwise a useful error message
	 */
	public static String evaluatePassword(String password) {
		if (password == null || password.length() == 0)
			return "Enter a password.";
		if (password.length() > MAXIMUM_LENGTH)
			return "Password cannot exceed 64 characters.";
		if (!hasOnlySupportedCharacters(password))
			return "Password contains an unsupported character.";
		if (!hasMinimumLength(password))
			return "Password must contain at least eight characters.";
		if (!hasUppercase(password))
			return "Password must contain at least one uppercase letter.";
		if (!hasLowercase(password))
			return "Password must contain at least one lowercase letter.";
		if (!hasNumber(password))
			return "Password must contain at least one number.";
		if (!hasSpecialCharacter(password))
			return "Password must contain at least one special character.";
		return "";
	}

	/**********
	 * <p> Method: getRequirementText(String password) </p>
	 *
	 * <p> Description: Build the dynamic feedback displayed while a user enters a new password.</p>
	 *
	 * @param password specifies the password currently being entered
	 *
	 * @return text showing whether each requirement has been satisfied
	 */
	public static String getRequirementText(String password) {
		String value = password == null ? "" : password;
		return requirementLine("At least eight characters", hasMinimumLength(value)) + "\n" +
				requirementLine("At least one uppercase letter", hasUppercase(value)) + "\n" +
				requirementLine("At least one lowercase letter", hasLowercase(value)) + "\n" +
				requirementLine("At least one number", hasNumber(value)) + "\n" +
				requirementLine("At least one special character",
						hasSpecialCharacter(value)) + "\n" +
				requirementLine("No unsupported characters",
						hasOnlySupportedCharacters(value)) + "\n" +
				requirementLine("No more than 64 characters",
						value.length() <= MAXIMUM_LENGTH);
	}

	// Return one consistently formatted line for the dynamic requirements display
	private static String requirementLine(String requirement, boolean satisfied) {
		return (satisfied ? "[Met] " : "[Not met] ") + requirement;
	}

	// Check the five required password properties
	private static boolean hasMinimumLength(String password) {
		return password.length() >= MINIMUM_LENGTH;
	}

	private static boolean hasUppercase(String password) {
		for (int i = 0; i < password.length(); i++)
			if (password.charAt(i) >= 'A' && password.charAt(i) <= 'Z') return true;
		return false;
	}

	private static boolean hasLowercase(String password) {
		for (int i = 0; i < password.length(); i++)
			if (password.charAt(i) >= 'a' && password.charAt(i) <= 'z') return true;
		return false;
	}

	private static boolean hasNumber(String password) {
		for (int i = 0; i < password.length(); i++)
			if (password.charAt(i) >= '0' && password.charAt(i) <= '9') return true;
		return false;
	}

	private static boolean hasSpecialCharacter(String password) {
		for (int i = 0; i < password.length(); i++)
			if (SPECIAL_CHARACTERS.indexOf(password.charAt(i)) >= 0) return true;
		return false;
	}

	private static boolean hasOnlySupportedCharacters(String password) {
		for (int i = 0; i < password.length(); i++) {
			char currentCharacter = password.charAt(i);
			boolean letter = (currentCharacter >= 'A' && currentCharacter <= 'Z') ||
					(currentCharacter >= 'a' && currentCharacter <= 'z');
			boolean number = currentCharacter >= '0' && currentCharacter <= '9';
			boolean special = SPECIAL_CHARACTERS.indexOf(currentCharacter) >= 0;
			if (!letter && !number && !special) return false;
		}
		return true;
	}
}
