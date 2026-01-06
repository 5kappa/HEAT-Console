package heat.model;

import java.time.LocalDate;

public class CardioWorkout extends Workout {
    private double distanceKm;

    public CardioWorkout(String name, String type, LocalDate date, double caloriesBurned,
                         int durationMinutes) {
        
        super(name, type, date, caloriesBurned, durationMinutes);

        this.distanceKm = calculateDistance(name, durationMinutes);
    }

    public CardioWorkout(int id, String name, String type, LocalDate date, double caloriesBurned,
                         int durationMinutes, double distanceKm) {
        
        super(id, name, type, date, caloriesBurned, durationMinutes);

        this.distanceKm = distanceKm;
    }

    private double calculateDistance(String activityName, int durationMinutes) {
        activityName = activityName.toLowerCase();
        
        // Estimate distance based on activity type
        if (activityName.contains("running") || activityName.contains("run")) {
            return durationMinutes * 0.133;  // 8 km/h
        }
        else if (activityName.contains("cycling") || activityName.contains("cycle") || activityName.contains("bike")) {
            return durationMinutes * 0.25;   // 15 km/h
        }
        else if (activityName.contains("walking") || activityName.contains("walk")) {
            return durationMinutes * 0.083;  // 5 km/h
        }
        else if (activityName.contains("swimming") || activityName.contains("swim")) {
            return durationMinutes * 0.033;  // 2 km/h
        }
        else if (activityName.contains("jumping rope")) {
            return 0.0;  // Stationary
        }
        else if (activityName.contains("dancing")) {
            return durationMinutes * 0.05;   // Moderate movement
        }
        else if (activityName.contains("boxing") || activityName.contains("taekwondo")) {
            return durationMinutes * 0.02;   // Martial arts
        }
        else if (activityName.contains("tennis") || activityName.contains("basketball") || 
                 activityName.contains("volleyball") || activityName.contains("football") ||
                 activityName.contains("badminton")) {
            return durationMinutes * 0.1;    // Court sports
        }
        else {
            // Default for HIIT exercises
            return 0.0;
        }
    }

    public double getDistanceKm() { return distanceKm; }

    @Override
    public String toString() {
        String typeStr = String.format("[%s]:", this.type);
        String distanceStr = String.format("~%.1f km", this.distanceKm);
        String durationStr = String.format("%d mins", this.durationMinutes);
        String caloriesBurnedStr = String.format("%.0f cal",  this.caloriesBurned);

        String str = String.format(" |   %11s %-24s  |   %-17s   |   %-37s   |   %-8s   |   %-8s   | %-10s  ",
            typeStr,
            this.name,
            distanceStr,
            " ".repeat(17) + "-",
            durationStr,
            caloriesBurnedStr,
            this.date.isEqual(LocalDate.now()) ? "  Today" : this.date.toString());
        
        return str;
    }
}