package HEAT.model;

import java.time.LocalDate;

public class StrengthWorkout extends Workout {
    private int setCount;
    private int repCount;
    private double bodyWeightFactor;
    private double bodyWeightUsedKg;
    private double externalWeightKg;
    private double trainingVolumeKg;

    public StrengthWorkout(String name, String type, LocalDate date, double caloriesBurned,
                           int durationMinutes, int setCount, int repCount, double bodyWeightKg,
                           double externalWeightKg, double bodyWeightFactor) {
        
        super(name, type, date, caloriesBurned, durationMinutes);

        this.setCount = setCount;
        this.repCount = repCount;
        this.bodyWeightFactor = bodyWeightFactor;
        this.bodyWeightUsedKg = bodyWeightKg * bodyWeightFactor;
        this.externalWeightKg = externalWeightKg;
        this.trainingVolumeKg = (bodyWeightUsedKg + externalWeightKg) * setCount * repCount;
    }

    public StrengthWorkout(int id, String name, String type, LocalDate date, double caloriesBurned,
                        int durationMinutes, int setCount, int repCount, double externalWeightKg,
                        double trainingVolumeKg, double bodyWeightFactor) {
        
        super(id, name, type, date, caloriesBurned, durationMinutes);

        this.setCount = setCount;
        this.repCount = repCount;
        this.externalWeightKg = externalWeightKg;
        this.trainingVolumeKg = trainingVolumeKg;
        this.bodyWeightFactor = bodyWeightFactor;
    }

    public int getSetCount() { return setCount; }
    public int getRepCount() { return repCount; }
    public double getBodyWeightFactor() { return bodyWeightFactor; }
    public double getExternalWeightKg() { return externalWeightKg; }
    public double getTrainingVolumeKg() { return trainingVolumeKg; }
    public double getBodyWeightUsedKg() { return bodyWeightUsedKg; }

    @Override
    public String toString() {
        String typeStr = String.format("[%s]:", this.type);
        String repsAndSetsStr = String.format("%d sets x %d reps", this.setCount, this.repCount);
        String totalWeightStr = String.format("%.1fkg (%.1fkg body + %.1fkg ext.)", this.trainingVolumeKg, this.bodyWeightUsedKg, this.externalWeightKg);
        String durationStr = String.format("%d mins", this.durationMinutes);
        String caloriesBurnedStr = String.format("%.0f cal",  this.caloriesBurned);

        String str = String.format(" |   %11s %-24s  |   %-17s   |   %-37s   |   %-8s   |   %-8s   | %-10s  ",
            typeStr,
            this.name,
            repsAndSetsStr,
            totalWeightStr,
            durationStr,
            caloriesBurnedStr,
            this.date.isEqual(LocalDate.now()) ? "  Today" : this.date.toString());
        
        return str;
    }
}