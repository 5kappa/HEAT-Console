package HEAT.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import HEAT.dao.DatabaseManager;
import HEAT.model.BodyMetric;
import HEAT.model.Goal;
import HEAT.model.GoalStatus;
import HEAT.model.User;

public class UserService {
    
    // ============================================================
    // Fields & Constructor
    // ============================================================

    private DatabaseManager db;
    private User currentUser = null;
    private List<BodyMetric> bodyMetricHistory = new ArrayList<>();

    private GoalService goalService;

    public UserService() {
        this.db = DatabaseManager.getInstance();

        try {
            User loadedUserProfile = db.loadUserProfile();
            if (loadedUserProfile != null) { currentUser = loadedUserProfile; }

            List<BodyMetric> loadedBodyMetrics = db.loadBodyMetrics();
            if (loadedBodyMetrics != null) { bodyMetricHistory = loadedBodyMetrics; }

            System.out.println("UserService initialized.");
            System.out.println("Body metric history loaded: " + bodyMetricHistory.size() + "\n");

        } catch (Exception e) {
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
            db.beginTransaction();

            double bmi = calculateBMI(weight, height);
            double bmr = calculateBMR(height, weight, age, sex);

            User user = new User(name, age, height, weight, sex, bmi, bmr);

            db.saveUserProfile(user);
            
            List<Goal> completedGoals = goalService.evaluateWeightGoals(weight);

            if (!completedGoals.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                for (Goal g : completedGoals) ids.add(g.getId());

                db.updateGoalStatusBatch(ids, GoalStatus.COMPLETED);
                System.out.println("\n\t\t\t\t\tYou reached your weight goal!");
            }

            db.commitTransaction();

            currentUser = user;

            if (!completedGoals.isEmpty()) {
                goalService.archiveCompletedGoals(completedGoals);
            }
            
        } catch (Exception e) {
            try {
                db.rollbackTransaction();
                System.err.println("Error saving user profile. Rolled back changes.");
            } catch (Exception ex) {
                System.err.println("Rollback also failed: " + ex.getMessage());
            }
            
            System.err.println("Failed to save user profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // [U] Profile Updating (for when body metrics are changed)
    public boolean updateProfile(User updatedUser) {
        try {
            db.beginTransaction();
            
            db.updateUserProfile(updatedUser);
            
            // Check Goals
            List<Goal> completedGoals = new ArrayList<>();
            if (updatedUser.getWeightKg() != currentUser.getWeightKg()) {
                 completedGoals = goalService.evaluateWeightGoals(updatedUser.getWeightKg());
            }
            
            if (!completedGoals.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                for (Goal g : completedGoals) ids.add(g.getId());
                
                db.updateGoalStatusBatch(ids, GoalStatus.COMPLETED);
                System.out.println("\n\t\t\t\t\tYou reached your weight goal!");
            }

            db.commitTransaction();
            
            this.currentUser = updatedUser;
            
            if (!completedGoals.isEmpty()) {
                goalService.archiveCompletedGoals(completedGoals);
            }

            return true;

        } catch (Exception e) {
            try { db.rollbackTransaction(); } catch (Exception ex) {}
            System.out.println("\t\t\t\t\t[ ! ]   Error updating profile: " + e.getMessage());
            return false;
        }
    }

    // [U] Profile Updating (for when User Profile is changed)
    public boolean correctProfileDetails(User updatedUser) {
        try {
            db.beginTransaction();
            db.updateUserProfile(updatedUser);

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

                    db.updateBodyMetric(updatedMetric);
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
                db.updateGoalStatusBatch(ids, GoalStatus.COMPLETED);
                System.out.println("\n\t\t\t\t\tYou reached your weight goal!");
            }

            db.commitTransaction();
            this.currentUser = updatedUser;

            if (!completedGoals.isEmpty()) {
                goalService.archiveCompletedGoals(completedGoals);
            }

            return true;

        } catch (Exception e) {
            try { db.rollbackTransaction(); } catch (Exception ex) {}
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
                %37s%6s%12s%-10s%13s%-19s
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
                "", "  \\   ", "",
                "", "", 
                String.format("Streak :  %d \uD83D\uDD25", getStreak())
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
            db.insertNewBodyMetric(bm);
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
            db.beginTransaction();

            db.updateBodyMetric(updated);

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

                db.saveUserProfile(updatedUser);

                goalService.evaluateWeightGoals(updatedUser.getWeightKg());
                
                this.currentUser = updatedUser;
            }

            db.commitTransaction();

            for (int i = 0; i < bodyMetricHistory.size(); i++) {
                if (bodyMetricHistory.get(i).getId() == updated.getId()) {
                    bodyMetricHistory.set(i, updated);
                    break;
                }
            }
            
            bodyMetricHistory.sort((m1, m2) -> m2.getDate().compareTo(m1.getDate()));

            return true;

        } catch (Exception e) {
            try { db.rollbackTransaction(); } catch (Exception ex) {}
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
            db.beginTransaction();
            
            db.deleteBodyMetric(bm.getId());

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
                    
                    db.updateUserProfile(updatedUser);
                    
                    goalService.evaluateWeightGoals(updatedUser.getWeightKg());

                    this.currentUser = updatedUser; 

                } else {
                    System.out.println("\t\t\t\t\tWarning: Deleted the only body metric. Profile stats may be stale.");
                }
            }
            
            db.commitTransaction();
            
            bodyMetricHistory.remove(bm);
            return true;

        } catch (Exception e) {
            try { db.rollbackTransaction(); } catch (Exception ex) {}
            System.out.println("\t\t\t\t\tError deleting body metric: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    // Streak & Calculations
    // ============================================================

    public void checkStreak(LocalDate activityDate) {
        LocalDate lastDate = currentUser.getLastWorkoutDate();
        int currentStreak = currentUser.getCurrentStreak();
        boolean isChanged = false;

        if (lastDate == null) {
            currentUser.setCurrentStreak(1);
            currentUser.setLastWorkoutDate(activityDate);
            isChanged = true;
        } 
        else if (activityDate.isAfter(lastDate)) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastDate, activityDate);

            if (daysBetween == 1) {
                currentUser.setCurrentStreak(currentStreak + 1);
                System.out.println("\t\t\t\t\tStreak Up! " + (currentStreak + 1) + " days in a row!");
            } else {
                currentUser.setCurrentStreak(1);
                System.out.println("\t\t\t\t\tStreak reset. Starting over at 1.");
            }
            
            currentUser.setLastWorkoutDate(activityDate);
            isChanged = true;
        }

        if (isChanged) {
            try {
                db.updateUserProfile(currentUser);
            } catch (SQLException e) {
                System.out.println("\t\t\t\t\t[ ! ] Failed to save streak progress.");
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