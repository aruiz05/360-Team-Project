package database;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import entityClasses.ExperienceRecord;
import entityClasses.LessonLearned;
import entityClasses.TeamMemberEffort;
import entityClasses.User;

/*******
 * <p> Title: Database Class. </p>
 * 
 * <p> Description: This is a persistent database built on H2.  Detailed documentation of H2 can
 * be found at https://www.h2database.com/html/main.html (Click on "PDF (2MB)" on the l3ft side
 * of the page under the heading "Reference" for a PDF of 438 pages.)  This class leverages H2
 * and provides numerous special supporting methods.
 * </p>
 * 
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 * 
 * @author Lynn Robert Carter
 * 
 * @version 2.00		2025-04-29 Updated and expanded from the version produce by Pravalika 
 * 							Mukkiri and Ishwarya Hidkimath Basavaraj
 * @version 2.01		2025-12-17 Minor updates for Spring 2026
 * @version 3.00        2026-10-01 Added persistent HW2 lesson and experience CRUD
 */

/*
 * The Database class is responsible for establishing and managing the connection to the database,
 * and performing operations such as user registration, login validation, handling invitation 
 * codes, and numerous other database related functions.
 */
public class Database {

	// JDBC driver name and database URL 
	static final String JDBC_DRIVER = "org.h2.Driver";   
	static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

	//  Database credentials 
	static final String USER = "sa"; 
	static final String PASS = ""; 

	//  Shared variables used within this class
	private final String databaseURL;			// Database URL used by this instance
	private Connection connection = null;		// Singleton to access the database 
	private Statement statement = null;			// The H2 Statement is used to construct queries
	
	// These are the easily accessible attributes of the currently logged-in user
	// This is only useful for single user applications
	private String currentUsername;
	private String currentPassword;
	private String currentFirstName;
	private String currentMiddleName;
	private String currentLastName;
	private String currentPreferredFirstName;
	private String currentEmailAddress;
	private boolean currentAdminRole;
	private boolean currentNewRole1;
	private boolean currentNewRole2;

	/*******
	 * <p> Method: Database </p>
	 * 
	 * <p> Description: The default constructor used to establish this singleton object.</p>
	 * 
	 */
	
	public Database () {
		databaseURL = DB_URL;
	}

	/*******
	 * <p> Method: Database(String url) </p>
	 *
	 * <p> Description: Package-protected constructor used by automated database tests so the
	 * application database is not changed while tests are running.</p>
	 *
	 * @param url specifies the H2 database URL used by the test
	 */
	Database(String url) {
		databaseURL = url;
	}
	
	
/*******
 * <p> Method: connectToDatabase </p>
 * 
 * <p> Description: Used to establish the H2 database connection to persistent storage.</p>
 *
 * @throws SQLException when the DriverManager is unable to establish a connection
 * 
 */
	public void connectToDatabase() throws SQLException {
		try {
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			connection = DriverManager.getConnection(databaseURL, USER, PASS);
			statement = connection.createStatement(); 
			// You can use this command to clear the database and restart from fresh.
			//statement.execute("DROP ALL OBJECTS");

			createTables();  // Create the necessary tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}

	
/*******
 * <p> Method: createTables </p>
 * 
 * <p> Description: Used to create new instances of the two database tables used by this class.</p>
 * 
 * @throws SQLException if the database tables cannot be created
 *
 */
	private void createTables() throws SQLException {
		// Create the user database
		String userTable = "CREATE TABLE IF NOT EXISTS userDB ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255) UNIQUE, "
				+ "password VARCHAR(255), "
				+ "firstName VARCHAR(255), "
				+ "middleName VARCHAR(255), "
				+ "lastName VARCHAR (255), "
				+ "preferredFirstName VARCHAR(255), "
				+ "emailAddress VARCHAR(255), "
				+ "adminRole BOOL DEFAULT FALSE, "
				+ "newRole1 BOOL DEFAULT FALSE, "
				+ "newRole2 BOOL DEFAULT FALSE, "
				+ "oneTimePassword VARCHAR(64), "
				+ "passwordResetRequired BOOL DEFAULT FALSE)";
		statement.execute(userTable);

		// Existing databases must also receive the new one-time-password columns.  Adding the
		// columns conditionally allows each team member to keep their current database.
		statement.execute("ALTER TABLE userDB ADD COLUMN IF NOT EXISTS " +
				"oneTimePassword VARCHAR(64)");
		statement.execute("ALTER TABLE userDB ADD COLUMN IF NOT EXISTS " +
				"passwordResetRequired BOOL DEFAULT FALSE");
		
		// Create the invitation codes table
	    String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
	            + "code VARCHAR(10) PRIMARY KEY, "
	    		+ "emailAddress VARCHAR(255), "
	            + "role VARCHAR(10))";
	    statement.execute(invitationCodesTable);

		// HW2 lessons are stored in separate tables so the existing TP1 account data and behavior
		// are not changed.  Lock state is persisted with each record even though curator controls
		// are outside the HW2 scope.
		String lessonsTable = "CREATE TABLE IF NOT EXISTS lessons ("
				+ "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
				+ "lessonOwner VARCHAR(255) NOT NULL, "
				+ "title VARCHAR(500) NOT NULL, "
				+ "problemSituation CLOB NOT NULL, "
				+ "lessonText CLOB NOT NULL, "
				+ "lessonLocked BOOL DEFAULT FALSE, "
				+ "titleLocked BOOL DEFAULT FALSE, "
				+ "problemLocked BOOL DEFAULT FALSE, "
				+ "lessonTextLocked BOOL DEFAULT FALSE, "
				+ "hiddenRoleInformation CLOB)";
		statement.execute(lessonsTable);

		String experienceTable = "CREATE TABLE IF NOT EXISTS experienceRecords ("
				+ "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
				+ "lessonId BIGINT NOT NULL, "
				+ "recordOwner VARCHAR(255) NOT NULL, "
				+ "whatWasDone CLOB NOT NULL, "
				+ "howItWasDone CLOB NOT NULL, "
				+ "elapsedMinutes DOUBLE NOT NULL, "
				+ "experienceLocked BOOL DEFAULT FALSE, "
				+ "whatLocked BOOL DEFAULT FALSE, "
				+ "howLocked BOOL DEFAULT FALSE, "
				+ "elapsedLocked BOOL DEFAULT FALSE, "
				+ "effortLocked BOOL DEFAULT FALSE, "
				+ "FOREIGN KEY (lessonId) REFERENCES lessons(id) ON DELETE CASCADE)";
		statement.execute(experienceTable);

		String effortTable = "CREATE TABLE IF NOT EXISTS experienceEfforts ("
				+ "experienceId BIGINT NOT NULL, "
				+ "entryOrder INT NOT NULL, "
				+ "memberName VARCHAR(255) NOT NULL, "
				+ "effortMinutes DOUBLE NOT NULL, "
				+ "PRIMARY KEY (experienceId, entryOrder), "
				+ "FOREIGN KEY (experienceId) REFERENCES experienceRecords(id) ON DELETE CASCADE)";
		statement.execute(effortTable);
	}


/*******
 * <p> Method: isDatabaseEmpty </p>
 * 
 * <p> Description: If the user database has no rows, true is returned, else false.</p>
 * 
 * @return true if the database is empty, else it returns false
 * 
 */
	public boolean isDatabaseEmpty() {
		String query = "SELECT COUNT(*) AS count FROM userDB";
		try {
			ResultSet resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				return resultSet.getInt("count") == 0;
			}
		}  catch (SQLException e) {
	        return false;
	    }
		return true;
	}
	
	
/*******
 * <p> Method: getNumberOfUsers </p>
 * 
 * <p> Description: Returns an integer .of the number of users currently in the user database. </p>
 * 
 * @return the number of user records in the database.
 * 
 */
	public int getNumberOfUsers() {
		String query = "SELECT COUNT(*) AS count FROM userDB";
		try {
			ResultSet resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				return resultSet.getInt("count");
			}
		} catch (SQLException e) {
	        return 0;
	    }
		return 0;
	}

/*******
 * <p> Method: register(User user) </p>
 * 
 * <p> Description: Creates a new row in the database using the user parameter. </p>
 * 
 * @throws SQLException when there is an issue creating the SQL command or executing it.
 * 
 * @param user specifies a user object to be added to the database.
 * 
 */
	public void register(User user) throws SQLException {
		String insertUser = "INSERT INTO userDB (userName, password, firstName, middleName, "
				+ "lastName, preferredFirstName, emailAddress, adminRole, newRole1, newRole2) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
			currentUsername = user.getUserName();
			pstmt.setString(1, currentUsername);
			
			currentPassword = user.getPassword();
			pstmt.setString(2, currentPassword);
			
			currentFirstName = user.getFirstName();
			pstmt.setString(3, currentFirstName);
			
			currentMiddleName = user.getMiddleName();			
			pstmt.setString(4, currentMiddleName);
			
			currentLastName = user.getLastName();
			pstmt.setString(5, currentLastName);
			
			currentPreferredFirstName = user.getPreferredFirstName();
			pstmt.setString(6, currentPreferredFirstName);
			
			currentEmailAddress = user.getEmailAddress();
			pstmt.setString(7, currentEmailAddress);
			
			currentAdminRole = user.getAdminRole();
			pstmt.setBoolean(8, currentAdminRole);
			
			currentNewRole1 = user.getNewRole1();
			pstmt.setBoolean(9, currentNewRole1);
			
			currentNewRole2 = user.getNewRole2();
			pstmt.setBoolean(10, currentNewRole2);
			
			pstmt.executeUpdate();
		}
		
	}
	
