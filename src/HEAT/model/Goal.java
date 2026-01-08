package heat.model;

import java.time.LocalDate;

public class Goal {
    private int id;
    private String goalTitle;
    private String exerciseName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String goalType;
    private double currentValue;
    private double targetValue;
    private GoalStatus status;

    // Constructor 1: Loading from database    
    public Goal(int id, String goalTitle, String exerciseName, LocalDate startDate, LocalDate endDate,
                String goalType, double currentValue, double targetValue, GoalStatus status) {

        this.id = id;
        this.goalTitle = goalTitle;
        this.exerciseName = exerciseName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.goalType = goalType;
        this.currentValue = currentValue;
        this.targetValue = targetValue;
        this.status = status;
    }

    // Constructor 2: Creating a new goal (user input)
    public Goal(String goalTitle, String exerciseName, LocalDate startDate, LocalDate endDate,
                String goalType, double currentValue, double targetValue) {

        this.goalTitle = goalTitle;
        this.exerciseName = exerciseName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.goalType = goalType;
        this.currentValue = currentValue;
        this.targetValue = targetValue;
        this.status = GoalStatus.ACTIVE;
    }

    public int getId() { return id; }
    public String getGoalTitle() { return goalTitle; }
    public String getExerciseName() { return exerciseName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getGoalType() { return goalType; }
    public double getCurrentValue() { return currentValue; }
    public double getTargetValue() { return targetValue; }
    public GoalStatus getStatus() { return status; }

    public void setId(int newId) { this.id = newId; }
    public void setStatus(GoalStatus newStatus) { this.status = newStatus; }
    public void setCurrentValue(double newValue) { this.currentValue = newValue; }

    @Override
    public String toString() {
        String unit = "";

        if (goalType.equals("weight loss") || goalType.equals("weight gain")) unit = "kg (bodyweight)";
        else if (goalType.equals("reps")) unit = "reps";
        else if (goalType.equals("duration")) unit = "mins";
        else if (goalType.equals("weight lifted")) unit = "kg (lifted)";
        else unit = "times worked out";

        String goalTargetValueStr, goalCurrentValueStr;

        if (unit.equals("kg (bodyweight)") || goalType.equals("kg (lifted)")) {
            goalTargetValueStr = String.format("%.1f %s", this.targetValue, unit);
            goalCurrentValueStr = String.format("%.1f %s", this.currentValue, unit);
        } else {
            goalTargetValueStr = String.format("%.0f %s", this.targetValue, unit);
            goalCurrentValueStr = String.format("%.0f %s", this.currentValue, unit);
        }

        String str = String.format(" |  %-31s  |  %-22s  |  %-20s  |  %-20s  | %-10s | %-10s | %-9s",
            this.goalTitle,
            this.exerciseName == null ? "-" : this.exerciseName,
            goalCurrentValueStr,
            goalTargetValueStr,
            this.startDate.isEqual(LocalDate.now()) ? "Today" : this.startDate.toString(),
            this.endDate == null ? "none" : this.endDate.isEqual(LocalDate.now()) ? "Today" : this.endDate.toString(),
            this.status.name()
        );

        return str;
    }
}