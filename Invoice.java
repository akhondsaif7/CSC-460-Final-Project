import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;


/**
 * Author: Isa Abdulrazaq
 * 
 * CSC 460 Program 4
 * Dr. McCann
 * 
 * Not meant to be instantiated. This "class" is a collection of functions that interact with the invoicing system within the database.
 * Invoices can be generated for users based on their plan's monthly rate, and pre-existing invoices can be paid.
 * Users can also assign new payment methods to their accounts.
 * 
 * Methods:
 *  showMenu() - displays a text based UI used to select actions to perform with regards to the invoicing system.
 *  getPayID() - helper method used to select the payment method of the user's choice based on their pre-existing options.
 *  newInvoice() - generates a new invoice for a user based on their plan's rate
 *  payInvoice() - marks an invoice of the user's choise as paid
 *  addNewPayMethod() - allows users to add a new payment method to their account
 */
public class Invoice {

    /**
     * Displays a menu of actions that can be preformed on the invoicing system.
     * Calls the appropriate method based on the desired action.
     * Pre-Conditions:
     *  Database connection established through the conn object
     * Post-Conditions:
     *  Changes may have been made to the Invoice or Payment_Method tables
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  io - a helper object used for prompting for and reading user input
     */
    public static void showMenu(Connection conn, ConsoleIO io) {
        while (true) {
            System.out.println();
            System.out.println("------ Invoices ------");
            System.out.println("1. Generate new invoice for a user");
            System.out.println("2. Pay for an invoice");
            System.out.println("3. Add new payment method for a user");
            System.out.println("4. back");

            int choice = io.readIntInRange("Choose an option: ", 1, 4);

            try {
                switch (choice) {
                    case 1:
                        newInvoice(conn, io);
                        break;
                    case 2:
                        payInvoice(conn, io);
                        break;
                    case 3:
                        addNewPayMethod(conn, io);
                        break;
                    case 4:
                        return;
                    default:
                        System.out.println("Unknown Option"); // should never get here
                        break;
                }
            } catch (SQLException e) {
                System.err.println("Invoicing operation failed!");
                DatabaseUtil.printSqlError(e);
            }
        }
    }

