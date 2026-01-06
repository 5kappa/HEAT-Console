package heat.ui;

import heat.service.*;
import heat.util.ConsoleUtils;

public class ConsoleDashboard {

    // ============================================================
    // Fields & Constructor
    // ============================================================
    
    private final UserService userService;
    private final InputHelper inputHelper;

    public ConsoleDashboard(WorkoutService workoutService, UserService userService, GoalService goalService) {
        this.userService = userService;
        this.inputHelper = new InputHelper(workoutService, userService, goalService);
        
        System.out.println("\n✓ Fitness Service initialized");
        System.out.println("✓ Database connection ready");
        System.out.println("\nCurrent streak: " + userService.getStreak());
    }

    // ============================================================
    // Main Menu
    // ============================================================

    public void displayMenu() {
        if (!(userService.isRegistered())) {
            inputHelper.captureUserProfileInput();
        }

        while (true) {
            ConsoleUtils.printBorder();
            ConsoleUtils.printCentered("What would you like to do?");
            System.out.println("");
            ConsoleUtils.printCentered("[ Daily Actions ]");
            System.out.println("\t\t\t\t\t[ 1 ]   Log Workout");
            System.out.println("\t\t\t\t\t[ 2 ]   Set a Goal");
            System.out.println("\t\t\t\t\t[ 3 ]   View Goals");
            System.out.println("\t\t\t\t\t[ 4 ]   View Motivational Quote");

            System.out.println("");
            ConsoleUtils.printCentered("[ Data ]");
            System.out.println("\t\t\t\t\t[ 5 ]   View Weight Progress");
            System.out.println("\t\t\t\t\t[ 6 ]   View Weekly Summary");
            System.out.println("\t\t\t\t\t[ 7 ]   View Personal Records");
            System.out.println("\t\t\t\t\t[ 8 ]   View All Workouts");

            System.out.println("");
            ConsoleUtils.printCentered("[ User Profile ]");
            System.out.println("\t\t\t\t\t[ 9 ]   Update Weight");
            System.out.println("\t\t\t\t\t[ 10 ]  Update Body Metrics");
            System.out.println("\t\t\t\t\t[ 11 ]  View Profile");
            System.out.println("\n\t\t\t\t\t[ 0 ]   Exit\n");
            int choice = ConsoleUtils.readRequiredInt("Enter choice: ", true);
            System.out.println("");

            switch (choice) {
                case 1: inputHelper.captureWorkoutInput(); break;
                case 2: inputHelper.captureGoalInput(); break;
                case 3: inputHelper.showGoalsMenu(); break;
                case 4: inputHelper.showQuote(); break;
                case 5: inputHelper.showBodyMetricHistory(); break;
                case 6: inputHelper.showWeeklySummary(); break;
                case 7: inputHelper.showPersonalRecords(); break;
                case 8: inputHelper.showAllWorkouts(); break;
                case 9: inputHelper.updateWeight(); break;
                case 10: inputHelper.updateBodyMetrics(); break;
                case 11: inputHelper.showUserProfile(); break;
                case 0:
                    System.out.println("\t\t\t\t\tGoodbye!");
                    return;
                default:
                    System.out.println("\t\t\t\t\t[!] Invalid choice. Please choose between 1-11.");
            }
        }
    }
}