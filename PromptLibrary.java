/* ------------------- PromptLibrary ------------------------ *
 * Class: CSC460 - S26
 * Author: Akhond Saif Al Masud
 * Assg: Project 4
 * Instructor & TAs: Lester I. McCann, Jianwei Shen, Muhammad Bilal
 * Packages: java.sql.Connection, java.sql.PreparedStatement,
 *           java.sql.ResultSet, java.sql.SQLException, java.sql.Timestamp,
 *           java.sql.Types
 * Purpose: this class handles the terminal menu for the Prompt Library
 * feature. Users can list prompt templates they can access, create prompt
 * templates, update their own templates, delete their own templates, and
 * display one template in full so it can be reused elsewhere in the JDBC
 * application.
 * Language: Java 25
 * Methods:
 *  - showMenu: displays the prompt-library submenu
 *  - listAccessiblePrompts: prints prompts visible to a selected user
 *  - addPrompt: inserts a new prompt template
 *  - updatePrompt: updates one selected prompt owned by a user
 *  - deletePrompt: deletes one selected prompt owned by a user
 *  - usePrompt: displays one selected prompt template in full
 * ---------------------------------------------------------- */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

public class PromptLibrary {

    /* ---------------------- PromptLibrary -----------------------
     * - Purpose: private constructor so nobody creates a PromptLibrary
     * object. This file is used through static menu-helper methods.
     * ---------------------------------------------------------------- */
    private PromptLibrary() {
    }

