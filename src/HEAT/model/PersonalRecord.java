package HEAT.model;

import java.time.LocalDate;

public class PersonalRecord {
    private String activityName;
    private int durationMinutes = 0;
    private int reps = 0;
    private double weightKg = 0.0;
    private LocalDate date;
    
    public PersonalRecord(String activityName, int durationMinutes, int reps, double weightKg, LocalDate date) {
        this.activityName = activityName;
        this.durationMinutes = durationMinutes;
        this.reps = reps;
        this.weightKg = weightKg;
        this.date = date;
    }
    
    public String getActivityName() { return activityName; }
    public int getDuration() { return durationMinutes; }
    public int getReps() { return reps; }
    public double getWeight() { return weightKg; }
    public LocalDate getDate() { return date; }

    public void setName(String newName) { this.activityName = newName; }

    @Override
    public String toString() {
        double PRValue;
        String PRValueStr;

        if (activityName.contains("(loaded)") || weightKg > 0) {
            PRValue = this.weightKg;
            PRValueStr = String.format("%.1f kg", PRValue);
        } else if (reps > 0) {
            PRValue = this.reps;
            PRValueStr = String.format("%d reps", (int) PRValue);
        } else {
            PRValue = this.durationMinutes;
            PRValueStr = String.format("%d mins", (int) PRValue);
        }

        String str = String.format(" |   %-60s   |   %-37s   |   %-37s   ",
            this.activityName,
            PRValueStr,
            this.date.toString()
        );

        return str;
    }
}