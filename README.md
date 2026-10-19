# Movie Ratings Database

Project for the course **Database Software Tools (srb. SAB - Softverski alati baza podataka)**,
School of Electrical Engineering, University of Belgrade, 2024/25.

A system for movies and the users who rate them. Movies have a title, a director, one or more genres and
any number of tags; users keep watchlists and rate movies from 1 to 10. On top of that data the system
blocks users who keep giving extreme ratings, rewards users who rate poorly rated movies in their favorite
genres, recommends movies, and describes each user by what they rate. The business rules live in the
database (a trigger, stored procedures and a function); the Java classes implement the interfaces that the
course's test suite calls.

## Technologies

| Part | Stack |
|---|---|
| Database | Microsoft SQL Server 2022 (`MovieDB`), T-SQL |
| Application | Java, plain JDBC (`java.sql`) |
| Driver | Microsoft JDBC Driver for SQL Server 13.2.1 (`mssql-jdbc-13.2.1.jre11.jar`, `.jre8.jar` for Java 8) |
| Tests | `SAB_projekat_2025_javni_test.jar`: the course's interfaces and public tests (JUnit 4) |

## Requirements

- **JDK 11 or newer** (checked with JDK 21); with Java 8 use the `jre8` driver instead
- **SQL Server 2022** (Developer or Express) on `localhost:1433`, with SQL Server authentication enabled
- `sqlcmd` or SQL Server Management Studio, to create the database

## Setting up the database

Create `MovieDB` in one of two ways.

**From the script** (works on any recent SQL Server version):

```
sqlcmd -S localhost -U sa -P <password> -C -i md210615.sql
```

**From the backup**: restore `md210615.bak` in SSMS (*Databases → Restore Database…*). The backup was made
on SQL Server 2022 and can't be restored on an older version; use the script there.

`md210615-tsql.sql` contains only the trigger, the procedures and the function, written as
`CREATE OR ALTER`. Run it against an existing `MovieDB` to update those objects without touching the tables
or the data.

The connection settings (server, port, database, user and password) are constants at the top of
[src/student/DB.java](src/student/DB.java); change them to match your server.

## Running the tests

The tests call `eraseAll()` between cases, which **deletes every row** in `MovieDB`. Don't run them against
a database whose data you want to keep.

### From the command line

From the project root:

```
javac -encoding UTF-8 -d out -cp SAB_projekat_2025_javni_test.jar src/StudentMain.java src/student/*.java
java -cp "out;SAB_projekat_2025_javni_test.jar;mssql-jdbc-13.2.1.jre11.jar" StudentMain
```

On Linux or macOS, separate the classpath entries with `:` instead of `;`.

`StudentMain` registers the implementation classes with the test handler and runs the public tests. The
expected output ends with:

```
Points won on public tests is: 15.0 out of 15
```

### From VS Code

With the **Extension Pack for Java** installed, open the project folder, then open `src/StudentMain.java`
and click **Run** above `main`. [.vscode/settings.json](.vscode/settings.json) sets `src/` as the source
folder, `out/` as the output folder and adds both JARs to the classpath, since VS Code only picks up JARs
from a `lib/` folder by default. If the imports from `rs.ac.bg.etf.sab` still show as errors, run
**Java: Clean Java Language Server Workspace** from the command palette.

## Database

| Table | Content |
|---|---|
| `User` | username (unique) and the number of rewards |
| `Movie` | title and director |
| `Genre` | name (unique) |
| `Tag` | name (unique) |
| `MovieGenre`, `MovieTag` | which genres and tags a movie has |
| `Watchlist` | the movies a user wants to watch |
| `Rating` | one score from 1 to 10 per user and movie |

Every foreign key is `ON UPDATE CASCADE ON DELETE NO ACTION`, every `Id` that isn't a foreign key is an
`IDENTITY` column, and text columns are `NVARCHAR(100)`. The model is in [md210615.pdf](md210615.pdf).

| Object | Rule |
|---|---|
| `TR_BLOCK_EXTREME_Rating` | Rejects a rating of 1 or 10 if, in any genre of that movie, the user already has more than three extreme ratings and fewer than three neutral ones (6, 7, 8). Neutral ratings, and changing an extreme rating into a non-extreme one, are always allowed. |
| `SP_REWARD_USER_Add` | Called after a new rating. Gives a reward when the user has rated at least 10 movies, the movie belongs to one of their favorite genres (their average there is at least 8), and the movie's average from other users is below 6. |
| `GetRecommendedMoviesFromFavoriteGenres` | From the user's favorite genres, the movies they haven't rated and don't have in their watchlist, with at least 4 ratings and an average of at least 7.5, or "hidden gems" with fewer than 4 ratings and an average of at least 9. Ordered by average (highest first), then by id. |
| `fn_UserDescription` | `radoznao` (curious) for 10+ rated movies covering 10+ tags, `fokusiran` (focused) for 10+ movies with fewer tags, `nedefinisan` (undefined) for fewer than 10 movies. |

Thematic specializations (tags that appear at least twice among the movies a user rated 8 or higher) are a
single query in `md210615_UsersOperations`.

## Structure

```
src/
  StudentMain.java                  creates the implementations and runs the public tests
  student/
    DB.java                         connection settings; every call opens a new connection
    md210615_GeneralOperations.java eraseAll
    md210615_GenresOperations.java  one class per interface from the test jar
    md210615_MoviesOperations.java
    md210615_RatingsOperations.java
    md210615_TagsOperations.java
    md210615_UsersOperations.java
    md210615_WatchlistsOperations.java
md210615.sql                        creates MovieDB: tables, trigger, procedures, function
md210615-tsql.sql                   only the trigger, procedures and function (CREATE OR ALTER)
md210615.bak                        backup of MovieDB (SQL Server 2022)
md210615.pdf                        database model
SAB_projekat_2025_javni_test.jar    interfaces and public tests from the course
mssql-jdbc-13.2.1.jre*.jar          JDBC driver
.vscode/settings.json               source folder and JARs for running from VS Code
```

<!-- ## Design decisions worth knowing

- **Deleting follows the assignment's rule.** A method that deletes a row doesn't delete the rows that
  reference it. `removeMovie` returns `null` while the movie still has ratings, watchlist entries or tags;
  remove those first with `removeRating`, `removeMovieFromWatchlist` and `removeAllTagsForMovie`. Genre
  links are part of the movie itself and are deleted with it. `removeGenre` and `removeUser` fail the same
  way while something still references them.
- **Rewards are given only when a rating is first added.** `updateRating` doesn't call
  `SP_REWARD_USER_Add`, so editing the same rating can't earn a second reward.
- **No reward without other ratings.** If nobody else has rated the movie, there is no average to compare
  with, so the user gets no reward.
- **A blocked rating returns `false`.** The trigger rolls back and throws error 50000; `addRating` and
  `updateRating` catch it and report failure without printing a stack trace.
- **`addMovie` and `removeMovie` run in a transaction**, so a movie is never left without a genre or half
  deleted.
- **`getAllTags` lists only tags still attached to a movie.** A tag's row stays in `Tag` after it is removed
  from every movie, but it isn't listed.
- **`getUserDescription` returns English text** (`curious`, `focused`, `undefined`), which is what the tests
  expect; the database function returns the Serbian words.

## Worth mentioning

Comments and the database function's results are in Serbian; table, column and method names are in
English. Class names carry the `md210615_` prefix required by the assignment. -->

## Author

Dušan Milivojević, 2024/25.