    /* ------------------------- showMenu -------------------------
     * - Purpose: displays the prompt-library submenu and routes each
     * menu choice to the method that owns that operation.
     * - Pre: database connection is open and ConsoleIO exists
     * - Post: user either completed an operation or returned to main menu
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    public static void showMenu(Connection conn, ConsoleIO io) {
        boolean done = false;

        while (!done) {
            System.out.println();
            System.out.println("----------- Prompt Library -----------");
            System.out.println("1. List accessible prompt templates");
            System.out.println("2. Add prompt template");
            System.out.println("3. Update my prompt template");
            System.out.println("4. Delete my prompt template");
            System.out.println("5. Use a prompt template");
            System.out.println("6. Back");

            int choice = io.readIntInRange("Choose an option: ", 1, 6);
            try {
                switch (choice) {
                    case 1:
                        listAccessiblePrompts(conn, io);
                        io.pause();
                        break;
                    case 2:
                        addPrompt(conn, io);
                        io.pause();
                        break;
                    case 3:
                        updatePrompt(conn, io);
                        io.pause();
                        break;
                    case 4:
                        deletePrompt(conn, io);
                        io.pause();
                        break;
                    case 5:
                        usePrompt(conn, io);
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
                System.err.println("Prompt Library operation failed.");
                DatabaseUtil.printSqlError(e);
            }
        }
    }

    /* ------------------- listAccessiblePrompts ------------------
     * - Purpose: asks for a user id and prints all prompt templates
     * visible to that user. Visible prompts are:
     *   1) prompts the user owns
     *   2) prompts shared to workspaces the user belongs to
     * - Pre: Prompt and WorkspaceMember tables exist
     * - Post: visible prompts printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void listAccessiblePrompts(Connection conn, ConsoleIO io)
            throws SQLException {

        System.out.println();
        UserAccounts.listUsers(conn);
        long userId = io.readLong("UsrID to view prompt library for: ");

        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        listAccessiblePrompts(conn, userId);
    }

    /* ------------------- listAccessiblePrompts ------------------
     * - Purpose: helper overload that prints prompts visible to one
     * already-validated user id.
     * - Pre: user exists
     * - Post: visible prompts printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: target user
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void listAccessiblePrompts(Connection conn, long userId)
            throws SQLException {

        String sql =
            "SELECT p.promptID, p.name, p.category, p.UsrID, u.full_name AS owner_name, p.wsID, p.created_at " +
            "FROM Prompt p " +
            "JOIN \"User\" u ON u.UsrID = p.UsrID " +
            "WHERE p.UsrID = ? " +
            "   OR p.wsID IN ( " +
            "       SELECT wm.wsID FROM WorkspaceMember wm WHERE wm.UsrID = ? " +
            "   ) " +
            "ORDER BY p.category NULLS FIRST, p.name, p.promptID";

        try (
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println();
                System.out.printf(
                    "%-8s %-24s %-16s %-8s %-20s %-8s %-10s %-20s%n",
                    "PromptID", "Name", "Category", "Owner",
                    "Owner Name", "wsID", "Scope", "Created");
                System.out.println(
                    "----------------------------------------------------------------------------------------------------------------");

                boolean found = false;
                while (rs.next()) {
                    found = true;

                    Object wsObj = rs.getObject("wsID");
                    String scope = (wsObj == null) ? "private" : "workspace";

                    System.out.printf(
                        "%-8d %-24s %-16s %-8d %-20s %-8s %-10s %-20s%n",
                        rs.getLong("promptID"),
                        DatabaseUtil.abbreviate(rs.getString("name"), 24),
                        DatabaseUtil.abbreviate(rs.getString("category"), 16),
                        rs.getLong("UsrID"),
                        DatabaseUtil.abbreviate(rs.getString("owner_name"), 20),
                        wsObj == null ? "-" : String.valueOf(rs.getLong("wsID")),
                        scope,
                        formatTimestamp(rs.getTimestamp("created_at"))
                    );
                }

                if (!found) {
                    System.out.println("No accessible prompt templates found.");
                }
            }
        }
    }

    /* -------------------------- addPrompt -----------------------
     * - Purpose: inserts a new prompt template owned by one selected
     * user. The template can be private (wsID NULL) or shared in one
     * workspace the owner belongs to.
     * - Pre: selected user exists
     * - Post: new Prompt row inserted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void addPrompt(Connection conn, ConsoleIO io)
            throws SQLException {

        System.out.println();
        UserAccounts.listUsers(conn);
        long userId = io.readLong("Owner UsrID for new prompt: ");

        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        long promptId = DatabaseUtil.nextId(conn, "Prompt", "promptID");
        String name = io.readRequiredString("Prompt name: ");
        String text = io.readRequiredString("Prompt text: ");
        String category = io.readOptionalString("Category (blank for none): ");

        Long workspaceId = chooseWorkspaceForPrompt(conn, io, userId);

        String sql =
            "INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, promptId);
            stmt.setLong(2, userId);

            if (workspaceId == null) {
                stmt.setNull(3, Types.NUMERIC);
            } else {
                stmt.setLong(3, workspaceId.longValue());
            }

            stmt.setString(4, name);
            stmt.setString(5, text);
            setNullableString(stmt, 6, category);

            stmt.executeUpdate();
            System.out.println("Prompt template added with promptID " + promptId + ".");
        }
    }

    /* ------------------------- updatePrompt ---------------------
     * - Purpose: updates one selected prompt owned by a user. The
     * owner can change the name, text, category, or workspace scope.
     * - Pre: selected owner exists
     * - Post: selected prompt fields updated, or no update if user backs out
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void updatePrompt(Connection conn, ConsoleIO io)
            throws SQLException {

        System.out.println();
        UserAccounts.listUsers(conn);
        long userId = io.readLong("UsrID updating their prompt: ");

        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        listOwnedPrompts(conn, userId);
        long promptId = io.readLong("promptID to update: ");

        if (!promptOwnedByUser(conn, promptId, userId)) {
            System.out.println("That prompt does not exist or is not owned by this user.");
            return;
        }

        boolean done = false;
        while (!done) {
            printPromptDetail(conn, promptId);

            System.out.println();
            System.out.println("Update field");
            System.out.println("1. Name");
            System.out.println("2. Text");
            System.out.println("3. Category");
            System.out.println("4. Privacy / workspace sharing");
            System.out.println("5. Back");

            int choice = io.readIntInRange("Choose a field: ", 1, 5);
            switch (choice) {
                case 1:
                    updateRequiredText(conn, promptId, "name", "New prompt name: ", io);
                    break;
                case 2:
                    updateRequiredText(conn, promptId, "text", "New prompt text: ", io);
                    break;
                case 3:
                    updateOptionalText(conn, promptId, "category",
                        "New category (blank to clear): ", io);
                    break;
                case 4:
                    updateWorkspaceScope(conn, io, promptId, userId);
                    break;
                case 5:
                    done = true;
                    break;
                default:
                    System.out.println("Unknown option.");
                    break;
            }
        }
    }

    /* ------------------------- deletePrompt ---------------------
     * - Purpose: deletes one prompt template owned by the selected user.
     * - Pre: selected owner exists and selected prompt belongs to owner
     * - Post: Prompt row deleted, or no deletion if user cancels
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void deletePrompt(Connection conn, ConsoleIO io)
            throws SQLException {

        System.out.println();
        UserAccounts.listUsers(conn);
        long userId = io.readLong("UsrID deleting their prompt: ");

        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        listOwnedPrompts(conn, userId);
        long promptId = io.readLong("promptID to delete: ");

        if (!promptOwnedByUser(conn, promptId, userId)) {
            System.out.println("That prompt does not exist or is not owned by this user.");
            return;
        }

        printPromptDetail(conn, promptId);
        if (!io.confirm("Delete this prompt template?")) {
            System.out.println("Prompt deletion cancelled.");
            return;
        }

        String sql = "DELETE FROM Prompt WHERE promptID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, promptId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
            System.out.println("Prompt template deleted.");
        }
    }

    /* --------------------------- usePrompt ----------------------
     * - Purpose: displays one accessible prompt template in full so
     * the user can reuse its text elsewhere in the application.
     * - Pre: selected user exists
     * - Post: one prompt printed in full, or access denied message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void usePrompt(Connection conn, ConsoleIO io)
            throws SQLException {

        System.out.println();
        UserAccounts.listUsers(conn);
        long userId = io.readLong("UsrID using a prompt template: ");

        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        listAccessiblePrompts(conn, userId);
        long promptId = io.readLong("promptID to use: ");

        String sql =
            "SELECT p.promptID, p.name, p.text, p.category, p.UsrID, u.full_name AS owner_name, p.wsID, p.created_at " +
            "FROM Prompt p " +
            "JOIN \"User\" u ON u.UsrID = p.UsrID " +
            "WHERE p.promptID = ? " +
            "  AND (p.UsrID = ? " +
            "       OR p.wsID IN (SELECT wm.wsID FROM WorkspaceMember wm WHERE wm.UsrID = ?))";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, promptId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("That prompt is not accessible to this user.");
                    return;
                }

                Object wsObj = rs.getObject("wsID");

                System.out.println();
                System.out.println("Prompt template");
                System.out.println("promptID: " + rs.getLong("promptID"));
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Category: " +
                    DatabaseUtil.valueOrDash(rs.getString("category")));
                System.out.println("Owner: " + rs.getLong("UsrID") +
                    " - " + rs.getString("owner_name"));
                System.out.println("wsID: " + (wsObj == null ? "-" : rs.getLong("wsID")));
                System.out.println("Scope: " + (wsObj == null ? "private" : "workspace"));
                System.out.println("Created: " + formatTimestamp(rs.getTimestamp("created_at")));
                System.out.println();
                System.out.println("----- Prompt Text -----");
                System.out.println(rs.getString("text"));
                System.out.println("-----------------------");
            }
        }
    }

    /* ------------------------ listOwnedPrompts ------------------
     * - Purpose: prints only the prompt templates owned by one user.
     * - Pre: selected user exists
     * - Post: owner's prompts printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: owner UsrID
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void listOwnedPrompts(Connection conn, long userId)
            throws SQLException {

        String sql =
            "SELECT promptID, name, category, wsID, created_at " +
            "FROM Prompt WHERE UsrID = ? ORDER BY name, promptID";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println();
                System.out.printf("%-8s %-26s %-16s %-8s %-10s %-20s%n",
                    "promptID", "Name", "Category", "wsID", "Scope", "Created");
                System.out.println(
                    "-------------------------------------------------------------------------------------");

                boolean found = false;
                while (rs.next()) {
                    found = true;

                    Object wsObj = rs.getObject("wsID");

                    System.out.printf("%-8d %-26s %-16s %-8s %-10s %-20s%n",
                        rs.getLong("promptID"),
                        DatabaseUtil.abbreviate(rs.getString("name"), 26),
                        DatabaseUtil.abbreviate(rs.getString("category"), 16),
                        wsObj == null ? "-" : String.valueOf(rs.getLong("wsID")),
                        wsObj == null ? "private" : "workspace",
                        formatTimestamp(rs.getTimestamp("created_at")));
                }

                if (!found) {
                    System.out.println("This user does not own any prompt templates.");
                }
            }
        }
    }

    /* ----------------------- printPromptDetail ------------------
     * - Purpose: prints the current values for one prompt before update
     * choices are shown.
     * - Pre: selected prompt exists
     * - Post: prompt detail printed if row is found
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long promptId: promptID to display
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void printPromptDetail(Connection conn, long promptId)
            throws SQLException {

        String sql =
            "SELECT promptID, UsrID, wsID, name, text, category, created_at " +
            "FROM Prompt WHERE promptID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, promptId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object wsObj = rs.getObject("wsID");

                    System.out.println();
                    System.out.println("Current prompt");
                    System.out.println("promptID: " + rs.getLong("promptID"));
                    System.out.println("Owner UsrID: " + rs.getLong("UsrID"));
                    System.out.println("Name: " + rs.getString("name"));
                    System.out.println("Category: " +
                        DatabaseUtil.valueOrDash(rs.getString("category")));
                    System.out.println("wsID: " + (wsObj == null ? "-" : rs.getLong("wsID")));
                    System.out.println("Scope: " + (wsObj == null ? "private" : "workspace"));
                    System.out.println("Created: " +
                        formatTimestamp(rs.getTimestamp("created_at")));
                    System.out.println("Text: " + rs.getString("text"));
                }
            }
        }
    }

    /* ---------------------- chooseWorkspaceForPrompt ------------
     * - Purpose: asks whether the prompt should be private or shared.
     * If shared, it lists workspaces the owner belongs to and returns
     * the chosen workspace id. Private prompts return null.
     * - Pre: selected user exists
     * - Post: selected workspace id returned, or null for private
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long userId: owner UsrID
     * - Returns:
     *      -> Long: chosen workspace id, or null for private
     * ------------------------------------------------------------ */
    private static Long chooseWorkspaceForPrompt(Connection conn, ConsoleIO io, long userId)
            throws SQLException {

        boolean shared = io.confirm("Share this prompt in a workspace?");
        if (!shared) {
            return null;
        }

        String sql =
            "SELECT w.wsID, w.owner " +
            "FROM Workspace w " +
            "JOIN WorkspaceMember wm ON wm.wsID = w.wsID " +
            "WHERE wm.UsrID = ? " +
            "ORDER BY w.wsID";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println();
                System.out.println("Workspaces this user belongs to");
                System.out.printf("%-8s %-8s%n", "wsID", "Owner");
                System.out.println("---------------------");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-8d %-8d%n",
                        rs.getLong("wsID"),
                        rs.getLong("owner"));
                }

                if (!found) {
                    System.out.println("This user is not a member of any workspace.");
                    System.out.println("Prompt will be stored as private.");
                    return null;
                }
            }
        }

        long workspaceId = io.readLong("Workspace wsID for sharing (0 for private): ");
        if (workspaceId == 0) {
            return null;
        }

        if (!isWorkspaceMember(conn, workspaceId, userId)) {
            System.out.println("That user is not a member of workspace " + workspaceId + ".");
            System.out.println("Prompt will be stored as private.");
            return null;
        }

        return Long.valueOf(workspaceId);
    }

    /* ---------------------- updateWorkspaceScope ----------------
     * - Purpose: updates wsID for one prompt so it becomes private
     * (wsID NULL) or shared in one workspace.
     * - Pre: selected prompt belongs to selected owner
     * - Post: Prompt.wsID updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long promptId: target promptID
     *      -> long userId: owner UsrID
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void updateWorkspaceScope(Connection conn, ConsoleIO io,
                                             long promptId, long userId)
            throws SQLException {

        Long workspaceId = chooseWorkspaceForPrompt(conn, io, userId);

        String sql = "UPDATE Prompt SET wsID = ? WHERE promptID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (workspaceId == null) {
                stmt.setNull(1, Types.NUMERIC);
            } else {
                stmt.setLong(1, workspaceId.longValue());
            }

            stmt.setLong(2, promptId);
            stmt.executeUpdate();
            System.out.println("Prompt sharing updated.");
        }
    }

    /* ----------------------- promptOwnedByUser ------------------
     * - Purpose: checks whether one prompt is owned by one user.
     * - Pre: database connection is open
     * - Post: caller knows whether the ownership pair exists
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long promptId: promptID to check
     *      -> long userId: UsrID to check
     * - Returns:
     *      -> boolean: true if the prompt belongs to the user
     * ------------------------------------------------------------ */
    private static boolean promptOwnedByUser(Connection conn, long promptId, long userId)
            throws SQLException {

        String sql = "SELECT COUNT(*) FROM Prompt WHERE promptID = ? AND UsrID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, promptId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    /* ----------------------- isWorkspaceMember ------------------
     * - Purpose: checks whether a user belongs to one workspace.
     * - Pre: database connection is open
     * - Post: caller knows whether WorkspaceMember contains the pair
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to check
     *      -> long userId: UsrID to check
     * - Returns:
     *      -> boolean: true if membership exists
     * ------------------------------------------------------------ */
    private static boolean isWorkspaceMember(Connection conn, long workspaceId, long userId)
            throws SQLException {

        String sql = "SELECT COUNT(*) FROM WorkspaceMember WHERE wsID = ? AND UsrID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    /* ----------------------- updateRequiredText -----------------
     * - Purpose: updates one required text column in Prompt. Used for
     * name and text updates.
     * - Pre: selected prompt exists and columnName is a trusted Prompt column
     * - Post: selected text column updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long promptId: promptID to update
     *      -> String columnName: Prompt column to update
     *      -> String prompt: terminal prompt text
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void updateRequiredText(Connection conn, long promptId,
                                           String columnName, String prompt,
                                           ConsoleIO io)
            throws SQLException {

        String value = io.readRequiredString(prompt);
        String sql = "UPDATE Prompt SET " + columnName + " = ? WHERE promptID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.setLong(2, promptId);
            stmt.executeUpdate();
            System.out.println("Prompt updated.");
        }
    }

    /* ----------------------- updateOptionalText -----------------
     * - Purpose: updates one optional text column in Prompt. Blank
     * input clears the field by storing SQL NULL.
     * - Pre: selected prompt exists and columnName is a trusted Prompt column
     * - Post: selected optional column updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long promptId: promptID to update
     *      -> String columnName: Prompt column to update
     *      -> String prompt: terminal prompt text
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void updateOptionalText(Connection conn, long promptId,
                                           String columnName, String prompt,
                                           ConsoleIO io)
            throws SQLException {

        String value = io.readOptionalString(prompt);
        String sql = "UPDATE Prompt SET " + columnName + " = ? WHERE promptID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableString(stmt, 1, value);
            stmt.setLong(2, promptId);
            stmt.executeUpdate();
            System.out.println("Prompt updated.");
        }
    }

    /* ------------------------ setNullableString -----------------
     * - Purpose: binds an optional String into a PreparedStatement.
     * Blank/null values become SQL NULL.
     * - Pre: statement exists and index is valid
     * - Post: statement parameter has been bound
     * - Params:
     *      -> PreparedStatement stmt: statement being filled
     *      -> int index: parameter position
     *      -> String value: optional text value
     * - Returns: None
     * ------------------------------------------------------------ */
    private static void setNullableString(PreparedStatement stmt, int index, String value)
            throws SQLException {

        if (value == null || value.trim().isEmpty()) {
            stmt.setNull(index, Types.VARCHAR);
        } else {
            stmt.setString(index, value);
        }
    }

    /* ------------------------ formatTimestamp -------------------
     * - Purpose: converts a SQL Timestamp into display text. Null
     * timestamps show as "-" to keep terminal output clean.
     * - Pre: timestamp may be null
     * - Post: timestamp converted to display string
     * - Params:
     *      -> Timestamp timestamp: SQL timestamp value to display
     * - Returns:
     *      -> String: timestamp text or "-"
     * ------------------------------------------------------------ */
    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return timestamp.toString();
    }
}
