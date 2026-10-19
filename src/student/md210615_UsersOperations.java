package student;

import rs.ac.bg.etf.sab.operations.UsersOperations;
import java.sql.*;
import java.util.*;

public class md210615_UsersOperations implements UsersOperations {

    @Override
    public Integer addUser(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        String check = "SELECT Id FROM [User] WHERE Username = ?";
        String insert = "INSERT INTO [User] (Username) VALUES (?)";

        try (Connection conn = DB.getConnection()) {
            // provera da li korisnik vec postoji
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return null;
                }
            }


            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Integer updateUser(Integer id, String newUsername) {
        if (id == null || newUsername == null || newUsername.trim().isEmpty()) return null;
        String check = "SELECT 1 FROM [User] WHERE Username = ? AND Id <> ?";
        String query = "UPDATE [User] SET Username=? WHERE Id=?";
        try (Connection conn = DB.getConnection()) {
            // korisnicko ime mora biti jedinstveno
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, newUsername);
                ps.setInt(2, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return null;
            }

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, newUsername);
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
    public Integer removeUser(Integer id) {
        if (id == null) return null;
        String query = "DELETE FROM [User] WHERE Id=?";
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
    public boolean doesUserExist(String username) {
        String query = "SELECT 1 FROM [User] WHERE Username=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public Integer getUserId(String username) {
        String query = "SELECT Id FROM [User] WHERE Username=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Integer> getAllUserIds() {
        List<Integer> ids = new ArrayList<>();
        String query = "SELECT Id FROM [User]";
        try (Connection conn = DB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }


    @Override
    public List<Integer> getRecommendedMoviesFromFavoriteGenres(Integer userId) {
        List<Integer> result = new ArrayList<>();
        if (userId == null) return result;

        // omiljeni zanrovi: prosecna ocena korisnika za filmove tog zanra >= 8
        // preporuka: (>= 4 ocene i prosek >= 7.5) ili "skriveni biser" (< 4 ocene i prosek >= 9),
        // bez filmova koje je korisnik vec ocenio ili ima u listi za gledanje
        String query =
                "WITH FavoriteGenres AS ( " +
                        "    SELECT mg.GenreId " +
                        "    FROM Rating r " +
                        "    JOIN MovieGenre mg ON r.MovieId = mg.MovieId " +
                        "    WHERE r.UserId = ? " +
                        "    GROUP BY mg.GenreId " +
                        "    HAVING AVG(CAST(r.Score AS DECIMAL(10,3))) >= 8 " +
                        "), MovieStats AS ( " +
                        "    SELECT MovieId, COUNT(*) AS Cnt, AVG(CAST(Score AS DECIMAL(10,3))) AS AvgScore " +
                        "    FROM Rating " +
                        "    GROUP BY MovieId " +
                        ") " +
                        "SELECT ms.MovieId " +
                        "FROM MovieStats ms " +
                        "WHERE EXISTS (SELECT 1 FROM MovieGenre mg JOIN FavoriteGenres fg ON fg.GenreId = mg.GenreId " +
                        "              WHERE mg.MovieId = ms.MovieId) " +
                        "  AND NOT EXISTS (SELECT 1 FROM Rating r WHERE r.UserId = ? AND r.MovieId = ms.MovieId) " +
                        "  AND NOT EXISTS (SELECT 1 FROM Watchlist w WHERE w.UserId = ? AND w.MovieId = ms.MovieId) " +
                        "  AND ((ms.Cnt >= 4 AND ms.AvgScore >= 7.5) OR (ms.Cnt < 4 AND ms.AvgScore >= 9)) " +
                        "ORDER BY ms.AvgScore DESC, ms.MovieId ASC";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }


    @Override
    public Integer getRewards(Integer userId) {
        if (userId == null) return null;
        String query = "SELECT RewardCount FROM [User] WHERE Id=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; //mozda return 0?
    }


    @Override
    public List<String> getThematicSpecializations(Integer userId) {
        List<String> tags = new ArrayList<>();
        if(userId == null) return tags;
        String query = "SELECT t.Name FROM Tag t JOIN MovieTag mt ON t.Id=mt.TagId JOIN Rating r ON r.MovieId=mt.MovieId WHERE r.UserId=? AND r.Score>=8 GROUP BY t.Name HAVING COUNT(DISTINCT r.MovieId)>=2";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tags.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }

    @Override
    public String getUserDescription(Integer userId) {
        if (userId == null) return null;
        String query = "SELECT dbo.fn_UserDescription(?)";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String result = rs.getString(1);
                if (result == null) return null;

                switch (result.trim().toLowerCase()) {
                    case "nedefinisan":
                        return "undefined";
                    case "fokusiran":
                        return "focused";
                    case "radoznao":
                        return "curious";
                    default:
                        return result; // ako u bazi već vrati engleski tekst
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}