package student;


import rs.ac.bg.etf.sab.operations.MoviesOperations;
import java.sql.*;
import java.util.*;


public class md210615_MoviesOperations implements MoviesOperations {


    @Override
    public Integer addMovie(String name, Integer genreId, String director) {
        if (name == null || genreId == null || director == null) return null;
        String checkGenre = "SELECT 1 FROM Genre WHERE Id = ?";
        String insertMovie = "INSERT INTO Movie (Title, Director) OUTPUT INSERTED.Id VALUES (?, ?)";
        String insertRelation = "INSERT INTO MovieGenre (MovieId, GenreId) VALUES (?, ?)";
        try (Connection conn = DB.getConnection()) {
            // provera da li postoji zanr
            try (PreparedStatement ps = conn.prepareStatement(checkGenre)) {
                ps.setInt(1, genreId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
            }

            // film i njegov zanr se dodaju u jednoj transakciji, da ne ostane film bez zanra
            conn.setAutoCommit(false);
            try {
                // ubacivanje filma i dohvatanje njegovog ID-ja
                Integer movieId = null;
                try (PreparedStatement ps = conn.prepareStatement(insertMovie)) {
                    ps.setString(1, name);
                    ps.setString(2, director);
                    ResultSet rs = ps.executeQuery(); // koristi executeQuery jer OUTPUT vraća ResultSet
                    if (rs.next()) movieId = rs.getInt(1);
                }

                if (movieId == null) {
                    conn.rollback();
                    return null;
                }

                // povezivanje filma sa žanrom
                try (PreparedStatement ps = conn.prepareStatement(insertRelation)) {
                    ps.setInt(1, movieId);
                    ps.setInt(2, genreId);
                    ps.executeUpdate();
                }

                conn.commit();
                return movieId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Integer updateMovieTitle(Integer id, String newTitle) {
        if (id == null || newTitle == null) return null;
        String query = "UPDATE Movie SET Title=? WHERE Id=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, newTitle);
            ps.setInt(2, id);
            int affected = ps.executeUpdate();
            return affected > 0 ? id : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Integer updateMovieDirector(Integer id, String newDirector) {
        if (id == null || newDirector == null) return null;
        String query = "UPDATE Movie SET Director=? WHERE Id=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, newDirector);
            ps.setInt(2, id);
            int affected = ps.executeUpdate();
            return affected > 0 ? id : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    // Ocene, liste za gledanje i tagove filma treba prvo obrisati njihovim metodama
    // (removeRating, removeMovieFromWatchlist, removeAllTagsForMovie); ako postoje, film se ne brise.
    // Veze sa zanrovima su deo samog filma i brisu se zajedno sa njim.
    @Override
    public Integer removeMovie(Integer id) {
        if (id == null) return null;
        String checkReferences =
                "SELECT 1 WHERE EXISTS (SELECT 1 FROM Rating WHERE MovieId = ?) " +
                "   OR EXISTS (SELECT 1 FROM Watchlist WHERE MovieId = ?) " +
                "   OR EXISTS (SELECT 1 FROM MovieTag WHERE MovieId = ?)";
        String deleteGenres = "DELETE FROM MovieGenre WHERE MovieId = ?";
        String deleteMovie = "DELETE FROM Movie WHERE Id = ?";
        try (Connection conn = DB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(checkReferences)) {
                    ps.setInt(1, id);
                    ps.setInt(2, id);
                    ps.setInt(3, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            return null;
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(deleteGenres)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                int affected;
                try (PreparedStatement ps = conn.prepareStatement(deleteMovie)) {
                    ps.setInt(1, id);
                    affected = ps.executeUpdate();
                }

                if (affected == 0) {
                    conn.rollback();
                    return null;
                }
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Integer addGenreToMovie(Integer movieId, Integer genreId) {
        if (movieId == null || genreId == null) return null;
        String checkMovie = "SELECT 1 FROM Movie WHERE Id = ?";
        String checkGenre = "SELECT 1 FROM Genre WHERE Id = ?";
        String checkExists = "SELECT 1 FROM MovieGenre WHERE MovieId = ? AND GenreId = ?";
        String insert = "INSERT INTO MovieGenre (MovieId, GenreId) VALUES (?, ?)";
        try (Connection conn = DB.getConnection()) {
            // provere
            try (PreparedStatement ps = conn.prepareStatement(checkMovie)) {
                ps.setInt(1, movieId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
            }
            try (PreparedStatement ps = conn.prepareStatement(checkGenre)) {
                ps.setInt(1, genreId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
            }
            try (PreparedStatement ps = conn.prepareStatement(checkExists)) {
                ps.setInt(1, movieId);
                ps.setInt(2, genreId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return null;
            }

            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, movieId);
                ps.setInt(2, genreId);
                ps.executeUpdate();
                return movieId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Integer removeGenreFromMovie(Integer movieId, Integer genreId) {
        if (movieId == null || genreId == null) return null;
        String query = "DELETE FROM MovieGenre WHERE MovieId=? AND GenreId=?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            ps.setInt(2, genreId);
            int affected = ps.executeUpdate();
            return affected > 0 ? movieId : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Integer> getAllMovieIds() {
        List<Integer> ids = new ArrayList<>();
        String query = "SELECT Id FROM Movie";
        try (Connection conn = DB.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }


    @Override
    public List<Integer> getMovieIds(String name, String director) {
        List<Integer> list = new ArrayList<>();
        if (name == null || director == null) return list;
        String query = "SELECT Id FROM Movie WHERE Title = ? AND Director = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, director);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    @Override
    public List<Integer> getMovieIdsByGenre(Integer genreId) {
        if(genreId==null) return null;
        List<Integer> ids = new ArrayList<>();
        String query = "SELECT DISTINCT MovieId FROM MovieGenre WHERE GenreId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, genreId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    @Override
    public List<Integer> getMovieIdsByDirector(String director) {
        if(director==null) return null;
        List<Integer> ids = new ArrayList<>();
        String query = "SELECT Id FROM Movie WHERE Director=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, director);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }


    @Override
    public List<Integer> getGenreIdsForMovie(Integer movieId) {
        List<Integer> ids = new ArrayList<>();
        if (movieId == null) return ids;
        String query = "SELECT GenreId FROM MovieGenre WHERE MovieId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }
}
