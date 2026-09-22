package guiListUsers;

import java.util.ArrayList;
import java.util.List;

import entityClasses.User;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewListUsers Class. </p>
 *
 * <p> Description: The JavaFX-based List Users page. This page allows
 * an administrator to view all user accounts currently registered in
 * the system. For each account, the username, user's name, email
 * address, and assigned roles are displayed. </p>
 */
public class ViewListUsers {

    /*
     * Window dimensions
     */
    private static double width =
            applicationMain.FoundationsMain.WINDOW_WIDTH;

    private static double height =
            applicationMain.FoundationsMain.WINDOW_HEIGHT;

    /*
     * GUI widgets
     */
    private static Label label_PageTitle =
            new Label("User Accounts");

    private static Label label_Description =
            new Label("All registered users and their assigned roles");

    private static TableView<User> table_Users =
            new TableView<User>();

    private static Button button_Return =
            new Button("Return to Admin Home");

    /*
     * Shared page information
     */
    private static ViewListUsers theView;

    protected static Stage theStage;
    protected static User theUser;

    private static Pane theRootPane;
    private static Scene theListUsersScene;

    /*******
     * <p> Method: displayListUsers(Stage ps, User user) </p>
     *
     * <p> Description: Displays the List Users page. The current list
     * of accounts is retrieved from the database each time this page
     * is displayed. </p>
     *
     * @param ps the JavaFX Stage used by the application
     * @param user the currently logged-in administrator
     */
    public static void displayListUsers(Stage ps, User user) {

        theStage = ps;
        theUser = user;

        // Create the GUI only once
        if (theView == null) {
            theView = new ViewListUsers();
        }

        // Reload the user data every time the page is displayed
        refreshUserTable();

        theStage.setTitle(
                "CSE 360 Foundation Code: List All Users"
        );

        theStage.setScene(theListUsersScene);
        theStage.show();
    }

    /*******
     * Private constructor used to build the GUI.
     */
    private ViewListUsers() {

        theRootPane = new Pane();

        theListUsersScene =
                new Scene(theRootPane, width, height);
        guiTools.ASUTheme.apply(theListUsersScene);

        /*
         * Page title
         */
        label_PageTitle.setFont(Font.font("Arial", 28));
        label_PageTitle.setMinWidth(width);
        label_PageTitle.setAlignment(Pos.CENTER);
        label_PageTitle.setLayoutX(0);
        label_PageTitle.setLayoutY(10);
        guiTools.ASUTheme.stylePageTitle(label_PageTitle);

        /*
         * Description
         */
        label_Description.setFont(Font.font("Arial", 16));
        label_Description.setMinWidth(width);
        label_Description.setAlignment(Pos.CENTER);
        label_Description.setLayoutX(0);
        label_Description.setLayoutY(55);

        /*
         * Username column
         */
        TableColumn<User, String> usernameColumn =
                new TableColumn<User, String>("Username");

        usernameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getUserName()
                )
        );

        usernameColumn.setPrefWidth(140);

        /*
         * Name column
         */
        TableColumn<User, String> nameColumn =
                new TableColumn<User, String>("Name");

        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        getFullName(data.getValue())
                )
        );

        nameColumn.setPrefWidth(200);

        /*
         * Email column
         */
        TableColumn<User, String> emailColumn =
                new TableColumn<User, String>("Email Address");

        emailColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        data.getValue().getEmailAddress()
                )
        );

        emailColumn.setPrefWidth(250);

        /*
         * Roles column
         */
        TableColumn<User, String> rolesColumn =
                new TableColumn<User, String>("Roles");

        rolesColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        getRoles(data.getValue())
                )
        );

        rolesColumn.setPrefWidth(170);

        /*
         * Configure table
         */
        table_Users.getColumns().add(usernameColumn);
        table_Users.getColumns().add(nameColumn);
        table_Users.getColumns().add(emailColumn);
        table_Users.getColumns().add(rolesColumn);

        table_Users.setLayoutX(20);
        table_Users.setLayoutY(95);

        table_Users.setPrefWidth(width - 40);
        table_Users.setPrefHeight(height - 190);

        table_Users.setEditable(false);

        table_Users.setPlaceholder(
                new Label("No user accounts were found.")
        );

        /*
         * Return button
         */
        button_Return.setFont(Font.font("Dialog", 18));
        button_Return.setMinWidth(220);
        button_Return.setAlignment(Pos.CENTER);

        button_Return.setLayoutX(20);
        button_Return.setLayoutY(height - 70);

        button_Return.setOnAction((event) -> {
            ControllerListUsers.returnToAdminHome();
        });

        /*
         * Add widgets to page
         */
        theRootPane.getChildren().addAll(
                label_PageTitle,
                label_Description,
                table_Users,
                button_Return
        );
    }

    /*******
     * <p> Method: refreshUserTable() </p>
     *
     * <p> Description: Gets the current user accounts from the
     * database and refreshes the TableView. </p>
     */
    private static void refreshUserTable() {

        List<User> users = ModelListUsers.getAllUsers();

        if (users == null) {
            users = new ArrayList<User>();
        }

        table_Users.setItems(
                FXCollections.observableArrayList(users)
        );
    }
    /*******
     * <p> Method: getFullName(User user) </p>
     *
     * <p> Description: Builds a readable full name from the user's
     * first, middle, and last names. </p>
     *
     * @param user the user whose name will be displayed
     * @return the user's full name
     */
    private static String getFullName(User user) {

        String firstName = safe(user.getFirstName());
        String middleName = safe(user.getMiddleName());
        String lastName = safe(user.getLastName());

        String fullName =
                firstName + " " +
                middleName + " " +
                lastName;

        return fullName.replaceAll("\\s+", " ").trim();
    }

    /*******
     * <p> Method: getRoles(User user) </p>
     *
     * <p> Description: Creates a readable list of all roles assigned
     * to the specified user. </p>
     *
     * @param user the user whose roles will be displayed
     * @return a comma-separated String containing the user's roles
     */
    private static String getRoles(User user) {

        List<String> roles = new ArrayList<String>();

        if (user.getAdminRole()) {
            roles.add("Admin");
        }

        if (user.getNewRole1()) {
            roles.add("Role1");
        }

        if (user.getNewRole2()) {
            roles.add("Role2");
        }

        if (roles.isEmpty()) {
            return "None";
        }

        return String.join(", ", roles);
    }

    /*******
     * <p> Method: safe(String value) </p>
     *
     * <p> Description: Prevents null database values from being
     * displayed as the word "null". </p>
     *
     * @param value the String to check
     * @return an empty String if null, otherwise the original value
     */
    private static String safe(String value) {

        if (value == null) {
            return "";
        }

        return value;
    }
}
