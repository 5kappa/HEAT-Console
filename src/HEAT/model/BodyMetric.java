package heat.model;

import java.time.LocalDate;

public class BodyMetric {
    private int id;
    private int age;
    private double heightCm;
    private double weightKg;
    private double BMI;
    private LocalDate date;

    public BodyMetric(int age, double heightCm, double weightKg, double BMI, LocalDate date) {
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.BMI = BMI;
        this.date = date;
    }

    public BodyMetric(int id, int age, double heightCm, double weightKg, double BMI, LocalDate date) {
        this.id = id;
        this.age = age;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.BMI = BMI;
        this.date = date;
    }

    public int getId() { return id; }
    public int getAge() { return age; }
    public double getHeightCm() { return heightCm; }
    public double getWeightKg() { return weightKg; }
    public double getBMI() { return BMI; }
    public LocalDate getDate() { return date; }

    public void setId(int newId) { this.id = newId; }

    @Override
    public String toString() {
        String heightStr = String.format("%.1f cm", this.heightCm);
        String weightStr = String.format("%.1f kg", this.weightKg);

        String str = String.format(" |   %-32s   |   %-32s   |   %-28.2f   |   %s",
            heightStr,
            weightStr,
            this.BMI,
            this.date.isEqual(LocalDate.now()) ? "Today" : this.date.toString()
        );

        return str;
    }
}
