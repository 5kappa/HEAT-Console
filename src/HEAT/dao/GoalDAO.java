package heat.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import heat.model.Goal;
import heat.model.GoalStatus;

public class GoalDAO {

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ============================================================
    // Goal Management (CRUD)
    // ============================================================

    public void addGoal(Goal g) throws SQLException {
        String sql = """
            INSERT INTO goals (goal_title, exercise_name, start_date, end_date, goal_type, current_value, target_value, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)    
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public void updateGoalStatus(int goalId, String newStatus) throws SQLException {
        String sql = "UPDATE goals SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql.toString())) {
            pstmt.setString(1, newStatus.name());
            for (int i = 0; i < goalIds.size(); i++) {
                pstmt.setInt(i + 2, goalIds.get(i));
            }
            pstmt.executeUpdate();
        }
    }

    public void updateGoalCurrentValue(int goalId, double newValue) throws SQLException {
        String sql = "UPDATE goals SET current_value = ? WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setDouble(1, newValue);
            pstmt.setInt(2, goalId);
            pstmt.executeUpdate();
        }
    }

    public List<Goal> loadGoals() throws SQLException {
        List<Goal> goals = new ArrayList<>();
        String sql = "SELECT * FROM goals ORDER BY id DESC";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
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

    // ============================================================
    // Data Queries (For Goal Verification)
    // ============================================================

    public double getMaxWeightLifted(String exerciseName, LocalDate startDate) throws SQLException {
        String sql = "SELECT MAX(weight_kg) FROM workouts WHERE exercise_name = ? AND date >= ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, exerciseName);
            pstmt.setString(2, startDate.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}