import rs.ac.bg.etf.sab.operations.*;
import rs.ac.bg.etf.sab.tests.TestHandler;
import rs.ac.bg.etf.sab.tests.TestRunner;
import student.*;
import java.sql.*;

public class StudentMain {
    public static void main(String[] args) throws Exception {
        GeneralOperations generalOperations = new md210615_GeneralOperations();
        GenresOperations genresOperations = new md210615_GenresOperations();
        MoviesOperations moviesOperations = new md210615_MoviesOperations();
        RatingsOperations ratingsOperation = new md210615_RatingsOperations();
        TagsOperations tagsOperations = new md210615_TagsOperations();
        UsersOperations usersOperations = new md210615_UsersOperations();
        WatchlistsOperations watchlistsOperations = new md210615_WatchlistsOperations();

        Connection c = DB.getConnection();
        if (c != null)
            System.out.println("Connection established!");
        else
            System.out.println("Connection failed!");

        TestHandler.createInstance(
                genresOperations,
                moviesOperations,
                ratingsOperation,
                tagsOperations,
                usersOperations,
                watchlistsOperations,
                generalOperations);
        TestRunner.runTests();
    }
}
