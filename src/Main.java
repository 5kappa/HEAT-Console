import java.io.IOException;
import java.sql.SQLException;

import heat.dao.DatabaseConnection;
import heat.dao.WorkoutDAO;
import heat.service.*;
import heat.ui.ConsoleDashboard;
import heat.util.ConsoleUtils;
public class Main {
    public static void main(String[] args) {

        ConsoleUtils.printSplashHeader();

        try {
            // Initialize Database Connection (Creates tables if missing)
            DatabaseConnection.getInstance();

            // Perform Data Setup (Loads CSVs if tables are empty)
            WorkoutDAO workoutDAO = new WorkoutDAO();
            workoutDAO.performInitialSetup();

        } catch (SQLException | IOException e) {
            System.err.println("Critical Error during startup: " + e.getMessage());
            e.printStackTrace();
        }

        // Initialize Services
        UserService userService = new UserService();
        GoalService goalService = new GoalService(userService);
        WorkoutService workoutService = new WorkoutService(goalService, userService);

        userService.setGoalService(goalService);

        ConsoleDashboard dashboard = new ConsoleDashboard(workoutService, userService, goalService);

        ConsoleUtils.printSplashFooter();
        ConsoleUtils.pause();

        ConsoleUtils.printWelcomeBanner();
        
        dashboard.displayMenu();
    }
}