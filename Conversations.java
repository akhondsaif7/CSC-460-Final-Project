/* ------------------- class Conversations ------------------------ *
 * Class: CSC460 - S26
 * Author: Matteo Galiano
 * Packages: java.sql.*
 * Purpose: This class handles the terminal menu for conversations and messages.
 * It can start a conversation for an existing user, optionally attach a persona
 * and workspace, add messages to a conversation, insert/update message feedback,
 * and list conversations/messages for review. It owns the SQL for Conversation,
 * Message, and MsgRating workflows currently implemented in the app.
 * Language: Java 25
 * Methods:
 *  - showMenu: displays the conversations/messages menu
 *  - startConversation: inserts a new Conversation row
 *  - addMessage: inserts a new Message row
 *  - updateMessageFeedback: inserts or updates MsgRating feedback
 *  - listConversationsForUser: prints conversations for one user
 *  - listMessagesInConversation: prints messages for one conversation
 *  - choosePersona, chooseWorkspace: optional selection helpers
 *  - printConversationSummary, printMessageDetail: display helpers
 *  - personaBelongsToUser, userIsWorkspaceMember, messageRatingExists: checks
 *  - setNullableLong, setNullableClobText, nullable labels, formatTimestamp: helpers
 * ------------------------------------------------------ */

import java.sql.*;

public class Conversations {

    /* ------------------------ Conversations --------------------------
     * - Purpose: private constructor so nobody creates a Conversations object.
     * This file is used through static menu/helper methods.
     * - Pre: class exists
     * - Post: object construction is blocked
     * - Params: None
     * - Returns: None
     * ------------------------------------------------------------------ */
    private Conversations() {
    }

