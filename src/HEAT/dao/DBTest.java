package heat.dao;
import java.sql.Connection;

public class DBTest {

    public static void main(String[] args) {

        
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();

        // Test connection
        try (Connection conn = dbConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connected to shared SQLite DB");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}