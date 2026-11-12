import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Author: Isa Abdulrazaq
 * 
 * CSC 460 Program 4
 * Dr. McCann
 * 
 * Not meant to be instantiated. This "class" is a collection of functions that interact with the Plans and Subscriptions system within the database.
 * Plan details can be updated and new plans can be created (so long as their names do not conflict with existing plans).
 * Performs input validation to ensure plan details are logical (i.e no negative costs or message limits)
 * 
 * Methods:
 *  showMenu() - displays a text based UI used to select actions to perform with regards to the Plans and Subscriptions system.
 *  updatePlan() - allows admin to update the details of a plan one field at a time.
 *  createPlan() - allows for the creation of a new plan
 *  exceedLimit() - used by Conversations.addMessage() to verify whether a user has exceeded their plan's message limit.
 */
public class PlanManagement {

    /**
     * Displays a menu of actions that can be preformed on the Plans and Subscriptions system.
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
            System.out.println("------ Subscriptions ------");
            System.out.println("1. Update existing plan.");
            System.out.println("2. Add new plan.");
            System.out.println("3. back");

            int choice = io.readIntInRange("Choose an option: ", 1, 3);
            try {
                switch (choice) {
                    case 1:
                        updatePlan(conn, io);
                        break;
                    case 2:
                        createNewPlan(conn, io);
                        break;
                    case 3:
                        return;
                    default:
                        System.out.println("Unknown option"); // should never get here
                        break;
                }
            } catch (SQLException e) {
                System.err.println("Subscription management failed!");
                DatabaseUtil.printSqlError(e);
            }
            
        }
    }

    /**
     * Allows admin to update the details of any of the existing plans
     * Performs input validation to ensure hte plan already exists (and allows for backtracking if it needs to be created instead)
     * Also validates that plan details are logical (no negative costs or message limits).
     * Pre-Conditions:
     *  Plans exist within the database
     * Post-Condition:
     *  A plan of the admin's choice has its cost, message limit, or pro model usage changed
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  io - a helper object used for prompting for and reading user input
     */
    private static void updatePlan(Connection conn, ConsoleIO io) throws SQLException {
        UserAccounts.printPlans(conn);
        String planToUpdate = io.readRequiredString("Select a Plan to Update: ");
        while (!DatabaseUtil.planExists(conn, planToUpdate)) {
            System.out.println("No such plan exists! Please select an existing plan.");
            // allow the option to return to the menu if the plan the admin wishes to update doesn't exist yets
            if (io.confirm("Would you like to go back and create a new plan instead?")) return;
            planToUpdate = io.readOptionalString("Select a Plan to Update: ");
        }
        // edit one field at a time
        // field to edit + new value will be decided during each iteration
        // want to display the current plan details to the user before they make each edit.
        PreparedStatement planDisplay = conn.prepareStatement("SELECT * FROM Plan WHERE plan = ?");
        planDisplay.setString(1, planToUpdate);
        while (true) {
            System.out.println("Current Plan details:");
            ResultSet planDetails = planDisplay.executeQuery();
            planDetails.next();
            System.out.println("Plan Name: " + planToUpdate);
            System.out.println("Max Daily Messages: " + planDetails.getInt("max_daily_msg"));
            System.out.println("Is Pro Model: " + (planDetails.getInt("pro_model") == 1 ? "Yes" : "No"));
            System.out.println("Cost: " + planDetails.getInt("cost"));

            System.out.println();
            System.out.println("Update field for plan " + planToUpdate);
            System.out.println("1. Maximum Daily Messages");
            System.out.println("2. Whether the plan uses a Pro Model or not");
            System.out.println("3. The cost of the plan");
            System.out.println("4. Done editing");
            int choice = io.readIntInRange("Choose an option: ", 1, 4);
            switch (choice) {
                case 1: {
                    PreparedStatement planUpdater = conn.prepareStatement("UPDATE Plan SET max_daily_msg=? WHERE plan=?");
                    planUpdater.setString(2, planToUpdate);
                    long newMax = io.readLong("Enter the new Max Message Count for the plan: ");
                    planUpdater.setLong(1, newMax);
                    planUpdater.executeUpdate();
                    planUpdater.close();
                    break;
                }
                case 2: {
                    PreparedStatement planUpdater = conn.prepareStatement("UPDATE Plan SET pro_model=? WHERE plan=?");
                    planUpdater.setString(2, planToUpdate);
                    boolean isPro = io.confirm("Does the plan use the Pro Model?");
                    planUpdater.setInt(1, (isPro ? 1 : 0)); // sets 1 for yes, 0 for no
                    planUpdater.executeUpdate();
                    planUpdater.close();
                    break;
                }
                case 3: {
                    PreparedStatement planUpdater = conn.prepareStatement("UPDATE Plan SET cost=? WHERE plan=?");
                    planUpdater.setString(2, planToUpdate);
                    long newCost = io.readLong("Enter the new cost for the plan: ");
                    planUpdater.setLong(1, newCost);
                    planUpdater.executeUpdate();
                    planUpdater.close();
                    break;
                }
                case 4:
                    planDisplay.close();
                    return;
                default:
                    System.out.println("Unknown option"); // should never get here
                    break;
            }
        }
    }


