/* ------------------- class ConsoleIO ------------------------ *
 * Class: CSC460 - S26
 * Author: Matteo Galiano
 * Assg: Project 4
 * Packages: java.util.Scanner
 * Purpose: This class is the terminal input helper for the JDBC application.
 * It wraps one Scanner and gives the rest of the program small reusable methods
 * for reading required text, optional text, whole numbers, bounded menu choices,
 * yes/no confirmations, and pause prompts. It does not know anything about SQL
 * or any database table; it only handles user input.
 * Language: Java 25
 * Methods:
 *  - ConsoleIO: constructor that saves the Scanner
 *  - readRequiredString: reads non-empty text
 *  - readOptionalString: reads text that may be blank/null
 *  - readLong: reads a whole number
 *  - readIntInRange: reads a bounded menu choice
 *  - confirm: reads a yes/no answer
 *  - pause: waits for Enter before returning
 * ------------------------------------------------------ */

import java.util.Scanner;

public class ConsoleIO {

    // Scanner shared by the whole terminal app
    private final Scanner scanner;

    /* ------------------------- ConsoleIO ---------------------------
     * - Purpose: constructor for the ConsoleIO helper. It stores the Scanner
     * that all input methods will read from.
     * - Pre: scanner exists and reads from the terminal
     * - Post: ConsoleIO object can read user input
     * - Params:
     *      -> Scanner scanner: scanner connected to terminal input
     * - Returns: None
     * ---------------------------------------------------------------- */
    public ConsoleIO(Scanner scanner) {
        this.scanner = scanner;        // Save scanner for later reads
    }

    /* ---------------------- readRequiredString ----------------------
     * - Purpose: reads a non-empty string from the terminal. If the user just
     * presses Enter, it keeps asking until they type something.
     * - Pre: terminal input exists
     * - Post: returned string is not empty after trimming
     * - Params:
     *      -> String prompt: text displayed before reading input
     * - Returns:
     *      -> String: trimmed non-empty user input
     * ---------------------------------------------------------------- */
    public String readRequiredString(String prompt) {
        while (true) {                         // Loop until valid non-empty text
            System.out.print(prompt);          // Show prompt to user
            String value = scanner.nextLine().trim(); // Read one full line
            if (!value.isEmpty()) {            // Empty strings are not accepted
                return value;
            }
            System.out.println("Please enter a value."); // Explain retry
        }
    }

    /* ---------------------- readOptionalString ----------------------
     * - Purpose: reads a string that can be blank. Blank input is converted to
     * null so SQL code can store NULL for optional fields.
     * - Pre: terminal input exists
     * - Post: returned value is either trimmed text or null
     * - Params:
     *      -> String prompt: text displayed before reading input
     * - Returns:
     *      -> String: trimmed input, or null if the user typed nothing
     * ---------------------------------------------------------------- */
    public String readOptionalString(String prompt) {
        System.out.print(prompt);              // Show prompt to user
        String value = scanner.nextLine().trim(); // Read one full line
        if (value.isEmpty()) {                 // Blank optional input means null
            return null;
        }
        return value;                          // Return actual typed value
    }

    /* --------------------------- readLong ---------------------------
     * - Purpose: reads a whole number from the terminal. If the user types
     * something that is not a long, it prints a friendly error and retries.
     * - Pre: terminal input exists
     * - Post: returned value is a valid long
     * - Params:
     *      -> String prompt: text displayed before reading input
     * - Returns:
     *      -> long: whole number typed by the user
     * ---------------------------------------------------------------- */
    public long readLong(String prompt) {
        while (true) {                         // Loop until parse succeeds
            System.out.print(prompt);          // Show prompt to user
            String value = scanner.nextLine().trim(); // Read raw input
            try {
                return Long.parseLong(value);  // Convert text to long
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number."); // Retry hint
            }
        }
    }

    /* ------------------------- readIntInRange ------------------------
     * - Purpose: reads a whole number and validates that it is inside a min/max
     * range. This is mainly used for terminal menus.
     * - Pre: terminal input exists and min <= max
     * - Post: returned value is inside the requested range
     * - Params:
     *      -> String prompt: text displayed before reading input
     *      -> int min: smallest accepted number
     *      -> int max: largest accepted number
     * - Returns:
     *      -> int: validated menu choice or bounded integer
     * ---------------------------------------------------------------- */
    public int readIntInRange(String prompt, int min, int max) {
        while (true) {                         // Loop until number is in range
            long value = readLong(prompt);     // Reuse whole-number parser
            if (value >= min && value <= max) { // Check range boundaries
                return (int) value;
            }
            System.out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    /* ---------------------------- confirm ---------------------------
     * - Purpose: asks a yes/no question and returns true for yes, false for no.
     * It accepts y/yes and n/no in any letter case.
     * - Pre: terminal input exists
     * - Post: returned value represents a clear yes/no answer
     * - Params:
     *      -> String prompt: question to ask before appending (y/n)
     * - Returns:
     *      -> boolean: true for yes, false for no
     * ---------------------------------------------------------------- */
    public boolean confirm(String prompt) {
        while (true) {                         // Loop until y/n style answer
            System.out.print(prompt + " (y/n): "); // Ask confirmation
            String value = scanner.nextLine().trim().toLowerCase(); // Normalize answer
            if (value.equals("y") || value.equals("yes")) {
                return true;                   // User confirmed
            }
            if (value.equals("n") || value.equals("no")) {
                return false;                  // User declined
            }
            System.out.println("Please answer y or n."); // Explain valid answers
        }
    }

    /* ----------------------------- pause ----------------------------
     * - Purpose: waits for the user to press Enter. Used after list/query output
     * so the menu does not immediately redraw and push the results away.
     * - Pre: terminal input exists
     * - Post: user has pressed Enter
     * - Params: None
     * - Returns: None
     * ---------------------------------------------------------------- */
    public void pause() {
        System.out.print("Press Enter to continue..."); // Tell user what to do
        scanner.nextLine();                             // Wait for Enter
    }
}
