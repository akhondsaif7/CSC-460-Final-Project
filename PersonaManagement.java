/* ------------------- class PersonaManagement ------------------------ *
 * Class: CSC460 - S26
 * Author: Cole Hageman
 * Assg: Project 4
 * Packages: java.sql.Connection, java.sql.PreparedStatement,
 *           java.sql.ResultSet, java.sql.SQLException, java.sql.Timestamp
 * Purpose: This class handles the terminal menu for persona management. It can
 * create personas for existing users, list personas owned by a user, and
 * delete personas while enforcing the conversation-count restriction from the
 * project requirements.
 * Language: Java 25
 * Methods:
 *  - showMenu: displays the persona-management menu
 *  - createPersona: inserts a new Persona row
 *  - deletePersona: deletes a Persona after validation
 *  - listPersonasForUser: prints personas for one user
 *  - personaBelongsToUser, countOngoingConversationsForPersona: checks
 *  - clearPersonaFromConversations, deletePersonaRow: delete helpers
 *  - formatTimestamp: terminal display helper
 * ------------------------------------------------------ */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PersonaManagement {

    /* ---------------------- PersonaManagement ------------------------
     * - Purpose: private constructor so nobody creates a PersonaManagement
     * object. This file is used through static menu/helper methods.
     * - Pre: class exists
     * - Post: object construction is blocked
     * - Params: None
     * - Returns: None
     * ------------------------------------------------------------------ */
    private PersonaManagement() {
    }

    /* ---------------------------- showMenu ----------------------------
     * - Purpose: displays the persona-management submenu and routes each user
     * choice to the method that owns that operation.
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
            System.out.println("---------- Persona Management ----------");
            System.out.println("1. Create persona");
            System.out.println("2. Delete persona");
            System.out.println("3. Back");

            int choice = io.readIntInRange("Choose an option: ", 1, 3);
            try {
                switch (choice) {
                    case 1:
                        createPersona(conn, io);
                        break;
                    case 2:
                        deletePersona(conn, io);
                        break;
                    case 3:
                        done = true;
                        break;
                    default:
                        System.out.println("Unknown option.");
                        break;
                }
            } catch (SQLException e) {
                System.err.println("Persona operation failed.");
                DatabaseUtil.printSqlError(e);
            }
        }
    }

    /* ------------------------- createPersona --------------------------
     * - Purpose: creates a new persona for an existing user. It lists users,
     * validates the selected owner, generates a personaID, reads persona data,
     * and inserts into Persona.
     * - Pre: selected user exists
     * - Post: new Persona row inserted, or operation cancelled for bad user
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void createPersona(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        System.out.println("Create Persona");
        UserAccounts.listUsers(conn);
        long userId = io.readLong("Owner UsrID: ");
        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        long personaId = DatabaseUtil.nextId(conn, "Persona", "personaID");
        String name = io.readRequiredString("Persona name: ");
        String instructions = io.readRequiredString("Custom instructions: ");

        String sql =
            "INSERT INTO Persona (personaID, UsrID, name, instructions, status) " +
            "VALUES (?, ?, ?, ?, 'active')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, personaId);
            stmt.setLong(2, userId);
            stmt.setString(3, name);
            stmt.setString(4, instructions);
            stmt.executeUpdate();
        }

        System.out.println("Persona created with personaID " + personaId + ".");
    }

    /* ------------------------- deletePersona --------------------------
     * - Purpose: deletes an existing persona after verifying ownership and
     * checking how many ongoing conversations currently use it.
     * - Pre: selected user exists and selected persona belongs to that user
     * - Post: Persona row deleted when allowed, or operation blocked
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> ConsoleIO io: terminal input helper
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void deletePersona(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println();
        System.out.println("Delete Persona");
        UserAccounts.listUsers(conn);
        long userId = io.readLong("Owner UsrID: ");
        if (!DatabaseUtil.userExists(conn, userId)) {
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }

        listPersonasForUser(conn, userId);
        long personaId = io.readLong("PersonaID to delete: ");
        if (!personaBelongsToUser(conn, personaId, userId)) {
            System.out.println("No persona with personaID " + personaId + " belongs to UsrID " + userId + ".");
            return;
        }

        long ongoingCount = countOngoingConversationsForPersona(conn, personaId, userId);
        if (ongoingCount > 5) {
            System.out.println(
                "Persona " + personaId + " is active for " + ongoingCount +
                " ongoing conversations. Delete blocked."
            );
            return;
        }

        boolean oldAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            clearPersonaFromConversations(conn, personaId, userId);
            deletePersonaRow(conn, personaId, userId);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }

        System.out.println(
            "Persona " + personaId + " deleted. It was active for " + ongoingCount +
            " ongoing conversations."
        );
    }

    /* ---------------------- listPersonasForUser -----------------------
     * - Purpose: lists all personas owned by one user so create/delete flows
     * can show valid persona choices.
     * - Pre: selected user exists
     * - Post: persona rows printed, or no-results message printed
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long userId: UsrID whose personas should be listed
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void listPersonasForUser(Connection conn, long userId) throws SQLException {
        String sql =
            "SELECT personaID, name, status, created_at FROM Persona " +
            "WHERE UsrID = ? ORDER BY personaID";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println();
                System.out.println("Personas for this user");
                System.out.printf(
                    "%-12s %-28s %-12s %-20s%n",
                    "PersonaID",
                    "Name",
                    "Status",
                    "Created"
                );
                System.out.println("------------------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf(
                        "%-12d %-28s %-12s %-20s%n",
                        rs.getLong("personaID"),
                        DatabaseUtil.abbreviate(rs.getString("name"), 28),
                        rs.getString("status"),
                        formatTimestamp(rs.getTimestamp("created_at"))
                    );
                }
                if (!found) {
                    System.out.println("No personas found for this user.");
                }
            }
        }
    }

    /* ---------------------- personaBelongsToUser ----------------------
     * - Purpose: verifies that a persona belongs to the selected user before
     * delete logic can proceed.
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
        String sql = "SELECT COUNT(*) FROM Persona WHERE personaID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, personaId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    /* ---------------- countOngoingConversationsForPersona -------------
     * - Purpose: counts ongoing conversations that currently use a persona for
     * the selected user. In this project, an existing Conversation row counts
     * as ongoing.
     * - Pre: database connection is open
     * - Post: count returned for the delete rule
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long personaId: personaID to count
     *      -> long userId: expected owner UsrID
     * - Returns:
     *      -> long: number of ongoing conversations using that persona
     * ------------------------------------------------------------------ */
    private static long countOngoingConversationsForPersona(
        Connection conn,
        long personaId,
        long userId
    ) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Conversation WHERE personaID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, personaId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /* ---------------- clearPersonaFromConversations -------------------
     * - Purpose: clears personaID from conversations that currently use the
     * selected persona so the Persona row can be deleted safely.
     * - Pre: delete rule has already allowed deletion
     * - Post: matching Conversation rows have personaID set to NULL
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long personaId: personaID being removed
     *      -> long userId: owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void clearPersonaFromConversations(
        Connection conn,
        long personaId,
        long userId
    ) throws SQLException {
        String sql = "UPDATE Conversation SET personaID = NULL WHERE personaID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, personaId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    /* ------------------------ deletePersonaRow ------------------------
     * - Purpose: deletes the final Persona row after delete checks and
     * dependent Conversation updates have already been handled.
     * - Pre: persona ownership verified and dependent conversation rows updated
     * - Post: Persona row deleted
     * - Params:
     *      -> Connection conn: active Oracle connection
     *      -> long personaId: personaID being deleted
     *      -> long userId: owner UsrID
     * - Returns: None
     * ------------------------------------------------------------------ */
    private static void deletePersonaRow(Connection conn, long personaId, long userId)
        throws SQLException {
        String sql = "DELETE FROM Persona WHERE personaID = ? AND UsrID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, personaId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
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