    /**
     * Helper method that allows the user to select a payment method associated with their UserID
     * Displays all of the active payment methods for the given userId
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  io - a helper object used for prompting for and reading user input
     *  userId - the UsrID for which to query payment methods 
     * Returns:
     *  The selected payment id
     *  -1 if the user decides to exit instead of selecting a payment id (i.e wants to insert a new payment method first)
     */
    private static long getPayID(Connection conn, ConsoleIO io, long userId) throws SQLException {
        // verify that the user has a payment method associated with them
        PreparedStatement paymentMethod = conn.prepareStatement("SELECT * FROM PaymentMethod WHERE UsrID = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        paymentMethod.setLong(1, userId);
        // find cardinality of the resulting query
        ResultSet payMethodRs = paymentMethod.executeQuery();
        payMethodRs.last();
        if (payMethodRs.getRow() < 1) {
            System.out.println("No payment options found for UsrID " + userId);
            System.out.println("Please add a payment option first before attempting to generate for an invoice");
            return -1;
        }
        // ask user to select a payment method
        System.out.printf(
            "%-30s %-30s %-20s%n",
            "Payment Method ID",
            "Type",
            "Billing Address"
        );
        System.out.println(
            "------------------------------------------------------------------------------------------------"
        );
        payMethodRs.first();
        // do while loop rather than standard while loop because .first() returns to the first entry rather than to before it (thus, a call to next() would skip it)
        do {
            System.out.printf(
                "%-30s %-20s %-40s%n",
                payMethodRs.getLong("payment_method_ID"),
                DatabaseUtil.abbreviate(payMethodRs.getString("Type"), 30),
                DatabaseUtil.abbreviate(payMethodRs.getString("billing_addr"), 40)
            );
        } while (payMethodRs.next());
        long payId = io.readLong("Select a Payment Method ID: "); // read the chosen pay id from the user
        // verify that the selected ID is actually valid
        while (true) {
            PreparedStatement verifyPayID = conn.prepareStatement("SELECT * from PaymentMethod where payment_method_id=? AND UsrID=?");
            verifyPayID.setLong(1, payId); verifyPayID.setLong(2, userId);
            ResultSet payIDExists = verifyPayID.executeQuery();
            if (payIDExists.next()) {
                verifyPayID.close();
                break;
            }
            System.out.println("Invalid Payment Method!");
            payId = io.readLong("Select a Payment Method ID: ");
        }
        paymentMethod.close();
        return payId;
    }

    /**
     * Prompts the admin to select a user to generate a new invoice for.
     * Once the admin has selected the appropriate UsrID, the cost of their plan is calculated
     * and a new invocie is added to the invoice table, set for the current date, with a payment method of their choice.
     * Pre-Conditions:
     *  Users exist within the database
     *  Users have an associated payment method
     *  Users have an associated plan
     * Post-Conditions:
     *  A new invoice is generated for the selected user.
     *  the invoice's id is displayed for the admin
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  io - a helper object used for prompting for and reading user input
     */
    private static void newInvoice(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println("Existing users: ");
        UserAccounts.listUsers(conn);
        System.out.println("");
        long userId = io.readLong("Enter the UsrID to generate an invoice for: ");
        if (!DatabaseUtil.userExists(conn, userId)) {         // Validate user exists
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }
    
        // user has valid payment method, so we can generate an invoice using the payment method of their choice
        PreparedStatement findCost = conn.prepareStatement("SELECT cost FROM Plan JOIN \"User\" USING (plan) WHERE UsrId=?");
        findCost.setLong(1, userId);
        ResultSet findCostRs = findCost.executeQuery();
        if (!findCostRs.next()) {
            System.out.println("No plan assoicated with UsrId " + userId);
            return;
        }
        // since UPlan is a one-to-one relationship, there should only ever be one result in the result set
        int cost = findCostRs.getInt("cost");
        findCost.close();
        if (cost == 0) {
            System.out.println("User is on a free plan!");
            System.out.println("No value to charge on invoice.");
            return;
        } 
        long payId = getPayID(conn, io, userId); // read the chosen pay id from the user
        if (payId == -1) return;
        long invId = DatabaseUtil.nextId(conn, "Invoice", "InvNo"); // fabricate a new invoice id
        Date currDate = Date.valueOf(LocalDate.now()); // set the date to the current day
        PreparedStatement createInvoice = conn.prepareStatement("INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, " +
            "status) VALUES (?,?,?,?,?,?)"
        );
        createInvoice.setLong(1, invId); createInvoice.setLong(2, userId);
        createInvoice.setLong(3, cost); createInvoice.setDate(4, currDate);
        createInvoice.setLong(5, payId); createInvoice.setString(6, "open");
        createInvoice.executeUpdate();

        System.out.println("Successfully invoiced UserID " + userId);
        System.out.println("An amount of " + cost);
        System.out.println("Invoice ID: " + invId);

        createInvoice.close();
    }

    /**
     * Allows for the user to mark an invoice as paid.
     * Displays all of the user's unpaid invoices and allows them to select one to pay.
     * Once selected, they have the option to change the payment method associated with it.
     * Otherwise, the Invoice is marked as paid.
     * If the user does not exist, or has no unpaid invoices, an appropriate error message is displayed instead.
     * Pre-Conditions:
     *  Users exist in the database
     *  Unpaid invoices exist in the database
     * Post-Conditions:
     *  The selected invoice is updated and labelled as paid.
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  io - a helper object used for prompting for and reading user input
     */
    private static void payInvoice(Connection conn, ConsoleIO io) throws SQLException {
        System.out.println("Existing users: ");
        UserAccounts.listUsers(conn);
        System.out.println("");
        long userId = io.readLong("Enter the UsrID to pay an invoice for: ");
        if (!DatabaseUtil.userExists(conn, userId)) {         // Validate user exists
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }
        // find all the unpaid invoices for the user
        PreparedStatement userInvoices = conn.prepareStatement("SELECT InvNo, amount, invoice_date, payment_method_ID FROM Invoice WHERE UsrID=? AND status='open'", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        userInvoices.setLong(1, userId);
        ResultSet userInvoicesRs = userInvoices.executeQuery();
        // verify that the user has any unpaid invoices first
        userInvoicesRs.last();
        if (userInvoicesRs.getRow() < 1) {
            System.out.println("No unpaid invoices for " + userId + " :)");
            return;
        }
        userInvoicesRs.first();
        System.out.println("Invoices for " + userId + ":");
        System.out.printf(
            "%-20s %-20s %-20s %-20s%n",
            "Invoice No.",
            "Amount",
            "Date",
            "Payment Method ID"
        );
        System.out.println(
            "------------------------------------------------------------------------------------------------"
        );
        
        ArrayList<Long> invoiceIds = new ArrayList<>(); // hold all found InvIDs. Used for input validation
        // do while loop rather than standard while loop because .first() returns to the first entry rather than to before it (thus, a call to next() would skip it)
        do {
            long currInvId = userInvoicesRs.getLong("InvNo");
            invoiceIds.add(currInvId);
            System.out.printf(
                "%-20s %-20s %-20s %-20s%n",
                currInvId,
                userInvoicesRs.getInt("amount"),
                userInvoicesRs.getDate("invoice_date"),
                userInvoicesRs.getLong("payment_method_ID")
            );
        } while (userInvoicesRs.next());
        userInvoices.close();
        long invId; // holds the selected invoice to pay for.
        // validate that the invoice id given belongs to the user's unpaid invoices
        while (true) {
            invId = io.readLong("Select an Invoice to pay for: ");
            if (invoiceIds.contains(invId)) break;
            else System.out.println("Invalid invoice ID!");
        }
        // ask user if they'd like to change the payment method
        while (true) {
            boolean changePayment = io.confirm("Would you like to change the payment method?");
            if (changePayment) {
                long newPayID = getPayID(conn, io, userId);
                if (newPayID == -1) {
                    System.out.println("Cancelling Invoice payment");
                    return;
                }
                PreparedStatement updatePayID = conn.prepareStatement("UPDATE Invoice SET payment_method_ID=? WHERE InvNo=?");
                updatePayID.setLong(1, newPayID); updatePayID.setLong(2, invId);
                updatePayID.executeUpdate();
                System.out.printf("Updated Invoice %l to use Payment Method %l\n", invId, newPayID);
                updatePayID.close();
            }
            else break;
        }
        if (!io.confirm("Confirm payment?")) {
            System.out.println("Invoice will not be paid");
            System.out.println("Changes made to the payment method will remain");
            return;
        }
        PreparedStatement payInvoice = conn.prepareStatement("UPDATE Invoice SET status='paid' WHERE InvNo=?");
        payInvoice.setLong(1, invId);
        payInvoice.executeUpdate();
        payInvoice.close();
    }

    /**
     * Allows the user to select their account and add a new payment method to associate with it
     * The id is calculated automatically, but all other details are read in from user input.
     * Pre-Conditions:
     *  User has an account within the database
     * Post-Conditions:
     *  A new payment method is added for the user, with an appropriate type and active status
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  io - a helper object used for prompting for and reading user input
     */
    private static void addNewPayMethod(Connection conn, ConsoleIO io) throws SQLException {
        // get user id
        System.out.println("Existing users: ");
        UserAccounts.listUsers(conn);
        System.out.println("");
        long userId = io.readLong("Enter the UsrID to add a payment method for: ");
        if (!DatabaseUtil.userExists(conn, userId)) {         // Validate user exists
            System.out.println("No user exists with UsrID " + userId + ".");
            return;
        }
        // read in type and billing address
        // allow user to re-enter their info incase of typos.
        String type;
        while (true) {
            type = io.readRequiredString("Please enter the type of payment method (Credit Card, Paypal, etc.): ");
            if (io.confirm("Confirm payment type as: " + type)) break;
        }
        String billingAddress;
        while (true) {
            billingAddress = io.readRequiredString("Please enter your billing address: ");
            if (io.confirm("Confirm billing address as: " + billingAddress)) break;
        }
        long payId = DatabaseUtil.nextId(conn, "PaymentMethod", "payment_method_ID");
        // now that all of the data has been collected, we can insert the payment method into the database
        PreparedStatement insert = conn.prepareStatement("INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) " +
                                                        "VALUES (?,?,?,?, 'active')");
        insert.setLong(1, payId); insert.setLong(2, userId);
        insert.setString(3, type); insert.setString(4, billingAddress);
        insert.executeUpdate();
        System.out.println("Successfully added payment method " + type + " to user " + userId);
        System.out.println("Payment method ID: " + payId);
        insert.close();
    }
}