package heat.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import heat.dao.DatabaseConnection;
import heat.dao.GoalDAO;
import heat.model.Goal;
import heat.model.GoalStatus;
import heat.model.StrengthWorkout;
import heat.model.Workout;

public class GoalService {
    
    // ============================================================
    // Fields & Constructor
    // ============================================================

    private List<Goal> goals = new ArrayList<>();
    private List<Goal> activeGoals = new ArrayList<>();
    
    private DatabaseConnection dbConnection;
    private GoalDAO goalDAO;
    
    private UserService userService;

    public GoalService(UserService userService) {
        this.dbConnection = DatabaseConnection.getInstance();
        this.goalDAO = new GoalDAO();
        this.userService = userService;

        try {
            List<Goal> loadedGoals = goalDAO.loadGoals();
            if (loadedGoals != null) { goals = loadedGoals; }

            this.activeGoals = new ArrayList<>();
            for (Goal g : this.goals) {
                if (g.getStatus() == GoalStatus.ACTIVE) {
                    this.activeGoals.add(g);
                }
            }

            System.out.println("GoalService initialized.");
            System.out.println("Goals loaded: " + getGoalsSize() + "\n");

        } catch (Exception e) {
            System.out.println("Warning: could not load persisted data: " + e.getMessage());
            e.printStackTrace();
        }

        checkGoalExpiration();
    }

    // ============================================================
    // Goal Management (CRUD)
    // ============================================================

    // [C] Create
    public boolean createGoal(Goal g) {
        try {
            if (isGoalCompleted(g.getCurrentValue(), g.getTargetValue(), g.getGoalType())) {
                System.out.println("\t\t\t\t\t[ ! ]   Could not create goal, already completed: " +  g.getGoalTitle() + "\n");
                return false;
            }

            dbConnection.beginTransaction();
            goalDAO.addGoal(g);
            dbConnection.commitTransaction();

            goals.add(0, g);
            activeGoals.add(0, g);

            return true;

        } catch (Exception e) {
            try {
                dbConnection.rollbackTransaction();
                System.err.println("\t\t\t\t\t[ ! ]   Error creating goal. Rolled back changes.");

            } catch (Exception ex) {
                System.err.println("\t\t\t\t\t[ ! ]   Rollback also failed: " + ex.getMessage());
            }
            System.err.println("\t\t\t\t\t[ ! ]   Failed to create goal: " + e.getMessage());
            e.printStackTrace();

            return false;
        }
    }

    // [U] Update
    public boolean updateGoal(Goal original, Goal updated) {
        if (original.getId() != updated.getId()) return false;

        try {
            dbConnection.beginTransaction();

            boolean isCompleted = isGoalCompleted(original.getCurrentValue(), updated.getTargetValue(), original.getGoalType());
            
            LocalDate today = LocalDate.now();
            boolean isExpired = (updated.getEndDate() != null && updated.getEndDate().isBefore(today));

            if (isCompleted) {
                updated.setStatus(GoalStatus.COMPLETED);
            } else if (isExpired) {
                updated.setStatus(GoalStatus.EXPIRED);
            } else {
                updated.setStatus(GoalStatus.ACTIVE);
            }

            goalDAO.updateGoal(updated);
            dbConnection.commitTransaction();

            // Update Master List
            for (int i = 0; i < goals.size(); i++) {
                if (goals.get(i).getId() == updated.getId()) {
                    goals.set(i, updated);
                    break;
                }
            }
            
            // Update Active List
            activeGoals.removeIf(g -> g.getId() == updated.getId());
            if (updated.getStatus() == GoalStatus.ACTIVE) {
                activeGoals.add(0, updated);
            }

            return true;
        } catch (Exception e) {
            try { dbConnection.rollbackTransaction(); } catch (Exception ex) {}
            System.err.println("\t\t\t\t\t[ ! ]   Failed to update goal: " + e.getMessage());
            return false;
        }
    }

