import java.io.IOException;
import java.sql.SQLException;

import heat.dao.DatabaseConnection;
import heat.dao.WorkoutDAO;
import heat.service.*;
import heat.ui.ConsoleDashboard;
public class Main {
    public static void main(String[] args) {

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

        System.out.print("""
                        []=============================================================================================================================================================[]
                                                                                                                                                                                         
                                      ,                   HHHHHHHHH     HHHHHHHHH         EEEEEEEEEEEEEEEEEEEEEE                 AAAAAAAAAAAAAAA           TTTTTTTTTTTTTTTTTTTTTTT              
                                      *@                  H:::::::H     H:::::::H         E::::::::::::::::::::E                 A:::::::::::::A           T:::::::::::::::::::::T              
                                      @:@                 H:::::::H     H:::::::H         E::::::::::::::::::::E                 A:::::::::::::A           T:::::::::::::::::::::T              
                                      @::@@@              HH::::::H     H::::::HH         EE::::::EEEEEEEEE::::E                 A:::::::::::::A           T:::::TT:::::::TT:::::T              
                                      @:::::@&              H:::::H     H:::::H             E:::::E       EEEEEE                  A:::::A:::::A            TTTTTT  T:::::T  TTTTTT              
                                      @::@::::@             H:::::H     H:::::H             E:::::E                              A:::::A A:::::A                   T:::::T                      
                                ,     @::@*@:::&  ,         H::::::HHHHH::::::H             E::::::EEEEEEEEEE                   A:::::AAAAA:::::A                  T:::::T                      
                                @,  #:::@'`@:::@ @@         H:::::::::::::::::H             E:::::::::::::::E                  A:::::::::::::::::A                 T:::::T                      
                               @:@.@:::@    @:::@:@         H:::::::::::::::::H             E:::::::::::::::E                 A:::::::::::::::::::A                T:::::T                      
                               @::::::@      @::::@ ,       H::::::HHHHH::::::H             E::::::EEEEEEEEEE                A:::::AAAAAAAAAAA:::::A               T:::::T                      
                              @::::::@       @::::@ @@      H:::::H     H:::::H             E:::::E                         A:::::A           A:::::A              T:::::T                      
                              @::@::@        @::@::@:@      H:::::H     H:::::H             E:::::E       EEEEEE           A:::::A             A:::::A             T:::::T                      
                              @:@'@:@         @' `@::@    HH::::::H     H::::::HH         EE::::::EEEEEEEE:::::E         AA::::::AA           AA::::::AA         TT:::::::TT                    
                              @:@. `@,        '   @:@     H:::::::H     H:::::::H  ####   E::::::::::::::::::::E  ####   A::::::::A           A::::::::A  ####   T:::::::::T  ####       
                               @:@                @*      H:::::::H     H:::::::H #::::#  E::::::::::::::::::::E #::::#  A::::::::A           A::::::::A #::::#  T:::::::::T #::::#       
                                `'                '       HHHHHHHHH     HHHHHHHHH  ####   EEEEEEEEEEEEEEEEEEEEEE  ####   AAAAAAAAAA           AAAAAAAAAA  ####   TTTTTTTTTTT  ####       
                                                                                                                                                                                         
                        []=============================================================================================================================================================[]
                                                                         Welcome to H.E.A.T.: Health, Exercise, and Activity Tracker                                                   
                        """);
        
        dashboard.displayMenu();
    }
}