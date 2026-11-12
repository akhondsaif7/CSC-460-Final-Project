/* ------------------- class BookmarkQueries ------------------------ *
 * Class: CSC460 - S26
 * Author: Matteo Galiano, Isa Abdulrazaq
 * Assg: Project 4
 * Packages: java.sql.*
 * Purpose: Query/report menu for required assignment queries. User can
 * type a number into the query menu, and then the relevant information
 * will be queried to the user. 
 * ------------------------------------------------------ */

import java.sql.*;

public class BookmarkQueries {

    /* ------------ BookmarkQueries() ---------------
     * - Purpose: private constructor so nobody can instantiate this utility class
     * - Pre: class has been loaded by the JVM
     * - Post: direct object construction is blocked for this class
     * - Params:
     *      -> None
     * - Returns:
     *      -> None
     * ------------------------------------------------- */
    private BookmarkQueries() {
    }

    /* ------------ showMenu(Connection conn, ConsoleIO io) ---------------
     * - Purpose: display the query menu and route each option to its query method
     * - Pre: database connection is active and input helper exists
     * - Post: user runs one or more queries, or exits this submenu
     * - Params:
     *      -> Connection conn: active Oracle connection shared by the app
     *      -> ConsoleIO io: helper used to read menu input from the terminal
     * - Returns:
     *      -> None
     * ------------------------------------------------- */
    public static void showMenu(Connection conn, ConsoleIO io) {
        boolean done = false; // submenu exit flag
        while (!done) {
            System.out.println();
            System.out.println("------------- Queries -------------");
            System.out.println("1. Bookmarked messages for a user");
            System.out.println("2. Most helpful persona");
            System.out.println("3. All users with unpaid invoices");
            System.out.println("4. Bookmarked messages by workspace");
            System.out.println("5. Back");

            int choice = io.readIntInRange("Choose an option: ", 1, 5); // bounded menu choice
            try {
                switch (choice) {
                    case 1:
                        listBookmarkedMessages(conn, io); // required query 1
                        io.pause(); // wait so user can read output
                        break;
                    case 2:
                        showMostHelpfulPersona(conn); // required persona query
                        io.pause();
                        break;
                    case 3:
                        allUnpaidInvoices(conn); // required unpaid invoices query
                        io.pause();
                        break;
                    case 4:
                        listBookmarkedMessagesByWorkspace(conn, io); // additional non-trivial query
                        io.pause();
                        break;
                    case 5:
                        done = true; // leave query submenu
                        break;
                    default:
                        System.out.println("Unknown option.");
                        break;
                }
            } catch (SQLException e) { // any SQL failure in selected query
                System.err.println("Query failed.");
                DatabaseUtil.printSqlError(e); // detailed SQL diagnostics
            }
        }
    }

