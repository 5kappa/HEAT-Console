package heat.dao;

import java.sql.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import heat.model.*;

public class WorkoutDAO {

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ============================================================
    // Workouts (CRUD)
    // ============================================================

    public void saveStrengthWorkout(Workout workout) throws SQLException {
        StrengthWorkout sw = (StrengthWorkout) workout;
        String sql = """
            INSERT INTO workouts (
                exercise_name, type, date, duration_minutes, calories_burned, 
                sets, reps, weight_kg, volume_kg, bodyweight_factor
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, workout.getName());
            pstmt.setString(2, workout.getType());
            pstmt.setString(3, workout.getDate().toString());
            pstmt.setInt(4, workout.getDurationMinutes());
            pstmt.setDouble(5, workout.getCaloriesBurned());
            pstmt.setInt(6, sw.getSetCount());
            pstmt.setInt(7, sw.getRepCount());
            pstmt.setDouble(8, sw.getExternalWeightKg());
            pstmt.setDouble(9, sw.getTrainingVolumeKg());
            pstmt.setDouble(10, sw.getBodyWeightFactor());
            
            if (pstmt.executeUpdate() > 0) {
                System.out.println("\t\t\t\t\tWorkout saved to database!");
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) sw.setId(rs.getInt(1));
                }
            } else {
                System.out.println("\t\t\t\t\t[ ! ]   Warning: No workout was saved.");
            }
        }
    }

    public void saveCardioWorkout(Workout workout) throws SQLException {
        CardioWorkout cw = (CardioWorkout) workout;
        String sql = """
            INSERT INTO workouts (
                exercise_name, type, date, duration_minutes, calories_burned, distance_km
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
    
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, workout.getName());
            pstmt.setString(2, workout.getType());
            pstmt.setString(3, workout.getDate().toString());
            pstmt.setInt(4, workout.getDurationMinutes());
            pstmt.setDouble(5, workout.getCaloriesBurned());
            pstmt.setDouble(6, cw.getDistanceKm());
            
            if (pstmt.executeUpdate() > 0) {
                System.out.println("\t\t\t\t\tWorkout saved to database!");
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) cw.setId(rs.getInt(1));
                }
            } else {
                System.out.println("\t\t\t\t\t[ ! ]   Warning: No workout was saved.");
            }
        }
    }

    public void updateWorkout(Workout w) throws SQLException {
        String sql;
        if (w instanceof StrengthWorkout) {
            sql = """
                UPDATE workouts SET exercise_name=?, type=?, date=?, duration_minutes=?, calories_burned=?, 
                sets=?, reps=?, weight_kg=?, volume_kg=?, bodyweight_factor=? WHERE id=?
                """;
        } else {
            sql = """
                UPDATE workouts SET exercise_name=?, type=?, date=?, duration_minutes=?, calories_burned=?, 
                distance_km=? WHERE id=?
                """;
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, w.getName());
            pstmt.setString(2, w.getType());
            pstmt.setString(3, w.getDate().toString());
            pstmt.setInt(4, w.getDurationMinutes());
            pstmt.setDouble(5, w.getCaloriesBurned());

            if (w instanceof StrengthWorkout) {
                StrengthWorkout sw = (StrengthWorkout) w;
                pstmt.setInt(6, sw.getSetCount());
                pstmt.setInt(7, sw.getRepCount());
                pstmt.setDouble(8, sw.getExternalWeightKg());
                pstmt.setDouble(9, sw.getTrainingVolumeKg());
                pstmt.setDouble(10, sw.getBodyWeightFactor());
                pstmt.setInt(11, sw.getId());
            } else {
                CardioWorkout cw = (CardioWorkout) w;
                pstmt.setDouble(6, cw.getDistanceKm());
                pstmt.setInt(7, cw.getId());
            }
            
            if (pstmt.executeUpdate() > 0) {
                System.out.println("\t\t\t\t\tWorkout updated in database!");
            } else {
                System.out.println("\t\t\t\t\t[ ! ]   Warning: Workout update failed (ID not found).");
            }
        }
    }

    public void deleteWorkout(int id) throws SQLException {
        String sql = "DELETE FROM workouts WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<Workout> loadWorkouts() throws SQLException {
        List<Workout> workouts = new ArrayList<>();
        String sql = "SELECT * FROM workouts ORDER BY date DESC, id DESC";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String type = rs.getString("type");
                LocalDate date = LocalDate.parse(rs.getString("date"));

                if (type.equalsIgnoreCase("Strength")) {
                    workouts.add(new StrengthWorkout(rs.getInt("id"),
                        rs.getString("exercise_name"), type, date,
                        rs.getDouble("calories_burned"), rs.getInt("duration_minutes"),
                        rs.getInt("sets"), rs.getInt("reps"),
                        rs.getDouble("weight_kg"), rs.getDouble("volume_kg"),
                        rs.getDouble("bodyweight_factor")));
                } else {
                    workouts.add(new CardioWorkout(rs.getInt("id"),
                        rs.getString("exercise_name"), type, date,
                        rs.getDouble("calories_burned"), rs.getInt("duration_minutes"),
                        rs.getDouble("distance_km")));
                }
            }
        }
        return workouts;
    }

    // ============================================================
    // Personal Records
    // ============================================================

    public void updatePersonalRecord(String exerciseName, double weight, int reps, int duration, LocalDate date) throws SQLException {
        String updateSql = """
            UPDATE personal_records 
            SET weight_kg = ?, reps = ?, duration_minutes = ?, date = ?
            WHERE exercise_name = ?
            """;
            
        try (PreparedStatement updateStmt = getConnection().prepareStatement(updateSql)) {
            updateStmt.setDouble(1, weight);
            updateStmt.setInt(2, reps);
            updateStmt.setInt(3, duration);
            updateStmt.setString(4, date.toString());
            updateStmt.setString(5, exerciseName);

            if (updateStmt.executeUpdate() == 0) {
                insertNewRecord(exerciseName, weight, reps, duration, date);
            } else {
                System.out.println("\t\t\t\t\tPR updated in the database!");
            }
        }
    }
    
    private void insertNewRecord(String exerciseName, double weight, int reps, int duration, LocalDate date) throws SQLException {
        String insertSql = """
            INSERT INTO personal_records (exercise_name, weight_kg, reps, duration_minutes, date)
            VALUES (?, ?, ?, ?, ?)    
            """;

        try (PreparedStatement insertStmt = getConnection().prepareStatement(insertSql)) {
            insertStmt.setString(1, exerciseName);
            insertStmt.setDouble(2, weight);
            insertStmt.setInt(3, reps);
            insertStmt.setInt(4, duration);
            insertStmt.setString(5, date.toString());

            if (insertStmt.executeUpdate() > 0) {
                System.out.println("\t\t\t\t\tPR saved to database!");
            }
        }
    }

    public void deletePR(String activityName) throws SQLException {
        String sql = "DELETE FROM personal_records WHERE exercise_name = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, activityName);
            pstmt.executeUpdate();
        }
    }

    public void recalculatePR(String rawName, String PRName, String type) throws SQLException {
        deletePR(PRName);

        String sql;
        String unit = "kg";

        if (type.equalsIgnoreCase("Cardio")) {
            sql = "SELECT * FROM workouts WHERE exercise_name = ? AND type = 'Cardio' ORDER BY duration_minutes DESC, date DESC LIMIT 1";
            unit = "mins";
        } 
        else if (PRName.endsWith("(reps)")) {
            sql = "SELECT * FROM workouts WHERE exercise_name = ? AND type = 'Strength' AND weight_kg = 0 ORDER BY reps DESC, date DESC LIMIT 1";
            unit = "reps";
        } 
        else if (PRName.endsWith("(loaded)")) {
            sql = "SELECT * FROM workouts WHERE exercise_name = ? AND type = 'Strength' AND weight_kg > 0 ORDER BY weight_kg DESC, reps DESC, date DESC LIMIT 1";
        } 
        else {
            sql = "SELECT * FROM workouts WHERE exercise_name = ? AND type = 'Strength' ORDER BY weight_kg DESC, reps DESC, date DESC LIMIT 1";
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, rawName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double weight = rs.getDouble("weight_kg");
                    int reps = rs.getInt("reps");
                    int duration = rs.getInt("duration_minutes");
                    LocalDate date = LocalDate.parse(rs.getString("date"));

                    if (unit.equals("mins")) System.out.printf("\t\t\t\t\tRecalculated PR for %s: %d %s\n", PRName, duration, unit);
                    else if (unit.equals("reps")) System.out.printf("\t\t\t\t\tRecalculated PR for %s: %d %s\n", PRName, reps, unit);
                    else System.out.printf("\t\t\t\t\tRecalculated PR for %s: %.1f %s\n", PRName, weight, unit);

                    updatePersonalRecord(PRName, weight, reps, duration, date);
                } else {
                    System.out.println("\t\t\t\t\tNo history left for " + PRName + ". PR cleared.");
                }
            }
        }
    }

    public Map<String, PersonalRecord> loadPersonalRecords() throws SQLException {
        Map<String, PersonalRecord> records = new HashMap<>();
        String sql = "SELECT id, exercise_name, duration_minutes, reps, weight_kg, date FROM personal_records";
        
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.put(rs.getString("exercise_name"), new PersonalRecord(
                    rs.getString("exercise_name"), rs.getInt("duration_minutes"),
                    rs.getInt("reps"), rs.getDouble("weight_kg"),
                    LocalDate.parse(rs.getString("date"))));
            }
        }
        return records;
    }

    // ============================================================
    // Activities & Quotes (Resources)
    // ============================================================

    public void performInitialSetup() throws SQLException, IOException {
        if (isTableEmpty("activities")) {
            loadActivitiesFromFile();
        }
        if (isTableEmpty("quotes")) {
            loadQuotesFromFile();
        }
    }

    private boolean isTableEmpty(String tableName) throws SQLException {
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    public List<Activity> loadActivities() throws SQLException {
        List<Activity> activitiesList = new ArrayList<>();
        String sql = "SELECT * FROM activities";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                activitiesList.add(new Activity(rs.getInt("id"), rs.getString("activity_name"),
                    rs.getString("workout_type"), rs.getString("category"),
                    rs.getDouble("met_value"), rs.getDouble("bodyweight_factor")));
            }
        }
        return activitiesList;
    }

    public void loadActivitiesFromFile() throws SQLException, IOException {
        String sql = "INSERT INTO activities (activity_name, workout_type, category, met_value, bodyweight_factor) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("src/heat/resources/activities.csv"))) {
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    try {
                        pstmt.setString(1, parts[0].trim());
                        pstmt.setString(2, parts[1].trim());
                        pstmt.setString(3, parts[2].trim());
                        pstmt.setDouble(4, Double.parseDouble(parts[3].trim()));
                        pstmt.setDouble(5, Double.parseDouble(parts[4].trim()));
                        pstmt.executeUpdate();
                    } catch (NumberFormatException e) {
                        System.out.println("\t\t\t\t\tSkipping invalid activities line: " + line);
                    }
                }
            }
        }
    }

    public List<Quote> loadQuotes() throws SQLException {
        List<Quote> quotesList = new ArrayList<>();
        String sql = "SELECT level, quote FROM quotes";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                quotesList.add(new Quote(rs.getString("level"), rs.getString("quote")));
            }
        }
        return quotesList;
    }

    public void loadQuotesFromFile() throws SQLException, IOException {
        String sql = "INSERT INTO quotes (level, quote) VALUES (?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("src/heat/resources/quotes.csv"))) {
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    pstmt.setString(1, parts[0].trim());
                    pstmt.setString(2, parts[1].trim());
                    pstmt.executeUpdate();
                }
            }
        }
    }
}