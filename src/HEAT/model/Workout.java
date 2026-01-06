package heat.model;

import java.time.LocalDate;

public abstract class Workout {
    protected int id;
    protected String name;
    protected String type;
    protected LocalDate date;
    protected double caloriesBurned;
    protected int durationMinutes;

    public Workout(int id, String name, String type, LocalDate date, double caloriesBurned, int durationMinutes) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.date = (date != null) ? date : LocalDate.now();
        this.caloriesBurned = caloriesBurned;
        this.durationMinutes = durationMinutes;
    }

    public Workout(String name, String type, LocalDate date, double caloriesBurned, int durationMinutes) {
        this.name = name;
        this.type = type;
        this.date = (date != null) ? date : LocalDate.now();
        this.caloriesBurned = caloriesBurned;
        this.durationMinutes = durationMinutes;
    }    

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public LocalDate getDate() { return date; }

    public int getDurationMinutes() { return durationMinutes; }
    public double getCaloriesBurned() { return caloriesBurned; }

    public void setId(int newId) { this.id = newId; } 

    @Override
    public String toString() {
        return String.format("[%s]: %s", type.toUpperCase(), name);
    }
}