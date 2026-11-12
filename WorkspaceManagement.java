/* ------------------- class WorkspaceManagement ------------------------ *
 * Class: CSC460 - S26
 * Author: Cole Hageman
 * Assg: Project 4
 * Packages: java.sql.Connection, java.sql.PreparedStatement,
 *           java.sql.ResultSet, java.sql.SQLException, java.sql.Timestamp,
 *           java.sql.Types
 * Purpose: This class handles the terminal menu for workspace organization. It
 * can create workspaces, modify workspace fields, and move conversations into
 * workspaces while enforcing the rule that the user must belong to the target
 * workspace.
 * Language: Java 25
 * Methods:
 *  - showMenu: displays the workspace menu
 *  - createWorkspace, modifyWorkspace: workspace create/update workflows
 *  - moveConversationIntoWorkspace: updates Conversation.wsID after checks
 *  - updateWorkspaceOwner, updateWorkspacePreferredLanguage, updateWorkspacePlan
 *  - listWorkspaces, listWorkspacesForUser, listConversationsForUser
 *  - printWorkspaceDetail, printPlans, readExistingPlan: display/input helpers
 *  - workspaceExists, userIsWorkspaceMember, conversationBelongsToUser: checks
 *  - ensureOwnerMembership, setNullableString, nullableLongLabel, formatTimestamp
 * ------------------------------------------------------ */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

public class WorkspaceManagement {

    /* --------------------- WorkspaceManagement -----------------------
     * - Purpose: private constructor so nobody creates a WorkspaceManagement
     * object. This file is used through static menu/helper methods.
     * - Pre: class exists
     * - Post: object construction is blocked
     * - Params: None
     * - Returns: None
     * ------------------------------------------------------------------ */
    private WorkspaceManagement() {
    }

