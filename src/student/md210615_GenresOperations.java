package student;


import rs.ac.bg.etf.sab.operations.GenresOperations;
import java.sql.*;
import java.util.*;


public class md210615_GenresOperations implements GenresOperations {


    @Override
    public Integer addGenre(String name) {
        String check = "SELECT Id FROM Genre WHERE Name = ?";
        String insert = "INSERT INTO Genre (Name) VALUES (?)";
        try (Connection conn = DB.getConnection()) {
            // ako već postoji, vrati null (test tako očekuje)
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return null;
            }

            // ako ne postoji, dodaj ga
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Integer updateGenre(Integer id, String newName) {
        String check = "SELECT Id FROM Genre WHERE Name = ?";
        String query = "UPDATE Genre SET Name = ? WHERE Id = ?";
        try (Connection conn = DB.getConnection()) {

            // prvo proveri da li već postoji žanr sa tim imenom
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, newName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return null; // ne može update ako već postoji
            }

            // ako ne postoji, izvrši promenu
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, newName);
                ps.setInt(2, id);
                int affected = ps.executeUpdate();
                return affected > 0 ? id : null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Integer removeGenre(Integer id) {
        String query = "DELETE FROM Genre WHERE Id=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0 ? id : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public boolean doesGenreExist(String name) {
        String query = "SELECT 1 FROM Genre WHERE Name=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;


    }@Override
    public Integer getGenreId(String name) {
        String query = "SELECT Id FROM Genre WHERE Name=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public List<Integer> getAllGenreIds() {
        List<Integer> ids = new ArrayList<>();
        String query = "SELECT Id FROM Genre";
        try (Connection conn = DB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }
}