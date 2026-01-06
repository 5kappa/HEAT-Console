package heat.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import heat.model.User;
import heat.model.BodyMetric;

public class UserDAO {

    private static final int DEFAULT_USER_ID = 1;
    private int currentUserId = DEFAULT_USER_ID; 

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ============================================================
    // User Profile
    // ============================================================

    public void saveUserProfile(User u) throws SQLException {
        String sql = ""; 
        int rowCount = 0;

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_profile")) {
                
            if (rs.next()) rowCount = rs.getInt(1);
        }

        if (rowCount == 0) {
            sql = "INSERT INTO user_profile (name, age, height_cm, weight_kg, sex, BMI, BMR, current_streak, last_workout_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            this.currentUserId = DEFAULT_USER_ID; 
            sql = "UPDATE user_profile SET name = ?, age = ?, height_cm = ?, weight_kg = ?, sex = ?, BMI = ?, BMR = ? WHERE id = " + DEFAULT_USER_ID;
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    public void updateUserProfile(User u) throws SQLException {
        String sql = "UPDATE user_profile SET name = ?, sex = ?, age = ?, height_cm = ?, weight_kg = ?, BMI = ?, BMR = ?, current_streak = ?, last_workout_date = ? WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

    public User loadUserProfile() throws SQLException {
        String sql = "SELECT * FROM user_profile";
        try (Statement stmt = getConnection().createStatement();
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

    // ============================================================
    // Body Metrics
    // ============================================================

    public void insertNewBodyMetric(BodyMetric bm) throws SQLException {
        String insertSql = "INSERT INTO body_metrics (age, height_cm, weight_kg, BMI, date) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement insertStmt = getConnection().prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
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
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<BodyMetric> loadBodyMetrics() throws SQLException {
        List<BodyMetric> bodyMetrics = new ArrayList<>();
        String sql = "SELECT id, age, height_cm, weight_kg, BMI, date FROM body_metrics ORDER BY date DESC, id DESC";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                bodyMetrics.add(new BodyMetric(rs.getInt("id"), rs.getInt("age"),
                    rs.getDouble("height_cm"), rs.getDouble("weight_kg"),
                    rs.getDouble("BMI"), LocalDate.parse(rs.getString("date"))));
            }
        }
        return bodyMetrics;
    }
}