    /* ---------------------------- showMenu ----------------------------
     * - Purpose: displays the workspace-organization submenu and routes each
     * user choice to the method that owns that operation.
     * - Pre: database connection is open and ConsoleIO exists
     * - Post: user either completed an operation or returned to main menu
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    public static void showMenu(Connection conn, ConsoleIO io) {
        boolean done = false;
        while (!done) {
            System.out.println();
            System.out.println("-------- Workspace Organization --------");
            System.out.println("1. Create workspace");
            System.out.println("2. Modify workspace");
            System.out.println("3. Move conversation into workspace");
            System.out.println("4. Back");

            int choice = io.readIntInRange("Choose an option: ", 1, 4);
            try {
                switch (choice) {
                    case 1:
                        createWorkspace(conn, io);
                        break;
                    case 2:
                        modifyWorkspace(conn, io);
                        break;
                    case 3:
                        moveConversationIntoWorkspace(conn, io);
                        break;
                    case 4:
                        done = true;
                        break;
                    default:
                        System.out.println("Unknown option.");
                        break;
                }
            } catch (SQLException e) {
                System.err.println("Workspace operation failed.");
                DatabaseUtil.printSqlError(e);
            }
        }
    }

    /* ------------------------ createWorkspace -------------------------
     * - Purpose: creates a new workspace. It validates the owner user, reads
     * simple workspace fields, inserts into Workspace, and ensures the owner is
     * also a member with role owner.
     * - Pre: selected owner user exists and chosen plan exists
     * - Post: new Workspace row inserted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void createWorkspace(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        System.out.println("Create Workspace");
        UserAccounts.listUsers(conn);
        long ownerId = io.readLong("Owner UsrID: ");
        if (!DatabaseUtil.userExists(conn, ownerId)) {
            System.out.println("No user exists with UsrID " + ownerId + ".");
            return;
        }

        printPlans(conn);
        String preferredLanguage = io.readOptionalString("Preferred language (blank for none): ");
        String plan = readExistingPlan(conn, io);
        long workspaceId = DatabaseUtil.nextId(conn, "Workspace", "wsID");

        String sql =
            "INSERT INTO Workspace (wsID, owner, preferred_language, plan) " +
            "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);
            stmt.setLong(2, ownerId);
            setNullableString(stmt, 3, preferredLanguage);
            stmt.setString(4, plan);
            stmt.executeUpdate();
        }
        ensureOwnerMembership(conn, workspaceId, ownerId);

        System.out.println("Workspace created with wsID " + workspaceId + ".");
    }

    /* ------------------------ modifyWorkspace -------------------------
     * - Purpose: updates one selected workspace. It lists workspaces, asks for
     * wsID, validates that the workspace exists, then repeatedly lets the user
     * update one field until they choose Back.
     * - Pre: selected workspace exists
     * - Post: selected workspace fields updated, or no update if user backs out
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void modifyWorkspace(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        listWorkspaces(conn);
        long workspaceId = io.readLong("Workspace wsID to modify: ");
        if (!workspaceExists(conn, workspaceId)) {
            System.out.println("No workspace exists with wsID " + workspaceId + ".");
            return;
        }

        boolean done = false;
        while (!done) {
            printWorkspaceDetail(conn, workspaceId);
            System.out.println();
            System.out.println("Update workspace field");
            System.out.println("1. Owner");
            System.out.println("2. Preferred language");
            System.out.println("3. Plan / tier");
            System.out.println("4. Back");

            int choice = io.readIntInRange("Choose a field: ", 1, 4);
            switch (choice) {
                case 1:
                    updateWorkspaceOwner(conn, io, workspaceId);
                    break;
                case 2:
                    updateWorkspacePreferredLanguage(conn, io, workspaceId);
                    break;
                case 3:
                    updateWorkspacePlan(conn, io, workspaceId);
                    break;
                case 4:
                    done = true;
                    break;
                default:
                    System.out.println("Unknown option.");
                    break;
            }
        }
    }

    /* ---------------- moveConversationIntoWorkspace -------------------
     * - Purpose: moves a user's conversation into a workspace after verifying
     * that the user exists, owns the conversation, and belongs to the target
     * workspace.
     * - Pre: selected user exists, conversation belongs to user, workspace
     * exists, and user is a workspace member
     * - Post: Conversation.wsID updated, or operation blocked
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void moveConversationIntoWorkspace(Connection conn, ConsoleIO io)
        throws SQLException {
        System.out.println();
        System.out.println("Move Conversation Into Workspace");
        long userId = io.readLong("UsrID moving the conversation: ");
        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        listConversationsForUser(conn, userId);
        long conversationId = io.readLong("Conversation ID (CID): ");
        if (!conversationBelongsToUser(conn, conversationId, userId)) {
            System.out.println("No conversation with CID " + conversationId + " belongs to UsrID " + userId + ".");
            return;
        }

        listWorkspacesForUser(conn, userId);
        long workspaceId = io.readLong("Workspace wsID: ");
        if (!workspaceExists(conn, workspaceId)) {
            System.out.println("No workspace exists with wsID " + workspaceId + ".");
            return;
        }
        if (!userIsWorkspaceMember(conn, workspaceId, userId)) {
            System.out.println(
                "User " + userId + " is not a member of workspace " + workspaceId +
                ". Conversation was not moved."
            );
            return;
        }

        String sql = "UPDATE Conversation SET wsID = ? WHERE CID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);
            stmt.setLong(2, conversationId);
            stmt.setLong(3, userId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                System.out.println("Conversation was not moved.");
                return;
            }
        }

        System.out.println("Conversation " + conversationId + " moved to workspace " + workspaceId + ".");
    }

    /* --------------------- updateWorkspaceOwner -----------------------
     * - Purpose: changes the owner of a workspace to another existing user and
     * ensures the new owner is also a workspace member with role owner and the
     * previous owner is no longer marked as owner.
     * - Pre: selected workspace exists and new owner user exists
     * - Post: Workspace.owner updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long workspaceId: wsID to update
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updateWorkspaceOwner(Connection conn, ConsoleIO io, long workspaceId)
        throws SQLException {
        UserAccounts.listUsers(conn);
        long ownerId = io.readLong("New owner UsrID: ");
        if (!DatabaseUtil.userExists(conn, ownerId)) {
            System.out.println("No user exists with UsrID " + ownerId + ".");
            return;
        }

        long oldOwnerId = getWorkspaceOwner(conn, workspaceId);
        String sql = "UPDATE Workspace SET owner = ? WHERE wsID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ownerId);
            stmt.setLong(2, workspaceId);
            stmt.executeUpdate();
        }
        ensureOwnerMembership(conn, workspaceId, ownerId);
        demoteOldWorkspaceOwner(conn, workspaceId, oldOwnerId, ownerId);
        System.out.println("Workspace owner updated.");
    }

    /* ---------------- updateWorkspacePreferredLanguage ----------------
     * - Purpose: updates preferred_language for one workspace. Blank input
     * clears the field by storing SQL NULL.
     * - Pre: selected workspace exists
     * - Post: preferred_language updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long workspaceId: wsID to update
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updateWorkspacePreferredLanguage(
        Connection conn,
        ConsoleIO io,
        long workspaceId
    ) throws SQLException {
        String preferredLanguage = io.readOptionalString("New preferred language (blank to clear): ");
        String sql = "UPDATE Workspace SET preferred_language = ? WHERE wsID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableString(stmt, 1, preferredLanguage);
            stmt.setLong(2, workspaceId);
            stmt.executeUpdate();
        }
        System.out.println("Workspace preferred language updated.");
    }

    /* ---------------------- updateWorkspacePlan -----------------------
     * - Purpose: updates the selected workspace's plan/tier. It prints all
     * plans and requires the user to choose an existing one before updating.
     * - Pre: selected workspace exists and chosen plan exists
     * - Post: workspace plan updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long workspaceId: wsID to update
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updateWorkspacePlan(Connection conn, ConsoleIO io, long workspaceId)
        throws SQLException {
        printPlans(conn);
        String plan = readExistingPlan(conn, io);
        String sql = "UPDATE Workspace SET plan = ? WHERE wsID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plan);
            stmt.setLong(2, workspaceId);
            stmt.executeUpdate();
        }
        System.out.println("Workspace plan updated.");
    }

    /* ------------------------- listWorkspaces -------------------------
     * - Purpose: lists every workspace with its owner, language, plan, and
     * created timestamp.
     * - Pre: Workspace table exists
     * - Post: workspace rows printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void listWorkspaces(Connection conn) throws SQLException {
        String sql =
            "SELECT w.wsID, w.owner, u.full_name, w.preferred_language, w.plan, w.created_at " +
            "FROM Workspace w " +
            "JOIN \"User\" u ON u.UsrID = w.owner " +
            "ORDER BY w.wsID";
        try (
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            System.out.println();
            System.out.printf(
                "%-8s %-8s %-24s %-16s %-14s %-20s%n",
                "wsID",
                "Owner",
                "Owner Name",
                "Language",
                "Plan",
                "Created"
            );
            System.out.println(
                "------------------------------------------------------------------------------------------------"
            );
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf(
                    "%-8d %-8d %-24s %-16s %-14s %-20s%n",
                    rs.getLong("wsID"),
                    rs.getLong("owner"),
                    DatabaseUtil.abbreviate(rs.getString("full_name"), 24),
                    DatabaseUtil.valueOrDash(rs.getString("preferred_language")),
                    rs.getString("plan"),
                    formatTimestamp(rs.getTimestamp("created_at"))
                );
            }
            if (!found) {
                System.out.println("No workspaces found.");
            }
        }
    }

    /* ---------------------- listWorkspacesForUser ---------------------
     * - Purpose: lists workspaces that the selected user belongs to so move
     * logic can show which workspaces are valid targets.
     * - Pre: selected user exists
     * - Post: workspace membership rows printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: member UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void listWorkspacesForUser(Connection conn, long userId) throws SQLException {
        String sql =
            "SELECT w.wsID, wm.role, w.plan, w.preferred_language, w.created_at " +
            "FROM Workspace w " +
            "JOIN WorkspaceMember wm ON wm.wsID = w.wsID " +
            "WHERE wm.UsrID = ? " +
            "ORDER BY w.wsID";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println();
                System.out.println("Workspaces this user belongs to");
                System.out.printf(
                    "%-8s %-12s %-14s %-16s %-20s%n",
                    "wsID",
                    "Role",
                    "Plan",
                    "Language",
                    "Created"
                );
                System.out.println("------------------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf(
                        "%-8d %-12s %-14s %-16s %-20s%n",
                        rs.getLong("wsID"),
                        rs.getString("role"),
                        rs.getString("plan"),
                        DatabaseUtil.valueOrDash(rs.getString("preferred_language")),
                        formatTimestamp(rs.getTimestamp("created_at"))
                    );
                }
                if (!found) {
                    System.out.println("This user does not belong to any workspaces.");
                }
            }
        }
    }

    /* ------------------- listConversationsForUser ---------------------
     * - Purpose: lists conversations owned by one user so move logic can show
     * which conversations are valid for the selected user.
     * - Pre: selected user exists
     * - Post: conversation rows printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void listConversationsForUser(Connection conn, long userId) throws SQLException {
        String sql =
            "SELECT CID, title, wsID, created_at FROM Conversation " +
            "WHERE UsrID = ? ORDER BY created_at DESC, CID DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println();
                System.out.println("Conversations for this user");
                System.out.printf("%-8s %-30s %-10s %-20s%n", "CID", "Title", "wsID", "Created");
                System.out.println("------------------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf(
                        "%-8d %-30s %-10s %-20s%n",
                        rs.getLong("CID"),
                        DatabaseUtil.abbreviate(rs.getString("title"), 30),
                        nullableLongLabel(rs, "wsID"),
                        formatTimestamp(rs.getTimestamp("created_at"))
                    );
                }
                if (!found) {
                    System.out.println("No conversations found for this user.");
                }
            }
        }
    }

    /* ---------------------- printWorkspaceDetail ----------------------
     * - Purpose: prints the current values for one workspace before update
     * choices are displayed.
     * - Pre: selected workspace exists
     * - Post: workspace detail printed if row is found
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to display
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void printWorkspaceDetail(Connection conn, long workspaceId) throws SQLException {
        String sql =
            "SELECT w.wsID, w.owner, u.full_name, w.preferred_language, w.plan, w.created_at " +
            "FROM Workspace w " +
            "JOIN \"User\" u ON u.UsrID = w.owner " +
            "WHERE w.wsID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println();
                    System.out.println("Current workspace");
                    System.out.println("wsID: " + rs.getLong("wsID"));
                    System.out.println(
                        "Owner: " + rs.getLong("owner") + " (" + rs.getString("full_name") + ")"
                    );
                    System.out.println(
                        "Preferred language: " +
                        DatabaseUtil.valueOrDash(rs.getString("preferred_language"))
                    );
                    System.out.println("Plan: " + rs.getString("plan"));
                    System.out.println("Created: " + formatTimestamp(rs.getTimestamp("created_at")));
                }
            }
        }
    }

    /* --------------------------- printPlans ---------------------------
     * - Purpose: prints all available plan/tier rows so workspace create/update
     * flows can show valid options.
     * - Pre: Plan table exists
     * - Post: plan rows printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void printPlans(Connection conn) throws SQLException {
        String sql = "SELECT plan, max_daily_msg, pro_model FROM Plan ORDER BY plan";
        try (
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            System.out.println();
            System.out.println("Available plans");
            System.out.printf("%-18s %-16s %-10s%n", "Plan", "Max Daily Msg", "Pro Model");
            System.out.println("----------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf(
                    "%-18s %-16d %-10s%n",
                    rs.getString("plan"),
                    rs.getLong("max_daily_msg"),
                    rs.getInt("pro_model") == 1 ? "yes" : "no"
                );
            }
            if (!found) {
                System.out.println("No plans found. Insert at least one Plan row first.");
            }
        }
    }

    /* ------------------------ readExistingPlan ------------------------
     * - Purpose: asks for a plan/tier name until the user enters one that
     * exists in the Plan table.
     * - Pre: database connection is open
     * - Post: returned plan exists in Plan
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns:
     *      -> String: existing plan/tier name
     * ------------------------------------------------------------------ */
    private static String readExistingPlan(Connection conn, ConsoleIO io) throws SQLException {
        while (true) {
            String plan = io.readRequiredString("Plan / tier: ");
            if (DatabaseUtil.planExists(conn, plan)) {
                return plan;
            }
            System.out.println("No plan exists with that name. Choose one from the list.");
        }
    }