    /* ------------ listBookmarkedMessages(Connection conn, ConsoleIO io) ---------------
     * - Purpose: list every bookmarked message for one selected user
     * - Pre: Bookmark/Message/Conversation tables exist and user chooses a UsrID
     * - Post: matching bookmark rows are printed, or empty-state message is shown
     * - Params:
     *      -> Connection conn: active Oracle connection shared by the app
     *      -> ConsoleIO io: helper used to read user id input from terminal
     * - Returns:
     *      -> None
     * ------------------------------------------------- */
    private static void listBookmarkedMessages(Connection conn, ConsoleIO io)
        throws SQLException {
        System.out.println();
        UserAccounts.listUsers(conn); // show valid users before prompting for id
        long userId = io.readLong("UsrID whose bookmarks should be listed: "); // target user
        if (!DatabaseUtil.userExists(conn, userId)) { // validate the supplied id first
            System.out.println("No user exists with UsrID " + userId + ".");
            return; // stop query when UsrID is invalid
        }

        // Join Bookmark -> Message -> Conversation to print bookmark context.
        String sql =
            "SELECT c.title AS conversation_title, m.MsgID, m.sender, " +
            "       DBMS_LOB.SUBSTR(m.content, 120, 1) AS content_preview, " +
            "       m.sent_at, b.bookmarked_at " +
            "FROM Bookmark b " +
            "JOIN Message m ON m.MsgID = b.MsgID " +
            "JOIN Conversation c ON c.CID = m.CID " +
            "WHERE b.UsrID = ? " +
            "ORDER BY b.bookmarked_at DESC, m.sent_at DESC, m.MsgID DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) { // compile SQL safely
            stmt.setLong(1, userId); // bind selected user id
            try (ResultSet rs = stmt.executeQuery()) { // execute and iterate results
                System.out.println();
                System.out.println("Bookmarked messages for UsrID " + userId);
                System.out.printf(
                    "%-30s %-8s %-14s %-20s %-20s %-42s%n",
                    "Conversation",
                    "MsgID",
                    "Sender",
                    "Message Time",
                    "Bookmarked",
                    "Message"
                );
                System.out.println(
                    "----------------------------------------------------------------------------------------------------------------"
                );

                boolean found = false; // tracks whether any row was returned
                while (rs.next()) { // print one row per bookmark
                    found = true;
                    System.out.printf(
                        "%-30s %-8d %-14s %-20s %-20s %-42s%n",
                        DatabaseUtil.abbreviate(
                            DatabaseUtil.valueOrDash(rs.getString("conversation_title")),
                            30
                        ),
                        rs.getLong("MsgID"),
                        DatabaseUtil.abbreviate(rs.getString("sender"), 14),
                        formatTimestamp(rs.getTimestamp("sent_at")),
                        formatTimestamp(rs.getTimestamp("bookmarked_at")),
                        DatabaseUtil.abbreviate(rs.getString("content_preview"), 42)
                    );
                }

                if (!found) { // explicit empty-state message for this user
                    System.out.println("This user has no bookmarked messages.");
                }
            }
        }
    }

    /* ------------ showMostHelpfulPersona(Connection conn) ---------------
     * - Purpose: find the persona with the highest thumbs-up percentage
     * - Pre: Persona/Conversation/Message/MsgRating tables contain feedback rows
     * - Post: top persona summary row is printed, or no-data message is shown
     * - Params:
     *      -> Connection conn: active Oracle connection shared by the app
     * - Returns:
     *      -> None
     * ------------------------------------------------- */
    private static void showMostHelpfulPersona(Connection conn) throws SQLException {
        // Aggregate thumbs-up ratio and pick the top row only.
        String sql =
            "SELECT persona_name, thumbs_up_percent, thumbs_up_count, feedback_count " +
            "FROM ( " +
            "    SELECT p.name AS persona_name, " +
            "           ROUND(100 * SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) / COUNT(*), 2) " +
            "               AS thumbs_up_percent, " +
            "           SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) AS thumbs_up_count, " +
            "           COUNT(*) AS feedback_count " +
            "    FROM Persona p " +
            "    JOIN Conversation c ON c.personaID = p.personaID AND c.UsrID = p.UsrID " +
            "    JOIN Message m ON m.CID = c.CID " +
            "    JOIN MsgRating r ON r.MsgID = m.MsgID " +
            "    WHERE r.rating IN (0, 1) " +
            "    GROUP BY p.personaID, p.name " +
            "    ORDER BY thumbs_up_percent DESC, feedback_count DESC, p.name " +
            ") " +
            "WHERE ROWNUM = 1";

        try (
            PreparedStatement stmt = conn.prepareStatement(sql); // prepare ranking query
            ResultSet rs = stmt.executeQuery() // run ranking query
        ) {
            System.out.println();
            System.out.println("Most Helpful Persona");
            if (rs.next()) { // top row exists
                System.out.printf(
                    "%-30s %-18s %-14s %-14s%n",
                    "Persona",
                    "Thumbs Up %",
                    "Thumbs Up",
                    "Feedback"
                );
                System.out.println("----------------------------------------------------------------------");
                System.out.printf(
                    "%-30s %-18.2f %-14d %-14d%n",
                    DatabaseUtil.abbreviate(rs.getString("persona_name"), 30),
                    rs.getDouble("thumbs_up_percent"),
                    rs.getLong("thumbs_up_count"),
                    rs.getLong("feedback_count")
                );
            } else { // no rating data available to rank personas
                System.out.println("Not enough feedback to identify the most helpful persona.");
            }
        }
    }

    /* ------------ listBookmarkedMessagesByWorkspace(Connection conn, ConsoleIO io) ---------------
     * - Purpose: list bookmarked messages tied to conversations from one workspace
     * - Pre: WorkspaceMember/User/Conversation/Message/Bookmark relations are populated
     * - Post: matching rows are printed for selected workspace, or empty-state message appears
     * - Params:
     *      -> Connection conn: active Oracle connection shared by the app
     *      -> ConsoleIO io: helper used to read workspace id from terminal
     * - Returns:
     *      -> None
     * ------------------------------------------------- */
    private static void listBookmarkedMessagesByWorkspace(Connection conn, ConsoleIO io)
        throws SQLException {

        System.out.println();
        listWorkspaces(conn);
        long workspaceId = io.readLong("Workspace ID: "); // workspace selector input

        // Non-trivial join across membership, conversations, messages, and bookmarks.
        String sql =
            "SELECT " +
            "    b.UsrID AS bookmarking_user_id, " +
            "    bu.full_name AS bookmarking_user_name, " +
            "    c.UsrID AS conversation_owner_id, " +
            "    cu.full_name AS conversation_owner_name, " +
            "    m.MsgID, " +
            "    DBMS_LOB.SUBSTR(m.content, 120, 1) AS content_preview, " +
            "    b.bookmarked_at " +
            "FROM WorkspaceMember wm " +
            "JOIN \"User\" cu ON cu.UsrID = wm.UsrID " +
            "JOIN Conversation c ON c.UsrID = cu.UsrID " +
            "JOIN Message m ON m.CID = c.CID " +
            "JOIN Bookmark b ON b.MsgID = m.MsgID " +
            "JOIN \"User\" bu ON bu.UsrID = b.UsrID " +
            "WHERE wm.wsID = ? " +
            "ORDER BY b.bookmarked_at DESC, m.MsgID";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) { // compile workspace query
            stmt.setLong(1, workspaceId); // bind requested workspace id

            try (ResultSet rs = stmt.executeQuery()) { // execute query and read each row
                System.out.println();
                System.out.println("Bookmarked messages for workspace " + workspaceId);
                System.out.printf(
                    "%-8s %-22s %-8s %-22s %-8s %-36s %-20s%n",
                    "BmkrID", "Bookmarking User", "OwnerID", "Conversation Owner",
                    "MsgID", "Message Preview", "Bookmarked At"
                );
                System.out.println(
                    "---------------------------------------------------------------" +
                    "--------------------------------------------------------------------------------"
                );

                boolean found = false; // tracks whether query produced at least one row
                while (rs.next()) { // print each bookmark row for this workspace
                    found = true;
                    System.out.printf(
                        "%-8d %-22s %-8d %-22s %-8d %-36s %-20s%n",
                        rs.getLong("bookmarking_user_id"),
                        DatabaseUtil.abbreviate(rs.getString("bookmarking_user_name"), 22),
                        rs.getLong("conversation_owner_id"),
                        DatabaseUtil.abbreviate(rs.getString("conversation_owner_name"), 22),
                        rs.getLong("MsgID"),
                        DatabaseUtil.abbreviate(rs.getString("content_preview"), 36),
                        formatTimestamp(rs.getTimestamp("bookmarked_at"))
                    );
                }

                if (!found) { // clear message when workspace has no matching bookmarks
                    System.out.println("No bookmarked messages found for that workspace.");
                }
            }
        }
    }

    /**
     * Performs a simple query on the database to collect the details of all users
     * with unpaid invoices along with the date of their last conversation.
     * Users with unpaid invoices but no conversations are also displayed
     * Pre-Conditions:
     *  Users with unpaid invoices exist
     * Post-Conditions
     *  Displays the UserID, name, amount owed, email and last conversation date of all debtors
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     */
    public static void allUnpaidInvoices(Connection conn) throws SQLException {
        // we can find all users with unpaid invoices along with the sum total of said invoices in one query.
        try (Statement allUnpaid = conn.createStatement()) {
            ResultSet unpaidDetails = allUnpaid.executeQuery("select UsrID, total_owed, full_name, email, last_convo from " + 
            // subquery to find the sum total of all unpaid invoices
            " (select UsrID, sum(amount) as total_owed from Invoice where status='open' GROUP BY UsrID) " + 
            // sub query to find the date of the last conversation the user had (if any)
            // if none exists, the user is still displayed but a NULL value is returned for the last_convo field
            " join \"User\" USING (UsrID) LEFT OUTER JOIN (select UsrID, max(Conversation.created_at) as last_convo from Conversation group by UsrID) USING (UsrID)");
            System.out.println("All Unpaid Invoices: ");
            System.out.printf(
                "%-10s %-20s %-20s %-30s %-20s%n",
                "User ID",
                "Total Owed",
                "Full Name",
                "Email",
                "Last Conversation Created On"
            );
            System.out.println(
                "------------------------------------------------------------------------------------------------"
            );
            while (unpaidDetails.next()) {
                System.out.printf(
                "%-10s %-20s %-20s %-30s %-20s%n",
                unpaidDetails.getLong("UsrID"),
                unpaidDetails.getInt("total_owed"),
                unpaidDetails.getString("full_name"),
                unpaidDetails.getString("email"),
                DatabaseUtil.valueOrDash(unpaidDetails.getDate("last_convo").toString())
            );
            }
            System.out.println(
                "------------------------------------------------------------------------------------------------"
            );
            allUnpaid.close();
        }
        
    }

    /* ------------ formatTimestamp(Timestamp timestamp) ---------------
     * - Purpose: convert a SQL Timestamp into safe terminal display text
     * - Pre: timestamp value may be null or non-null
     * - Post: caller receives either '-' for null or the timestamp string value
     * - Params:
     *      -> Timestamp timestamp: timestamp value from a SQL result set
     * - Returns:
     *      -> String: display-ready timestamp text
     * ------------------------------------------------- */
    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-"; // placeholder shown when timestamp is null
        }
        return timestamp.toString(); // default JDBC string representation
    }
        /* -------------------------- listWorkspaces --------------------------
    * - Purpose: prints all workspaces so the user can choose a valid wsID.
    * - Pre: Workspace table exists
    * - Post: workspace rows displayed, or empty-table message printed
    * - Params:
    *      -> Connection conn: active Oracle connection
    * - Returns: None
    * ------------------------------------------------------------------- */
    private static void listWorkspaces(Connection conn) throws SQLException {
        String sql =
            "SELECT wsID, owner, created_at, preferred_language, plan " +
            "FROM Workspace ORDER BY wsID";

        try (
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            System.out.println();
            System.out.printf("%-8s %-8s %-22s %-18s %-14s%n",
                    "wsID", "Owner", "Created", "Language", "Plan");
            System.out.println("--------------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-8d %-8d %-22s %-18s %-14s%n",
                        rs.getLong("wsID"),
                        rs.getLong("owner"),
                        formatTimestamp(rs.getTimestamp("created_at")),
                        DatabaseUtil.valueOrDash(rs.getString("preferred_language")),
                        rs.getString("plan"));
            }

            if (!found) {
                System.out.println("No workspaces found.");
            }
        }
    }
}
