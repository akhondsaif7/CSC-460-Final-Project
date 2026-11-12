/* ------------------- class DatabaseUtil ------------------------ *
 * Class: CSC460 - S26
 * Author: Matteo Galiano
 * Assg: Project 4
 * Packages: java.sql.Connection, java.sql.PreparedStatement,
 *           java.sql.ResultSet, java.sql.SQLException, java.sql.Statement
 * Purpose: This class contains generic database helpers used by the feature
 * classes. It loads the Oracle driver, prints SQL errors, generates simple
 * numeric primary keys with MAX(id)+1, checks whether common records exist,
 * counts query results, and formats values for terminal output. It does not
 * own any menu or feature workflow.
 * Language: Java 25
 * Methods:
 *  - loadOracleDriver: loads Oracle JDBC driver
 *  - printSqlError: prints SQLException chain
 *  - nextId: returns MAX(id)+1 for an insert
 *  - existsById: generic id existence checker
 *  - count: generic one-parameter count helper
 *  - planExists, userExists, conversationExists, messageExists: table checks
 *  - valueOrDash, abbreviate: terminal output helpers
 * ------------------------------------------------------ */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {

    /* ------------------------- DatabaseUtil ---------------------------
     * - Purpose: private constructor so nobody creates a DatabaseUtil object.
     * All helpers are static and are called directly from other classes.
     * - Pre: class exists
     * - Post: object construction is blocked
     * - Params: None
     * - Returns: None
     * ------------------------------------------------------------------ */
    private DatabaseUtil() {
    }

    /* ------------------------ loadOracleDriver ------------------------
     * - Purpose: loads the Oracle JDBC driver class before the program tries
     * to connect to Oracle. If the driver is missing, it prints what jar is
     * needed and returns false.
     * - Pre: classpath may or may not contain ojdbc8.jar
     * - Post: driver loaded if present, or error printed if missing
     * - Params: None
     * - Returns:
     *      -> boolean: true if driver loaded, false otherwise
     * ------------------------------------------------------------------ */
    public static boolean loadOracleDriver() {
        try {
            Class.forName("oracle.jdbc.OracleDriver"); // Ask JVM to load driver
            return true;
        } catch (ClassNotFoundException e) {            // Driver jar missing
            System.err.println("Could not load Oracle JDBC driver.");
            System.err.println("Make sure ojdbc8.jar is on your CLASSPATH.");
            return false;
        }
    }

    /* -------------------------- printSqlError --------------------------
     * - Purpose: helper that prints Oracle SQL error information in a readable
     * way. It walks through chained SQLExceptions so follow-up Oracle errors are
     * not hidden.
     * - Pre: SQLException exists
     * - Post: SQL error info printed to terminal
     * - Params:
     *      -> SQLException e: Oracle SQL exception
     * - Returns: None
     * ------------------------------------------------------------------- */
    public static void printSqlError(SQLException e) {
        SQLException current = e;                       // Current error in chain
        while (current != null) {                       // Print all SQL errors
            System.err.println("SQLState: " + current.getSQLState());
            System.err.println("Error Code: " + current.getErrorCode());
            System.err.println("Message: " + current.getMessage());
            current = current.getNextException();       // Move to next SQL error
            if (current != null) {
                System.err.println("--- next SQL exception ---");
            }
        }
    }

    /* ----------------------------- nextId -----------------------------
     * - Purpose: gets the next numeric id for a table by selecting MAX(id)+1.
     * This keeps insert code simple for this project schema, where there are no
     * sequences in the provided SQL file.
     * - Pre: tableName and idColumn are trusted constants from this program
     * - Post: returned value can be used as the next id for an insert
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> String tableName: table to scan for max id
     *      -> String idColumn: numeric id column in that table
     * - Returns:
     *      -> long: next id value, or 1 if the table is empty
     * ------------------------------------------------------------------ */
    public static long nextId(Connection conn, String tableName, String idColumn)
        throws SQLException {
        String sql = "SELECT NVL(MAX(" + idColumn + "), 0) + 1 AS next_id FROM " + tableName;
        try (
            Statement stmt = conn.createStatement();    // Simple statement for trusted SQL
            ResultSet rs = stmt.executeQuery(sql)       // Run MAX(id)+1 query
        ) {
            rs.next();                                  // Count query always returns one row
            return rs.getLong("next_id");              // Return generated id
        }
    }

    /* --------------------------- existsById ---------------------------
     * - Purpose: generic helper to check if a row exists in a table by its id.
     * Feature-specific helpers call this with their own table/id names.
     * - Pre: tableName and idColumn are trusted constants from this program
     * - Post: caller knows whether the row exists
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> String tableName: table being checked
     *      -> String idColumn: id column in the table
     *      -> long id: id value to search for
     * - Returns:
     *      -> boolean: true if at least one matching row exists
     * ------------------------------------------------------------------ */
    public static boolean existsById(
        Connection conn,
        String tableName,
        String idColumn,
        long id
    ) throws SQLException {
        String sql = "SELECT COUNT(*) AS row_count FROM " + tableName + " WHERE " + idColumn + " = ?";
        return count(conn, sql, id) > 0;                // Reuse one-value count helper
    }

    /* ------------------------------ count -----------------------------
     * - Purpose: runs a SELECT COUNT(*) style query that has exactly one long
     * parameter. This avoids repeating the same PreparedStatement code.
     * - Pre: sql returns one numeric column and has one ? placeholder
     * - Post: count result has been read from Oracle
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> String sql: count query with one parameter
     *      -> long value: value to bind to the parameter
     * - Returns:
     *      -> long: count returned by the query
     * ------------------------------------------------------------------ */
    public static long count(Connection conn, String sql, long value) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, value);                     // Bind only parameter
            try (ResultSet rs = stmt.executeQuery()) {  // Execute count query
                rs.next();                              // Count query always returns one row
                return rs.getLong(1);                   // Return first column
            }
        }
    }

    /* --------------------------- planExists ---------------------------
     * - Purpose: checks if a Plan row exists for the plan/tier name typed by
     * the user. Used before inserting/updating users.
     * - Pre: database connection is open
     * - Post: caller knows whether the plan is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> String plan: plan/tier name to check
     * - Returns:
     *      -> boolean: true if Plan contains that plan value
     * ------------------------------------------------------------------ */
    public static boolean planExists(Connection conn, String plan) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Plan WHERE plan = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plan);                    // Bind plan name
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();                              // Count query returns one row
                return rs.getLong(1) > 0;               // Existing plan if count > 0
            }
        }
    }

    /* ---------------------------- userExists ---------------------------
     * - Purpose: checks if a user exists in the quoted User table.
     * - Pre: database connection is open
     * - Post: caller knows whether the UsrID is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID to check
     * - Returns:
     *      -> boolean: true if the user exists
     * ------------------------------------------------------------------ */
    public static boolean userExists(Connection conn, long userId) throws SQLException {
        return existsById(conn, "\"User\"", "UsrID", userId); // User is quoted in schema
    }

    /* ----------------------- conversationExists -----------------------
     * - Purpose: checks if a conversation exists before message/list actions
     * use its CID.
     * - Pre: database connection is open
     * - Post: caller knows whether the CID is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long conversationId: CID to check
     * - Returns:
     *      -> boolean: true if the conversation exists
     * ------------------------------------------------------------------ */
    public static boolean conversationExists(Connection conn, long conversationId)
        throws SQLException {
        return existsById(conn, "Conversation", "CID", conversationId); // Check Conversation.CID
    }

    /* -------------------------- messageExists -------------------------
     * - Purpose: checks if a message exists before feedback/rating actions use
     * its MsgID.
     * - Pre: database connection is open
     * - Post: caller knows whether the MsgID is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long messageId: MsgID to check
     * - Returns:
     *      -> boolean: true if the message exists
     * ------------------------------------------------------------------ */
    public static boolean messageExists(Connection conn, long messageId) throws SQLException {
        return existsById(conn, "Message", "MsgID", messageId); // Check Message.MsgID
    }

    /* -------------------------- valueOrDash --------------------------
     * - Purpose: terminal output helper that shows '-' instead of blank/null
     * strings, so tables are easier to read.
     * - Pre: value may be null, blank, or real text
     * - Post: returned string is safe for display
     * - Params:
     *      -> String value: value to display
     * - Returns:
     *      -> String: '-' for blank/null, otherwise the original value
     * ------------------------------------------------------------------ */
    public static String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {  // Missing display value
            return "-";
        }
        return value;                                   // Real value, unchanged
    }

    /* ---------------------------- abbreviate --------------------------
     * - Purpose: shortens long strings for terminal tables. It also flattens
     * newlines so CLOB previews do not break table formatting.
     * - Pre: maxLength is positive and value may be null
     * - Post: returned string fits the requested display width
     * - Params:
     *      -> String value: text to shorten
     *      -> int maxLength: maximum output length
     * - Returns:
     *      -> String: shortened one-line display value
     * ------------------------------------------------------------------ */
    public static String abbreviate(String value, int maxLength) {
        if (value == null) {                            // Null displays as dash
            return "-";
        }

        String oneLine = value.replace('\n', ' ').replace('\r', ' ').trim(); // Flatten CLOB text
        if (oneLine.length() <= maxLength) {            // Already short enough
            return oneLine;
        }
        if (maxLength <= 3) {                           // Tiny widths cannot fit ellipsis
            return oneLine.substring(0, maxLength);
        }
        return oneLine.substring(0, maxLength - 3) + "..."; // Leave space for dots
    }
}
