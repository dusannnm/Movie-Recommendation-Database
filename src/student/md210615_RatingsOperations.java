package student;


import rs.ac.bg.etf.sab.operations.RatingsOperations;
import java.sql.*;
import java.util.*;


public class md210615_RatingsOperations implements RatingsOperations {


    // TR_BLOCK_EXTREME_Rating odbija ocenu sa THROW 50000 'User blocked ...'
    private boolean isBlockedSQLException(SQLException ex) {
        String msg = (ex.getMessage() == null ? "" : ex.getMessage()).toLowerCase();
        return ex.getErrorCode() == 50000 || msg.contains("blocked");
    }

    @Override
    public boolean addRating(Integer userId, Integer movieId, Integer score) {
        if (userId == null || movieId == null || score == null) return false;
        if (score < 1 || score > 10) return false;

        String existsSql = "SELECT 1 FROM Rating WHERE UserId = ? AND MovieId = ?";
        String insertSql = "INSERT INTO Rating (UserId, MovieId, Score) VALUES (?, ?, ?)";

        try (Connection conn = DB.getConnection()) {

            // 1️⃣ Proveri da li korisnik već ocenio film
            try (PreparedStatement psExists = conn.prepareStatement(existsSql)) {
                psExists.setInt(1, userId);
                psExists.setInt(2, movieId);
                try (ResultSet rs = psExists.executeQuery()) {
                    if (rs.next()) return false; // već postoji ocena
                }
            }

            // 2️⃣ Pokušaj da uneseš ocenu (može da baci grešku ako trigger blokira)
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psInsert.setInt(1, userId);
                psInsert.setInt(2, movieId);
                psInsert.setInt(3, score);
                psInsert.executeUpdate();

                // Ako je uspešno, pozovi reward proceduru
                try (CallableStatement cs = conn.prepareCall("EXEC SP_REWARD_USER_Add ?, ?")) {
                    cs.setInt(1, userId);
                    cs.setInt(2, movieId);
                    cs.execute();
                }

                return true;
            } catch (SQLException ex) {
                // trigger blokirao — ne logovati kao grešku
                if (!isBlockedSQLException(ex)) ex.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateRating(Integer userId, Integer movieId, Integer newScore) {
        if (userId == null || movieId == null || newScore == null) return false;
        if (newScore < 1 || newScore > 10) return false;

        String updateSql = "UPDATE Rating SET Score = ? WHERE UserId = ? AND MovieId = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {

            ps.setInt(1, newScore);
            ps.setInt(2, userId);
            ps.setInt(3, movieId);

            // nagrada se dodeljuje samo pri prvom ocenjivanju filma (addRating),
            // da korisnik ne bi dobijao novu nagradu svakom izmenom iste ocene
            try {
                return ps.executeUpdate() > 0;
            } catch (SQLException ex) {
                // trigger blokirao — ne logovati kao grešku
                if (!isBlockedSQLException(ex)) ex.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean removeRating(Integer userId, Integer movieId) {
        if (userId == null || movieId == null) return false;
        String query = "DELETE FROM Rating WHERE UserId=? AND MovieId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public Integer getRating(Integer userId, Integer movieId) {
        if (userId == null || movieId == null) return null;
        String query = "SELECT Score FROM Rating WHERE UserId=? AND MovieId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public List<Integer> getRatedMoviesByUser(Integer userId) {
        List<Integer> movies = new ArrayList<>();
        if (userId == null) return movies;
        String query = "SELECT MovieId FROM Rating WHERE UserId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) movies.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    @Override
    public List<Integer> getUsersWhoRatedMovie(Integer movieId) {
        List<Integer> users = new ArrayList<>();
        if(movieId == null) return users;
        String query = "SELECT UserId FROM Rating WHERE MovieId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
