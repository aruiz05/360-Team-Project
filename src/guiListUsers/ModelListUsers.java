package guiListUsers;

import java.util.List;

import database.Database;
import entityClasses.User;

/*******
 * <p> Title: ModelListUsers Class. </p>
 *
 * <p> Description: The model for the List Users page. This class
 * obtains the list of user accounts from the database for display
 * by the administrator. </p>
 */
public class ModelListUsers {

    // Reference to the application's database
    private static Database theDatabase =
            applicationMain.FoundationsMain.database;

    /*******
     * <p> Method: getAllUsers() </p>
     *
     * <p> Description: Retrieves all user accounts currently stored
     * in the database. </p>
     *
     * @return a list of User objects representing all accounts
     */
    protected static List<User> getAllUsers() {
        return theDatabase.getAllUsers();
    }
}