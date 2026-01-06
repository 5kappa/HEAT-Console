package HEAT.model;

public class Activity {
    private int id;
    private String activityName;
    private String workoutType;
    private String category;
    private double metValue;
    private double bodyWeightFactor;

    public Activity(int id, String activityName, String workoutType, String category, double metValue, double bodyWeightFactor) {
        this.id = id;
        this.activityName = activityName;
        this.workoutType = workoutType;
        this.category = category;
        this.metValue = metValue;
        this.bodyWeightFactor = bodyWeightFactor;
    }

    public Activity(String activityName, String workoutType, String category, double metValue, double bodyWeightFactor) {
        this.activityName = activityName;
        this.workoutType = workoutType;
        this.category = category;
        this.metValue = metValue;
        this.bodyWeightFactor = bodyWeightFactor;
    }

    public int getId() { return id; }
    public String getActivityName() { return activityName; }
    public String getWorkoutType() { return workoutType; }
    public String getCategory() { return category; }
    public double getMetValue() { return metValue; }
    public double getBodyWeightFactor() { return bodyWeightFactor; }
}