/*******
 *  <p> Method: List getUserList() </p>
 *  
 *  <P> Description: Generate an List of Strings, one for each user in the database,
 *  starting with {@code <Select User>} at the start of the list. </p>
 *  
 *  @return a list of userNames found in the database.
 */
	public List<String> getUserList () {
		List<String> userList = new ArrayList<String>();
		userList.add("<Select a User>");
		String query = "SELECT userName FROM userDB";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				userList.add(rs.getString("userName"));
			}
		} catch (SQLException e) {
	        return null;
	    }
//		System.out.println(userList);
		return userList;
	}
	
	/*******
	 * <p> Method: List&lt;User&gt; getAllUsers() </p>
	 *
	 * <p> Description: Retrieves all user accounts from the database so an
	 * administrator can view the username, name, email address, and assigned
	 * roles for each user.</p>
	 *
	 * @return a list containing all users currently stored in the database.
	 */
	public List<User> getAllUsers() {

	    List<User> users = new ArrayList<User>();

	    String query = "SELECT userName, firstName, middleName, lastName, "
	            + "preferredFirstName, emailAddress, adminRole, newRole1, newRole2 "
	            + "FROM userDB ORDER BY userName";

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {

	        ResultSet rs = pstmt.executeQuery();

	        while (rs.next()) {

	            User user = new User(
	                    rs.getString("userName"),
	                    "",     // Password is intentionally not needed for this screen
	                    rs.getString("firstName"),
	                    rs.getString("middleName"),
	                    rs.getString("lastName"),
	                    rs.getString("preferredFirstName"),
	                    rs.getString("emailAddress"),
	                    rs.getBoolean("adminRole"),
	                    rs.getBoolean("newRole1"),
	                    rs.getBoolean("newRole2")
	            );

	            users.add(user);
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return users;
	}
	

	/*******
	 * <p> Method: deleteUser(String username, String administratorUsername) </p>
	 *
	 * <p> Description: Remove an account only when the acting user still has the Admin role
	 * and is not removing their own account.  Both checks are part of the deletion statement.</p>
	 *
	 * @param username specifies the account to be removed
	 * @param administratorUsername specifies the signed-in administrator
	 * @return true when one account was removed, else false
	 */
	public boolean deleteUser(String username, String administratorUsername) {
		if (username == null || administratorUsername == null ||
				username.isBlank() || administratorUsername.isBlank()) return false;

		String query = "DELETE FROM userDB WHERE userName = ? AND userName <> ? " +
				"AND EXISTS (SELECT 1 FROM userDB AS administrator " +
				"WHERE administrator.userName = ? AND administrator.adminRole = TRUE)";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
			pstmt.setString(2, administratorUsername);
			pstmt.setString(3, administratorUsername);
			return pstmt.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

/*******
 * <p> Method: boolean loginAdmin(User user) </p>
 * 
 * <p> Description: Check to see that a user with the specified username, password, and role
 * 		is the same as a row in the table for the username, password, and role. </p>
 * 
 * @param user specifies the specific user that should be logged in playing the Admin role.
 * 
 * @return true if the specified user has been logged in as an Admin else false.
 * 
 */
	public boolean loginAdmin(User user){
		// Validates an admin user's login credentials so the user can login in as an Admin.
		String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND "
				+ "adminRole = TRUE AND passwordResetRequired = FALSE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			ResultSet rs = pstmt.executeQuery();
			return rs.next();	// If a row is returned, rs.next() will return true		
		} catch  (SQLException e) {
	        e.printStackTrace();
	    }
		return false;
	}
	
	
/*******
 * <p> Method: boolean loginRole1(User user) </p>
 * 
 * <p> Description: Check to see that a user with the specified username, password, and role
 * 		is the same as a row in the table for the username, password, and role. </p>
 * 
 * @param user specifies the specific user that should be logged in playing the Student role.
 * 
 * @return true if the specified user has been logged in as an Student else false.
 * 
 */
	public boolean loginRole1(User user) {
		// Validates a student user's login credentials.
		String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND "
				+ "newRole1 = TRUE AND passwordResetRequired = FALSE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			ResultSet rs = pstmt.executeQuery();
			return rs.next();
		} catch  (SQLException e) {
		       e.printStackTrace();
		}
		return false;
	}

	/*******
	 * <p> Method: boolean loginRole2(User user) </p>
	 * 
	 * <p> Description: Check to see that a user with the specified username, password, and role
	 * 		is the same as a row in the table for the username, password, and role. </p>
	 * 
	 * @param user specifies the specific user that should be logged in playing the Reviewer role.
	 * 
	 * @return true if the specified user has been logged in as an Student else false.
	 * 
	 */
	// Validates a reviewer user's login credentials.
	public boolean loginRole2(User user) {
		String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND "
				+ "newRole2 = TRUE AND passwordResetRequired = FALSE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			ResultSet rs = pstmt.executeQuery();
			return rs.next();
		} catch  (SQLException e) {
		       e.printStackTrace();
		}
		return false;
	}
	
	
	/*******
	 * <p> Method: boolean doesUserExist(User user) </p>
	 * 
	 * <p> Description: Check to see that a user with the specified username is  in the table. </p>
	 * 
	 * @param userName specifies the specific user that we want to determine if it is in the table.
	 * 
	 * @return true if the specified user is in the table else false.
	 * 
	 */
	// Checks if a user already exists in the database based on their userName.
	public boolean doesUserExist(String userName) {
	    String query = "SELECT COUNT(*) FROM userDB WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            // If the count is greater than 0, the user exists
	            return rs.getInt(1) > 0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false; // If an error occurs, assume user doesn't exist
	}

	
	/*******
	 * <p> Method: int getNumberOfRoles(User user) </p>
	 * 
	 * <p> Description: Determine the number of roles a specified user plays. </p>
	 * 
	 * @param user specifies the specific user that we want to determine if it is in the table.
	 * 
	 * @return the number of roles this user plays (0 - 5).
	 * 
	 */	
	// Get the number of roles that this user plays
	public int getNumberOfRoles (User user) {
		int numberOfRoles = 0;
		if (user.getAdminRole()) numberOfRoles++;
		if (user.getNewRole1()) numberOfRoles++;
		if (user.getNewRole2()) numberOfRoles++;
		return numberOfRoles;
	}	

	
	/*******
	 * <p> Method: String generateInvitationCode(String emailAddress, String role) </p>
	 * 
	 * <p> Description: Given an email address and a roles, this method establishes and invitation
	 * code and adds a record to the InvitationCodes table.  When the invitation code is used, the
	 * stored email address is used to establish the new user and the record is removed from the
	 * table.</p>
	 * 
	 * @param emailAddress specifies the email address for this new user.
	 * 
	 * @param role specified the role that this new user will play.
	 * 
	 * @return the code of six characters so the new user can use it to securely setup an account.
	 * 
	 */
	// Generates a new invitation code and inserts it into the database.
	public String generateInvitationCode(String emailAddress, String role) {
	    String code = UUID.randomUUID().toString().substring(0, 6); // Generate a random 6-character code
	    String query = "INSERT INTO InvitationCodes (code, emailaddress, role) VALUES (?, ?, ?)";

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.setString(2, emailAddress);
	        pstmt.setString(3, role);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return code;
	}

	
	/*******
	 * <p> Method: int getNumberOfInvitations() </p>
	 * 
	 * <p> Description: Determine the number of outstanding invitations in the table.</p>
	 *  
	 * @return the number of invitations in the table.
	 * 
	 */
	// Number of invitations in the database
	public int getNumberOfInvitations() {
		String query = "SELECT COUNT(*) AS count FROM InvitationCodes";
		try {
			ResultSet resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				return resultSet.getInt("count");
			}
		} catch  (SQLException e) {
	        e.printStackTrace();
	    }
		return 0;
	}
	
	
	/*******
	 * <p> Method: boolean emailaddressHasBeenUsed(String emailAddress) </p>
	 * 
	 * <p> Description: Determine if an email address has been user to establish a user.</p>
	 * 
	 * @param emailAddress is a string that identifies a user in the table
	 *  
	 * @return true if the email address is in the table, else return false.
	 * 
	 */
	// Check to see if an email address is already in the database
	public boolean emailaddressHasBeenUsed(String emailAddress) {
	    String query = "SELECT COUNT(*) AS count FROM InvitationCodes WHERE emailAddress = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, emailAddress);
	        ResultSet rs = pstmt.executeQuery();
	 //     System.out.println(rs);
	        if (rs.next()) {
	            // Mark the code as used
	        	return rs.getInt("count")>0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return false;
	}
	
	
	/*******
	 * <p> Method: String getRoleGivenAnInvitationCode(String code) </p>
	 * 
	 * <p> Description: Get the role associated with an invitation code.</p>
	 * 
	 * @param code is the 6 character String invitation code
	 *  
	 * @return the role for the code or an empty string.
	 * 
	 */
	// Obtain the roles associated with an invitation code.
	public String getRoleGivenAnInvitationCode(String code) {
	    String query = "SELECT * FROM InvitationCodes WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            return rs.getString("role");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return "";
	}

	
	/*******
	 * <p> Method: String getEmailAddressUsingCode (String code ) </p>
	 * 
	 * <p> Description: Get the email addressed associated with an invitation code.</p>
	 * 
	 * @param code is the 6 character String invitation code
	 *  
	 * @return the email address for the code or an empty string.
	 * 
	 */
	// For a given invitation code, return the associated email address of an empty string
	public String getEmailAddressUsingCode (String code ) {
	    String query = "SELECT emailAddress FROM InvitationCodes WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            return rs.getString("emailAddress");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return "";
	}
	
	
	/*******
	 * <p> Method: void removeInvitationAfterUse(String code) </p>
	 * 
	 * <p> Description: Remove an invitation record once it is used.</p>
	 * 
	 * @param code is the 6 character String invitation code
	 *  
	 */
	// Remove an invitation using an email address once the user account has been setup
	public void removeInvitationAfterUse(String code) {
	    String query = "SELECT COUNT(*) AS count FROM InvitationCodes WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	        	int counter = rs.getInt(1);
	            // Only do the remove if the code is still in the invitation table
	        	if (counter > 0) {
        			query = "DELETE FROM InvitationCodes WHERE code = ?";
	        		try (PreparedStatement pstmt2 = connection.prepareStatement(query)) {
	        			pstmt2.setString(1, code);
	        			pstmt2.executeUpdate();
	        		}catch (SQLException e) {
	        	        e.printStackTrace();
	        	    }
	        	}
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return;
	}
	
	
	/*******
	 * <p> Method: String getFirstName(String username) </p>
	 * 
	 * <p> Description: Get the first name of a user given that user's username.</p>
	 * 
	 * @param username is the username of the user
	 * 
	 * @return the first name of a user given that user's username 
	 *  
	 */
	// Get the First Name
	public String getFirstName(String username) {
		String query = "SELECT firstName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("firstName"); // Return the first name if user exists
	        }
			
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}
	

	/*******
	 * <p> Method: void updateFirstName(String username, String firstName) </p>
	 * 
	 * <p> Description: Update the first name of a user given that user's username and the new
	 *		first name.</p>
	 * 
	 * @param username is the username of the user
	 * 
	 * @param firstName is the new first name for the user
	 *  
	 */
	// update the first name
	public void updateFirstName(String username, String firstName) {
	    String query = "UPDATE userDB SET firstName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, firstName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentFirstName = firstName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}

	
	/*******
	 * <p> Method: String getMiddleName(String username) </p>
	 * 
	 * <p> Description: Get the middle name of a user given that user's username.</p>
	 * 
	 * @param username is the username of the user
	 * 
	 * @return the middle name of a user given that user's username 
	 *  
	 */
	// get the middle name
	public String getMiddleName(String username) {
		String query = "SELECT MiddleName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("middleName"); // Return the middle name if user exists
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}

	
	/*******
	 * <p> Method: void updateMiddleName(String username, String middleName) </p>
	 * 
	 * <p> Description: Update the middle name of a user given that user's username and the new
	 * 		middle name.</p>
	 * 
	 * @param username is the username of the user
	 *  
	 * @param middleName is the new middle name for the user
	 *  
	 */
	// update the middle name
	public void updateMiddleName(String username, String middleName) {
	    String query = "UPDATE userDB SET middleName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, middleName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentMiddleName = middleName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	
	/*******
	 * <p> Method: String getLastName(String username) </p>
	 * 
	 * <p> Description: Get the last name of a user given that user's username.</p>
	 * 
	 * @param username is the username of the user
	 * 
	 * @return the last name of a user given that user's username 
	 *  
	 */
	// get he last name
	public String getLastName(String username) {
		String query = "SELECT LastName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("lastName"); // Return last name role if user exists
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}
	
	
	/*******
	 * <p> Method: void updateLastName(String username, String lastName) </p>
	 * 
	 * <p> Description: Update the middle name of a user given that user's username and the new
	 * 		middle name.</p>
	 * 
	 * @param username is the username of the user
	 *  
	 * @param lastName is the new last name for the user
	 *  
	 */
	// update the last name
	public void updateLastName(String username, String lastName) {
	    String query = "UPDATE userDB SET lastName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, lastName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentLastName = lastName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	
	/*******
	 * <p> Method: String getPreferredFirstName(String username) </p>
	 * 
	 * <p> Description: Get the preferred first name of a user given that user's username.</p>
	 * 
	 * @param username is the username of the user
	 * 
	 * @return the preferred first name of a user given that user's username 
	 *  
	 */
	// get the preferred first name
	public String getPreferredFirstName(String username) {
		String query = "SELECT preferredFirstName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("firstName"); // Return the preferred first name if user exists
	        }
			
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}
	
	
	/*******
	 * <p> Method: void updatePreferredFirstName(String username, String preferredFirstName) </p>
	 * 
	 * <p> Description: Update the preferred first name of a user given that user's username and
	 * 		the new preferred first name.</p>
	 * 
	 * @param username is the username of the user
	 *  
	 * @param preferredFirstName is the new preferred first name for the user
	 *  
	 */
	// update the preferred first name of the user
	public void updatePreferredFirstName(String username, String preferredFirstName) {
	    String query = "UPDATE userDB SET preferredFirstName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, preferredFirstName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentPreferredFirstName = preferredFirstName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	
	/*******
	 * <p> Method: String getEmailAddress(String username) </p>
	 * 
	 * <p> Description: Get the email address of a user given that user's username.</p>
	 * 
	 * @param username is the username of the user
	 * 
	 * @return the email address of a user given that user's username 
	 *  
	 */
	// get the email address
	public String getEmailAddress(String username) {
		String query = "SELECT emailAddress FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("emailAddress"); // Return the email address if user exists
	        }
			
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}
	
	
	/*******
	 * <p> Method: void updateEmailAddress(String username, String emailAddress) </p>
	 * 
	 * <p> Description: Update the email address name of a user given that user's username and
	 * 		the new email address.</p>
	 * 
	 * @param username is the username of the user
	 *  
	 * @param emailAddress is the new preferred first name for the user
	 *  
	 */
	// update the email address
	public void updateEmailAddress(String username, String emailAddress) {
	    String query = "UPDATE userDB SET emailAddress = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, emailAddress);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentEmailAddress = emailAddress;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	
	/*******
	 * <p> Method: boolean getUserAccountDetails(String username) </p>
	 * 
	 * <p> Description: Get all the attributes of a user given that user's username.</p>
	 * 
	 * @param username is the username of the user
	 * 
	 * @return true of the get is successful, else false
	 *  
	 */
	// get the attributes for a specified user
	public boolean getUserAccountDetails(String username) {
		String query = "SELECT * FROM userDB WHERE username = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();			
			rs.next();
	    	currentUsername = rs.getString(2);
	    	currentPassword = rs.getString(3);
	    	currentFirstName = rs.getString(4);
	    	currentMiddleName = rs.getString(5);
	    	currentLastName = rs.getString(6);
	    	currentPreferredFirstName = rs.getString(7);
	    	currentEmailAddress = rs.getString(8);
	    	currentAdminRole = rs.getBoolean(9);
	    	currentNewRole1 = rs.getBoolean(10);
	    	currentNewRole2 = rs.getBoolean(11);
			return true;
	    } catch (SQLException e) {
			return false;
	    }
	}
	

	/*******
	 * <p> Method: boolean setOneTimePassword(String username, String password) </p>
	 *
	 * <p> Description: Store a one-time password for an existing user and mark that account as
	 * requiring a permanent password reset.  The user's current permanent password is not changed.
	 * </p>
	 *
	 * @param username specifies the user whose one-time password is being established
	 *
	 * @param password specifies the validated one-time password
	 *
	 * @return true if exactly one account was updated, else false
	 */
	public boolean setOneTimePassword(String username, String password) {
		String query = "UPDATE userDB SET oneTimePassword = ?, " +
				"passwordResetRequired = TRUE WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, password);
			pstmt.setString(2, username);
			return pstmt.executeUpdate() == 1;
		} catch (SQLException e) {
			return false;
		}
	}


	/*******
	 * <p> Method: boolean consumeOneTimePassword(String username, String password) </p>
	 *
	 * <p> Description: Check a one-time password and immediately clear it when it is correct.  The
	 * reset-required state remains true until a valid permanent password is saved.  Performing the
	 * comparison and clear in one update prevents a successful credential from being reused.</p>
	 *
	 * @param username specifies the user attempting to log in
	 *
	 * @param password specifies the submitted one-time password
	 *
	 * @return true if the one-time password was correct and was consumed, else false
	 */
	public boolean consumeOneTimePassword(String username, String password) {
		String query = "UPDATE userDB SET oneTimePassword = NULL WHERE userName = ? AND " +
				"oneTimePassword = ? AND passwordResetRequired = TRUE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
			pstmt.setString(2, password);
			return pstmt.executeUpdate() == 1;
		} catch (SQLException e) {
			return false;
		}
	}


	/*******
	 * <p> Method: boolean isPasswordResetRequired(String username) </p>
	 *
	 * <p> Description: Determine whether the specified user must establish a new permanent
	 * password before normal login is permitted.</p>
	 *
	 * @param username specifies the user account being checked
	 *
	 * @return true if the account requires a password reset, else false
	 */
	public boolean isPasswordResetRequired(String username) {
		String query = "SELECT passwordResetRequired FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
			ResultSet rs = pstmt.executeQuery();
			return rs.next() && rs.getBoolean("passwordResetRequired");
		} catch (SQLException e) {
			return false;
		}
	}


	/*******
	 * <p> Method: boolean completePasswordReset(String username, String password) </p>
	 *
	 * <p> Description: Save a validated permanent password, remove any one-time credential, and
	 * clear the reset-required state.  All changes are performed by one database update so the
	 * account cannot be left partially reset.</p>
	 *
	 * @param username specifies the user completing the forced reset
	 *
	 * @param password specifies the validated new permanent password
	 *
	 * @return true if exactly one account was updated, else false
	 */
	public boolean completePasswordReset(String username, String password) {
		String query = "UPDATE userDB SET password = ?, oneTimePassword = NULL, " +
				"passwordResetRequired = FALSE WHERE userName = ? AND " +
				"passwordResetRequired = TRUE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, password);
			pstmt.setString(2, username);
			boolean success = pstmt.executeUpdate() == 1;
			if (success && username.equals(currentUsername))
				currentPassword = password;
			return success;
		} catch (SQLException e) {
			return false;
		}
	}

	
	/*******
	 * <p> Method: boolean updateUserRole(String username, String role, String value) </p>
	 * 
	 * <p> Description: Update a specified role for a specified user's and set and update all the
	 * 		current user attributes.</p>
	 * 
	 * @param username is the username of the user
	 *  
	 * @param role is string that specifies the role to update
	 * 
	 * @param value is the string that specified TRUE or FALSE for the role
	 * 
	 * @return true if the update was successful, else false
	 *  
	 */
	// Update a users role
	public boolean updateUserRole(String username, String role, String value) {
		if (username == null || username.isBlank() || role == null || value == null ||
				(!value.equals("true") && !value.equals("false"))) return false;

		String column;
		if (role.equals("Admin")) column = "adminRole";
		else if (role.equals("Role1")) column = "newRole1";
		else if (role.equals("Role2")) column = "newRole2";
		else return false;

		boolean roleValue = Boolean.parseBoolean(value);
		if (!roleValue && getUserRoleCount(username) <= 1) return false;

		String query = "UPDATE userDB SET " + column + " = ? WHERE username = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setBoolean(1, roleValue);
			pstmt.setString(2, username);
			if (pstmt.executeUpdate() != 1) return false;

			if (role.equals("Admin")) currentAdminRole = roleValue;
			else if (role.equals("Role1")) currentNewRole1 = roleValue;
			else currentNewRole2 = roleValue;
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/*******
	 * <p> Method: int getUserRoleCount(String username) </p>
	 *
	 * <p> Description: Counts the roles assigned to an account before a role is removed.</p>
	 *
	 * @param username specifies the account to inspect
	 * @return the number of assigned roles, or -1 if the account cannot be found
	 */
	private int getUserRoleCount(String username) {
		String query = "SELECT adminRole, newRole1, newRole2 FROM userDB WHERE username = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
			ResultSet rs = pstmt.executeQuery();
			if (!rs.next()) return -1;

			int count = 0;
			if (rs.getBoolean("adminRole")) count++;
			if (rs.getBoolean("newRole1")) count++;
			if (rs.getBoolean("newRole2")) count++;
			return count;
		} catch (SQLException e) {
			return -1;
		}
	}
	
	
	// Attribute getters for the current user
	/*******
	 * <p> Method: String getCurrentUsername() </p>
	 * 
	 * <p> Description: Get the current user's username.</p>
	 * 
	 * @return the username value is returned
	 *  
	 */
	public String getCurrentUsername() { return currentUsername;};

	
	/*******
	 * <p> Method: String getCurrentPassword() </p>
	 * 
	 * <p> Description: Get the current user's password.</p>
	 * 
	 * @return the password value is returned
	 *  
	 */
	public String getCurrentPassword() { return currentPassword;};

	
	/*******
	 * <p> Method: String getCurrentFirstName() </p>
	 * 
	 * <p> Description: Get the current user's first name.</p>
	 * 
	 * @return the first name value is returned
	 *  
	 */
	public String getCurrentFirstName() { return currentFirstName;};

	
	/*******
	 * <p> Method: String getCurrentMiddleName() </p>
	 * 
	 * <p> Description: Get the current user's middle name.</p>
	 * 
	 * @return the middle name value is returned
	 *  
	 */
	public String getCurrentMiddleName() { return currentMiddleName;};

	
	/*******
	 * <p> Method: String getCurrentLastName() </p>
	 * 
	 * <p> Description: Get the current user's last name.</p>
	 * 
	 * @return the last name value is returned
	 *  
	 */
	public String getCurrentLastName() { return currentLastName;};

	
	/*******
	 * <p> Method: String getCurrentPreferredFirstName( </p>
	 * 
	 * <p> Description: Get the current user's preferred first name.</p>
	 * 
	 * @return the preferred first name value is returned
	 *  
	 */
	public String getCurrentPreferredFirstName() { return currentPreferredFirstName;};

	
	/*******
	 * <p> Method: String getCurrentEmailAddress() </p>
	 * 
	 * <p> Description: Get the current user's email address name.</p>
	 * 
	 * @return the email address value is returned
	 *  
	 */
	public String getCurrentEmailAddress() { return currentEmailAddress;};

	
	/*******
	 * <p> Method: boolean getCurrentAdminRole() </p>
	 * 
	 * <p> Description: Get the current user's Admin role attribute.</p>
	 * 
	 * @return true if this user plays an Admin role, else false
	 *  
	 */
	public boolean getCurrentAdminRole() { return currentAdminRole;};

	
	/*******
	 * <p> Method: boolean getCurrentNewRole1() </p>
	 * 
	 * <p> Description: Get the current user's Student role attribute.</p>
	 * 
	 * @return true if this user plays a Student role, else false
	 *  
	 */
	public boolean getCurrentNewRole1() { return currentNewRole1;};

	
	/*******
	 * <p> Method: boolean getCurrentNewRole2() </p>
	 * 
	 * <p> Description: Get the current user's Reviewer role attribute.</p>
	 * 
	 * @return true if this user plays a Reviewer role, else false
	 *  
	 */
	public boolean getCurrentNewRole2() { return currentNewRole2;};


	/*-*******************************************************************************************

	HW2 lesson and experience persistence

	**********************************************************************************************/

	/*******
	 * <p> Method: long createLesson(LessonLearned lesson) </p>
	 *
	 * <p> Description: Insert a validated lesson and return its database-generated identifier.</p>
	 *
	 * @param lesson specifies the validated lesson values
	 * @return the generated lesson identifier
	 * @throws SQLException when the lesson cannot be inserted
	 */
	public long createLesson(LessonLearned lesson) throws SQLException {
		String query = "INSERT INTO lessons (lessonOwner, title, problemSituation, lessonText, " +
				"lessonLocked, titleLocked, problemLocked, lessonTextLocked) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(query,
				Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, lesson.getOwner());
			pstmt.setString(2, lesson.getTitle());
			pstmt.setString(3, lesson.getProblemSituation());
			pstmt.setString(4, lesson.getLessonLearned());
			pstmt.setBoolean(5, lesson.isLessonLocked());
			pstmt.setBoolean(6, lesson.isTitleLocked());
			pstmt.setBoolean(7, lesson.isProblemSituationLocked());
			pstmt.setBoolean(8, lesson.isLessonLearnedLocked());
			if (pstmt.executeUpdate() != 1)
				throw new SQLException("The lesson was not inserted.");
			try (ResultSet keys = pstmt.getGeneratedKeys()) {
				if (keys.next()) return keys.getLong(1);
			}
		}
		throw new SQLException("The lesson identifier was not generated.");
	}

	/*******
	 * <p> Method: LessonLearned getLesson(long id) </p>
	 *
	 * @param id specifies the lesson identifier
	 * @return the lesson, or null when it does not exist
	 * @throws SQLException when the lesson cannot be read
	 */
	public LessonLearned getLesson(long id) throws SQLException {
		String query = "SELECT id, lessonOwner, title, problemSituation, lessonText, " +
				"lessonLocked, titleLocked, problemLocked, lessonTextLocked " +
				"FROM lessons WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setLong(1, id);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) return lessonFromResult(rs);
			}
		}
		return null;
	}

	/*******
	 * <p> Method: List&lt;LessonLearned&gt; getLessonsByOwner(String owner) </p>
	 *
	 * <p> Description: Read only the lessons owned by one contributor.  This ownership condition
	 * is part of the SQL query so another contributor's content never enters the returned list.</p>
	 *
	 * @param owner specifies the contributor whose lessons are requested
	 * @return all matching lessons ordered by identifier
	 * @throws SQLException when the list cannot be read
	 */
	public List<LessonLearned> getLessonsByOwner(String owner) throws SQLException {
		List<LessonLearned> lessons = new ArrayList<LessonLearned>();
		String query = "SELECT id, lessonOwner, title, problemSituation, lessonText, " +
				"lessonLocked, titleLocked, problemLocked, lessonTextLocked " +
				"FROM lessons WHERE lessonOwner = ? ORDER BY id";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, owner);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) lessons.add(lessonFromResult(rs));
			}
		}
		return lessons;
	}

	/*******
	 * <p> Method: boolean updateLesson(LessonLearned lesson) </p>
	 *
	 * @param lesson specifies the validated values and existing identifier
	 * @return true when exactly one lesson was changed
	 * @throws SQLException when the update cannot be completed
	 */
	public boolean updateLesson(LessonLearned lesson) throws SQLException {
		String query = "UPDATE lessons SET title = ?, problemSituation = ?, lessonText = ? " +
				"WHERE id = ? AND lessonOwner = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, lesson.getTitle());
			pstmt.setString(2, lesson.getProblemSituation());
			pstmt.setString(3, lesson.getLessonLearned());
			pstmt.setLong(4, lesson.getId());
			pstmt.setString(5, lesson.getOwner());
			return pstmt.executeUpdate() == 1;
		}
	}

	/*******
	 * <p> Method: boolean deleteLesson(long id) </p>
	 *
	 * <p> Description: Delete one lesson.  Database foreign keys delete only that lesson's
	 * experience and effort rows in the same database transaction.</p>
	 *
	 * @param id specifies the lesson identifier
	 * @return true when one lesson was removed
	 * @throws SQLException when deletion cannot be completed
	 */
	public boolean deleteLesson(long id) throws SQLException {
		String query = "DELETE FROM lessons WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setLong(1, id);
			return pstmt.executeUpdate() == 1;
		}
	}

	/*******
	 * <p> Method: int getLessonCount() </p>
	 *
	 * @return the total number of stored lessons
	 * @throws SQLException when the count cannot be read
	 */
	public int getLessonCount() throws SQLException {
		try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM lessons")) {
			return rs.next() ? rs.getInt(1) : 0;
		}
	}

	/*******
	 * <p> Method: long createExperience(ExperienceRecord experience) </p>
	 *
	 * <p> Description: Insert one validated experience and all team efforts atomically.  A
	 * rollback prevents a partially saved experience if any effort row fails.</p>
	 *
	 * @param experience specifies the validated experience values
	 * @return the generated experience identifier
	 * @throws SQLException when the record cannot be inserted
	 */
	public long createExperience(ExperienceRecord experience) throws SQLException {
		boolean originalAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);
		try {
			String query = "INSERT INTO experienceRecords (lessonId, recordOwner, whatWasDone, " +
					"howItWasDone, elapsedMinutes, experienceLocked, whatLocked, howLocked, " +
					"elapsedLocked, effortLocked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			long id;
			try (PreparedStatement pstmt = connection.prepareStatement(query,
					Statement.RETURN_GENERATED_KEYS)) {
				pstmt.setLong(1, experience.getLessonId());
				pstmt.setString(2, experience.getOwner());
				pstmt.setString(3, experience.getWhatWasDone());
				pstmt.setString(4, experience.getHowItWasDone());
				pstmt.setDouble(5, experience.getElapsedMinutes());
				pstmt.setBoolean(6, experience.isExperienceLocked());
				pstmt.setBoolean(7, experience.isWhatWasDoneLocked());
				pstmt.setBoolean(8, experience.isHowItWasDoneLocked());
				pstmt.setBoolean(9, experience.isElapsedTimeLocked());
				pstmt.setBoolean(10, experience.isTeamEffortLocked());
				if (pstmt.executeUpdate() != 1)
					throw new SQLException("The experience was not inserted.");
				try (ResultSet keys = pstmt.getGeneratedKeys()) {
					if (!keys.next()) throw new SQLException(
							"The experience identifier was not generated.");
					id = keys.getLong(1);
				}
			}
			insertEfforts(id, experience.getTeamEfforts());
			connection.commit();
			return id;
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(originalAutoCommit);
		}
	}

	/*******
	 * <p> Method: ExperienceRecord getExperience(long id) </p>
	 *
	 * @param id specifies the experience identifier
	 * @return the matching record, or null when it does not exist
	 * @throws SQLException when the record cannot be read
	 */
	public ExperienceRecord getExperience(long id) throws SQLException {
		String query = "SELECT id, lessonId, recordOwner, whatWasDone, howItWasDone, " +
				"elapsedMinutes, experienceLocked, whatLocked, howLocked, elapsedLocked, " +
				"effortLocked FROM experienceRecords WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setLong(1, id);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) return experienceFromResult(rs);
			}
		}
		return null;
	}

	/*******
	 * <p> Method: List&lt;ExperienceRecord&gt; getExperiencesForLesson(long lessonId) </p>
	 *
	 * @param lessonId specifies the parent lesson
	 * @return related experience records ordered by identifier
	 * @throws SQLException when the records cannot be read
	 */
	public List<ExperienceRecord> getExperiencesForLesson(long lessonId) throws SQLException {
		List<ExperienceRecord> experiences = new ArrayList<ExperienceRecord>();
		String query = "SELECT id, lessonId, recordOwner, whatWasDone, howItWasDone, " +
				"elapsedMinutes, experienceLocked, whatLocked, howLocked, elapsedLocked, " +
				"effortLocked FROM experienceRecords WHERE lessonId = ? ORDER BY id";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setLong(1, lessonId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) experiences.add(experienceFromResult(rs));
			}
		}
		return experiences;
	}

	/*******
	 * <p> Method: boolean updateExperience(ExperienceRecord experience) </p>
	 *
	 * <p> Description: Update the record and replace its effort rows atomically.</p>
	 *
	 * @param experience specifies the validated replacement values
	 * @return true when one experience was changed
	 * @throws SQLException when the update cannot be completed
	 */
	public boolean updateExperience(ExperienceRecord experience) throws SQLException {
		boolean originalAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);
		try {
			String query = "UPDATE experienceRecords SET whatWasDone = ?, howItWasDone = ?, " +
					"elapsedMinutes = ? WHERE id = ? AND lessonId = ? AND recordOwner = ?";
			int changed;
			try (PreparedStatement pstmt = connection.prepareStatement(query)) {
				pstmt.setString(1, experience.getWhatWasDone());
				pstmt.setString(2, experience.getHowItWasDone());
				pstmt.setDouble(3, experience.getElapsedMinutes());
				pstmt.setLong(4, experience.getId());
				pstmt.setLong(5, experience.getLessonId());
				pstmt.setString(6, experience.getOwner());
				changed = pstmt.executeUpdate();
			}
			if (changed != 1) {
				connection.rollback();
				return false;
			}
			try (PreparedStatement pstmt = connection.prepareStatement(
					"DELETE FROM experienceEfforts WHERE experienceId = ?")) {
				pstmt.setLong(1, experience.getId());
				pstmt.executeUpdate();
			}
			insertEfforts(experience.getId(), experience.getTeamEfforts());
			connection.commit();
			return true;
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(originalAutoCommit);
		}
	}

	/*******
	 * <p> Method: boolean deleteExperience(long id) </p>
	 *
	 * @param id specifies the experience identifier
	 * @return true when one experience was removed
	 * @throws SQLException when deletion cannot be completed
	 */
	public boolean deleteExperience(long id) throws SQLException {
		try (PreparedStatement pstmt = connection.prepareStatement(
				"DELETE FROM experienceRecords WHERE id = ?")) {
			pstmt.setLong(1, id);
			return pstmt.executeUpdate() == 1;
		}
	}

	/*******
	 * @return the total number of stored experience records
	 * @throws SQLException when the count cannot be read
	 */
	public int getExperienceCount() throws SQLException {
		try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM experienceRecords")) {
			return rs.next() ? rs.getInt(1) : 0;
		}
	}

	// The following package-protected helpers seed Phase Three-owned state for HW2 tests.  They are
	// deliberately unavailable to the contributor UI, which must respect but not create locks.
	boolean setLessonLocksForTesting(long id, boolean lessonLocked, boolean titleLocked,
			boolean problemLocked, boolean lessonTextLocked) throws SQLException {
		String query = "UPDATE lessons SET lessonLocked = ?, titleLocked = ?, problemLocked = ?, " +
				"lessonTextLocked = ? WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setBoolean(1, lessonLocked);
			pstmt.setBoolean(2, titleLocked);
			pstmt.setBoolean(3, problemLocked);
			pstmt.setBoolean(4, lessonTextLocked);
			pstmt.setLong(5, id);
			return pstmt.executeUpdate() == 1;
		}
	}

	boolean updateLessonCoreForTesting(long id, String title, String problem,
			String lessonText) throws SQLException {
		String query = "UPDATE lessons SET title = ?, problemSituation = ?, lessonText = ? " +
				"WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, title);
			pstmt.setString(2, problem);
			pstmt.setString(3, lessonText);
			pstmt.setLong(4, id);
			return pstmt.executeUpdate() == 1;
		}
	}

	boolean setHiddenRoleInformationForTesting(long id, String information) throws SQLException {
		try (PreparedStatement pstmt = connection.prepareStatement(
				"UPDATE lessons SET hiddenRoleInformation = ? WHERE id = ?")) {
			pstmt.setString(1, information);
			pstmt.setLong(2, id);
			return pstmt.executeUpdate() == 1;
		}
	}

	String getHiddenRoleInformationForTesting(long id) throws SQLException {
		try (PreparedStatement pstmt = connection.prepareStatement(
				"SELECT hiddenRoleInformation FROM lessons WHERE id = ?")) {
			pstmt.setLong(1, id);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next() ? rs.getString(1) : null;
			}
		}
	}

	boolean setExperienceLocksForTesting(long id, boolean experienceLocked,
			boolean whatLocked, boolean howLocked, boolean elapsedLocked,
			boolean effortLocked) throws SQLException {
		String query = "UPDATE experienceRecords SET experienceLocked = ?, whatLocked = ?, " +
				"howLocked = ?, elapsedLocked = ?, effortLocked = ? WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setBoolean(1, experienceLocked);
			pstmt.setBoolean(2, whatLocked);
			pstmt.setBoolean(3, howLocked);
			pstmt.setBoolean(4, elapsedLocked);
			pstmt.setBoolean(5, effortLocked);
			pstmt.setLong(6, id);
			return pstmt.executeUpdate() == 1;
		}
	}

	private LessonLearned lessonFromResult(ResultSet rs) throws SQLException {
		return new LessonLearned(rs.getLong("id"), rs.getString("lessonOwner"),
				rs.getString("title"), rs.getString("problemSituation"),
				rs.getString("lessonText"), rs.getBoolean("lessonLocked"),
				rs.getBoolean("titleLocked"), rs.getBoolean("problemLocked"),
				rs.getBoolean("lessonTextLocked"));
	}

	private ExperienceRecord experienceFromResult(ResultSet rs) throws SQLException {
		long id = rs.getLong("id");
		return new ExperienceRecord(id, rs.getLong("lessonId"), rs.getString("recordOwner"),
				rs.getString("whatWasDone"), rs.getString("howItWasDone"),
				rs.getDouble("elapsedMinutes"), getEfforts(id),
				rs.getBoolean("experienceLocked"), rs.getBoolean("whatLocked"),
				rs.getBoolean("howLocked"), rs.getBoolean("elapsedLocked"),
				rs.getBoolean("effortLocked"));
	}

	private List<TeamMemberEffort> getEfforts(long experienceId) throws SQLException {
		List<TeamMemberEffort> efforts = new ArrayList<TeamMemberEffort>();
		String query = "SELECT memberName, effortMinutes FROM experienceEfforts " +
				"WHERE experienceId = ? ORDER BY entryOrder";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setLong(1, experienceId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) efforts.add(new TeamMemberEffort(rs.getString("memberName"),
						rs.getDouble("effortMinutes")));
			}
		}
		return efforts;
	}

	private void insertEfforts(long experienceId, List<TeamMemberEffort> efforts)
			throws SQLException {
		String query = "INSERT INTO experienceEfforts (experienceId, entryOrder, memberName, " +
				"effortMinutes) VALUES (?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			for (int index = 0; index < efforts.size(); index++) {
				TeamMemberEffort effort = efforts.get(index);
				pstmt.setLong(1, experienceId);
				pstmt.setInt(2, index);
				pstmt.setString(3, effort.getMemberName());
				pstmt.setDouble(4, effort.getEffortMinutes());
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		}
	}

	
	/*******
	 * <p> Debugging method</p>
	 * 
	 * <p> Description: Debugging method that dumps the database of the console.</p>
	 * 
	 * @throws SQLException if there is an issues accessing the database.
	 * 
	 */
	// Dumps the database.
	public void dump() throws SQLException {
		String query = "SELECT * FROM userDB";
		ResultSet resultSet = statement.executeQuery(query);
		ResultSetMetaData meta = resultSet.getMetaData();
		while (resultSet.next()) {
		for (int i = 0; i < meta.getColumnCount(); i++) {
		System.out.println(
		meta.getColumnLabel(i + 1) + ": " +
				resultSet.getString(i + 1));
		}
		System.out.println();
		}
		resultSet.close();
	}


	/*******
	 * <p> Method: void closeConnection()</p>
	 * 
	 * <p> Description: Closes the database statement and connection.</p>
	 * 
	 */
	// Closes the database statement and connection.
	public void closeConnection() {
		try{ 
			if(statement!=null) statement.close(); 
		} catch(SQLException se2) { 
			se2.printStackTrace();
		} 
		try { 
			if(connection!=null) connection.close(); 
		} catch(SQLException se){ 
			se.printStackTrace(); 
		} 
	}
}