    /* ---------------------------- showMenu ----------------------------
     * - Purpose: displays the conversations/messages submenu and routes each
     * user choice to the method that owns that operation.
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
            System.out.println("------ Conversations and Messages ------");
            System.out.println("1. Start a new conversation");
            System.out.println("2. Add message to a conversation");
            System.out.println("3. Update/add message rating and feedback");
            System.out.println("4. List conversations for a user");
            System.out.println("5. List messages in a conversation");
            System.out.println("6. Back");

            int choice = io.readIntInRange("Choose an option: ", 1, 6); // Valid menu choice
            try {
                switch (choice) {
                    case 1:
                        startConversation(conn, io);        // Insert Conversation
                        break;
                    case 2:
                        addMessage(conn, io);               // Insert Message
                        break;
                    case 3:
                        updateMessageFeedback(conn, io);    // Insert/update MsgRating
                        break;
                    case 4:
                        listConversationsForUser(conn, io); // Display conversations
                        io.pause();                         // Let user read output
                        break;
                    case 5:
                        listMessagesInConversation(conn, io); // Display messages
                        io.pause();                         // Let user read output
                        break;
                    case 6:
                        done = true;                        // Return to main menu
                        break;
                    default:
                        System.out.println("Unknown option."); // Should not happen
                        break;
                }
            } catch (SQLException e) {                      // Oracle operation failed
                System.err.println("Conversation/message operation failed.");
                DatabaseUtil.printSqlError(e);              // Print readable SQL error
            }
        }
    }

    /* ------------------------ startConversation -----------------------
     * - Purpose: creates a new conversation. It asks for the owner user, lets the
     * user optionally choose a persona and workspace that are valid for that
     * owner, asks for a title, generates a CID, and inserts Conversation.
     * - Pre: chosen user exists, optional persona/workspace selections are valid
     * - Post: new Conversation row inserted, or operation cancelled by bad user
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void startConversation(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        System.out.println("Start New Conversation");
        UserAccounts.listUsers(conn);                         // Show possible owners
        long userId = io.readLong("Conversation owner UsrID: ");
        if (!DatabaseUtil.userExists(conn, userId)) {         // Validate owner user
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        Long personaId = choosePersona(conn, io, userId);     // Optional persona
        Long workspaceId = chooseWorkspace(conn, io, userId); // Optional workspace
        String title = io.readOptionalString("Conversation title (blank for generated title): ");

        long conversationId = DatabaseUtil.nextId(conn, "Conversation", "CID"); // New CID
        if (title == null) {                                  // Generate title if blank
            title = "Conversation " + conversationId;
        }

        String sql =
            "INSERT INTO Conversation (CID, UsrID, personaID, wsID, title) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);                  // Conversation.CID
            stmt.setLong(2, userId);                          // Conversation owner
            setNullableLong(stmt, 3, personaId);              // Optional personaID
            setNullableLong(stmt, 4, workspaceId);            // Optional wsID
            stmt.setString(5, title);                         // Conversation title
            stmt.executeUpdate();                             // Insert row
        }

        System.out.println("Conversation created with CID " + conversationId + ".");
    }

    /* --------------------------- addMessage ---------------------------
     * - Purpose: adds one message to an existing conversation. It first lists
     * available conversations for easier CID selection, validates the CID,
     * prints a quick conversation summary, reads sender/content, generates
     * MsgID, and inserts Message.
     * - Pre: conversation exists and content/sender are non-empty
     * - Post: new Message row inserted, or operation stops for bad CID
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void addMessage(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        System.out.println("Add Message");
        listConversationChoices(conn);
        long conversationId = io.readLong("Conversation ID (CID): ");
        if (!DatabaseUtil.conversationExists(conn, conversationId)) { // Validate CID
            System.out.println("No conversation exists with CID " + conversationId + ".");
            return;
        }
        printConversationSummary(conn, conversationId);       // Show target conversation

        if (PlanManagement.exceededLimit(conn, conversationId)) {
            System.out.println("Exceeded daily message limit!");
            System.out.println("Cannot add any more messages!");
            return;
        }

        String sender = io.readRequiredString("Sender (user, normal, pro): ").toLowerCase();
        while (
            !("user".equals(sender) || "normal".equals(sender) || "pro".equals(sender))
        ) {
            System.out.println("Sender must be one of: user, normal, pro.");
            sender = io.readRequiredString("Sender (user, normal, pro): ").toLowerCase();
        }
        String content = io.readRequiredString("Message content: ");
        long messageId = DatabaseUtil.nextId(conn, "Message", "MsgID"); // New MsgID

        String sql =
            "INSERT INTO Message (MsgID, CID, sender, content) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);                       // Message.MsgID
            stmt.setLong(2, conversationId);                  // Parent conversation
            stmt.setString(3, sender);                        // Sender label
            stmt.setString(4, content);                       // Message CLOB text
            stmt.executeUpdate();                             // Insert row
        }

        System.out.println("Message added with MsgID " + messageId + ".");
    }

    /* --------------------- listConversationChoices --------------------
     * - Purpose: prints all current conversations with CID, owner, and created
     * timestamp so message-related flows can ask for CID without forcing the
     * user to guess ids.
     * - Pre: Conversation and User tables exist
     * - Post: conversation choices printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void listConversationChoices(Connection conn) throws SQLException {
        // Fetch the conversation id plus just enough context for selection.
        String sql =
            "SELECT c.CID, c.title, c.UsrID, u.full_name, c.created_at " +
            "FROM Conversation c " +
            "JOIN \"User\" u ON u.UsrID = c.UsrID " +
            "ORDER BY c.created_at DESC, c.CID DESC";   // Query to execute

        try (
            // Run the query once and stream rows for table output.
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            // Header keeps CID selection readable in the terminal.
            System.out.println();
            System.out.println("Available conversations");
            System.out.printf(
                "%-8s %-32s %-8s %-24s %-20s%n",
                "CID",
                "Title",
                "UsrID",
                "Owner",
                "Created"
            );
            System.out.println(
                "--------------------------------------------------------------------------------------------------------"
            );   // Show the existent conversations

            // Used to detect and report an empty conversation table.
            boolean found = false;
            // Print one line per conversation.
            while (rs.next()) {
                found = true;
                System.out.printf(
                    "%-8d %-32s %-8d %-24s %-20s%n",
                    rs.getLong("CID"),
                    DatabaseUtil.abbreviate(rs.getString("title"), 32),
                    rs.getLong("UsrID"),
                    DatabaseUtil.abbreviate(rs.getString("full_name"), 24),
                    formatTimestamp(rs.getTimestamp("created_at"))
                );    // Print info of conversation chosen
            }

            // Keep feedback explicit if there are no rows.
            if (!found) { System.out.println("No conversations found."); }
            System.out.println();
        }
    }

    /* ---------------------- updateMessageFeedback ---------------------
     * - Purpose: creates or updates feedback for one message. It first lists
     * available messages with MsgID context, then if MsgRating already has a
     * row for the MsgID it updates rating/feedback/rated_at; otherwise it
     * inserts a new MsgRating row.
     * - Pre: message exists and rating choice is thumbs up, thumbs down, or unrated
     * - Post: MsgRating row exists with the selected rating and feedback
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void updateMessageFeedback
                            (Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        System.out.println("Update/Add Message Feedback");
        if (!listMessageChoices(conn)) {
            return;
        }
        long messageId = io.readLong("Message ID (MsgID): ");
        if (!DatabaseUtil.messageExists(conn, messageId)) {   // Validate MsgID
            System.out.println("No message exists with MsgID " + messageId + ".");
            return;
        }

        printMessageDetail(conn, messageId);                  // Show current rating/feedback
        int ratingChoice = io.readIntInRange("Rating (1 thumbs up, 2 thumbs down, 3 unrated): ", 1, 3);
        String feedback = io.readOptionalString("User feedback (blank for none): ");

        if (messageRatingExists(conn, messageId)) {           // Existing feedback row
            String sql =
                "UPDATE MsgRating " +
                "SET rating = ?, usr_feedback = ?, rated_at = SYSTIMESTAMP " +
                "WHERE MsgID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setNullableRating(stmt, 1, ratingChoice);
                setNullableClobText(stmt, 2, feedback);       // Optional feedback CLOB
                stmt.setLong(3, messageId);                   // Target MsgID
                stmt.executeUpdate();                         // Update row
            }
            System.out.println("Message feedback updated.");
        } else {                                              // No feedback row yet
            String sql =
                "INSERT INTO MsgRating (MsgID, rating, usr_feedback) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, messageId);                   // MsgRating.MsgID
                setNullableRating(stmt, 2, ratingChoice);
                setNullableClobText(stmt, 3, feedback);       // Optional feedback CLOB
                stmt.executeUpdate();                         // Insert row
            }
            System.out.println("Message feedback added.");
        }
    }

    /* -------------------- listConversationsForUser --------------------
     * - Purpose: lists conversations owned by one user. The output includes CID,
     * title, created timestamp, workspace id, persona name, and message count.
     * - Pre: selected user exists
     * - Post: conversations printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void listConversationsForUser(Connection conn, ConsoleIO io)
        throws SQLException {
        System.out.println();
        UserAccounts.listUsers(conn);
        long userId = io.readLong("UsrID: ");
        if (!DatabaseUtil.userExists(conn, userId)) {         // Validate user
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        // Left joins keep conversations even if they have no persona/messages
        String sql =
            "SELECT c.CID, c.title, c.created_at, c.wsID, p.name AS persona_name, " +
            "       COUNT(m.MsgID) AS message_count " +
            "FROM Conversation c " +
            "LEFT JOIN Persona p ON p.personaID = c.personaID AND p.UsrID = c.UsrID " +
            "LEFT JOIN Message m ON m.CID = c.CID " +
            "WHERE c.UsrID = ? " +
            "GROUP BY c.CID, c.title, c.created_at, c.wsID, p.name " +
            "ORDER BY c.created_at DESC, c.CID DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);                         // Bind owner user
            try (ResultSet rs = stmt.executeQuery()) {       // Run list query
                System.out.println();
                System.out.printf(
                    "%-8s %-30s %-20s %-10s %-22s %-8s%n",
                    "CID",
                    "Title",
                    "Created",
                    "WS",
                    "Persona",
                    "Msgs"
                );
                System.out.println(
                    "------------------------------------------------------------------------------------------------"
                );
                boolean found = false;                       // Tracks empty result set
                while (rs.next()) {                          // Print each conversation
                    found = true;
                    System.out.printf(
                        "%-8d %-30s %-20s %-10s %-22s %-8d%n",
                        rs.getLong("CID"),
                        DatabaseUtil.abbreviate(rs.getString("title"), 30),
                        formatTimestamp(rs.getTimestamp("created_at")),
                        nullableLongLabel(rs, "wsID"),
                        DatabaseUtil.valueOrDash(rs.getString("persona_name")),
                        rs.getLong("message_count")
                    );
                }
                if (!found) {                                // User owns no conversations
                    System.out.println("No conversations found for this user.");
                }
            }
        }
    }

    /* ------------------ listMessagesInConversation --------------------
     * - Purpose: lists messages in one conversation. It also shows any rating
     * and feedback preview from MsgRating when that row exists.
     * - Pre: selected conversation exists
     * - Post: messages printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void listMessagesInConversation(Connection conn, ConsoleIO io)
        throws SQLException {
        System.out.println();
        listConversationChoices(conn);
        long conversationId = io.readLong("Conversation ID (CID): ");
        if (!DatabaseUtil.conversationExists(conn, conversationId)) { // Validate CID
            System.out.println("No conversation exists with CID " + conversationId + ".");
            return;
        }

        printConversationSummary(conn, conversationId);       // Show context first
        String sql =
            "SELECT m.MsgID, m.sender, DBMS_LOB.SUBSTR(m.content, 120, 1) AS content_preview, " +
            "       m.sent_at, r.rating, DBMS_LOB.SUBSTR(r.usr_feedback, 80, 1) AS feedback " +
            "FROM Message m " +
            "LEFT JOIN MsgRating r ON r.MsgID = m.MsgID " +
            "WHERE m.CID = ? " +
            "ORDER BY m.sent_at, m.MsgID";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);                 // Bind target CID
            try (ResultSet rs = stmt.executeQuery()) {       // Run message list query
                System.out.println();
                System.out.printf(
                    "%-8s %-14s %-20s %-8s %-42s %-30s%n",
                    "MsgID",
                    "Sender",
                    "Sent",
                    "Rating",
                    "Message",
                    "Feedback"
                );
                System.out.println(
                    "----------------------------------------------------------------------------------------------------------------"
                );
                boolean found = false;                       // Tracks empty result set
                while (rs.next()) {                          // Print each message
                    found = true;
                    System.out.printf(
                        "%-8d %-14s %-20s %-8s %-42s %-30s%n",
                        rs.getLong("MsgID"),
                        DatabaseUtil.abbreviate(rs.getString("sender"), 14),
                        formatTimestamp(rs.getTimestamp("sent_at")),
                        ratingLabel(rs, "rating"),
                        DatabaseUtil.abbreviate(rs.getString("content_preview"), 42),
                        DatabaseUtil.abbreviate(rs.getString("feedback"), 30)
                    );
                }
                if (!found) {                                // Conversation has no messages
                    System.out.println("No messages found in this conversation.");
                }
            }
        }
    }

    /* ------------------------ listMessageChoices ----------------------
     * - Purpose: prints all current messages with MsgID, conversation owner,
     * sender, timestamp, and preview text so rating/feedback flows can select
     * the correct message id.
     * - Pre: Message, Conversation, and User tables exist
     * - Post: message choices printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     * - Returns:
     *      -> boolean: true if at least one message exists, false if none
     * ------------------------------------------------------------------ */
    private static boolean listMessageChoices(Connection conn) throws SQLException {
        // Pull all message ids with conversation/user context for feedback edits.
        String sql =
            "SELECT m.MsgID, m.CID, c.UsrID, u.full_name, m.sender, " +
            "       DBMS_LOB.SUBSTR(m.content, 80, 1) AS content_preview, m.sent_at " +
            "FROM Message m " +
            "JOIN Conversation c ON c.CID = m.CID " +
            "JOIN \"User\" u ON u.UsrID = c.UsrID " +
            "ORDER BY m.sent_at DESC, m.MsgID DESC";

        try (
            // Execute the lookup query and iterate over every message option.
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            // Print a chooser table before prompting for MsgID.
            System.out.println();
            System.out.println("Available messages");
            System.out.printf(
                "%-8s %-8s %-8s %-22s %-10s %-20s %-34s%n",
                "MsgID",
                "CID",
                "UsrID",
                "Owner",
                "Sender",
                "Sent",
                "Preview"
            );
            System.out.println(
                "------------------------------------------------------------------------------------------------------------------------------"
            );

            // Return value indicates whether caller should continue prompting.
            boolean found = false;
            // Emit each candidate message row.
            while (rs.next()) {
                found = true;
                System.out.printf(
                    "%-8d %-8d %-8d %-22s %-10s %-20s %-34s%n",
                    rs.getLong("MsgID"),
                    rs.getLong("CID"),
                    rs.getLong("UsrID"),
                    DatabaseUtil.abbreviate(rs.getString("full_name"), 22),
                    DatabaseUtil.abbreviate(rs.getString("sender"), 10),
                    formatTimestamp(rs.getTimestamp("sent_at")),
                    DatabaseUtil.abbreviate(rs.getString("content_preview"), 34)
                );
            }

            // No rows means no valid MsgID can be chosen in this flow.
            if (!found) {
                System.out.println("No messages found.");
                return false;
            }
            // Spacing before the next prompt keeps the UI readable.
            System.out.println();
            return true;
        }
    }

