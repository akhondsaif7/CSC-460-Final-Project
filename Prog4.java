/* ------------------- Prog4 ------------------------ *
 * Class: CSC460 - S26
 * Authors: Matteo Galiano, Cole Hageman, Isa Abdulrazaq, Akhond Saif Al Masud
 * Assg: Project 4
 * Instructor & TAs: Lester I. McCann, Jianwei Shen, Muhammad Bilal
 * Packages: java.sql.Connection, java.sql.DriverManager,
 *           java.sql.SQLException, java.util.Scanner
 * Purpose: this program is the entry point for the Project 4 JDBC terminal
 * application. It loads the Oracle JDBC driver, opens one Oracle connection,
 * creates the shared terminal input helper, and displays the main menu that
 * routes the user to account management, conversation/message handling, or
 * the required bookmarked-message query.
 * Language: Java 25
 * Methods:
 *  - main: main brain of the program
 *  - runMainMenu: shows the top level terminal menu and routes choices
 * ------------------------------------------------------ */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Prog4 {

    // Oracle URL on lectura/aloe for the CSC460 Oracle server
    private static final String ORACLE_URL =
        "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";

    // Hardcoded Oracle credentials for this project account
    private static final String USERNAME = ""; // add your own
    private static final String PASSWORD = ""; // add your own

    /* ------------------------------ main ---------------------------------
     * - Purpose: main method where the program enters. It loads the Oracle
     * JDBC driver, connects to Oracle with the hardcoded credentials, creates
     * a ConsoleIO object for terminal input, and then calls the main menu.
     * - Pre: ojdbc8.jar is on the classpath and Oracle credentials are valid
     * - Post: program done after the user exits the menu
     * - Params:
     *      -> String[] args: command line args, not used here
     * - Returns: None (its a main)
     * --------------------------------------------------------------------- */
    public static void main(String[] args) {
        if (!DatabaseUtil.loadOracleDriver()) {     // Stop if driver cannot load
            return;
        }

        // Open the database connection and the terminal scanner together
        try (
            Connection conn = DriverManager.getConnection(ORACLE_URL, USERNAME, PASSWORD);
            Scanner scanner = new Scanner(System.in)
        ) {
            ConsoleIO io = new ConsoleIO(scanner);  // Shared input helper
            System.out.println("Connected to Oracle successfully.");
            runMainMenu(conn, io);                  // Start terminal UI
        } catch (SQLException e) {                  // Connection or SQL failure
            System.err.println("Database connection failed.");
            DatabaseUtil.printSqlError(e);          // Print readable SQL info
        }
    }

    /* -------------------------- runMainMenu ---------------------------
     * - Purpose: top level terminal menu for the whole JDBC application. It
     * keeps asking the user what section they want, and sends the shared
     * database connection/input helper to the class that owns that section.
     * - Pre: database connection is open and ConsoleIO exists
     * - Post: user has either used feature menus or exited the program
     * - Params:
     *      -> Connection conn: active Oracle connection used by all features
     *      -> ConsoleIO io: terminal input helper used for menu choices
     * - Returns: None
     * -------------------------------------------------------------------- */
    private static void runMainMenu(Connection conn, ConsoleIO io) {
        boolean done = false;
        while (!done) {
            System.out.println();
            System.out.println("======================================================");
            System.out.println(" CSC460 Project 4 Terminal Menu. Type the number you want. ");
            System.out.println("======================================================");
            System.out.println("1. Manage user accounts");
            System.out.println("2. Handle conversations and messages");
            System.out.println("3. Manage prompt library");
            System.out.println("4. Handle support tickets");            
            System.out.println("5. Show persona menu");
            System.out.println("6. Subscription Tracking");
            System.out.println("7. Billing Operations");
            System.out.println("8. See workspace info");
            System.out.println("9. Queries and reports");
            System.out.println("10. Exit");
            int choice = io.readIntInRange("Choose an option: ", 1, 10); // Valid menu pick
          
            switch (choice) {
                case 1:
                    UserAccounts.showMenu(conn, io);
                    break;
                case 2:
                    Conversations.showMenu(conn, io);
                    break;
                case 3:
                    PromptLibrary.showMenu(conn, io);
                    break;
                case 4:
                    SupportTicketLifecycle.showMenu(conn, io);
                    break;
                case 5:
                    PersonaManagement.showMenu(conn, io);
                    break;
                case 6:
                    PlanManagement.showMenu(conn, io);       // manage user subscription tiers
                    break;
                case 7:
                    Invoice.showMenu(conn, io);             // Create and pay for invoices
                    break;
                case 8:
                    WorkspaceManagement.showMenu(conn, io);
                    break;
                case 9:
                    BookmarkQueries.showMenu(conn, io);     // Required query section
                    break;
                case 10:
                    done = true;                            // Leave program
                    break;
                default:
                    System.out.println("Unknown option.");
                    break;
            }
        }

        System.out.println("Goodbye.");
    }
}
