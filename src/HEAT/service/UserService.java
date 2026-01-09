package heat.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import heat.dao.DatabaseConnection;
import heat.dao.UserDAO;
import heat.dao.GoalDAO;
import heat.model.BodyMetric;
import heat.model.Goal;
import heat.model.GoalStatus;
import heat.model.User;

public class UserService {
    
    // ============================================================
    // Fields & Constructor
    // ============================================================

    private DatabaseConnection dbConnection;
    private UserDAO userDAO;
    private GoalDAO goalDAO;

    private User currentUser = null;
    private List<BodyMetric> bodyMetricHistory = new ArrayList<>();

    private GoalService goalService;

    public UserService() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.userDAO = new UserDAO();
        this.goalDAO = new GoalDAO();

        try {
            User loadedUserProfile = userDAO.loadUserProfile();
            if (loadedUserProfile != null) { currentUser = loadedUserProfile; }

            List<BodyMetric> loadedBodyMetrics = userDAO.loadBodyMetrics();
            if (loadedBodyMetrics != null) { bodyMetricHistory = loadedBodyMetrics; }

            System.out.println("[OK] UserService: " + bodyMetricHistory.size() + " body metric entries loaded");

        } catch (SQLException e) {
            System.out.println("Warning: could not load persisted data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setGoalService(GoalService gs) { this.goalService = gs; }

    // ============================================================
    // User Profile Management
    // ============================================================

    // [C] Register New User
    public void saveUserProfile(String name, int age, double height, double weight, String sex) {
        try {
            dbConnection.beginTransaction();

            double bmi = calculateBMI(weight, height);
            double bmr = calculateBMR(height, weight, age, sex);

            User user = new User(name, age, height, weight, sex, bmi, bmr);

            userDAO.saveUserProfile(user);
            
            List<Goal> completedGoals = goalService.evaluateWeightGoals(weight);

            if (!completedGoals.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                for (Goal g : completedGoals) ids.add(g.getId());

                goalDAO.updateGoalStatusBatch(ids, GoalStatus.COMPLETED);
                System.out.println("\n\t\t\t\t\tYou reached your weight goal!");
            }

            dbConnection.commitTransaction();

            currentUser = user;

            if (!completedGoals.isEmpty()) {
                goalService.archiveCompletedGoals(completedGoals);
            }
            
        } catch (SQLException e) {
            try {
                dbConnection.rollbackTransaction();
                System.err.println("Error saving user profile. Rolled back changes.");
            } catch (SQLException ex) {
                System.err.println("Rollback also failed: " + ex.getMessage());
            }
            
            System.err.println("Failed to save user profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // [U] Profile Updating (for when body metrics are changed)
    public boolean updateProfile(User updatedUser) {
        try {
            dbConnection.beginTransaction();
            
            userDAO.updateUserProfile(updatedUser);
            
            // Check Goals
            List<Goal> completedGoals = new ArrayList<>();
            if (updatedUser.getWeightKg() != currentUser.getWeightKg()) {
                 completedGoals = goalService.evaluateWeightGoals(updatedUser.getWeightKg());
            }
            
            if (!completedGoals.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                for (Goal g : completedGoals) ids.add(g.getId());
                
                goalDAO.updateGoalStatusBatch(ids, GoalStatus.COMPLETED);
                System.out.println("\n\t\t\t\t\tYou reached your weight goal!");
            }

            dbConnection.commitTransaction();
            
            this.currentUser = updatedUser;
            
            if (!completedGoals.isEmpty()) {
                goalService.archiveCompletedGoals(completedGoals);
            }

            return true;

        } catch (SQLException e) {
            try { dbConnection.rollbackTransaction(); } catch (SQLException ex) {}
            System.out.println("\t\t\t\t\t[ ! ]   Error updating profile: " + e.getMessage());
            return false;
        }
    }

    // [U] Profile Updating (for when User Profile is changed)
    public boolean correctProfileDetails(User updatedUser) {
        try {
            dbConnection.beginTransaction();
            userDAO.updateUserProfile(updatedUser);

            if (!bodyMetricHistory.isEmpty()) {
                BodyMetric latest = bodyMetricHistory.get(0); 
                
                if (latest.getWeightKg() != updatedUser.getWeightKg() ||
                    latest.getHeightCm() != updatedUser.getHeightCm() ||
                    latest.getAge() != updatedUser.getAge()) {
                    
                    System.out.println("\t\t\t\t\tSyncing latest history entry with profile corrections...");

                    BodyMetric updatedMetric = new BodyMetric(
                        latest.getId(),
                        updatedUser.getAge(),
                        updatedUser.getHeightCm(),
                        updatedUser.getWeightKg(),
                        updatedUser.getBMI(),
                        latest.getDate()
                    );

                    userDAO.updateBodyMetric(updatedMetric);
                    bodyMetricHistory.set(0, updatedMetric);
                }
            }
            
            List<Goal> completedGoals = new ArrayList<>();
            if (updatedUser.getWeightKg() != currentUser.getWeightKg()) {
                 completedGoals = goalService.evaluateWeightGoals(updatedUser.getWeightKg());
            }

            if (!completedGoals.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                for (Goal g : completedGoals) ids.add(g.getId());
                goalDAO.updateGoalStatusBatch(ids, GoalStatus.COMPLETED);
                System.out.println("\n\t\t\t\t\tYou reached your weight goal!");
            }

            dbConnection.commitTransaction();
            this.currentUser = updatedUser;

            if (!completedGoals.isEmpty()) {
                goalService.archiveCompletedGoals(completedGoals);
            }

            return true;

        } catch (SQLException e) {
            try { dbConnection.rollbackTransaction(); } catch (SQLException ex) {}
            System.out.println("\t\t\t\t\tError correcting profile: " + e.getMessage());
            return false;
        }
    }

    // [R] View Profile
    public String showProfileDetails() {
        String profileDetails = String.format("""
                %37s%6s
                %37s%6s%12s%-10s%13s%-19s%12s%-15s%37s
                %37s%6s%12s%-10s%13s%-19s%12s%-15s%37s
                %37s%6s
                """, 
                "", "_O/   ",
                "", "  \\   ", "",
                String.format("Age  :  %d", getAge()), "", 
                String.format("Height  :  %.1f cm", getHeightCm()), "", 
                String.format("BMI  :  %.2f", getBMI()), "", 
                "", "  /\\_ ", "",
                String.format("Sex  :  %s", getSex()), "", 
                String.format("Weight  :  %.1f kg", getWeightKg()), "", 
                String.format("BMR  :  %.2f", getBMR()), "", 
                "", "  \\   "
            );
        return profileDetails;
    }

    // ============================================================
    // Body Metric History
    // ============================================================

    public List<BodyMetric> getBodyMetricHistory() {
        return bodyMetricHistory;
    }

    public void addBodyMetric(BodyMetric bm) {
        try {
            userDAO.insertNewBodyMetric(bm);
            bodyMetricHistory.add(0, bm);
        } catch (SQLException e) {
            System.out.println("\t\t\t\t\t[ ! ]   Error adding body metric: " + e.getMessage());
        }
    }

    public boolean updateBodyMetric(BodyMetric original, BodyMetric updated) {
        boolean isLatest = false;
        if (!bodyMetricHistory.isEmpty() && bodyMetricHistory.get(0).getId() == updated.getId()) {
            isLatest = true;
        }

        try {
            dbConnection.beginTransaction();

            userDAO.updateBodyMetric(updated);

            if (isLatest) {
                System.out.println("\t\t\t\t\tSyncing user profile with updated metric...");
                
                User updatedUser = new User(
                    currentUser.getName(),
                    updated.getAge(),
                    updated.getHeightCm(),
                    updated.getWeightKg(),
                    currentUser.getSex(),
                    calculateBMI(updated.getWeightKg(), updated.getHeightCm()),
                    calculateBMR(updated.getHeightCm(), updated.getWeightKg(), updated.getAge(), currentUser.getSex()),
                    currentUser.getCurrentStreak(),
                    currentUser.getLastWorkoutDate()
                );

                userDAO.saveUserProfile(updatedUser);

                goalService.evaluateWeightGoals(updatedUser.getWeightKg());
                
                this.currentUser = updatedUser;
            }

            dbConnection.commitTransaction();

            for (int i = 0; i < bodyMetricHistory.size(); i++) {
                if (bodyMetricHistory.get(i).getId() == updated.getId()) {
                    bodyMetricHistory.set(i, updated);
                    break;
                }
            }
            
            bodyMetricHistory.sort((m1, m2) -> m2.getDate().compareTo(m1.getDate()));

            return true;

        } catch (SQLException e) {
            try { dbConnection.rollbackTransaction(); } catch (SQLException ex) {}
            System.out.println("\t\t\t\t\t[ ! ]   Error updating body metric: " + e.getMessage());
            return false;
        }
    }    

    public boolean deleteBodyMetric(BodyMetric bm) {
        boolean isLatest = false;
        
        // Check Index 0 (Top of list) for the latest entry
        if (!bodyMetricHistory.isEmpty() && bodyMetricHistory.get(0).getId() == bm.getId()) {
            isLatest = true;
        }

        try {
            dbConnection.beginTransaction();
            
            userDAO.deleteBodyMetric(bm.getId());

            if (isLatest) {
                // If we are deleting the newest entry, revert to the *next* one down
                if (bodyMetricHistory.size() > 1) {
                    BodyMetric previous = bodyMetricHistory.get(1); 
                    
                    System.out.println("\t\t\t\t\tReverting user profile to previous entry (" + previous.getWeightKg() + "kg)...");

                    User updatedUser = new User(
                        currentUser.getName(), 
                        previous.getAge(), 
                        previous.getHeightCm(), 
                        previous.getWeightKg(), 
                        currentUser.getSex(),
                        previous.getBMI(),
                        calculateBMR(previous.getHeightCm(), previous.getWeightKg(), previous.getAge(), currentUser.getSex()),
                        currentUser.getCurrentStreak(),
                        currentUser.getLastWorkoutDate()
                    );
                    
                    userDAO.updateUserProfile(updatedUser);
                    
                    goalService.evaluateWeightGoals(updatedUser.getWeightKg());

                    this.currentUser = updatedUser; 

                } else {
                    System.out.println("\t\t\t\t\tWarning: Deleted the only body metric. Profile stats may be stale.");
                }
            }
            
            dbConnection.commitTransaction();
            
            bodyMetricHistory.remove(bm);
            return true;

        } catch (SQLException e) {
            try { dbConnection.rollbackTransaction(); } catch (SQLException ex) {}
            System.out.println("\t\t\t\t\tError deleting body metric: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    // Streak & Calculations
    // ============================================================

    public void recalculateStreak(List<LocalDate> allWorkoutDates) {
        if (allWorkoutDates == null || allWorkoutDates.isEmpty()) {
            if (currentUser.getCurrentStreak() != 0) {
                currentUser.setCurrentStreak(0);
                currentUser.setLastWorkoutDate(null);
                updateUserProfileSilent();
            }
            return;
        }

        List<LocalDate> sortedDates = allWorkoutDates.stream()
            .distinct()
            .sorted((d1, d2) -> d2.compareTo(d1))
            .toList();

        int newStreak = 1;
        LocalDate lastDate = sortedDates.get(0);

        for (int i = 0; i < sortedDates.size() - 1; i++) {
            LocalDate current = sortedDates.get(i);
            LocalDate previous = sortedDates.get(i + 1);

            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(previous, current);

            if (daysBetween == 1) {
                newStreak++;
            } else if (daysBetween > 1) {
                break;
            }
        }

        if (currentUser.getCurrentStreak() != newStreak || !lastDate.equals(currentUser.getLastWorkoutDate())) {
            currentUser.setCurrentStreak(newStreak);
            currentUser.setLastWorkoutDate(lastDate);
            
            updateUserProfileSilent();
            System.out.println("\t\t\t\t\tStreak recalculated: " + newStreak + " day(s)");
        }
    }

    private void updateUserProfileSilent() {
        try {
            userDAO.updateUserProfile(currentUser);
        } catch (SQLException e) {
            System.out.println("\t\t\t\t\t[ ! ]   Failed to save streak progress.");
        }
    }

    public void validateStreakOnStartup() {
        if (currentUser == null || currentUser.getLastWorkoutDate() == null) {
            return;
        }

        LocalDate lastDate = currentUser.getLastWorkoutDate();
        LocalDate today = LocalDate.now();

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today);

        if (daysBetween > 1) {
            if (currentUser.getCurrentStreak() > 0) {
                System.out.println("[ ! ] Notice: It's been " + daysBetween + " days. Streak reset to 0.");
                currentUser.setCurrentStreak(0);
                
                try {
                    userDAO.updateUserProfile(currentUser);
                } catch (SQLException e) {
                    System.out.println("\t\t\t\t\t[ ! ] Failed to save streak reset.");
                }
            }
        }
    }

    public double calculateBMI(double weight, double height) {
        return weight / Math.pow(height / 100.0, 2);
    }

    public double calculateBMR(double height, double weight, int age, String sex) {
        if ("M".equalsIgnoreCase(sex)) {
            return 88.36 + (13.4 * weight) + (4.8 * height) - (5.7 * age);
        } else {
            return 447.6 + (9.2 * weight) + (3.1 * height) - (4.3 * age);
        }
    }

    // ============================================================
    // Getters & Helpers
    // ============================================================

    public User getCurrentUser() { return currentUser; }

    public String getName() { return currentUser != null ? currentUser.getName() : ""; }
    public int getAge() { return currentUser != null ? currentUser.getAge() : 0; }
    public double getHeightCm() { return currentUser != null ? currentUser.getHeightCm() : 0.0; }
    public double getWeightKg() { return currentUser != null ? currentUser.getWeightKg() : 0.0; }
    public String getSex() { return currentUser != null ? currentUser.getSex() : ""; }
    public double getBMI() { return currentUser != null ? currentUser.getBMI() : 0.0; }
    public double getBMR() { return currentUser != null ? currentUser.getBMR() : 0.0; }
    public int getStreak() { return currentUser != null ? currentUser.getCurrentStreak() : 0; }

    public boolean hasHistory() {
        return !bodyMetricHistory.isEmpty();
    }

    public boolean isRegistered() {
        return currentUser != null && currentUser.getName() != null && !currentUser.getName().isBlank();
    }
}