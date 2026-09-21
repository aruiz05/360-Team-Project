package guiListUsers;

/*******
 * <p> Title: ControllerListUsers Class. </p>
 *
 * <p> Description: Controller actions for the List Users page. </p>
 */
public class ControllerListUsers {

    /*******
     * Default constructor.
     */
    public ControllerListUsers() {
    }

    /*******
     * <p> Method: returnToAdminHome() </p>
     *
     * <p> Description: Returns the administrator to the Admin Home
     * page after viewing the list of user accounts. </p>
     */
    protected static void returnToAdminHome() {

        guiAdminHome.ViewAdminHome.displayAdminHome(
                ViewListUsers.theStage,
                ViewListUsers.theUser
        );
    }
}