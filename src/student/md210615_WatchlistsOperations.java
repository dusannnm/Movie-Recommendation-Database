package student;

import rs.ac.bg.etf.sab.operations.WatchlistsOperations;
import java.sql.*;
import java.util.*;

public class md210615_WatchlistsOperations implements WatchlistsOperations {

    @Override
    public boolean addMovieToWatchlist(Integer userId, Integer movieId) {
        if (userId == null || movieId == null) return false;

        String checkUser = "SELECT 1 FROM [User] WHERE Id=?";
        String checkMovie = "SELECT 1 FROM Movie WHERE Id=?";
        String checkExists = "SELECT 1 FROM Watchlist WHERE UserId=? AND MovieId=?";
        String insert = "INSERT INTO Watchlist (UserId, MovieId) VALUES (?, ?)";

        try (Connection conn = DB.getConnection()) {
            // provera da li film i korisnik postoje
            try (PreparedStatement ps = conn.prepareStatement(checkUser)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(checkMovie)) {
                ps.setInt(1, movieId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }

            if (isMovieInWatchlist(userId, movieId)) return false;

            // dodavanje ako je sve ok
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, userId);
                ps.setInt(2, movieId);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean removeMovieFromWatchlist(Integer userId, Integer movieId) {
        if (userId == null || movieId == null) return false;
        String query = "DELETE FROM Watchlist WHERE UserId=? AND MovieId=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
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
    public boolean isMovieInWatchlist(Integer userId, Integer movieId) {
        if (userId == null || movieId == null) return false;
        String query = "SELECT 1 FROM Watchlist WHERE UserId=? AND MovieId=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Integer> getMoviesInWatchlist(Integer userId) {
        List<Integer> movies = new ArrayList<>();
        if (userId == null) return movies;
        String query = "SELECT MovieId FROM Watchlist WHERE UserId=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) movies.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    @Override
    public List<Integer> getUsersWithMovieInWatchlist(Integer movieId) {
        List<Integer> users = new ArrayList<>();
        if (movieId == null) return users;
        String query = "SELECT UserId FROM Watchlist WHERE MovieId=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}