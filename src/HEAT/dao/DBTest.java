package HEAT.dao;
import java.sql.Connection;

public class DBTest {

    public static void main(String[] args) {

        
        DatabaseManager db = DatabaseManager.getInstance();

        // Test connection
        try (Connection conn = db.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connected to shared SQLite DB");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}