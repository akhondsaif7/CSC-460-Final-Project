/* ---------------------- class UserAccounts ------------------------ *
 * Class: CSC460 - S26
 * Author: Matteo Galiano
 * Assg: Project 4
 * Packages: java.sql.*, java.util.LinkedHashMap, java.util.Map
 * Purpose: This class handles the terminal menu for managing user accounts.
 * It can list users, add a user, update user fields, and delete a user. The
 * delete flow enforces the assignment rule that deletion fails if the user has
 * unpaid invoices or open/pending support tickets, then it transfers owned
 * workspaces and removes dependent rows before deleting the user row.
 * Language: Java 25
 * Methods:
 *  - showMenu: displays the user account menu
 *  - listUsers: prints all users
 *  - addUser: inserts a new User row
 *  - updateUser: lets the user update one selected account
 *  - deleteUser: validates and deletes one selected account
 *  - updatePreferredLanguage, updatePlan, updateRequiredText: update helpers
 *  - printPlans, printUserDetail, readExistingPlan: display/input helpers
 *  - collectWorkspaceTransfers, transferOwnedWorkspaces: workspace transfer helpers
 *  - countUnpaidInvoices, countOpenSupportTickets: deletion blocker checks
 *  - deleteDependentContentRows, deleteBillingRows, deleteUserRow: delete helpers
 *  - setNullableString, formatTimestamp, rollbackQuietly: small helpers
 * ----------------------------------------------------------------- */

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserAccounts {

    /* ------------------------- UserAccounts --------------------------
     * - Purpose: private constructor so nobody creates a UserAccounts object.
     * This file is used through static menu/helper methods.
     * - Pre: class exists
     * - Post: object construction is blocked
     * - Params: None
     * - Returns: None
     * ------------------------------------------------------------------ */
    private UserAccounts() {
    }

    /* ---------------------------- showMenu ----------------------------
     * - Purpose: displays the user-account submenu and routes each user choice
     * to the method that owns that operation.
     * - Pre: database connection is open and ConsoleIO exists
     * - Post: user either completed an operation or returned to main menu
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    public static void showMenu(Connection conn, ConsoleIO io) {
        boolean done = false;                         // Submenu exit flag
        while (!done) {                               // Keep submenu open
            System.out.println();
            System.out.println("----------- User Accounts -----------");
            System.out.println("1. Add user");
            System.out.println("2. Update user");
            System.out.println("3. Delete user");
            System.out.println("4. List users");
            System.out.println("5. Back");

            int choice = io.readIntInRange("Choose an option: ", 1, 5); // Valid menu choice
            try {
                switch (choice) {
                    case 1:
                        addUser(conn, io);             // Insert User
                        break;
                    case 2:
                        updateUser(conn, io);          // Update User fields
                        break;
                    case 3:
                        deleteUser(conn, io);          // Delete User with blockers
                        break;
                    case 4:
                        listUsers(conn);               // Display users
                        io.pause();                    // Let user read output
                        break;
                    case 5:
                        done = true;                   // Return to main menu
                        break;
                    default:
                        System.out.println("Unknown option."); // Should not happen
                        break;
                }
            } catch (SQLException e) {                 // Oracle operation failed
                System.err.println("User account operation failed.");
                DatabaseUtil.printSqlError(e);         // Print readable SQL error
                rollbackQuietly(conn);                 // Leave connection in usable state
            }
        }
    }

    /* ---------------------------- listUsers ---------------------------
     * - Purpose: prints every row from the User table with key account fields.
     * Other menus reuse this before asking for a UsrID.
     * - Pre: User table exists
     * - Post: users printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ------------------------------------------------------------------ */
    public static void listUsers(Connection conn) throws SQLException {
        String sql =
            "SELECT UsrID, full_name, email, preferred_language, plan, created_at " +
            "FROM \"User\" ORDER BY UsrID";

        try (
            PreparedStatement stmt = conn.prepareStatement(sql); // Prepare list query
            ResultSet rs = stmt.executeQuery()                  // Run immediately
        ) {
            System.out.println();
            System.out.printf(
                "%-8s %-24s %-32s %-16s %-14s %-20s%n",
                "UsrID",
                "Name",
                "Email",
                "Language",
                "Plan",
                "Created"
            );
            System.out.println(
                "-----------------------------------------------------------------------------------------------"
            );

            boolean found = false;                     // Tracks empty result set
            while (rs.next()) {                        // Print each user row
                found = true;
                System.out.printf(
                    "%-8d %-24s %-32s %-16s %-14s %-20s%n",
                    rs.getLong("UsrID"),
                    DatabaseUtil.abbreviate(rs.getString("full_name"), 24),
                    DatabaseUtil.abbreviate(rs.getString("email"), 32),
                    DatabaseUtil.valueOrDash(rs.getString("preferred_language")),
                    rs.getString("plan"),
                    formatTimestamp(rs.getTimestamp("created_at"))
                );
            }

            if (!found) {                              // User table is empty
                System.out.println("No users found.");
            }
        }
    }

    /* ----------------------------- addUser ----------------------------
     * - Purpose: inserts a new user. It shows valid plans, gets required account
     * fields from the terminal, validates the selected plan, generates UsrID,
     * and inserts into the quoted User table.
     * - Pre: Plan table has at least one valid plan row
     * - Post: new User row inserted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void addUser(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        System.out.println("Add User");
        printPlans(conn);                                      // Show valid plan/tier names

        long userId = DatabaseUtil.nextId(conn, "\"User\"", "UsrID"); // New UsrID
        String fullName = io.readRequiredString("Full name: ");
        String email = io.readRequiredString("Email: ");
        String preferredLanguage = io.readOptionalString("Preferred language (blank for none): ");
        String plan = readExistingPlan(conn, io);              // Validate plan exists

        String sql =
            "INSERT INTO \"User\" " +
            "(UsrID, full_name, email, preferred_language, plan) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);                           // User primary key
            stmt.setString(2, fullName);                       // Required full name
            stmt.setString(3, email);                          // Required unique email
            setNullableString(stmt, 4, preferredLanguage);     // Optional language
            stmt.setString(5, plan);                           // Plan/tier name
            stmt.executeUpdate();                              // Insert row
        }

        System.out.println("User added with UsrID " + userId + ".");
    }

    /* ---------------------------- updateUser --------------------------
     * - Purpose: updates one selected user. It lists users, asks for UsrID,
     * validates that the user exists, then repeatedly lets the user update one
     * field until they choose Back.
     * - Pre: selected user exists
     * - Post: selected fields updated, or no update if user backs out
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updateUser(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        listUsers(conn);                                      // Show users before asking id
        long userId = io.readLong("UsrID to update: ");
        if (!DatabaseUtil.userExists(conn, userId)) {         // Validate target user
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        boolean done = false;                                 // Update-menu exit flag
        while (!done) {                                       // Allow multiple field edits
            printUserDetail(conn, userId);                    // Show current values
            System.out.println();
            System.out.println("Update field");
            System.out.println("1. Full name");
            System.out.println("2. Email");
            System.out.println("3. Preferred language");
            System.out.println("4. Plan / tier");
            System.out.println("5. Back");

            int choice = io.readIntInRange("Choose a field: ", 1, 5); // Valid field choice
            switch (choice) {     // Map menu choice to each functionality
                case 1:
                    updateRequiredText(conn, userId, "full_name", "New full name: ", io);
                    break;
                case 2:
                    updateRequiredText(conn, userId, "email", "New email: ", io);
                    break;
                case 3:
                    updatePreferredLanguage(conn, io, userId);
                    break;
                case 4:
                    updatePlan(conn, io, userId);
                    break;
                case 5:
                    done = true;                              // Return to account menu
                    break;
                default:
                    System.out.println("Unknown option.");    // Should not happen
                    break;
            }
        }
    }

    /* ---------------------------- deleteUser --------------------------
     * - Purpose: deletes one selected user while enforcing the assignment rule.
     * It fails if the user has unpaid invoices or open/pending support tickets.
     * If deletion is allowed, it transfers owned workspaces, deletes dependent
     * content/billing rows, deletes the User row, and commits the transaction.
     * - Pre: selected user exists and blocker checks pass
     * - Post: user deleted and transaction committed, or rollback on SQL error
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void deleteUser(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        listUsers(conn);                                      // Show users before asking id
        long userId = io.readLong("UsrID to delete: ");
        if (!DatabaseUtil.userExists(conn, userId)) {         // Validate target user
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        long unpaidInvoices = countUnpaidInvoices(conn, userId);       // Required blocker 1
        long openTickets = countOpenSupportTickets(conn, userId);      // Required blocker 2
        if (unpaidInvoices > 0 || openTickets > 0) {                   // Deletion must fail
            System.out.println();
            System.out.println("Cannot delete this user.");
            if (unpaidInvoices > 0) {
                System.out.println("- Unpaid invoices: " + unpaidInvoices);
            }
            if (openTickets > 0) {
                System.out.println("- Open or pending support tickets: " + openTickets);
            }
            System.out.println("Resolve those records first, then retry deletion.");
            return;
        }

        Map<Long, Long> workspaceTransfers = collectWorkspaceTransfers(conn, io, userId);
        if (workspaceTransfers == null) {                    // User cancelled transfer prompt
            System.out.println("User deletion cancelled.");
            return;
        }

        long conversationCount = countUserRows(conn, "Conversation", "UsrID", userId);
        long messageCount = countMessagesOwnedByUser(conn, userId);
        long invoiceCount = countUserRows(conn, "Invoice", "UsrID", userId);
        long paymentMethodCount = countUserRows(conn, "PaymentMethod", "UsrID", userId);

        System.out.println();
        System.out.println("Deletion summary for UsrID " + userId + ":");
        System.out.println("- Conversations to delete: " + conversationCount);
        System.out.println("- Messages to delete: " + messageCount);
        System.out.println("- Paid/closed billing records to remove: " + invoiceCount);
        System.out.println("- Payment methods to remove: " + paymentMethodCount);
        if (!workspaceTransfers.isEmpty()) {                 // Owned workspace transfers needed
            System.out.println("- Owned workspaces to transfer: " + workspaceTransfers.size());
        }

        if (!io.confirm("Hard-delete this user and their message history")) {
            System.out.println("User deletion cancelled.");
            return;
        }

        boolean oldAutoCommit = conn.getAutoCommit();         // Remember original mode
        try {
            conn.setAutoCommit(false);                        // Start manual transaction
            transferOwnedWorkspaces(conn, workspaceTransfers); // Move owned workspaces away
            deleteDependentContentRows(conn, userId);          // Delete content/support rows
            deleteBillingRows(conn, userId);                   // Delete invoices/payment methods
            deleteUserRow(conn, userId);                       // Delete final User row
            conn.commit();                                     // Commit all-or-nothing delete
            System.out.println(
                "User " + userId + " deleted. Their conversations and messages were deleted."
            );
        } catch (SQLException e) {
            conn.rollback();                                  // Undo partial delete
            throw e;                                          // Let menu print SQL error
        } finally {
            conn.setAutoCommit(oldAutoCommit);                // Restore connection mode
        }
    }

    /* --------------------- updatePreferredLanguage --------------------
     * - Purpose: updates preferred_language for one user. Blank input clears the
     * field by storing SQL NULL.
     * - Pre: selected user exists
     * - Post: preferred_language updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long userId: UsrID to update
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updatePreferredLanguage(Connection conn, ConsoleIO io, long userId)
        throws SQLException {
        String value = io.readOptionalString("New preferred language (blank to clear): ");
        String sql = "UPDATE \"User\" SET preferred_language = ? WHERE UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setNullableString(stmt, 1, value);                // Optional language value
            stmt.setLong(2, userId);                          // Target user
            stmt.executeUpdate();                             // Update row
        }
        System.out.println("Preferred language updated.");
    }

    /* ---------------------------- updatePlan --------------------------
     * - Purpose: updates the selected user's plan/tier. It prints all plans and
     * requires the user to choose an existing plan before running the update.
     * - Pre: selected user exists and chosen plan exists
     * - Post: user plan updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long userId: UsrID to update
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updatePlan(Connection conn, ConsoleIO io, long userId)
        throws SQLException {
        printPlans(conn);                                      // Show valid plan names
        String plan = readExistingPlan(conn, io);              // Validate chosen plan
        String sql = "UPDATE \"User\" SET plan = ? WHERE UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plan);                           // New tier
            stmt.setLong(2, userId);                           // Target user
            stmt.executeUpdate();                              // Update row
        }
        System.out.println("Plan updated.");
    }

    /* ------------------------- readExistingPlan -----------------------
     * - Purpose: asks for a plan/tier name until the user enters one that exists
     * in the Plan table.
     * - Pre: database connection is open
     * - Post: returned plan exists in Plan
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns:
     *      -> String: existing plan/tier name
     * ------------------------------------------------------------------ */
    private static String readExistingPlan(Connection conn, ConsoleIO io) throws SQLException {
        while (true) {                                         // Loop until valid plan
            String plan = io.readRequiredString("Plan / tier: ");
            if (DatabaseUtil.planExists(conn, plan)) {         // Check Plan table
                return plan;
            }
            System.out.println("No plan exists with that name. Choose one from the list.");
        }
    }

    /* ----------------------------- printPlans -------------------------
     * - Purpose: prints all available plan/tier rows so add/update user flows
     * can show valid options.
     * - Pre: Plan table exists
     * - Post: plans printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ------------------------------------------------------------------ */
    public static void printPlans(Connection conn) throws SQLException {
        String sql = "SELECT plan, max_daily_msg, pro_model, cost FROM Plan ORDER BY plan";
        try (
            PreparedStatement stmt = conn.prepareStatement(sql); // Prepare plan query
            ResultSet rs = stmt.executeQuery()                  // Run plan query
        ) {
            System.out.println();
            System.out.println("Available plans");
            System.out.printf("%-18s %-16s %-10s %-10s%n", "Plan", "Max Daily Msg", "Pro Model", "Cost");
            System.out.println("------------------------------------------------------------");
            boolean found = false;                         // Tracks empty Plan table
            while (rs.next()) {                            // Print each plan
                found = true;
                System.out.printf(
                    "%-18s %-16d %-10s %-10s%n",
                    rs.getString("plan"),
                    rs.getLong("max_daily_msg"),
                    rs.getInt("pro_model") == 1 ? "yes" : "no",
                    rs.getInt("cost")
                );
            }
            if (!found) {                                  // No plan rows exist
                System.out.println("No plans found. Insert at least one Plan row first.");
            }
        }
    }

    /* -------------------------- printUserDetail -----------------------
     * - Purpose: prints the current values for one user before update choices
     * are displayed.
     * - Pre: selected user exists
     * - Post: user detail printed if row is found
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID to display
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void printUserDetail(Connection conn, long userId) throws SQLException {
        String sql =
            "SELECT UsrID, full_name, email, preferred_language, plan, created_at " +
            "FROM \"User\" WHERE UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);                          // Bind target user
            try (ResultSet rs = stmt.executeQuery()) {        // Load user row
                if (rs.next()) {                              // Print only if found
                    System.out.println();
                    System.out.println("Current user");
                    System.out.println("UsrID: " + rs.getLong("UsrID"));
                    System.out.println("Name: " + rs.getString("full_name"));
                    System.out.println("Email: " + rs.getString("email"));
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

    /* --------------------- collectWorkspaceTransfers ------------------
     * - Purpose: finds workspaces owned by the user being deleted and asks for a
     * replacement owner for each one. It returns null if the user cancels.
     * - Pre: selected user exists
     * - Post: transfer map prepared, or null returned to cancel delete
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long deletedUserId: UsrID being deleted
     * - Returns:
     *      -> Map<Long, Long>: wsID -> new owner UsrID, or null if cancelled
     * ------------------------------------------------------------------ */
    private static Map<Long, Long> collectWorkspaceTransfers(
        Connection conn,
        ConsoleIO io,
        long deletedUserId
    ) throws SQLException {
        Map<Long, Long> transfers = new LinkedHashMap<Long, Long>(); // Preserve prompt order
        String sql = "SELECT wsID FROM Workspace WHERE owner = ? ORDER BY wsID";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, deletedUserId);                    // Find owned workspaces
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {                            // Prompt once per workspace
                    long workspaceId = rs.getLong("wsID");
                    System.out.println();
                    System.out.println(
                        "User owns workspace " + workspaceId +
                        ". It must be transferred before account deletion."
                    );
                    printWorkspaceMembers(conn, workspaceId);  // Show possible new owners
                    while (true) {                             // Keep asking for valid owner
                        long newOwnerId = io.readLong(
                            "New owner UsrID for workspace " + workspaceId +
                            " (0 to cancel): "
                        );
                        if (newOwnerId == 0) {                 // Cancel whole delete
                            return null;
                        }
                        if (newOwnerId == deletedUserId) {     // Cannot transfer to same user
                            System.out.println("Choose a different user.");
                            continue;
                        }
                        if (!DatabaseUtil.userExists(conn, newOwnerId)) { // Owner must exist
                            System.out.println("No user exists with UsrID " + newOwnerId + ".");
                            continue;
                        }
                        if (!isWorkspaceMember(conn, workspaceId, newOwnerId)) {
                            boolean addMember = io.confirm(
                                "That user is not a workspace member. Add them before transfer"
                            );
                            if (!addMember) {                 // Let user choose another owner
                                continue;
                            }
                        }
                        transfers.put(workspaceId, newOwnerId); // Save transfer choice
                        break;
                    }
                }
            }
        }

        return transfers;                                      // Empty map means no workspaces
    }

    /* ----------------------- printWorkspaceMembers --------------------
     * - Purpose: prints members of one workspace while choosing a new owner for
     * a user deletion transfer.
     * - Pre: workspace exists
     * - Post: workspace members printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to display members for
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void printWorkspaceMembers(Connection conn, long workspaceId)
        throws SQLException {
        String sql =
            "SELECT u.UsrID, u.full_name, wm.role " +
            "FROM WorkspaceMember wm " +
            "JOIN \"User\" u ON u.UsrID = wm.UsrID " +
            "WHERE wm.wsID = ? ORDER BY wm.role, u.UsrID";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);                    // Bind target workspace
            try (ResultSet rs = stmt.executeQuery()) {       // Load members
                System.out.println("Current members");
                System.out.printf("%-8s %-26s %-12s%n", "UsrID", "Name", "Role");
                System.out.println("-----------------------------------------------");
                boolean found = false;                       // Tracks empty membership list
                while (rs.next()) {                          // Print each member
                    found = true;
                    System.out.printf(
                        "%-8d %-26s %-12s%n",
                        rs.getLong("UsrID"),
                        DatabaseUtil.abbreviate(rs.getString("full_name"), 26),
                        rs.getString("role")
                    );
                }
                if (!found) {                                // Should be unusual for workspace
                    System.out.println("No workspace members found.");
                }
            }
        }
    }

    /* ---------------------- transferOwnedWorkspaces -------------------
     * - Purpose: applies the workspace transfers collected during user deletion.
     * If the chosen new owner is not already a member, this inserts them as an
     * admin member first, then updates Workspace.owner and fixes owner roles.
     * - Pre: transfer map contains valid users/workspaces
     * - Post: every owned workspace has a new owner
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> Map<Long, Long> workspaceTransfers: wsID -> new owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void transferOwnedWorkspaces(
        Connection conn,
        Map<Long, Long> workspaceTransfers
    ) throws SQLException {
        for (Map.Entry<Long, Long> entry : workspaceTransfers.entrySet()) { // Each transfer
            long workspaceId = entry.getKey();                // Workspace being moved
            long newOwnerId = entry.getValue();               // Replacement owner
            long oldOwnerId = getWorkspaceOwner(conn, workspaceId); // Current owner
            if (!isWorkspaceMember(conn, workspaceId, newOwnerId)) {
                insertWorkspaceMember(conn, workspaceId, newOwnerId); // Add member first
            }
            String sql = "UPDATE Workspace SET owner = ? WHERE wsID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, newOwnerId);                  // New owner
                stmt.setLong(2, workspaceId);                 // Target workspace
                stmt.executeUpdate();                         // Update Workspace.owner
            }
            promoteWorkspaceOwner(conn, workspaceId, newOwnerId); // New owner role
            demoteOldWorkspaceOwner(conn, workspaceId, oldOwnerId); // Old owner role
        }
    }

    /* ------------------------ getWorkspaceOwner ----------------------
     * - Purpose: reads the current owner of a workspace before transferring it
     * so Java can demote the previous owner after Workspace.owner changes.
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
            stmt.setLong(1, workspaceId);                    // Workspace id
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("owner");              // Current owner
                }
            }
        }
        throw new SQLException("Workspace " + workspaceId + " was not found.");
    }

    /* ---------------------- insertWorkspaceMember ---------------------
     * - Purpose: adds a user to a workspace as admin when they need to become
     * owner during deletion transfer but are not yet a member.
     * - Pre: workspace and user exist and pair is not already a member
     * - Post: WorkspaceMember row inserted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to add user to
     *      -> long userId: UsrID to add
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void insertWorkspaceMember(Connection conn, long workspaceId, long userId)
        throws SQLException {
        String sql =
            "INSERT INTO WorkspaceMember (wsID, UsrID, role) VALUES (?, ?, 'admin')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);                    // Workspace id
            stmt.setLong(2, userId);                         // New member id
            stmt.executeUpdate();                            // Insert membership
        }
    }

    /* ---------------------- promoteWorkspaceOwner --------------------
     * - Purpose: updates the replacement workspace owner's membership row to
     * role owner after Workspace.owner has been transferred.
     * - Pre: Workspace.owner already equals userId and membership row exists
     * - Post: replacement owner's WorkspaceMember role is owner
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID whose owner changed
     *      -> long userId: replacement owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void promoteWorkspaceOwner(Connection conn, long workspaceId, long userId)
        throws SQLException {
        String sql = "UPDATE WorkspaceMember SET role = 'owner' WHERE wsID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);                    // Workspace id
            stmt.setLong(2, userId);                         // New owner id
            stmt.executeUpdate();                            // Promote owner role
        }
    }

    /* -------------------- demoteOldWorkspaceOwner --------------------
     * - Purpose: changes the previous workspace owner's membership role back
     * to admin after Workspace.owner has been transferred.
     * - Pre: Workspace.owner no longer equals oldOwnerId
     * - Post: previous owner no longer has role owner for that workspace
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID whose owner changed
     *      -> long oldOwnerId: previous owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void demoteOldWorkspaceOwner(Connection conn, long workspaceId, long oldOwnerId)
        throws SQLException {
        String sql =
            "UPDATE WorkspaceMember SET role = 'admin' " +
            "WHERE wsID = ? AND UsrID = ? AND role = 'owner'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);                    // Workspace id
            stmt.setLong(2, oldOwnerId);                     // Previous owner id
            stmt.executeUpdate();                            // Demote old owner role
        }
    }

    /* ------------------------- isWorkspaceMember ----------------------
     * - Purpose: checks whether a user is already a member of a workspace.
     * - Pre: database connection is open
     * - Post: caller knows whether the membership pair exists
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long workspaceId: wsID to check
     *      -> long userId: UsrID to check
     * - Returns:
     *      -> boolean: true if WorkspaceMember contains the pair
     * ------------------------------------------------------------------ */
    private static boolean isWorkspaceMember(Connection conn, long workspaceId, long userId)
        throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM WorkspaceMember WHERE wsID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);                    // Bind wsID
            stmt.setLong(2, userId);                         // Bind UsrID
            try (ResultSet rs = stmt.executeQuery()) {       // Run count query
                rs.next();                                   // Count returns one row
                return rs.getLong(1) > 0;                    // True if member exists
            }
        }
    }

    /* ------------------------ countUnpaidInvoices ---------------------
     * - Purpose: counts invoices that make user deletion fail. The program
     * treats draft, open, failed, and overdue as unpaid for this requirement.
     * - Pre: selected user exists
     * - Post: unpaid invoice count returned
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID to check
     * - Returns:
     *      -> long: number of unpaid invoices
     * ------------------------------------------------------------------ */
    private static long countUnpaidInvoices(Connection conn, long userId) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM Invoice " +
            "WHERE UsrID = ? AND status IN ('draft', 'open', 'failed', 'overdue')";
        return DatabaseUtil.count(conn, sql, userId);        // Reuse count helper
    }

    /* --------------------- countOpenSupportTickets --------------------
     * - Purpose: counts support tickets that make user deletion fail. The
     * program treats open and pending tickets as still unresolved.
     * - Pre: selected user exists
     * - Post: open/pending ticket count returned
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID to check
     * - Returns:
     *      -> long: number of open/pending support tickets
     * ------------------------------------------------------------------ */
    private static long countOpenSupportTickets(Connection conn, long userId)
        throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM SupportTicket " +
            "WHERE UsrID = ? AND status IN ('open', 'pending')";
        return DatabaseUtil.count(conn, sql, userId);        // Reuse count helper
    }

    /* -------------------------- countUserRows -------------------------
     * - Purpose: counts rows in one table that belong to the selected user.
     * Used for deletion summary output.
     * - Pre: table/column strings are trusted constants from this file
     * - Post: count returned for summary
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> String tableName: table to count rows in
     *      -> String userColumn: column that stores UsrID
     *      -> long userId: UsrID to count for
     * - Returns:
     *      -> long: number of rows for that user
     * ------------------------------------------------------------------ */
    private static long countUserRows(
        Connection conn,
        String tableName,
        String userColumn,
        long userId
    ) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM " + tableName + " WHERE " + userColumn + " = ?";
        return DatabaseUtil.count(conn, sql, userId);        // Run one-parameter count
    }

    /* ---------------------- countMessagesOwnedByUser ------------------
     * - Purpose: counts messages that belong to conversations owned by the
     * selected user. Used for deletion summary output.
     * - Pre: selected user exists
     * - Post: message count returned for summary
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID to count messages for
     * - Returns:
     *      -> long: number of messages in user's conversations
     * ------------------------------------------------------------------ */
    private static long countMessagesOwnedByUser(Connection conn, long userId)
        throws SQLException {
        String sql =
            "SELECT COUNT(*) " +
            "FROM Message m " +
            "JOIN Conversation c ON c.CID = m.CID " +
            "WHERE c.UsrID = ?";
        return DatabaseUtil.count(conn, sql, userId);        // Count joined messages
    }

    /* ------------------------- deleteBillingRows ----------------------
     * - Purpose: deletes invoices and payment methods for a user after the
     * unpaid-invoice blocker has already passed.
     * - Pre: selected user has no unpaid invoices
     * - Post: Invoice and PaymentMethod rows for the user are deleted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID being deleted
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void deleteBillingRows(Connection conn, long userId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Invoice WHERE UsrID = ?")) {
            stmt.setLong(1, userId);                         // Target invoices
            stmt.executeUpdate();                            // Delete invoice rows
        }

        try (
            PreparedStatement stmt =
                conn.prepareStatement("DELETE FROM PaymentMethod WHERE UsrID = ?")
        ) {
            stmt.setLong(1, userId);                         // Target payment methods
            stmt.executeUpdate();                            // Delete payment method rows
        }
    }

    /* --------------------- deleteDependentContentRows -----------------
     * - Purpose: deletes rows that depend on the user before deleting the User
     * row. This handles the user's message history by deleting ratings,
     * bookmarks, messages, and conversations, then removes prompts/personas,
     * closed support tickets, and workspace memberships.
     * - Pre: deletion blockers passed and transaction is open
     * - Post: dependent non-billing rows for the user are deleted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID being deleted
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void deleteDependentContentRows(Connection conn, long userId)
        throws SQLException {
        executeUserDelete(
            conn,
            "DELETE FROM MsgRating " +
            "WHERE MsgID IN (" +
            "    SELECT m.MsgID " +
            "    FROM Message m " +
            "    JOIN Conversation c ON c.CID = m.CID " +
            "    WHERE c.UsrID = ?" +
            ")",
            userId
        );                                                  // Delete ratings on owned messages
        executeUserDelete(
            conn,
            "DELETE FROM Bookmark " +
            "WHERE MsgID IN (" +
            "    SELECT m.MsgID " +
            "    FROM Message m " +
            "    JOIN Conversation c ON c.CID = m.CID " +
            "    WHERE c.UsrID = ?" +
            ")",
            userId
        );                                                  // Delete bookmarks pointing to owned messages
        executeUserDelete(conn, "DELETE FROM Bookmark WHERE UsrID = ?", userId); // Delete user's bookmarks
        executeUserDelete(
            conn,
            "DELETE FROM Message " +
            "WHERE CID IN (SELECT CID FROM Conversation WHERE UsrID = ?)",
            userId
        );                                                  // Delete messages in user's conversations
        executeUserDelete(conn, "DELETE FROM Conversation WHERE UsrID = ?", userId);
        executeUserDelete(conn, "DELETE FROM Prompt WHERE UsrID = ?", userId);
        executeUserDelete(conn, "DELETE FROM Persona WHERE UsrID = ?", userId);
        executeUserDelete(conn, "DELETE FROM SupportTicket WHERE UsrID = ?", userId);
        executeUserDelete(conn, "DELETE FROM WorkspaceMember WHERE UsrID = ?", userId);
    }

    /* -------------------------- executeUserDelete ---------------------
     * - Purpose: small helper for delete statements that all take one UsrID
     * parameter. It avoids repeating PreparedStatement setup.
     * - Pre: sql has exactly one ? placeholder
     * - Post: delete statement executed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> String sql: delete SQL with one user id parameter
     *      -> long userId: UsrID to bind
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void executeUserDelete(Connection conn, String sql, long userId)
        throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);                         // Bind UsrID
            stmt.executeUpdate();                            // Execute delete
        }
    }

    /* ---------------------------- deleteUserRow -----------------------
     * - Purpose: deletes the final row from the User table after blockers and
     * dependent rows have already been handled.
     * - Pre: dependent rows have been deleted or transferred
     * - Post: User row deleted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID being deleted
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void deleteUserRow(Connection conn, long userId) throws SQLException {
        String sql = "DELETE FROM \"User\" WHERE UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);                         // Target User row
            stmt.executeUpdate();                            // Delete user
        }
    }

    /* ------------------------- updateRequiredText ---------------------
     * - Purpose: updates one required text column in the User table. Used for
     * full_name and email updates.
     * - Pre: selected user exists and columnName is a trusted User column
     * - Post: selected text column updated
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID to update
     *      -> String columnName: User column to update
     *      -> String prompt: prompt for new value
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updateRequiredText(
        Connection conn,
        long userId,
        String columnName,
        String prompt,
        ConsoleIO io
    ) throws SQLException {
        String value = io.readRequiredString(prompt);         // New required value
        String sql = "UPDATE \"User\" SET " + columnName + " = ? WHERE UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);                         // New field value
            stmt.setLong(2, userId);                          // Target user
            stmt.executeUpdate();                             // Update row
        }
        System.out.println("User updated.");
    }

    /* ------------------------- setNullableString ----------------------
     * - Purpose: binds an optional String into a PreparedStatement. Blank/null
     * values become SQL NULL.
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
        if (value == null || value.trim().isEmpty()) {        // Missing optional string
            stmt.setNull(index, Types.VARCHAR);
        } else {
            stmt.setString(index, value);                     // Real string value
        }
    }

    /* ------------------------- formatTimestamp ------------------------
     * - Purpose: converts a SQL Timestamp into display text. Null timestamps
     * show as '-' to keep terminal output clean.
     * - Pre: timestamp may be null
     * - Post: timestamp converted to display string
     * - Params:
     *      -> Timestamp timestamp: SQL timestamp value to display
     * - Returns:
     *      -> String: timestamp text or '-'
     * ------------------------------------------------------------------ */
    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {                              // Missing timestamp
            return "-";
        }
        return timestamp.toString();                          // Default JDBC timestamp format
    }

    /* -------------------------- rollbackQuietly -----------------------
     * - Purpose: rolls back a failed transaction if auto-commit was turned off,
     * and then restores auto-commit. It intentionally ignores rollback errors
     * because the original SQL error is more useful to print.
     * - Pre: connection may or may not be in manual transaction mode
     * - Post: connection is back in auto-commit mode when possible
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void rollbackQuietly(Connection conn) {
        try {
            if (!conn.getAutoCommit()) {                      // Only rollback manual tx
                conn.rollback();                              // Undo open transaction
                conn.setAutoCommit(true);                     // Restore default mode
            }
        } catch (SQLException ignored) {
            // The original SQL error is more useful to the user.
        }
    }
}
