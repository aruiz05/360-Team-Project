/*******
 * Provides the Model, View, and Controller for the Contributor Lesson Details window.
 *
 * <p>The package displays current core lesson information and lock status and manages the
 * experience records linked to the selected lesson. The View never exposes separate information
 * added by another role. Validation, ownership checks, lock checks, and persistence are delegated
 * through the Model to non-GUI code so the displayed behavior and automated tests use the same
 * rules.</p>
 */
package guiContributorLessonDetails;