    // [D] Delete
    public boolean deleteGoal(Goal g) {
        try {
            dbConnection.beginTransaction();
            goalDAO.deleteGoal(g.getId());
            dbConnection.commitTransaction();

            goals.removeIf(existing -> existing.getId() == g.getId());
            activeGoals.removeIf(existing -> existing.getId() == g.getId());

            return true;
        } catch (SQLException e) {
            System.out.println("\t\t\t\t\t[ ! ]   Error deleting goal: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    // Evaluation Logic (Progress Checking)
    // ============================================================

    public List<Goal> refreshGoalsForWorkout(Workout w) throws SQLException {
        List<Goal> newlyCompletedGoals = new ArrayList<>();
        List<Goal> revivedGoals = new ArrayList<>();

        for (Goal g : goals) {
            if (g.getStatus() == GoalStatus.EXPIRED) continue;

            if (w.getDate().isBefore(g.getStartDate())) continue;
            if (g.getEndDate() != null && w.getDate().isAfter(g.getEndDate())) continue;

            String type = g.getGoalType();
            String goalExercise = g.getExerciseName();

            if (type.equals("weight loss") || type.equals("weight gain")) continue;
            
            if (!goalExercise.equalsIgnoreCase(w.getName())) continue;

            boolean isRelevantToGoal = false;
            if (type.equals("frequency")) isRelevantToGoal = true;
            else if ((type.equals("weight lifted") || type.equals("reps")) && w instanceof StrengthWorkout) isRelevantToGoal = true;
            else if (type.equals("duration") || type.equals("total_duration")) isRelevantToGoal = true;
            else if (type.equals("total_reps")) isRelevantToGoal = true; 

            if (isRelevantToGoal) {
                double newValue = getCurrentValue(type, goalExercise, g.getStartDate());
                g.setCurrentValue(newValue);
                goalDAO.updateGoalCurrentValue(g.getId(), newValue);

                boolean metTarget = isGoalCompleted(newValue, g.getTargetValue(), type);

                if (g.getStatus() == GoalStatus.ACTIVE && metTarget) {
                    g.setStatus(GoalStatus.COMPLETED);
                    newlyCompletedGoals.add(g);
                }
                else if (g.getStatus() == GoalStatus.COMPLETED && !metTarget) {
                    g.setStatus(GoalStatus.ACTIVE);
                    goalDAO.updateGoalStatus(g.getId(), "ACTIVE");
                    revivedGoals.add(g);
                    System.out.println("\t\t\t\t\tGoal downgraded to ACTIVE: " + g.getGoalTitle());
                }
            }
        }

        for (Goal revived : revivedGoals) {
            if (!activeGoals.contains(revived)) {
                activeGoals.add(revived);
            }
        }

        return newlyCompletedGoals;
    }

    public List<Goal> evaluateWeightGoals(double currentWeight) throws SQLException {
        List<Goal> completedGoals = new ArrayList<>();

        for (Goal g : activeGoals) {
            String type = g.getGoalType();

            if (!type.equals("weight loss") && !type.equals("weight gain")) {
                continue;
            }

            g.setCurrentValue(currentWeight);
            goalDAO.updateGoalCurrentValue(g.getId(), currentWeight);

            boolean isComplete = false;

            if (type.equals("weight loss")) {
                if (currentWeight <= g.getTargetValue()) isComplete = true;
            }
            else if (type.equals("weight gain")) {
                if (currentWeight >= g.getTargetValue()) isComplete = true;
            }

            if (isComplete) {
                g.setStatus(GoalStatus.COMPLETED);
                completedGoals.add(g);
            }
        }

        return completedGoals;
    }

    public boolean isGoalCompleted(double currentValue, double targetValue, String goalType) throws NullPointerException {
        if (goalType.equals("weight loss")) {
            return currentValue <= targetValue;
        } else {
            return currentValue >= targetValue;
        }
    }

    public double getCurrentValue(String goalType, String exerciseName, LocalDate startDate) {
        try {
            if (goalType.equals("weight loss") || goalType.equals("weight gain")) {
                return userService.getWeightKg();
            }
            else if (goalType.equals("reps")) {
                return (double) goalDAO.getMostRepsDone(exerciseName, startDate);
            }
            else if (goalType.equals("duration")) {
                return (double) goalDAO.getTotalMinutes(exerciseName, startDate);
            }
            else if (goalType.equals("weight lifted")) {
                return goalDAO.getMaxWeightLifted(exerciseName, startDate);
            }
            else {
                return (double) goalDAO.getWorkoutFrequency(exerciseName, startDate);
            }
        } catch (SQLException e) {
            System.out.println("\t\t\t\t\t[ ! ]   Error fetching current goal value: " + e.getMessage());
            return 0.0;
        }
    }

    // ============================================================
    // Maintenance (Archiving & Expiration)
    // ============================================================

    public void archiveCompletedGoals(List<Goal> goalsToArchive) throws SQLException {
        if (goalsToArchive == null || goalsToArchive.isEmpty()) return;

        for (Goal completedGoal : goalsToArchive) {
            activeGoals.removeIf(g -> g.getId() == completedGoal.getId());

            for (Goal masterGoal : goals) {
                if (masterGoal.getId() == completedGoal.getId()) {
                    masterGoal.setStatus(GoalStatus.COMPLETED);
                    masterGoal.setCurrentValue(completedGoal.getCurrentValue());
                    break;
                }
            }
            System.out.println("\t\t\t\t\tGoal completed: " + completedGoal.getGoalTitle());
        }
    }

    private void checkGoalExpiration() {
        List<Integer> expiredIds = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Goal g : activeGoals) {
            if (g.getEndDate() != null) {
                if (g.getEndDate().isBefore(today)) {
                    expiredIds.add(g.getId());
                    g.setStatus(GoalStatus.EXPIRED);
                }
            }
        }

        if (!expiredIds.isEmpty()) {
            try {
                dbConnection.beginTransaction(); 
                
                goalDAO.updateGoalStatusBatch(expiredIds, GoalStatus.EXPIRED);
                
                dbConnection.commitTransaction(); 

                archiveExpiredGoals(expiredIds);

                System.out.println("\t\t\t\t\tCleaned up " + expiredIds.size() + " expired goals.");
            } catch (SQLException e) {
                try { dbConnection.rollbackTransaction(); } catch (Exception ex) {}
                System.out.println("\t\t\t\t\t[ ! ]   Failed to update expired goals: " + e.getMessage());
            }
        }
    }

    private void archiveExpiredGoals(List<Integer> expiredIds) {
        activeGoals.removeIf(g -> expiredIds.contains(g.getId()));

        for (Goal masterGoal : goals) {
            if (expiredIds.contains(masterGoal.getId())) {
                masterGoal.setStatus(GoalStatus.EXPIRED);
            }
        }
    }

    // ============================================================
    // Getters & Helpers
    // ============================================================

    public List<Goal> getAllGoals() {
        return goals;
    }

    public List<Goal> getActiveGoals() {
        return activeGoals;
    }

    public int getGoalsSize() {
        return goals.size();
    }

    public List<Integer> getCompletedGoalsId(List<Goal> completedGoals) {
        List<Integer> completedIds = new ArrayList<>();
        for (Goal g : completedGoals) {
            completedIds.add(g.getId());
        }
        return completedIds;
    }
}