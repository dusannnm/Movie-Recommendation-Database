package student;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    private static final String SERVER = "localhost";
    private static final int PORT = 1433;
    private static final String DATABASE = "MovieDB";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "123";

    private static final String URL =
            "jdbc:sqlserver://" + SERVER + ":" + PORT +
                    ";databaseName=" + DATABASE +
                    ";encrypt=false;trustServerCertificate=true;";

    private DB() { }

    // svaki poziv otvara novu konekciju; pozivalac je zatvara (try-with-resources)
    public static Connection getConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
