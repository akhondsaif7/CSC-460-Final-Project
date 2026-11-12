/* ---------------- SupportTicketLifecycle ------------------- *
 * Class: CSC460 - S26
 * Author: Akhond Saif Al Masud
 * Assg: Project 4
 * Instructor & TAs: Lester I. McCann, Jianwei Shen, Muhammad Bilal
 * Packages: java.sql.Connection, java.sql.PreparedStatement,
 *           java.sql.ResultSet, java.sql.SQLException, java.sql.Timestamp
 * Purpose: this class manages the support-ticket lifecycle for the JDBC
 * terminal application. It can create a support ticket for a user, assign
 * or reassign it to a support agent, update ticket status, close a ticket,
 * and display the elapsed resolution time when a ticket is completed.
 * Language: Java 25
 * Methods:
 *  - showMenu: displays the support-ticket submenu
 *  - listTickets: prints all support tickets
 *  - createTicket: inserts a new support ticket
 *  - assignTicket: changes the assigned support agent
 *  - updateTicketStatus: changes a ticket between open/pending/closed
 *  - closeTicket: closes a ticket and prints resolution duration
 *  - printTicketDetail: prints one ticket row in full
 *  - supportTicketExists: validates ticketID input
 *  - supportAgentExists: validates sno input
 *  - formatTimestamp: converts Timestamp to display text
 *  - printResolutionDuration: prints elapsed time from created_at to closed_at
 *  - rollbackQuietly: rolls back a failed transaction when needed
 * ----------------------------------------------------------- */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SupportTicketLifecycle {

    /* ------------------- SupportTicketLifecycle -------------------
     * - Purpose: private constructor so nobody creates this utility class.
     * ---------------------------------------------------------------- */
    private SupportTicketLifecycle() {
    }

    /* -------------------------- showMenu ---------------------------
     * - Purpose: displays the support-ticket submenu and routes each
     * choice to the method that owns that ticket operation.
     * - Pre: database connection is open and ConsoleIO exists
     * - Post: user completed ticket work or returned to previous menu
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ---------------------------------------------------------------- */
    public static void showMenu(Connection conn, ConsoleIO io) {
        boolean done = false;

        while (!done) { // print options
            System.out.println();
            System.out.println("-------- Support Ticket Lifecycle --------");
            System.out.println("1. Create support ticket");
            System.out.println("2. Assign / reassign ticket agent");
            System.out.println("3. Update ticket status");
            System.out.println("4. Close ticket and show duration");
            System.out.println("5. List tickets");
            System.out.println("6. Back");

            int choice = io.readIntInRange("Choose an option: ", 1, 6);
            try {
                switch (choice) { // respond to user's choice
                    case 1:
                        createTicket(conn, io);
                        break;
                    case 2:
                        assignTicket(conn, io);
                        break;
                    case 3:
                        updateTicketStatus(conn, io);
                        break;
                    case 4:
                        closeTicket(conn, io);
                        break;
                    case 5:
                        listTickets(conn);
                        io.pause();
                        break;
                    case 6:
                        done = true;
                        break;
                    default:
                        System.out.println("Unknown option.");
                        break;
                }
            } catch (SQLException e) {
                System.err.println("Support-ticket operation failed.");
                DatabaseUtil.printSqlError(e);
                rollbackQuietly(conn);
            }
        }
    }

    /* ------------------------- listTickets -------------------------
     * - Purpose: prints all support tickets with key lifecycle fields.
     * - Pre: SupportTicket table exists
     * - Post: ticket rows displayed, or empty-table message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ---------------------------------------------------------------- */
    public static void listTickets(Connection conn) throws SQLException {
        String sql =
            "SELECT ticketID, UsrID, sno, topic, status, created_at, closed_at " +
            "FROM SupportTicket ORDER BY ticketID";

        try (
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            System.out.println();
            System.out.printf("%-8s %-8s %-8s %-28s %-10s %-22s %-22s%n",
                    "ticketID", "UsrID", "sno", "Topic", "Status", "Created", "Closed");
            System.out.println("----------------------------------------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) { // at least one more row exists in the resut
                found = true;
                System.out.printf("%-8d %-8d %-8d %-28s %-10s %-22s %-22s%n",
                        rs.getLong("ticketID"),
                        rs.getLong("UsrID"),
                        rs.getLong("sno"),
                        DatabaseUtil.abbreviate(rs.getString("topic"), 28),
                        rs.getString("status"),
                        formatTimestamp(rs.getTimestamp("created_at")),
                        formatTimestamp(rs.getTimestamp("closed_at")));
            }

            if (!found) {
                System.out.println("No support tickets found.");
            }
        }
    }

    /* ------------------------- createTicket ------------------------
     * - Purpose: inserts a new support ticket for a selected user and
     * assigns it to a selected support agent. New tickets begin as open.
     * - Pre: selected user and support agent exist
     * - Post: SupportTicket row inserted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ---------------------------------------------------------------- */
    private static void createTicket(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        UserAccounts.listUsers(conn);

        long userId = io.readLong("UsrID for the new ticket: ");
        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }
        listSupportAgents(conn);
        long agentNo = io.readLong("Assigned support-agent sno: ");
        if (!supportAgentExists(conn, agentNo)) {
            System.out.println("No support agent exists with sno " + agentNo + ".");
            return;
        }
        // generate ticketID
        long ticketId = DatabaseUtil.nextId(conn, "SupportTicket", "ticketID");
        String topic = io.readRequiredString("Ticket topic: ");

        String sql =
            "INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status) " +
            "VALUES (?, ?, ?, ?, 'open')";
        // add ticket
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ticketId);
            stmt.setLong(2, userId);
            stmt.setLong(3, agentNo);
            stmt.setString(4, topic);
            stmt.executeUpdate();
        }

        System.out.println("Support ticket added with ticketID " + ticketId + ".");
    }

    /* ------------------------- assignTicket ------------------------
     * - Purpose: reassigns an existing ticket to a different support agent.
     * - Pre: selected ticket and support agent exist
     * - Post: SupportTicket.sno updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ---------------------------------------------------------------- */
    private static void assignTicket(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        // shows the snos of support agents by printing all tickets
        listTickets(conn);

        long ticketId = io.readLong("ticketID to assign: ");
        if (!supportTicketExists(conn, ticketId)) {
            System.out.println("No support ticket exists with ticketID " + ticketId + ".");
            return;
        }
        listSupportAgents(conn);
        long agentNo = io.readLong("New support-agent sno: ");
        if (!supportAgentExists(conn, agentNo)) {
            System.out.println("No support agent exists with sno " + agentNo + ".");
            return;
        }

        String sql = "UPDATE SupportTicket SET sno = ? WHERE ticketID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, agentNo);
            stmt.setLong(2, ticketId);
            stmt.executeUpdate();
        }

        System.out.println("Ticket agent updated.");
    }

    /* ---------------------- updateTicketStatus ---------------------
     * - Purpose: changes the status of an existing ticket. If the chosen
     * status is closed, this method also sets closed_at to SYSTIMESTAMP.
     * Reopening a closed ticket clears closed_at.
     * - Pre: selected ticket exists
     * - Post: ticket status updated consistently with table constraints
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ---------------------------------------------------------------- */
    private static void updateTicketStatus(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        listTickets(conn);

        long ticketId = io.readLong("ticketID to update: ");
        if (!supportTicketExists(conn, ticketId)) {
            System.out.println("No support ticket exists with ticketID " + ticketId + ".");
            return;
        }

        System.out.println();
        System.out.println("New status");
        System.out.println("1. open");
        System.out.println("2. pending");
        System.out.println("3. closed");

        int choice = io.readIntInRange("Choose status: ", 1, 3);

        String status;
        switch (choice) { // respond to user's choice
            case 1:
                status = "open";
                break;
            case 2:
                status = "pending";
                break;
            case 3:
                status = "closed";
                break;
            default:
                System.out.println("Unknown status.");
                return;
        }

        String sql;
        if ("closed".equals(status)) { // closes ticlet
            sql = "UPDATE SupportTicket SET status = 'closed', closed_at = SYSTIMESTAMP " +
                  "WHERE ticketID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, ticketId);
                stmt.executeUpdate();
            }
        } else { // changes status to open or pending
            sql = "UPDATE SupportTicket SET status = ?, closed_at = NULL WHERE ticketID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setLong(2, ticketId);
                stmt.executeUpdate();
            }
        }

        System.out.println("Ticket status updated.");
    }

    /* -------------------------- closeTicket ------------------------
     * - Purpose: closes one selected support ticket, stamps closed_at,
     * prints the completed ticket details, and displays resolution time.
     * - Pre: selected ticket exists
     * - Post: ticket closed and duration displayed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ---------------------------------------------------------------- */
    private static void closeTicket(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        listTickets(conn); // lists tickets so user can see snos

        long ticketId = io.readLong("ticketID to close: ");
        if (!supportTicketExists(conn, ticketId)) {
            System.out.println("No support ticket exists with ticketID " + ticketId + ".");
            return;
        }

        String sql =
            "UPDATE SupportTicket " +
            "SET status = 'closed', closed_at = SYSTIMESTAMP " +
            "WHERE ticketID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ticketId);
            stmt.executeUpdate();
        }

        System.out.println("Ticket closed.");
        printTicketDetail(conn, ticketId);
    }

    /* ----------------------- printTicketDetail ---------------------
     * - Purpose: prints one selected ticket in full and shows resolution
     * duration if the ticket has been closed.
     * - Pre: selected ticket exists
     * - Post: ticket information displayed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long ticketId: ticketID to display
     * - Returns: None
     * ---------------------------------------------------------------- */
    private static void printTicketDetail(Connection conn, long ticketId) throws SQLException {
        String sql =
            "SELECT ticketID, UsrID, sno, topic, status, created_at, closed_at " +
            "FROM SupportTicket WHERE ticketID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ticketId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp created = rs.getTimestamp("created_at");
                    Timestamp closed = rs.getTimestamp("closed_at");

                    System.out.println();
                    System.out.println("Current support ticket");
                    System.out.println("ticketID   " + rs.getLong("ticketID"));
                    System.out.println("UsrID      " + rs.getLong("UsrID"));
                    System.out.println("sno        " + rs.getLong("sno"));
                    System.out.println("Topic      " + rs.getString("topic"));
                    System.out.println("Status     " + rs.getString("status"));
                    System.out.println("Created    " + formatTimestamp(created));
                    System.out.println("Closed     " + formatTimestamp(closed));

                    if (closed != null) {
                        printResolutionDuration(created, closed);
                    }
                }
            }
        }
    }

    /* ---------------------- supportTicketExists --------------------
     * - Purpose: checks whether one ticketID exists in SupportTicket.
     * - Pre: database connection is open
     * - Post: caller knows whether ticket exists
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long ticketId: ticketID to validate
     * - Returns:
     *      -> boolean true if the ticket exists
     * ---------------------------------------------------------------- */
    private static boolean supportTicketExists(Connection conn, long ticketId)
            throws SQLException {

        String sql = "SELECT COUNT(*) FROM SupportTicket WHERE ticketID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ticketId);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    /* ----------------------- supportAgentExists --------------------
     * - Purpose: checks whether one support-agent sno exists.
     * - Pre: database connection is open
     * - Post: caller knows whether agent exists
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long supportNo: sno to validate
     * - Returns:
     *      -> boolean true if the support agent exists
     * ---------------------------------------------------------------- */
    private static boolean supportAgentExists(Connection conn, long supportNo)
            throws SQLException {

        String sql = "SELECT COUNT(*) FROM SupportAgent WHERE sno = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, supportNo);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    /* ---------------------- printResolutionDuration ----------------
     * - Purpose: computes and prints elapsed time from ticket creation
     * to ticket closing using the stored timestamps.
     * - Pre: created and closed timestamps are non-null, and closed >= created
     * - Post: duration printed in days, hours, minutes, and seconds
     * - Params:
     *      -> Timestamp created: created_at value
     *      -> Timestamp closed: closed_at value
     * - Returns: None
     * ---------------------------------------------------------------- */
    private static void printResolutionDuration(Timestamp created, Timestamp closed) {
        long millis = closed.getTime() - created.getTime();

        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        System.out.println("Duration   " + days + "d " + hours + "h "
                + minutes + "m " + seconds + "s");
    }

    /* ------------------------ formatTimestamp ----------------------
     * - Purpose: converts a Timestamp into clean display text. Null
     * timestamps print as - for terminal readability.
     * - Pre: timestamp may be null
     * - Post: formatted string returned
     * - Params:
     *      -> Timestamp timestamp: SQL timestamp to display
     * - Returns:
     *      -> String timestamp text or -
     * ---------------------------------------------------------------- */
    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return timestamp.toString();
    }

    /* ------------------------ rollbackQuietly ----------------------
     * - Purpose: rolls back a failed transaction if auto-commit was
     * turned off, then restores default auto-commit mode when possible.
     * - Pre: connection may or may not be in manual transaction mode
     * - Post: connection is usable again when rollback succeeds
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ---------------------------------------------------------------- */
    private static void rollbackQuietly(Connection conn) {
        try {
            if (!conn.getAutoCommit()) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        }
    }
    /* ---------------------- listSupportAgents ----------------------
    * - Purpose: prints all support agents so the user can choose a valid sno.
    * - Pre: SupportAgent table exists
    * - Post: support-agent rows displayed, or empty-table message printed
    * - Params:
    *      -> Connection conn: active Oracle connection
    * - Returns: None
    * ---------------------------------------------------------------- */
    private static void listSupportAgents(Connection conn) throws SQLException {
        String sql =
            "SELECT sno, name, status, hired_at " +
            "FROM SupportAgent ORDER BY sno";

        try (
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            System.out.println();
            System.out.printf("%-8s %-24s %-12s %-22s%n",
                    "sno", "Name", "Status", "Hired");
            System.out.println("----------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-8d %-24s %-12s %-22s%n",
                        rs.getLong("sno"),
                        DatabaseUtil.abbreviate(rs.getString("name"), 24),
                        rs.getString("status"),
                        formatTimestamp(rs.getTimestamp("hired_at")));
            }

            if (!found) {
                System.out.println("No support agents found.");
            }
        }
    }
}