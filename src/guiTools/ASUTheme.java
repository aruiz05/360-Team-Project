package guiTools;

import java.net.URL;

import javafx.scene.Scene;
import javafx.scene.control.Labeled;

/*******
 * <p> Title: ASUTheme Class. </p>
 *
 * <p> Description: Applies the shared ASU-inspired stylesheet and visual style classes to the
 * application's JavaFX scenes.  This class changes presentation only.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00		2026-09-22 Initial version
 */
public class ASUTheme {

	private static final String STYLESHEET = "/applicationMain/application.css";

	/**
	 * Default constructor is not used.
	 */
	private ASUTheme() {
	}

	/**********
	 * <p> Method: apply(Scene scene) </p>
	 *
	 * <p> Description: Adds the shared application stylesheet to a scene.</p>
	 *
	 * @param scene specifies the JavaFX scene to style
	 */
	public static void apply(Scene scene) {
		URL stylesheet = ASUTheme.class.getResource(STYLESHEET);
		if (stylesheet != null && !scene.getStylesheets().contains(stylesheet.toExternalForm()))
			scene.getStylesheets().add(stylesheet.toExternalForm());
	}

	/**********
	 * <p> Method: stylePageTitle(Labeled title) </p>
	 *
	 * <p> Description: Applies the shared maroon-and-gold page header style.</p>
	 *
	 * @param title specifies the label used as the page title
	 */
	public static void stylePageTitle(Labeled title) {
		if (!title.getStyleClass().contains("page-title"))
			title.getStyleClass().add("page-title");
	}

	/**********
	 * <p> Method: styleSecondary(Labeled control) </p>
	 *
	 * <p> Description: Applies the outlined secondary-action style.</p>
	 *
	 * @param control specifies the button or other labeled control to style
	 */
	public static void styleSecondary(Labeled control) {
		if (!control.getStyleClass().contains("secondary-button"))
			control.getStyleClass().add("secondary-button");
	}

	/**********
	 * <p> Method: styleDanger(Labeled control) </p>
	 *
	 * <p> Description: Applies the destructive-action button style.</p>
	 *
	 * @param control specifies the destructive button to style
	 */
	public static void styleDanger(Labeled control) {
		if (!control.getStyleClass().contains("danger-button"))
			control.getStyleClass().add("danger-button");
	}
}
