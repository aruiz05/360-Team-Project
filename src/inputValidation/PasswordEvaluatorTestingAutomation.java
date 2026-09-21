package inputValidation;

/*******
 * <p> Title: PasswordEvaluatorTestingAutomation Class. </p>
 *
 * <p> Description: Automated tests for the shared password requirements used by one-time and
 * permanent password creation.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-18 Initial version
 */
public class PasswordEvaluatorTestingAutomation {

	private static int passed = 0;
	private static int failed = 0;

	/**********
	 * <p> Method: main(String[] args) </p>
	 *
	 * <p> Description: Run the automated password evaluator tests.</p>
	 *
	 * @param args command-line arguments are not used
	 */
	public static void main(String[] args) {
		test("Valid password", "Valid123!", true);
		test("Minimum length", "Abcdef1!", true);
		test("Empty password", "", false);
		test("Below minimum", "Abc1!", false);
		test("Missing uppercase", "password1!", false);
		test("Missing lowercase", "PASSWORD1!", false);
		test("Missing number", "Password!", false);
		test("Missing special", "Password1", false);
		test("Unsupported space", "Password 1!", false);
		test("Maximum length", buildMaximumPassword(), true);
		test("Over maximum", buildMaximumPassword() + "a", false);

		System.out.println("Password evaluator tests passed: " + passed);
		System.out.println("Password evaluator tests failed: " + failed);
		if (failed > 0) throw new AssertionError("Password evaluator testing failed.");
	}

	// Run one test and update the summary counters
	private static void test(String name, String password, boolean expectedValid) {
		boolean actualValid = PasswordEvaluator.evaluatePassword(password).length() == 0;
		if (actualValid == expectedValid) passed++;
		else {
			failed++;
			System.out.println("FAILED: " + name);
		}
	}

	// Create a valid password containing exactly 64 characters
	private static String buildMaximumPassword() {
		String password = "Aa1!";
		while (password.length() < PasswordEvaluator.MAXIMUM_LENGTH) password += "a";
		return password;
	}
}
