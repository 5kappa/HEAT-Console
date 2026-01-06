package heat.dao;

import java.sql.*;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
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

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void closeConnection() {
        try {
            if (this.connection != null) this.connection.close();
        } catch (SQLException e) {
            System.out.println("[ ! ] Error closing connection: " + e.getMessage());
        }
    }

    // Transaction Helpers
    public void beginTransaction() throws SQLException {
        if (connection == null || connection.isClosed()) connection = getConnection();
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

    // Table Initialization
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
}