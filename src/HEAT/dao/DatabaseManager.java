package HEAT.dao; 

import java.sql.*;
import java.util.*;
import HEAT.model.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DatabaseManager {

    private static final int DEFAULT_USER_ID = 1;
    private static DatabaseManager instance;

    private int currentUserId = DEFAULT_USER_ID; 
    private Connection connection;

    // ============================================================
    // Connection Management
    // ============================================================

    private DatabaseManager() {
        final String URL = "jdbc:sqlite:data/HEATDatabase.db";
        try {
            connection = DriverManager.getConnection(URL);
            initializeTables();
            System.out.println("Database initialized successfully.\n");
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void closeConnection() {
        try {
            if (this.connection != null) {
                this.connection.close();
            }
        } catch (SQLException e) {
            System.out.println("\t\t\t\t\t[ ! ]   Error closing connection: " + e.getMessage());
        }
    }

    public void beginTransaction() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = getConnection();
        }
        connection.setAutoCommit(false);
    }
    
    public void commitTransaction() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.commit();
            connection.setAutoCommit(true);
        }
    }
    
    public void rollbackTransaction() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.rollback();
            connection.setAutoCommit(true);
        }
    }

    // ============================================================
    // Database Initialization (Tables)
    // ============================================================

    private void initializeTables() throws SQLException {
        String createWorkoutTable = """
            CREATE TABLE IF NOT EXISTS workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_name TEXT NOT NULL,
                type TEXT NOT NULL,
                date DATE DEFAULT CURRENT_DATE,
                duration_minutes INTEGER,
                calories_burned REAL,
                distance_km REAL,
                sets INTEGER,
                reps INTEGER,
                weight_kg REAL,
                volume_kg REAL,
                bodyweight_factor REAL
            )
            """;
            
        String createPRTable = """
            CREATE TABLE IF NOT EXISTS personal_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_name TEXT UNIQUE NOT NULL,
                duration_minutes INTEGER,
                reps INTEGER,
                weight_kg REAL,
                date DATE DEFAULT CURRENT_DATE
            )
            """;
        
        String createBodyMetricsTable = """
            CREATE TABLE IF NOT EXISTS body_metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                age INTEGER,
                height_cm REAL NOT NULL,
                weight_kg REAL NOT NULL,
                BMI REAL NOT NULL,
                date DATE DEFAULT CURRENT_DATE
            )
            """;
        
        String createUserProfileTable = """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                age INTEGER,
                height_cm REAL NOT NULL,
                weight_kg REAL NOT NULL,
                sex TEXT NOT NULL,
                BMI REAL NOT NULL,
                BMR REAL NOT NULL,
                current_streak INTEGER,
                last_workout_date DATE
            )    
            """;

        String createGoalsTable = """
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_title TEXT NOT NULL,
                exercise_name TEXT,
                start_date DATE NOT NULL,
                end_date DATE,
                goal_type TEXT NOT NULL,
                current_value DOUBLE NOT NULL,
                target_value DOUBLE NOT NULL,
                status TEXT NOT NULL
            )    
            """;

        String createActivitiesTable = """
            CREATE TABLE IF NOT EXISTS activities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                activity_name TEXT NOT NULL,
                workout_type TEXT NOT NULL,
                category TEXT NOT NULL,
                met_value DOUBLE NOT NULL,
                bodyweight_factor DOUBLE NOT NULL
            )    
            """;
        
        String createQuotesTable = """
            CREATE TABLE IF NOT EXISTS quotes (
                id INTEGER PRIMARY KEY,
                level TEXT NOT NULL,
                quote TEXT NOT NULL
            )    
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createWorkoutTable);
            stmt.executeUpdate(createPRTable);
            stmt.executeUpdate(createBodyMetricsTable);
            stmt.executeUpdate(createUserProfileTable);
            stmt.executeUpdate(createGoalsTable);
            stmt.executeUpdate(createActivitiesTable);
            stmt.executeUpdate(createQuotesTable);
        }
    }

    public void performInitialSetup() throws SQLException, IOException {
        if (isTableEmpty("activities")) {
            loadActivitiesFromFile();
        }
        if (isTableEmpty("quotes")) {
            loadQuotesFromFile();
        }
    }

    private boolean isTableEmpty(String tableName) throws SQLException {
        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    // ============================================================
    // Workouts & PRs
    // ============================================================

    public void saveStrengthWorkout(Workout workout) throws SQLException {
        StrengthWorkout sw = (StrengthWorkout)workout;
        String sql = """
            INSERT INTO workouts (
                exercise_name, type, date, duration_minutes, calories_burned, 
                sets, reps, weight_kg, volume_kg, bodyweight_factor
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                System.out.println("\t\t\t\t\t[ ! ]   Workout saved to database!");
                updateUserStreak(currentUserId);
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) sw.setId(rs.getInt(1));
                }
            } else {
                System.out.println("\t\t\t\t\t[ ! ]   Warning: No workout was saved.");
            }
        }
    }

    public void saveCardioWorkout(Workout workout) throws SQLException {
        CardioWorkout cw = (CardioWorkout)workout;
        String sql = """
            INSERT INTO workouts (
                exercise_name, type, date, duration_minutes, calories_burned, distance_km
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
    
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, workout.getName());
            pstmt.setString(2, workout.getType());
            pstmt.setString(3, workout.getDate().toString());
            pstmt.setInt(4, workout.getDurationMinutes());
            pstmt.setDouble(5, workout.getCaloriesBurned());
            pstmt.setDouble(6, cw.getDistanceKm());
            
            if (pstmt.executeUpdate() > 0) {
                System.out.println("\t\t\t\t\tWorkout saved to database!");
                updateUserStreak(currentUserId);
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

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // PR Operations
    public void updatePersonalRecord(String exerciseName, double weight, int reps, int duration, LocalDate date) throws SQLException {
        String updateSql = """
            UPDATE personal_records 
            SET weight_kg = ?, reps = ?, duration_minutes = ?, date = ?
            WHERE exercise_name = ?
            """;
            
        try (PreparedStatement updateStmt = this.connection.prepareStatement(updateSql)) {
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

        try (PreparedStatement insertStmt = this.connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, exerciseName);
            insertStmt.setDouble(2, weight);
            insertStmt.setInt(3, reps);
            insertStmt.setInt(4, duration);
            insertStmt.setString(5, date.toString());

            if (insertStmt.executeUpdate() > 0) {
                System.out.println("\t\t\t\t\tPR saved to database!");
                updateUserStreak(currentUserId);
            }
        }
    }

    public void deletePR(String activityName) throws SQLException {
        String sql = "DELETE FROM personal_records WHERE exercise_name = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
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

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
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

    // ============================================================
    // User Profile & Body Metrics
    // ============================================================

    public void saveUserProfile(User u) throws SQLException {
        String sql = ""; 
        int rowCount = 0;

        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_profile")) {
                
            if (rs.next()) rowCount = rs.getInt(1);

            if (rowCount == 0) {
                sql = "INSERT INTO user_profile (name, age, height_cm, weight_kg, sex, BMI, BMR, current_streak, last_workout_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                this.currentUserId = DEFAULT_USER_ID; 
                sql = "UPDATE user_profile SET name = ?, age = ?, height_cm = ?, weight_kg = ?, sex = ?, BMI = ?, BMR = ? WHERE id = " + DEFAULT_USER_ID;
            }

            try (PreparedStatement pstmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, u.getName());
                pstmt.setInt(2, u.getAge());
                pstmt.setDouble(3, u.getHeightCm());
                pstmt.setDouble(4, u.getWeightKg());
                pstmt.setString(5, u.getSex());
                pstmt.setDouble(6, u.getBMI());
                pstmt.setDouble(7, u.getBMR());

                if (rowCount == 0) {
                    pstmt.setInt(8, 0);
                    pstmt.setNull(9, java.sql.Types.DATE);
                }

                pstmt.executeUpdate();

                if (rowCount == 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) this.currentUserId = generatedKeys.getInt(1);
                    }
                }
            }
        }
    }

    public void updateUserProfile(User u) throws SQLException {
        String sql = "UPDATE user_profile SET name = ?, sex = ?, age = ?, height_cm = ?, weight_kg = ?, BMI = ?, BMR = ?, current_streak = ?, last_workout_date = ? WHERE id = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, u.getName());
            pstmt.setString(2, u.getSex());
            pstmt.setInt(3, u.getAge());
            pstmt.setDouble(4, u.getHeightCm());
            pstmt.setDouble(5, u.getWeightKg());
            pstmt.setDouble(6, u.getBMI());
            pstmt.setDouble(7, u.getBMR());
            pstmt.setInt(8, u.getCurrentStreak());
            
            if (u.getLastWorkoutDate() != null) {
                pstmt.setString(9, u.getLastWorkoutDate().toString());
            } else {
                pstmt.setNull(9, java.sql.Types.DATE);
            }

            pstmt.setInt(10, this.currentUserId);
            pstmt.executeUpdate();
            System.out.println("\t\t\t\t\tUser profile updated in database."); 
        }
    }

    public void insertNewBodyMetric(BodyMetric bm) throws SQLException {
        String insertSql = "INSERT INTO body_metrics (age, height_cm, weight_kg, BMI, date) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement insertStmt = this.connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setInt(1, bm.getAge());
            insertStmt.setDouble(2, bm.getHeightCm());
            insertStmt.setDouble(3, bm.getWeightKg());
            insertStmt.setDouble(4, bm.getBMI());
            insertStmt.setString(5, bm.getDate().toString());

            if (insertStmt.executeUpdate() > 0) {
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) bm.setId(rs.getInt(1));
                }
            } else {
                System.out.println("\t\t\t\t\t[ ! ]   Warning: No body metric was saved.");
            }
        }
    }

    public void updateBodyMetric(BodyMetric bm) throws SQLException {
        String sql = "UPDATE body_metrics SET weight_kg = ?, height_cm = ?, age = ?, BMI = ?, date = ? WHERE id = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setDouble(1, bm.getWeightKg());
            pstmt.setDouble(2, bm.getHeightCm());
            pstmt.setInt(3, bm.getAge());
            pstmt.setDouble(4, bm.getBMI());
            pstmt.setString(5, bm.getDate().toString());
            pstmt.setInt(6, bm.getId());

            if (pstmt.executeUpdate() > 0) System.out.println("\t\t\t\t\tBody metric updated in database.");
        }
    }

    public void deleteBodyMetric(int id) throws SQLException {
        String sql = "DELETE FROM body_metrics WHERE id = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private void updateUserStreak(int userId) throws SQLException {
        String sql = "SELECT current_streak, last_workout_date FROM user_profile WHERE id = ?";
        String updateSql = "UPDATE user_profile SET current_streak = ?, last_workout_date = ? WHERE id = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String lastDateStr = rs.getString("last_workout_date");
                    LocalDate lastDate = null;

                    if (lastDateStr != null && !lastDateStr.isBlank()) {
                        try {
                            lastDate = LocalDate.parse(lastDateStr);
                        } catch (Exception e) {
                            System.out.println("\t\t\t\t\t[ ! ]   Warning: Could not parse date: " + lastDateStr);
                        }
                    }

                    int newStreak = 1;
                    if (lastDate != null) {
                        long daysSinceLastWorkout = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
                        if (daysSinceLastWorkout == 0) return;
                        if (daysSinceLastWorkout == 1) newStreak = rs.getInt("current_streak") + 1;
                    }

                    try (PreparedStatement updateStmt = this.connection.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, newStreak);
                        updateStmt.setString(2, LocalDate.now().toString()); 
                        updateStmt.setInt(3, userId);
                        updateStmt.executeUpdate();
                    }
                }
            }
        }
    }

    // ============================================================
    // Goals
    // ============================================================

    public void addGoal(Goal g) throws SQLException {
        String sql = """
            INSERT INTO goals (goal_title, exercise_name, start_date, end_date, goal_type, current_value, target_value, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)    
            """;

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, g.getGoalTitle());
            pstmt.setString(2, g.getExerciseName());
            pstmt.setString(3, g.getStartDate().toString());

            if (g.getEndDate() != null) pstmt.setString(4, g.getEndDate().toString());
            else pstmt.setNull(4, java.sql.Types.DATE);

            pstmt.setString(5, g.getGoalType());
            pstmt.setDouble(6, g.getCurrentValue());
            pstmt.setDouble(7, g.getTargetValue());
            pstmt.setString(8, g.getStatus().toString());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) g.setId(rs.getInt(1));
            }
        }
    }

    public void updateGoal(Goal g) throws SQLException {
        String sql = "UPDATE goals SET goal_title=?, end_date=?, target_value=?, status=? WHERE id=?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, g.getGoalTitle());

            if (g.getEndDate() != null) pstmt.setString(2, g.getEndDate().toString());
            else pstmt.setNull(2, java.sql.Types.DATE);

            pstmt.setDouble(3, g.getTargetValue());
            pstmt.setString(4, g.getStatus().name());
            pstmt.setInt(5, g.getId());

            if (pstmt.executeUpdate() > 0) System.out.println("\t\t\t\t\tGoal updated in database.");
        }
    }

    public void deleteGoal(int id) throws SQLException {
        String sql = "DELETE FROM goals WHERE id = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public void updateGoalStatus(int goalId, String newStatus) throws SQLException {
        String sql = "UPDATE goals SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, goalId);
            pstmt.executeUpdate();
        }
    }

    public void updateGoalStatusBatch(List<Integer> goalIds, GoalStatus newStatus) throws SQLException {
        if (goalIds.isEmpty()) return;

        StringBuilder sql = new StringBuilder("UPDATE goals SET status = ? WHERE id IN (");
        for (int i = 0; i < goalIds.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql.toString())) {
            pstmt.setString(1, newStatus.name());
            for (int i = 0; i < goalIds.size(); i++) {
                pstmt.setInt(i + 2, goalIds.get(i));
            }
            pstmt.executeUpdate();
        }
    }

    public void updateGoalCurrentValue(int goalId, double newValue) throws SQLException {
        String sql = "UPDATE goals SET current_value = ? WHERE id = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setDouble(1, newValue);
            pstmt.setInt(2, goalId);
            pstmt.executeUpdate();
        }
    }

    // ============================================================
    // Data Queries
    // ============================================================

    public double getMaxWeightLifted(String exerciseName, LocalDate startDate) throws SQLException {
        String sql = "SELECT MAX(weight_kg) FROM workouts WHERE exercise_name = ? AND date >= ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, exerciseName);
            pstmt.setString(2, startDate.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    public int getMostRepsDone(String exerciseName, LocalDate startDate) throws SQLException {
        String sql = "SELECT MAX(reps) FROM workouts WHERE exercise_name = ? AND date >= ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, exerciseName);
            pstmt.setString(2, startDate.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getTotalMinutes(String exerciseName, LocalDate startDate) throws SQLException {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM workouts WHERE exercise_name = ? AND date >= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, exerciseName);
            pstmt.setString(2, startDate.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getWorkoutFrequency(String exerciseName, LocalDate startDate) throws SQLException {
        String sql = "SELECT COUNT(exercise_name) FROM workouts WHERE exercise_name = ? AND date >= ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, exerciseName);
            pstmt.setString(2, startDate.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // ============================================================
    // Loading Data (Startup)
    // ============================================================

    public List<Workout> loadWorkouts() throws SQLException {
        List<Workout> workouts = new ArrayList<>();
        String sql = "SELECT * FROM workouts ORDER BY date DESC, id DESC";

        try (Statement stmt = this.connection.createStatement();
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

    public Map<String, PersonalRecord> loadPersonalRecords() throws SQLException {
        Map<String, PersonalRecord> records = new HashMap<>();
        String sql = "SELECT id, exercise_name, duration_minutes, reps, weight_kg, date FROM personal_records";
        
        try (Statement stmt = this.connection.createStatement();
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

    public List<BodyMetric> loadBodyMetrics() throws SQLException {
        List<BodyMetric> bodyMetrics = new ArrayList<>();
        String sql = "SELECT id, age, height_cm, weight_kg, BMI, date FROM body_metrics ORDER BY date DESC, id DESC";

        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                bodyMetrics.add(new BodyMetric(rs.getInt("id"), rs.getInt("age"),
                    rs.getDouble("height_cm"), rs.getDouble("weight_kg"),
                    rs.getDouble("BMI"), LocalDate.parse(rs.getString("date"))));
            }
        }
        return bodyMetrics;
    }

    public User loadUserProfile() throws SQLException {
        String sql = "SELECT * FROM user_profile";
        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next() && rs.getString("name") != null) {
                String dateStr = rs.getString("last_workout_date");
                return new User(rs.getString("name"), rs.getInt("age"),
                    rs.getDouble("height_cm"), rs.getDouble("weight_kg"),
                    rs.getString("sex"), rs.getDouble("BMI"), rs.getDouble("BMR"),
                    rs.getInt("current_streak"), (dateStr != null) ? LocalDate.parse(dateStr) : null);
            }
        }
        return null;
    }

    public List<Goal> loadGoals() throws SQLException {
        List<Goal> goals = new ArrayList<>();
        String sql = "SELECT * FROM goals ORDER BY id DESC";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
           while (rs.next()) {
                LocalDate endDate = (rs.getString("end_date") != null) ? LocalDate.parse(rs.getString("end_date")) : null;
                goals.add(new Goal(rs.getInt("id"), rs.getString("goal_title"), rs.getString("exercise_name"),
                    LocalDate.parse(rs.getString("start_date")), endDate, rs.getString("goal_type"),
                    rs.getDouble("current_value"), rs.getDouble("target_value"),
                    GoalStatus.valueOf(rs.getString("status"))));
           }
        }
        return goals;
    }    

    public List<Quote> loadQuotes() throws SQLException {
        List<Quote> quotesList = new ArrayList<>();
        String sql = "SELECT level, quote FROM quotes";
        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                quotesList.add(new Quote(rs.getString("level"), rs.getString("quote")));
            }
        }
        return quotesList;
    }

    public List<Activity> loadActivities() throws SQLException {
        List<Activity> activitiesList = new ArrayList<>();
        String sql = "SELECT * FROM activities";
        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                activitiesList.add(new Activity(rs.getInt("id"), rs.getString("activity_name"),
                    rs.getString("workout_type"), rs.getString("category"),
                    rs.getDouble("met_value"), rs.getDouble("bodyweight_factor")));
            }
        }
        return activitiesList;
    }

    // ============================================================
    // File I/O (CSV Import)
    // ============================================================

    public void loadActivitiesFromFile() throws SQLException, IOException {
        String sql = "INSERT INTO activities (activity_name, workout_type, category, met_value, bodyweight_factor) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("src/HEAT/resources/activities.csv"))) {
            
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

    public void loadQuotesFromFile() throws SQLException, IOException {
        String sql = "INSERT INTO quotes (level, quote) VALUES (?, ?)";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader("src/HEAT/resources/quotes.csv"))) {
            
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