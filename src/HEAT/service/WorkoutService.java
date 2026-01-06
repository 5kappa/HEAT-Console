package HEAT.service;

import java.util.*;
import java.time.LocalDate;
import java.sql.*;
import HEAT.dao.DatabaseManager;
import HEAT.model.*;

public class WorkoutService {

    // ============================================================
    // Fields & Constructor
    // ============================================================

    private final Random random = new Random();
 
    private Map<String, List<String>> quoteCatalog = new LinkedHashMap<>();
    private Map<String, List<String>> activitiesByCategory = new LinkedHashMap<>();
    private Map<String, Activity> activitiesByName = new LinkedHashMap<>();

    private List<Workout> workouts = new ArrayList<>();
    
    private Map<String, PersonalRecord> personalRecords = new LinkedHashMap<>();

    private UserService userService;
    private GoalService goalService;
    private DatabaseManager db;

    public WorkoutService(GoalService goalService, UserService userService) {
        this.db = DatabaseManager.getInstance();
        this.goalService = goalService;
        this.userService = userService;

        try {
            List<Workout> loadedWorkouts = db.loadWorkouts();
            if (loadedWorkouts != null) { workouts = loadedWorkouts; }

            Map<String, PersonalRecord> loadedPRs = db.loadPersonalRecords();
            if (loadedPRs != null) { personalRecords = loadedPRs; }

            List<Quote> loadedQuotes = db.loadQuotes();
            if (loadedQuotes != null) { this.quoteCatalog = sortQuotes(loadedQuotes); }

            List<Activity> loadedActivities = db.loadActivities();
            if (loadedActivities != null) { sortActivities(loadedActivities); }
            
            System.out.println("WorkoutService initialized.");
            System.out.println("Workouts loaded: " + workouts.size());
            System.out.println("PRs loaded: " + personalRecords.size() + "\n");
            
        } catch (Exception e) {
            System.out.println("Warning: could not load persisted data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // Workout Management (CRUD & Views)
    // ============================================================

    // [C] Create
    public void logWorkout(Workout w) {
        try {
            db.beginTransaction();

            // Save workout (DATABASE)
            if (w instanceof StrengthWorkout) {
                db.saveStrengthWorkout(w);
            } else {
                db.saveCardioWorkout(w);
            }

            // Check and update PR (DATABASE)
            boolean newRecordAchieved = isNewPR(w, fetchOldPR(w));
            if (newRecordAchieved) {
                System.out.print("\t\t\t\t\tNew PR for " + w.getName() + ": ");
                if (w instanceof StrengthWorkout) {
                    StrengthWorkout sw = (StrengthWorkout) w;
                    System.out.printf("%.1f kg, %d reps\n", sw.getExternalWeightKg(), sw.getRepCount());
                } else {
                    System.out.println(w.getDurationMinutes() + " mins");
                }

                updatePRDatabase(w);
            }

            // Check and remove completed goals (DATABASE)
            List<Goal> completedGoals = goalService.refreshGoalsForWorkout(w);
            List<Integer> completedGoalsIds = new ArrayList<>();

            if (!completedGoals.isEmpty()) {
                completedGoalsIds = goalService.getCompletedGoalsId(completedGoals);
                db.updateGoalStatusBatch(completedGoalsIds, GoalStatus.COMPLETED);

                for (Goal g : completedGoals) {
                    System.out.println("\t\t\t\t\tGoal completed: " + g.getGoalTitle());
                }
            }

            db.commitTransaction();

            // Save workout (LOCAL)
            workouts.add(0, w);

            // Update PRs (LOCAL)
            if (newRecordAchieved) addPersonalRecord(w);

            // Update goals list (LOCAL)
            if (!completedGoals.isEmpty()) {
                goalService.archiveCompletedGoals(completedGoals);
            }
            
            // Check Streak (Since a workout was just logged)
            userService.checkStreak(w.getDate());
            
        } catch (Exception e) {
            try {
                db.rollbackTransaction();
                System.err.println("\t\t\t\t\t[ ! ]   Error saving workout. Rolled back changes.");
            } catch (Exception ex) {
                System.err.println("\t\t\t\t\t[ ! ]   Rollback also failed: " + ex.getMessage());
            }
            
            System.err.println("\t\t\t\t\t[ ! ]   Failed to log workout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // [U] Update
    public boolean updateWorkout(Workout original, Workout updated) {
        if (original.getId() != updated.getId()) {
            System.err.println("\t\t\t\t\t[ ! ]   Error: ID mismatch during update.");
            return false;
        }

        try {
            db.beginTransaction();

            db.updateWorkout(updated);

            String prKey = generateKey(updated);
            PersonalRecord existingPR = personalRecords.get(prKey);
            
            boolean wasTheRecordHolder = false;

            if (existingPR != null && existingPR.getDate().isEqual(original.getDate())) {
                if (original instanceof StrengthWorkout sw) {
                    if (prKey.endsWith("(reps)")) {
                        wasTheRecordHolder = (sw.getRepCount() >= existingPR.getReps());
                    } else {
                        wasTheRecordHolder = (sw.getExternalWeightKg() >= existingPR.getWeight());
                    }
                } else if (original instanceof CardioWorkout cw) {
                    wasTheRecordHolder = (cw.getDurationMinutes() == existingPR.getDuration());
                }
            }

            if (wasTheRecordHolder) {
                db.recalculatePR(updated.getName(), prKey, updated.getType());
                this.personalRecords = db.loadPersonalRecords();
            } else {
                if (isNewPR(updated, existingPR)) {
                    updatePRDatabase(updated);
                    addPersonalRecord(updated);
                }
            }

            goalService.refreshGoalsForWorkout(updated);

            db.commitTransaction();

            // Update local cache
            for (int i = 0; i < workouts.size(); i++) {
                if (workouts.get(i).getId() == updated.getId()) {
                    workouts.set(i, updated);
                    break;
                }
            }

            workouts.sort((w1, w2) -> w2.getDate().compareTo(w1.getDate()));

            return true;
        } catch (Exception e) {
            try { db.rollbackTransaction(); } catch (Exception ex) {}
            System.err.println("\t\t\t\t\t[ ! ]   Failed to update workout: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // [D] Delete
    public boolean deleteWorkout(Workout w) {
        try {
            db.beginTransaction();

            db.deleteWorkout(w.getId());

            String PRName = generateKey(w);
            PersonalRecord currentPR = personalRecords.get(PRName);

            if (matchesCurrentPR(w, currentPR)) {
                db.recalculatePR(w.getName(), PRName, w.getType());
                this.personalRecords = db.loadPersonalRecords();
            }

            goalService.refreshGoalsForWorkout(w);

            db.commitTransaction();

            workouts.removeIf(existing -> existing.getId() == w.getId());

            return true;
        } catch (Exception e) {
            try { db.rollbackTransaction(); } catch (Exception ex) {}
            System.err.println("\t\t\t\t\t[ ! ]   Failed to delete workout: " + e.getMessage());
            return false;
        }
    }

    // [R] Views & Getters
    public List<Workout> getAllWorkouts() {
        return workouts;
    }

    public List<Workout> getWeeklyWorkouts() {
        List<Workout> weekly = new ArrayList<>();
        LocalDate cutoff = LocalDate.now().minusDays(7); 

        for (Workout w : workouts) {
            if (!w.getDate().isBefore(cutoff)) {
                weekly.add(w);
            } else {
                break; 
            }
        }
        return weekly;
    }

    public int getWorkoutsSize() {
        return workouts.size();
    }

    public int getWeeklyWorkoutsSize() {
        return getWeeklyWorkouts().size();
    }

    public double computeTotalCalories(List<Workout> workoutList) {
        double totalCalories = 0;
        for (Workout w : workoutList) {
            totalCalories += w.getCaloriesBurned();
        }
        return totalCalories;
    }

    public double computeTotalTrainingVolumeKg(List<Workout> workoutList) {
        double totalTrainingVolumeKg = 0.0;
        for (Workout w : workoutList) {
            if (w instanceof StrengthWorkout) {
                StrengthWorkout sw = (StrengthWorkout) w;
                totalTrainingVolumeKg += sw.getTrainingVolumeKg();
            }
        }
        return totalTrainingVolumeKg;
    }

    // ============================================================
    // Personal Records Management
    // ============================================================

    public List<PersonalRecord> getAllPRs() {
        return new ArrayList<>(personalRecords.values());
    }

    public int getPRsSize() {
        return personalRecords.size();
    }

    public boolean deletePR(String prName) {
        try {
            db.deletePR(prName);
            personalRecords.remove(prName);
            return true;
        } catch (SQLException e) {
            System.out.println("\t\t\t\t\t[ ! ]   Error deleting PR: " + e.getMessage());
            return false;
        }
    }

    private void addPersonalRecord(Workout w) {
        String finalKey = generateKey(w);
        int durationMinutes = w.getDurationMinutes();
        LocalDate date = w.getDate();

        int reps = 0;
        double weight = 0.0;

        if (w instanceof StrengthWorkout) {
            StrengthWorkout sw = (StrengthWorkout) w;
            reps = sw.getRepCount();
            weight = sw.getExternalWeightKg();
        }

        personalRecords.put(finalKey, new PersonalRecord(finalKey, durationMinutes, reps, weight, date));
    }

    private void updatePRDatabase(Workout w) throws SQLException {
        String exerciseName = generateKey(w);
        int duration = w.getDurationMinutes();
        LocalDate date = w.getDate();

        double weight = 0.0;
        int reps = 0;

        if (w instanceof StrengthWorkout) {
            StrengthWorkout sw = (StrengthWorkout) w;
            reps = sw.getRepCount();
            weight = sw.getExternalWeightKg(); 
        }

        db.updatePersonalRecord(exerciseName, weight, reps, duration, date);
    }

    private PersonalRecord fetchOldPR(Workout w) throws SQLException {
        String lookupKey = generateKey(w);
        return personalRecords.get(lookupKey);
    }

    private boolean isNewPR(Workout w, PersonalRecord oldPR) {
        if (oldPR == null) return true;

        if (w instanceof StrengthWorkout) {
            StrengthWorkout sw = (StrengthWorkout) w;

            if (sw.getExternalWeightKg() > 0) {
                if (sw.getExternalWeightKg() > oldPR.getWeight()) return true;
                if (sw.getExternalWeightKg() < oldPR.getWeight()) return false;
            }

            return sw.getRepCount() > oldPR.getReps();

        } else if (w instanceof CardioWorkout) {
            return w.getDurationMinutes() > oldPR.getDuration();
        } 
        
        return false;
    }

    private boolean matchesCurrentPR(Workout w, PersonalRecord currentPR) {
        if (currentPR == null) return false;

        if (w instanceof StrengthWorkout) {
            StrengthWorkout sw = (StrengthWorkout) w;
            if (sw.getExternalWeightKg() > 0) {
                return sw.getExternalWeightKg() == currentPR.getWeight()
                    && sw.getRepCount() == currentPR.getReps();
            }
            return sw.getRepCount() == currentPR.getReps();
        } else if (w instanceof CardioWorkout) {
            return w.getDurationMinutes() == currentPR.getDuration();
        }

        return false;
    }

    private String generateKey(Workout w) {
        String baseName = w.getName();
        
        if (w instanceof StrengthWorkout) {
            StrengthWorkout sw = (StrengthWorkout) w;
            if (sw.getBodyWeightFactor() != 0 && sw.getExternalWeightKg() > 0) {
                return baseName + " (loaded)";
            } else if (sw.getExternalWeightKg() == 0 && sw.getBodyWeightFactor() != 0) {
                return baseName + " (reps)";
            }
        }
        return baseName;
    }

    // ============================================================
    // Activity & Metadata Helpers
    // ============================================================

    public List<String> getActivityNamesByCategory(String category) {
        return this.activitiesByCategory.get(category);
    }

    public double getMetForActivity(String activityName) {
        double metValue = 0.0;
        if (this.activitiesByName.get(activityName) != null) {
            metValue = this.activitiesByName.get(activityName).getMetValue();
        }
        return metValue;
    }

    public double getBodyWeightFactorForActivity(String activityName) {
        double bodyWeightFactor = 0.0;
        if (this.activitiesByName.get(activityName) != null) {
            bodyWeightFactor = this.activitiesByName.get(activityName).getBodyWeightFactor();
        }
        return bodyWeightFactor;
    }

    public static double calculateCaloriesBurned(double metValue, double weightKg, int durationMinutes) {
        return metValue * 3.5 * weightKg * durationMinutes / 200;
    }

    private void sortActivities(List<Activity> activitiesList) {
        Map<String, List<String>> activityCategoryMap = new HashMap<>();
        Map<String, Activity> activityNameMap = new HashMap<>();

        for (Activity a : activitiesList) {
            activityCategoryMap.putIfAbsent(a.getCategory(), new ArrayList<>());
            activityCategoryMap.get(a.getCategory()).add(a.getActivityName());

            activityNameMap.put(a.getActivityName(), a);
        }

        this.activitiesByCategory = activityCategoryMap;
        this.activitiesByName = activityNameMap;
    }

    // ============================================================
    // Motivation & Quotes
    // ============================================================

    public String getQuote() {
        String level = getMotivationLevel(userService.getStreak());
        List<String> quotes = fetchQuotesByLevel(level);

        if (quotes == null || quotes.isEmpty()) {
            return "Stay consistent.";
        }
        return quotes.get(random.nextInt(quotes.size()));
    }

    private String getMotivationLevel(int streak) {
        if (streak < 3) return "harsh";
        if (streak < 7) return "firm";
        return "standard";
    }

    private List<String> fetchQuotesByLevel(String level) {
        return this.quoteCatalog.get(level);
    }

    private Map<String, List<String>> sortQuotes(List<Quote> quotesList) {
        Map<String, List<String>> quoteMap = new HashMap<>();
        for (Quote q : quotesList) {
            String level = q.getLevel().toLowerCase();
            String text = q.getQuote();

            quoteMap.putIfAbsent(level, new ArrayList<>());
            quoteMap.get(level).add(text);
        }
        return quoteMap;
    }

    // ============================================================
    // Database Testing Utilities
    // ============================================================

    public void testDatabaseConnection() {
        System.out.println("\n=== DATABASE CONNECTION TEST ===");
        try {
            try (Connection conn = db.getConnection()) {
                System.out.println("✓ Connected to database successfully!");
                System.out.println("Database URL: " + conn.getMetaData().getURL());
                
                DatabaseMetaData meta = conn.getMetaData();
                String[] types = {"TABLE"};
                ResultSet tables = meta.getTables(null, null, "%", types);
                
                System.out.println("\nTables found:");
                boolean hasTables = false;
                while (tables.next()) {
                    hasTables = true;
                    String tableName = tables.getString("TABLE_NAME");
                    System.out.println("  - " + tableName);
                }
                
                if (!hasTables) {
                    System.out.println("  No tables found! Run DatabaseManager.initializeDatabase()");
                }
                
            } catch (SQLException e) {
                System.err.println("✗ Database connection failed: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("✗ Test failed: " + e.getMessage());
        }
        System.out.println("================================\n");
    }
}