    /* ------------------------ workspaceExists -------------------------
     * - Purpose: checks whether a workspace exists before update/move actions
     * use its wsID.
     * - Pre: database connection is open
     * - Post: caller knows whether the wsID is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to check
     * - Returns:
     *      -> boolean: true if the workspace exists
     * ------------------------------------------------------------------ */
    private static boolean workspaceExists(Connection conn, long workspaceId) throws SQLException {
        return DatabaseUtil.existsById(conn, "Workspace", "wsID", workspaceId);
    }

    /* ---------------------- userIsWorkspaceMember ---------------------
     * - Purpose: verifies that a user belongs to a workspace before that
     * workspace can be used as a move target.
     * - Pre: database connection is open
     * - Post: caller knows whether membership is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to check
     *      -> long userId: UsrID to check
     * - Returns:
     *      -> boolean: true if WorkspaceMember contains that pair
     * ------------------------------------------------------------------ */
    private static boolean userIsWorkspaceMember(
        Connection conn,
        long workspaceId,
        long userId
    ) throws SQLException {
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

    /* -------------------- conversationBelongsToUser -------------------
     * - Purpose: verifies that a conversation belongs to the selected user
     * before workspace move logic can update it.
     * - Pre: database connection is open
     * - Post: caller knows whether conversation ownership is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long conversationId: CID to check
     *      -> long userId: expected owner UsrID
     * - Returns:
     *      -> boolean: true if Conversation has that CID/user pair
     * ------------------------------------------------------------------ */
    private static boolean conversationBelongsToUser(
        Connection conn,
        long conversationId,
        long userId
    ) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Conversation WHERE CID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    /* ------------------------ getWorkspaceOwner ----------------------
     * - Purpose: reads the current owner of a workspace before changing it so
     * Java can demote the previous owner after the update.
     * - Pre: workspace exists
     * - Post: current owner UsrID returned
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to inspect
     * - Returns:
     *      -> long: current Workspace.owner UsrID
     * ------------------------------------------------------------------ */
    private static long getWorkspaceOwner(Connection conn, long workspaceId) throws SQLException {
        String sql = "SELECT owner FROM Workspace WHERE wsID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("owner");
                }
            }
        }
        throw new SQLException("Workspace " + workspaceId + " was not found.");
    }

    /* ---------------------- ensureOwnerMembership ---------------------
     * - Purpose: ensures that a workspace owner also has a WorkspaceMember row
     * with role owner. This bridges the owner-membership rule in Java without
     * requiring a database trigger.
     * - Pre: workspace exists and owner user exists
     * - Post: WorkspaceMember row exists with role owner for that owner
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to update
     *      -> long ownerId: owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void ensureOwnerMembership(Connection conn, long workspaceId, long ownerId)
        throws SQLException {
        if (userIsWorkspaceMember(conn, workspaceId, ownerId)) {
            String sql = "UPDATE WorkspaceMember SET role = 'owner' WHERE wsID = ? AND UsrID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, workspaceId);
                stmt.setLong(2, ownerId);
                stmt.executeUpdate();
            }
            return;
        }

        String sql = "INSERT INTO WorkspaceMember (wsID, UsrID, role) VALUES (?, ?, 'owner')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);
            stmt.setLong(2, ownerId);
            stmt.executeUpdate();
        }
    }

    /* -------------------- demoteOldWorkspaceOwner --------------------
     * - Purpose: changes the previous workspace owner's membership role back
     * to admin after Workspace.owner has been updated.
     * - Pre: Workspace.owner no longer equals oldOwnerId unless owner did not
     * change
     * - Post: previous owner no longer has role owner for that workspace
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID whose owner changed
     *      -> long oldOwnerId: previous owner UsrID
     *      -> long newOwnerId: replacement owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void demoteOldWorkspaceOwner(
        Connection conn,
        long workspaceId,
        long oldOwnerId,
        long newOwnerId
    ) throws SQLException {
        if (oldOwnerId == newOwnerId) {
            return;
        }

        String sql =
            "UPDATE WorkspaceMember SET role = 'admin' " +
            "WHERE wsID = ? AND UsrID = ? AND role = 'owner'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);
            stmt.setLong(2, oldOwnerId);
            stmt.executeUpdate();
        }
    }

    /* ----------------------- setNullableString ------------------------
     * - Purpose: binds an optional String into a PreparedStatement. Null text
     * becomes SQL NULL, otherwise the real String value is bound.
     * - Pre: statement exists and index is valid
     * - Post: statement parameter has been bound
     * - Params:
     *      -> PreparedStatement stmt: statement being filled
     *      -> int index: parameter position
     *      -> String value: optional text value
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void setNullableString(PreparedStatement stmt, int index, String value)
        throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.VARCHAR);
        } else {
            stmt.setString(index, value);
        }
    }

    /* ------------------------ nullableLongLabel -----------------------
     * - Purpose: reads a nullable numeric column from a ResultSet and returns a
     * printable label. SQL NULL becomes '-'.
     * - Pre: ResultSet is on a valid row and column exists
     * - Post: numeric value converted to display text
     * - Params:
     *      -> ResultSet rs: current query row
     *      -> String columnName: nullable numeric column name
     * - Returns:
     *      -> String: number as text, or '-' if SQL NULL
     * ------------------------------------------------------------------ */
    private static String nullableLongLabel(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        if (rs.wasNull()) {
            return "-";
        }
        return String.valueOf(value);
    }

    /* ------------------------- formatTimestamp ------------------------
     * - Purpose: converts a SQL Timestamp into display text. Null timestamps
     * show as '-' to keep terminal tables clean.
     * - Pre: timestamp may be null
     * - Post: timestamp converted to display string
     * - Params:
     *      -> Timestamp timestamp: SQL timestamp value to display
     * - Returns:
     *      -> String: timestamp text or '-'
     * ------------------------------------------------------------------ */
    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return timestamp.toString();
    }
}
