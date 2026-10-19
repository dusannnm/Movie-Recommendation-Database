CREATE DATABASE MovieDB;
GO
USE [MovieDB];
GO


-- TABLES


CREATE TABLE [User] (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(100) UNIQUE NOT NULL,
    RewardCount INT NOT NULL DEFAULT 0
    );
GO

CREATE TABLE Genre (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(100) UNIQUE NOT NULL
);
GO

CREATE TABLE Movie (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Title NVARCHAR(100) NOT NULL,
    Director NVARCHAR(100)
);
GO

CREATE TABLE MovieGenre (
    MovieId INT NOT NULL,
    GenreId INT NOT NULL,
    CONSTRAINT PK_MovieGenre PRIMARY KEY (MovieId, GenreId),
    CONSTRAINT FK_MovieGenre_Movie FOREIGN KEY (MovieId)
        REFERENCES Movie(Id)
        ON UPDATE CASCADE ON DELETE NO ACTION,
    CONSTRAINT FK_MovieGenre_Genre FOREIGN KEY (GenreId)
        REFERENCES Genre(Id)
        ON UPDATE CASCADE ON DELETE NO ACTION
);
GO

CREATE TABLE Tag (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(100) UNIQUE NOT NULL
);
GO

CREATE TABLE MovieTag (
    MovieId INT NOT NULL,
    TagId INT NOT NULL,
    CONSTRAINT PK_MovieTag PRIMARY KEY (MovieId, TagId),
    CONSTRAINT FK_MovieTag_Movie FOREIGN KEY (MovieId)
      REFERENCES Movie(Id)
      ON UPDATE CASCADE ON DELETE NO ACTION,
    CONSTRAINT FK_MovieTag_Tag FOREIGN KEY (TagId)
      REFERENCES Tag(Id)
      ON UPDATE CASCADE ON DELETE NO ACTION
);
GO

CREATE TABLE Watchlist (
    UserId INT NOT NULL,
    MovieId INT NOT NULL,
    AddedAt DATETIME DEFAULT GETDATE(),
    CONSTRAINT PK_Watchlist PRIMARY KEY (UserId, MovieId),
    CONSTRAINT FK_Watchlist_User FOREIGN KEY (UserId)
       REFERENCES [User](Id)
       ON UPDATE CASCADE ON DELETE NO ACTION,
    CONSTRAINT FK_Watchlist_Movie FOREIGN KEY (MovieId)
       REFERENCES Movie(Id)
       ON UPDATE CASCADE ON DELETE NO ACTION
);
GO

CREATE TABLE Rating (
    UserId INT NOT NULL,
    MovieId INT NOT NULL,
    Score INT CHECK (Score BETWEEN 1 AND 10),
    RatedAt DATETIME DEFAULT GETDATE(),
    CONSTRAINT PK_Rating PRIMARY KEY (UserId, MovieId),
    CONSTRAINT FK_Rating_User FOREIGN KEY (UserId)
        REFERENCES [User](Id)
        ON UPDATE CASCADE ON DELETE NO ACTION,
    CONSTRAINT FK_Rating_Movie FOREIGN KEY (MovieId)
        REFERENCES Movie(Id)
        ON UPDATE CASCADE ON DELETE NO ACTION
);
GO


-- TRIGGER - Block Extreme Ratings
-- Korisnik ne moze da da ekstremnu ocenu (1 ili 10) filmu iz zanra u kom vec ima
-- vise od tri ekstremne ocene, a manje od tri neutralne ocene (6, 7, 8).
-- Neutralne ocene i promena ekstremne ocene u neekstremnu su uvek dozvoljene.

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE OR ALTER TRIGGER TR_BLOCK_EXTREME_Rating
ON Rating
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN MovieGenre mg ON mg.MovieId = i.MovieId
        WHERE i.Score IN (1, 10)
          -- ekstremne ocene koje korisnik vec ima u tom zanru (bez ove ocene)
          AND (SELECT COUNT(*)
               FROM Rating r
               JOIN MovieGenre rmg ON rmg.MovieId = r.MovieId
               WHERE r.UserId = i.UserId
                 AND rmg.GenreId = mg.GenreId
                 AND r.MovieId <> i.MovieId
                 AND r.Score IN (1, 10)) > 3
          -- neutralne ocene korisnika u tom zanru
          AND (SELECT COUNT(*)
               FROM Rating r
               JOIN MovieGenre rmg ON rmg.MovieId = r.MovieId
               WHERE r.UserId = i.UserId
                 AND rmg.GenreId = mg.GenreId
                 AND r.Score BETWEEN 6 AND 8) < 3
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50000, 'User blocked for too many extreme ratings in this genre.', 1;
    END
