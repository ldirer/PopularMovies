package com.example.laurent.popularmovies.data;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.example.laurent.popularmovies.data.MovieContract.*;

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
                MovieEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_RATING + " DOUBLE NOT NULL, " +
                MovieEntry.COLUMN_RELEASE_DATE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_SYNOPSIS + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_IMAGE_URI + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_POPULARITY + " DOUBLE NOT NULL, " +
                // No boolean field in sqlite, we use an integer representation.
                MovieEntry.COLUMN_IN_LIST_POPULARITY + " INTEGER, " +
                MovieEntry.COLUMN_IN_LIST_RATING + " INTEGER " +
                ");";

        Log.v(LOG_TAG, createMovieTable);

        db.execSQL(createMovieTable);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME + ";");
        onCreate(db);
    }
}