    /**
     * Allows for the creation of a new plan. All details are read in from the user using ConsoleIO.
     * Performs input validation to ensure plan details are logical.
     * All fields must be read in.
     * Pre-Conditions:
     *  None.
     * Post-Conditions:
     *  A new plan is added to the database.
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  io - a helper object used for prompting for and reading user input
     */
    private static void createNewPlan(Connection conn, ConsoleIO io) throws SQLException {
        String planName = io.readRequiredString("Enter the new plan's name: ");
        while (DatabaseUtil.planExists(conn, planName)) {
            System.out.println("Plan with this name already exists!");
            planName = io.readRequiredString("Enter a unique name for the plan: ");
        }
        long maxMsg = io.readLong("Enter the Maximum Daily Message Count for the plan: ");
        while (maxMsg <= 0) {
            System.out.println("Please enter a positive value");
            System.out.println("Users need to be able to send messages using the plan");
            maxMsg = io.readLong("Enter the Maximum Daily Message Count for the plan: ");
        }
        // I hate that this has to be an int but such is the way of Oracle
        int proModel = (io.confirm("Does the plan use the Pro Model?") ? 1 : 0);
        long cost = io.readLong("Enter the cost for the plan: ");
        while (cost < 0) {
            System.out.println("Cannot have negative cost!");
            cost = io.readLong("Enter the cost for the plan: ");
        }

        PreparedStatement insert = conn.prepareStatement("INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES (?,?,?,?)");
        insert.setString(1, planName); insert.setLong(2, maxMsg);
        insert.setInt(3, proModel); insert.setLong(4, maxMsg);
        insert.executeUpdate();
    }

    /**
     * Checks whether the user has exceeded their maximum messages per day
     * Why does it take CID as an argument rather than UsrID? Mostly because of the way
     * Conversations.addMessage() was written (which is the only caller of this function).
     * Pre-Conditons:
     *  CID is a valid conversation id (has been verified by Conversations.addMessage())
     * Arguments:
     *  conn - Connection to the Oracle DBMS
     *  CID - the Conversation ID associated with the message that will be added to.
     * Returns:
     *  True if the user has exceeded their message limit for the day (and thus cannot add new messages)
     *  False if not.
     */
    public static boolean exceededLimit(Connection conn, long CID) throws SQLException {
        int msgsSentToday = 0;
        // first find the UsrID associated with the conversation
        Statement findUsrId = conn.createStatement();
        ResultSet findUsrIdRs = findUsrId.executeQuery("SELECT * FROM Conversation WHERE CID=" + CID);
        findUsrIdRs.next();
        int userId = findUsrIdRs.getInt("UsrID");
        findUsrId.close(); // done with findUsrID

        // now that we have the user id, we can check what plan they're on and what their limit is
        Statement plan = conn.createStatement();
        ResultSet planCost = plan.executeQuery("SELECT max_daily_msg FROM \"User\" JOIN Plan USING (plan) WHERE UsrID=" + userId);
        planCost.next(); // all users should have a plan
        int limit = planCost.getInt("max_daily_msg");
        plan.close();
        // now, because of the Schema, to check for every message sent by the user today
        // we need to find a list of all of their conversations, and check every message associated with those.
        Statement findAllCIDs = conn.createStatement();
        ResultSet allCIDRs = findAllCIDs.executeQuery("SELECT CID FROM Conversation WHERE UsrID=" + userId);
        ArrayList<Long> allCIDs = new ArrayList<>();
        while (allCIDRs.next()) {
            allCIDs.add(allCIDRs.getLong("CID"));
        }
        findAllCIDs.close();
        // now that we have all the CIDs associated with the user,
        // we can search for every message sent today that uses those CIDs
        PreparedStatement msgStat = conn.prepareStatement("SELECT count(MsgID) FROM Message WHERE CID=? AND trunc(sent_at)=trunc(?) AND sender='user'");
        Date todayDate = Date.valueOf(LocalDate.now());
        msgStat.setDate(2, todayDate); // only want to check messages sent today
        for (Long currCID : allCIDs) {
            msgStat.setLong(1, currCID);
            ResultSet msgsForCID = msgStat.executeQuery();
            msgsForCID.next();
            msgsSentToday += msgsForCID.getInt("count(MsgID)");
            msgsForCID.close();
        }
        msgStat.close();
        return msgsSentToday >= limit;
    }

}