END;
GO


-- STORED PROCEDURE - Reward User
-- Poziva se nakon sto korisnik oceni film. Korisnik dobija nagradu ako:
--   * je ocenio najmanje 10 filmova (ukljucujuci ovaj),
--   * film pripada bar jednom omiljenom zanru korisnika (prosek korisnika za taj zanr >= 8),
--   * globalni prosek filma bez ocene ovog korisnika postoji i manji je od 6.

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE OR ALTER PROCEDURE SP_REWARD_USER_Add
    @UserId INT,
    @MovieId INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @UserRatedCount INT, @GlobalAvg DECIMAL(10,3);

    SELECT @UserRatedCount = COUNT(*) FROM Rating WHERE UserId = @UserId;
    IF @UserRatedCount < 10 RETURN;

    SELECT @GlobalAvg = AVG(CAST(Score AS DECIMAL(10,3)))
    FROM Rating
    WHERE MovieId = @MovieId AND UserId <> @UserId;

    -- ako niko drugi nije ocenio film, globalni prosek ne postoji -> nema nagrade
    IF @GlobalAvg IS NULL OR @GlobalAvg >= 6 RETURN;

    IF EXISTS (
        SELECT 1
        FROM MovieGenre mg
        JOIN MovieGenre umg ON umg.GenreId = mg.GenreId
        JOIN Rating r ON r.MovieId = umg.MovieId AND r.UserId = @UserId
        WHERE mg.MovieId = @MovieId
        GROUP BY mg.GenreId
        HAVING AVG(CAST(r.Score AS DECIMAL(10,3))) >= 8
    )
        UPDATE [User] SET RewardCount = RewardCount + 1 WHERE Id = @UserId;
END;
GO


-- FUNCTION - User Description

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE OR ALTER FUNCTION fn_UserDescription(@UserId INT)
RETURNS NVARCHAR(20)
AS
BEGIN
    DECLARE @MovieCount INT, @TagCount INT;

    SELECT @MovieCount = COUNT(DISTINCT r.MovieId)
    FROM Rating r
    WHERE r.UserId = @UserId;

    SELECT @TagCount = COUNT(DISTINCT mt.TagId)
    FROM Rating r
    JOIN MovieTag mt ON mt.MovieId = r.MovieId
    WHERE r.UserId = @UserId;

    IF @MovieCount < 10 RETURN 'nedefinisan';
    IF @TagCount >= 10 RETURN 'radoznao';
    RETURN 'fokusiran';
END;
GO


-- STORED PROCEDURE - Recommended Movies From Favorite Genres
-- Omiljeni zanrovi: prosecna ocena korisnika za filmove tog zanra >= 8.
-- Preporucuju se filmovi iz tih zanrova koje korisnik nije ocenio i nema u listi za gledanje:
--   * najmanje 4 ocene i prosek >= 7.5, ili
--   * "skriveni biseri": manje od 4 ocene i prosek >= 9.
-- Redosled: prosek opadajuce, pa Id rastuce.

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE OR ALTER PROCEDURE GetRecommendedMoviesFromFavoriteGenres
    @UserId INT
AS
BEGIN
    SET NOCOUNT ON;

    WITH FavoriteGenres AS (
        SELECT mg.GenreId
        FROM Rating r
        JOIN MovieGenre mg ON r.MovieId = mg.MovieId
        WHERE r.UserId = @UserId
        GROUP BY mg.GenreId
        HAVING AVG(CAST(r.Score AS DECIMAL(10,3))) >= 8
    ), MovieStats AS (
        SELECT MovieId, COUNT(*) AS Cnt, AVG(CAST(Score AS DECIMAL(10,3))) AS AvgScore
        FROM Rating
        GROUP BY MovieId
    )
    SELECT ms.MovieId
    FROM MovieStats ms
    WHERE EXISTS (SELECT 1 FROM MovieGenre mg JOIN FavoriteGenres fg ON fg.GenreId = mg.GenreId
                  WHERE mg.MovieId = ms.MovieId)
      AND NOT EXISTS (SELECT 1 FROM Rating r WHERE r.UserId = @UserId AND r.MovieId = ms.MovieId)
      AND NOT EXISTS (SELECT 1 FROM Watchlist w WHERE w.UserId = @UserId AND w.MovieId = ms.MovieId)
      AND ((ms.Cnt >= 4 AND ms.AvgScore >= 7.5) OR (ms.Cnt < 4 AND ms.AvgScore >= 9))
    ORDER BY ms.AvgScore DESC, ms.MovieId ASC;
END;
GO