    /* -------------------------- choosePersona -------------------------
     * - Purpose: lists personas owned by the selected user and lets the user
     * choose one, or choose 0 for no persona. It validates ownership before
     * returning a personaID.
     * - Pre: selected user exists
     * - Post: returned personaID belongs to user, or null means no persona
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long userId: conversation owner UsrID
     * - Returns:
     *      -> Long: selected personaID, or null for none
     * ------------------------------------------------------------------ */
    private static Long choosePersona(Connection conn, ConsoleIO io, long userId)
        throws SQLException {
        String sql =
            "SELECT personaID, name, status FROM Persona " +
            "WHERE UsrID = ? ORDER BY personaID";
        boolean found = false;                               // Tracks if user has personas
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);                         // Bind owner user
            try (ResultSet rs = stmt.executeQuery()) {       // Load user's personas
                System.out.println();
                System.out.println("Personas for this user");
                System.out.printf("%-12s %-28s %-12s%n", "PersonaID", "Name", "Status");
                System.out.println("-------------------------------------------------------");
                while (rs.next()) {                          // Print each persona
                    found = true;
                    System.out.printf(
                        "%-12d %-28s %-12s%n",
                        rs.getLong("personaID"),
                        DatabaseUtil.abbreviate(rs.getString("name"), 28),
                        rs.getString("status")
                    );
                }
            }
        }

        if (!found) {                                        // No persona rows to choose
            System.out.println("No personas found. Conversation will not use a persona.");
            return null;
        }

        while (true) {                                       // Keep asking until valid id/0
            long personaId = io.readLong("PersonaID to use (0 for none): ");
            if (personaId == 0) {                            // User chose no persona
                return null;
            }
            if (personaBelongsToUser(conn, personaId, userId)) { // Verify ownership
                return personaId;
            }
            System.out.println("That persona does not belong to this user.");
        }
    }

    /* ------------------------- chooseWorkspace ------------------------
     * - Purpose: lists workspaces where the selected user is a member and lets
     * the user choose one, or choose 0 for personal/no workspace. It verifies
     * membership before returning a wsID.
     * - Pre: selected user exists
     * - Post: returned wsID has the user as a member, or null means personal
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     *      -> long userId: conversation owner UsrID
     * - Returns:
     *      -> Long: selected wsID, or null for no workspace
     * ------------------------------------------------------------------ */
    private static Long chooseWorkspace(Connection conn, ConsoleIO io, long userId)
        throws SQLException {
        String sql =
            "SELECT w.wsID, wm.role, w.plan, w.created_at " +
            "FROM Workspace w " +
            "JOIN WorkspaceMember wm ON wm.wsID = w.wsID " +
            "WHERE wm.UsrID = ? ORDER BY w.wsID";
        boolean found = false;                               // Tracks if memberships exist
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);                         // Bind member user
            try (ResultSet rs = stmt.executeQuery()) {       // Load user's workspaces
                System.out.println();
                System.out.println("Workspaces this user can use");
                System.out.printf(
                    "%-10s %-12s %-14s %-20s%n",
                    "wsID",
                    "Role",
                    "Plan",
                    "Created"
                );
                System.out.println("--------------------------------------------------------------");
                while (rs.next()) {                          // Print each workspace
                    found = true;
                    System.out.printf(
                        "%-10d %-12s %-14s %-20s%n",
                        rs.getLong("wsID"),
                        rs.getString("role"),
                        rs.getString("plan"),
                        formatTimestamp(rs.getTimestamp("created_at"))
                    );
                }
            }
        }

        if (!found) {                                        // No memberships to choose
            System.out.println("No workspace memberships found. Conversation will be personal.");
            return null;
        }

        while (true) {                                       // Keep asking until valid wsID/0
            long workspaceId = io.readLong("Workspace wsID to use (0 for personal): ");
            if (workspaceId == 0) {                          // User chose personal conversation
                return null;
            }
            if (userIsWorkspaceMember(conn, workspaceId, userId)) { // Verify membership
                return workspaceId;
            }
            System.out.println("That user is not a member of this workspace.");
        }
    }

    /* --------------------- printConversationSummary -------------------
     * - Purpose: prints one compact line describing a conversation. Used before
     * adding/listing messages so the user can confirm the selected CID.
     * - Pre: conversation exists
     * - Post: summary printed if row is found
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long conversationId: CID to summarize
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void printConversationSummary(Connection conn, long conversationId)
        throws SQLException {
        String sql =
            "SELECT c.CID, c.title, c.UsrID, u.full_name, c.created_at " +
            "FROM Conversation c " +
            "JOIN \"User\" u ON u.UsrID = c.UsrID " +
            "WHERE c.CID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);                 // Bind target CID
            try (ResultSet rs = stmt.executeQuery()) {       // Load summary row
                if (rs.next()) {                             // Print only if found
                    System.out.println();
                    System.out.println(
                        "Conversation " + rs.getLong("CID") +
                        " | " + DatabaseUtil.valueOrDash(rs.getString("title")) +
                        " | owner " + rs.getLong("UsrID") +
                        " (" + rs.getString("full_name") + ")" +
                        " | created " + formatTimestamp(rs.getTimestamp("created_at"))
                    );
                }
            }
        }
    }

    /* ------------------------ printMessageDetail ----------------------
     * - Purpose: prints one message with its conversation, sender, sent time,
     * content preview, current rating, and current feedback. Used before asking
     * for new feedback so the user sees what they are editing.
     * - Pre: message exists
     * - Post: message detail printed if row is found
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long messageId: MsgID to display
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void printMessageDetail(Connection conn, long messageId)
        throws SQLException {
        String sql =
            "SELECT m.MsgID, m.sender, DBMS_LOB.SUBSTR(m.content, 240, 1) AS content_preview, " +
            "       m.sent_at, c.CID, c.title, r.rating, " +
            "       DBMS_LOB.SUBSTR(r.usr_feedback, 120, 1) AS feedback " +
            "FROM Message m " +
            "JOIN Conversation c ON c.CID = m.CID " +
            "LEFT JOIN MsgRating r ON r.MsgID = m.MsgID " +
            "WHERE m.MsgID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);                       // Bind target MsgID
            try (ResultSet rs = stmt.executeQuery()) {        // Load message detail
                if (rs.next()) {                              // Print only if found
                    System.out.println();
                    System.out.println("Message " + rs.getLong("MsgID"));
                    System.out.println(
                        "Conversation: " + rs.getLong("CID") +
                        " - " + DatabaseUtil.valueOrDash(rs.getString("title"))
                    );
                    System.out.println("Sender: " + rs.getString("sender"));
                    System.out.println("Sent: " + formatTimestamp(rs.getTimestamp("sent_at")));
                    System.out.println(
                        "Content: " +
                        DatabaseUtil.abbreviate(rs.getString("content_preview"), 240)
                    );
                    System.out.println(
                        "Current rating: " + ratingLabel(rs, "rating")
                    );
                    System.out.println(
                        "Current feedback: " +
                        DatabaseUtil.abbreviate(rs.getString("feedback"), 120)
                    );
                }
            }
        }
    }

    /* ----------------------- personaBelongsToUser ---------------------
     * - Purpose: verifies that a persona belongs to the selected user before it
     * can be attached to a new conversation.
     * - Pre: database connection is open
     * - Post: caller knows whether persona ownership is valid
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long personaId: personaID to check
     *      -> long userId: expected owner UsrID
     * - Returns:
     *      -> boolean: true if Persona has that personaID/user pair
     * ------------------------------------------------------------------ */
    private static boolean personaBelongsToUser(
        Connection conn,
        long personaId,
        long userId
    ) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM Persona WHERE personaID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, personaId);                       // Bind personaID
            stmt.setLong(2, userId);                          // Bind expected owner
            try (ResultSet rs = stmt.executeQuery()) {        // Run count query
                rs.next();                                    // Count returns one row
                return rs.getLong(1) > 0;                     // True if pair exists
            }
        }
    }

    /* ---------------------- userIsWorkspaceMember ---------------------
     * - Purpose: verifies that a user belongs to a workspace before that
     * workspace can be attached to a new conversation.
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
        String sql =
            "SELECT COUNT(*) FROM WorkspaceMember WHERE wsID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, workspaceId);                    // Bind wsID
            stmt.setLong(2, userId);                         // Bind member UsrID
            try (ResultSet rs = stmt.executeQuery()) {       // Run count query
                rs.next();                                   // Count returns one row
                return rs.getLong(1) > 0;                    // True if membership exists
            }
        }
    }

    /* ----------------------- messageRatingExists ----------------------
     * - Purpose: checks if a MsgRating row already exists for a message so the
     * feedback flow knows whether to INSERT or UPDATE.
     * - Pre: database connection is open
     * - Post: caller knows whether feedback row exists
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long messageId: MsgID to check
     * - Returns:
     *      -> boolean: true if MsgRating contains that MsgID
     * ------------------------------------------------------------------ */
    private static boolean messageRatingExists(Connection conn, long messageId)
        throws SQLException {
        String sql = "SELECT COUNT(*) FROM MsgRating WHERE MsgID = ?";
        return DatabaseUtil.count(conn, sql, messageId) > 0; // Reuse count helper
    }

    /* -------------------------- setNullableLong -----------------------
     * - Purpose: binds a nullable Long into a PreparedStatement. Null becomes
     * SQL NULL, otherwise the long value is bound.
     * - Pre: statement exists and index is valid
     * - Post: statement parameter has been bound
     * - Params:
     *      -> PreparedStatement stmt: statement being filled
     *      -> int index: parameter position
     *      -> Long value: optional Long to bind
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void setNullableLong(PreparedStatement stmt, int index, Long value)
        throws SQLException {
        if (value == null) {                                  // Missing numeric value
            stmt.setNull(index, Types.NUMERIC);
        } else {
            stmt.setLong(index, value);                       // Real numeric value
        }
    }

    /* ------------------------ setNullableClobText ---------------------
     * - Purpose: binds optional feedback text for a CLOB column. Blank/null user
     * input becomes SQL NULL.
     * - Pre: statement exists and index is valid
     * - Post: statement parameter has been bound
     * - Params:
     *      -> PreparedStatement stmt: statement being filled
     *      -> int index: parameter position
     *      -> String value: optional feedback text
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void setNullableClobText(PreparedStatement stmt, int index, String value)
        throws SQLException {
        if (value == null || value.trim().isEmpty()) {        // Missing feedback
            stmt.setNull(index, Types.CLOB);
        } else {
            stmt.setString(index, value);                     // JDBC can bind text to CLOB
        }
    }

    /* ------------------------ setNullableRating -----------------------
     * - Purpose: binds the message feedback rating choice into a nullable
     * numeric column. Thumbs up maps to 1, thumbs down maps to 0, and unrated
     * maps to SQL NULL.
     * - Pre: statement exists, index is valid, and ratingChoice is from the
     * feedback menu options
     * - Post: statement parameter has been bound with 1, 0, or NULL
     * - Params:
     *      -> PreparedStatement stmt: statement being filled
     *      -> int index: parameter position
     *      -> int ratingChoice: menu rating choice (1, 2, or 3)
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void setNullableRating(PreparedStatement stmt, int index, int ratingChoice)
                                                                                    throws SQLException {
        if (ratingChoice == 1) { stmt.setInt(index, 1); } // Thumbs up
        else if (ratingChoice == 2) { stmt.setInt(index, 0); }  // Thumbs down
        else { stmt.setNull(index, Types.NUMERIC); }    // Null rating (unrated)
    }

    /* ------------------------- nullableLongLabel ----------------------
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
    private static String nullableLongLabel(ResultSet rs, String columnName)
        throws SQLException {
        long value = rs.getLong(columnName);                  // Read long value
        if (rs.wasNull()) {                                   // Check if SQL NULL
            return "-";
        }
        return String.valueOf(value);                         // Return number as text
    }

    /* ---------------------------- ratingLabel -------------------------
     * - Purpose: reads a nullable boolean-style rating column from a ResultSet
     * and returns a printable label. SQL NULL becomes Unrated.
     * - Pre: ResultSet is on a valid row and column exists
     * - Post: rating value converted to display text
     * - Params:
     *      -> ResultSet rs: current query row
     *      -> String columnName: nullable rating column name
     * - Returns:
     *      -> String: rating label
     * ------------------------------------------------------------------ */
    private static String ratingLabel(ResultSet rs, String columnName)
        throws SQLException {
        int value = rs.getInt(columnName);                    // Read int value
        if (rs.wasNull()) {                                   // Check if SQL NULL
            return "Unrated";
        }
        if (value == 1) {
            return "Thumbs Up";
        }
        if (value == 0) {
            return "Thumbs Down";
        }
        return String.valueOf(value);                         // Return number as text
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
        if (timestamp == null) {                              // Missing timestamp
            return "-";
        }
        return timestamp.toString();                          // Default JDBC timestamp format
    }
}
