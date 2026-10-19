package student;


import rs.ac.bg.etf.sab.operations.TagsOperations;
import java.sql.*;
import java.util.*;


public class md210615_TagsOperations implements TagsOperations {


    @Override
    public Integer addTag(Integer movieId, String tag) {
        if (movieId == null || tag == null || tag.trim().isEmpty()) return null;

        String checkMovie = "SELECT 1 FROM Movie WHERE Id = ?";
        String checkTag = "SELECT Id FROM Tag WHERE Name = ?";
        String insertTag = "INSERT INTO Tag (Name) OUTPUT INSERTED.Id VALUES (?)";
        String checkExists = "SELECT 1 FROM MovieTag mt JOIN Tag t ON mt.TagId=t.Id WHERE mt.MovieId=? AND t.Name=?";
        String map = "INSERT INTO MovieTag (MovieId, TagId) VALUES (?, ?)";

        try (Connection conn = DB.getConnection()) {
            // provera da li film postoji
            try (PreparedStatement ps = conn.prepareStatement(checkMovie)) {
                ps.setInt(1, movieId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                }
            }

            // 2provera da li par (movieId, tag)  postoji
            try (PreparedStatement ps = conn.prepareStatement(checkExists)) {
                ps.setInt(1, movieId);
                ps.setString(2, tag);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return null;
                }
            }

            // dodavanje ili nalazenje taga
            Integer tagId = null;
            try (PreparedStatement ps = conn.prepareStatement(checkTag)) {
                ps.setString(1, tag);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) tagId = rs.getInt(1);
                }
            }

            if (tagId == null) {
                try (PreparedStatement ps = conn.prepareStatement(insertTag)) {
                    ps.setString(1, tag);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) tagId = rs.getInt(1);
                    }
                }
            }

            // povezivanje filma i taga
            try (PreparedStatement ps = conn.prepareStatement(map)) {
                ps.setInt(1, movieId);
                ps.setInt(2, tagId);
                ps.executeUpdate();
                return movieId; //
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public Integer removeTag(Integer movieId, String tag) {
        if (movieId == null || tag == null || tag.trim().isEmpty()) return null;
        String query = "DELETE FROM MovieTag WHERE MovieId=? AND TagId=(SELECT Id FROM Tag WHERE Name=?)";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            ps.setString(2, tag);
            int affected = ps.executeUpdate();
            return affected > 0 ? movieId : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public int removeAllTagsForMovie(Integer movieId) {
        if (movieId == null) return 0;
        String query = "DELETE FROM MovieTag WHERE MovieId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public boolean hasTag(Integer movieId, String tag) {
        if (movieId == null || tag == null || tag.trim().isEmpty()) return false;
        String query = "SELECT 1 FROM MovieTag mt JOIN Tag t ON mt.TagId=t.Id WHERE mt.MovieId=? AND t.Name=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            ps.setString(2, tag);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<String> getTagsForMovie(Integer movieId) {
        List<String> tags = new ArrayList<>();
        if(movieId == null) return tags;
        String query = "SELECT t.Name FROM MovieTag mt JOIN Tag t ON mt.TagId=t.Id WHERE mt.MovieId=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tags.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }


    @Override
    public List<Integer> getMovieIdsByTag(String tag) {
        List<Integer> movies = new ArrayList<>();
        if(tag == null || tag.trim().isEmpty()) return movies;
        String query = "SELECT mt.MovieId FROM MovieTag mt JOIN Tag t ON mt.TagId=t.Id WHERE t.Name=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, tag);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) movies.add(rs.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }


    @Override
    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();
        // samo tagovi koji su dodeljeni bar jednom filmu (red u Tag ostaje i kad se tag ukloni sa svih filmova)
        String query = "SELECT t.Name FROM Tag t WHERE EXISTS (SELECT 1 FROM MovieTag mt WHERE mt.TagId = t.Id)";
        try (Connection conn = DB.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) tags.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tags;
    }
}