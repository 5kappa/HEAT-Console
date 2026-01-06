package HEAT.model;

import java.time.LocalDate;

public class User {
    private String name;
    private int age;
    private double heightCm;
    private double weightKg;
    private String sex = "";
    private double BMI;
    private double BMR;
    private int currentStreak;
    private LocalDate lastWorkoutDate;

    public User(String name, int age, double heightCm, double weightKg, String sex, double bmi, double bmr, int currentStreak, LocalDate lastWorkoutDate) {
        this.name = name;
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.sex = (sex == null) ? "" : sex;
        this.BMI = bmi;
        this.BMR = bmr;
        this.currentStreak = currentStreak;
        this.lastWorkoutDate = lastWorkoutDate;
    }

    public User(String name, int age, double heightCm, double weightKg, String sex, double bmi, double bmr) {
        this(name, age, heightCm, weightKg, sex, bmi, bmr, 0, null);
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    public double getHeightCm() { return heightCm; }
    public double getWeightKg() { return weightKg; }
    public String getSex() { return sex; }
    public double getBMI() { return BMI; }
    public double getBMR() { return BMR; }
    public int getCurrentStreak() { return currentStreak; }
    public LocalDate getLastWorkoutDate() { return lastWorkoutDate; }

    public void setAge(int newAge) { this.age = newAge; }
    public void setHeightCm(double newHeight) { this.heightCm = newHeight; }
    public void setWeightKg(double newWeight) { this.weightKg = newWeight; }
    public void setBMI(double newBMI) { this.BMI = newBMI; }
    public void setBMR(double newBMR) { this.BMR = newBMR; }
    public void setCurrentStreak(int newStreak) { this.currentStreak = newStreak; }
    public void setLastWorkoutDate(LocalDate newDate) { this.lastWorkoutDate = newDate; }
    
    @Override
    public String toString() {
        String profileDetails = String.format("""
                %37s%6s
                %37s%6s%12s%-10s%13s%-19s%12s%-15s%37s
                %37s%6s%12s%-10s%13s%-19s%12s%-15s%37s
                %37s%6s
                """, 
                "", "_O/   ",
                "", "  \\   ", "",
                String.format("Age  :  %d", age), "", 
                String.format("Height  :  %.1f cm", heightCm), "", 
                String.format("BMI  :  %.2f", BMI), "", 
                "", "  /\\_ ", "",
                String.format("Sex  :  %s", sex), "", 
                String.format("Weight  :  %.1f kg", weightKg), "", 
                String.format("BMR  :  %.2f", BMR), "", 
                "", "  \\   "
            );
        return profileDetails;
    }
}