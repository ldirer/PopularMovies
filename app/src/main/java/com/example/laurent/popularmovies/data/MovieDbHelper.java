package com.example.laurent.popularmovies.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.example.laurent.popularmovies.data.MovieContract.MovieEntry;
import static com.example.laurent.popularmovies.data.MovieContract.ReviewEntry;
import static com.example.laurent.popularmovies.data.MovieContract.TrailerEntry;

public class MovieDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "movies.db";
    public static final int DATABASE_VERSION = 1;
    private static final String LOG_TAG = MovieDbHelper.class.getSimpleName();

    public MovieDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String createMovieTable = "CREATE TABLE " + MovieEntry.TABLE_NAME + "(" +
                MovieEntry.COLUMN_ID + " INTEGER PRIMARY KEY NOT NULL, " +
                MovieEntry.COLUMN_INSERT_ORDER + " INTEGER UNIQUE NOT NULL, " +
                MovieEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_RATING + " DOUBLE NOT NULL, " +
                MovieEntry.COLUMN_RELEASE_DATE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_SYNOPSIS + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_IMAGE_URI + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_IMAGE_BACKDROP_URI + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_POPULARITY + " DOUBLE NOT NULL, " +
                // No boolean field in sqlite, we use an integer representation.
                MovieEntry.COLUMN_IN_LIST_POPULARITY + " INTEGER, " +
                MovieEntry.COLUMN_IN_LIST_RATING + " INTEGER, " +
                // A movie is not in favorites by default.
                MovieEntry.COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0" +
                ");";

        final String createReviewsTable = "CREATE TABLE " + ReviewEntry.TABLE_NAME + "(" +
                ReviewEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ReviewEntry.COLUMN_REVIEW_AUTHOR + " TEXT, " +
                ReviewEntry.COLUMN_REVIEW_BODY + " TEXT, " +
                ReviewEntry.COLUMN_MOVIE_KEY + " INTEGER NON NULL, " +
                " FOREIGN KEY (" + ReviewEntry.COLUMN_MOVIE_KEY + ") REFERENCES " +
                MovieEntry.TABLE_NAME + " (" + MovieEntry.COLUMN_ID + ")"
                + ");";


        final String createTrailersTable = "CREATE TABLE " + TrailerEntry.TABLE_NAME + "(" +
                TrailerEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TrailerEntry.COLUMN_TRAILER_NAME + " TEXT, " +
                TrailerEntry.COLUMN_TRAILER_URL + " TEXT, " +
                TrailerEntry.COLUMN_MOVIE_KEY + " INTEGER NON NULL, " +
                " FOREIGN KEY (" + TrailerEntry.COLUMN_MOVIE_KEY + ") REFERENCES " +
                MovieEntry.TABLE_NAME + " (" + MovieEntry.COLUMN_ID + ")"
                + ");";
        Log.v(LOG_TAG, createMovieTable);
        Log.v(LOG_TAG, createReviewsTable);
        Log.v(LOG_TAG, createTrailersTable);

        db.execSQL(createMovieTable);
        db.execSQL(createReviewsTable);
//        db.execSQL(createTrailersTable);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME + ", " + ReviewEntry.TABLE_NAME +
                ", " + TrailerEntry.TABLE_NAME + ";");
        onCreate(db);
    }
}
