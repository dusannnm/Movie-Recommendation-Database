package student;

import rs.ac.bg.etf.sab.operations.GeneralOperations;
import java.sql.*;


public class md210615_GeneralOperations implements GeneralOperations {
    @Override
    public void eraseAll() {
        try (Connection conn = DB.getConnection(); Statement st = conn.createStatement()) {
            //conn.setAutoCommit(false);

            st.executeUpdate("DELETE FROM Watchlist");
            st.executeUpdate("DELETE FROM Rating");
            st.executeUpdate("DELETE FROM MovieTag");
            st.executeUpdate("DELETE FROM MovieGenre");
            st.executeUpdate("DELETE FROM Tag");
            st.executeUpdate("DELETE FROM Movie");
            st.executeUpdate("DELETE FROM Genre");
            st.executeUpdate("DELETE FROM [User]");


            //conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
