/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.laurent.popularmovies.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class MovieProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final String LOG_TAG = MovieProvider.class.getSimpleName();
    private MovieDbHelper mOpenHelper;


    static final int MOVIES = 100;
    static final int MOVIE = 101;
    static final int TOGGLE_FAVORITE = 102;
    static final int MOVIE_REVIEWS = 103;
    static final int MOVIE_TRAILERS = 104;

    private static final SQLiteQueryBuilder sMovieWithReviewsQueryBuilder;
    private static final SQLiteQueryBuilder sMovieWithTrailersQueryBuilder;
    static {
        sMovieWithReviewsQueryBuilder = new SQLiteQueryBuilder();
        sMovieWithReviewsQueryBuilder.setTables(
                MovieContract.MovieEntry.TABLE_NAME + " INNER JOIN " +
                        MovieContract.ReviewEntry.TABLE_NAME + " ON " +
                        MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry.COLUMN_ID + " = " +
                        MovieContract.ReviewEntry.TABLE_NAME + "." + MovieContract.ReviewEntry.COLUMN_MOVIE_KEY);
    }

    static {
        sMovieWithTrailersQueryBuilder = new SQLiteQueryBuilder();
        sMovieWithTrailersQueryBuilder.setTables(
                MovieContract.MovieEntry.TABLE_NAME + " INNER JOIN " +
                        MovieContract.TrailerEntry.TABLE_NAME + " ON " +
                        MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry.COLUMN_ID + " = " +
                        MovieContract.TrailerEntry.TABLE_NAME + "." + MovieContract.TrailerEntry.COLUMN_MOVIE_KEY);
    }

    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MovieContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, MovieContract.PATH_MOVIES, MOVIES);
        matcher.addURI(authority, MovieContract.PATH_MOVIES + "/*", MOVIE);
        matcher.addURI(authority, MovieContract.PATH_MOVIES + "/*/toggle_favorite", TOGGLE_FAVORITE);
        matcher.addURI(authority, MovieContract.PATH_MOVIES + "/*/reviews", MOVIE_REVIEWS);
        matcher.addURI(authority, MovieContract.PATH_MOVIES + "/*/trailers", MOVIE_TRAILERS);
        return matcher;
    }

    /*
        Students: We've coded this for you.  We just create a new WeatherDbHelper for later use
        here.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new MovieDbHelper(getContext());
        return true;
    }

    /*
        Students: Here's where you'll code the getType function that uses the UriMatcher.  You can
        test this by uncommenting testGetType in TestProvider.

     */
    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case MOVIES:
                return MovieContract.MovieEntry.CONTENT_TYPE;
            case MOVIE:
                return MovieContract.MovieEntry.CONTENT_ITEM_TYPE;
            case TOGGLE_FAVORITE:
                return MovieContract.MovieEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case MOVIE_REVIEWS: {
                retCursor = sMovieWithReviewsQueryBuilder.query(
                        mOpenHelper.getReadableDatabase(),
                        projection,
                        getSelectionWithIdFromUri(uri, selection),
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case MOVIE_TRAILERS: {
                retCursor = sMovieWithTrailersQueryBuilder.query(
                        mOpenHelper.getReadableDatabase(),
                        projection,
                        getSelectionWithIdFromUri(uri, selection),
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case MOVIES: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case MOVIE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        getSelectionWithIdFromUri(uri, selection),
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    /*
        Student: Add the ability to insert Locations to the implementation of this function.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case MOVIE_REVIEWS: {
                // Add the movieId foreign key from the uri into the values.
                long movieId = MovieContract.MovieEntry.getIdFromUri(uri);
                values.put(MovieContract.ReviewEntry.COLUMN_MOVIE_KEY, movieId);
                long _id = db.insert(MovieContract.ReviewEntry.TABLE_NAME, null, values);
                returnUri = MovieContract.ReviewEntry.buildReviewUri(_id);
                break;
            }
            case MOVIE_TRAILERS: {
                // Add the movieId foreign key from the uri into the values.
                long movieId = MovieContract.MovieEntry.getIdFromUri(uri);
                values.put(MovieContract.TrailerEntry.COLUMN_MOVIE_KEY, movieId);
                long _id = db.insert(MovieContract.TrailerEntry.TABLE_NAME, null, values);
                returnUri = MovieContract.TrailerEntry.buildTrailerUri(_id);
                break;
            }
            case MOVIES: {
                // I wanted to use the following, but the behavior about the returned id does not seem to match the documented one.
                // https://code.google.com/p/android/issues/detail?id=13045
                // long _id = db.insertWithOnConflict(MovieContract.MovieEntry.TABLE_NAME, null, values,
                // SQLiteDatabase.CONFLICT_IGNORE);

                // We first try to select a record with the given id. If it exists we don't do anything, otherwise we insert.
                // Basically doing db.insertWithOnConflict with CONFLICT_IGNORE should do.
                long _id;
                Cursor c = db.query(MovieContract.MovieEntry.TABLE_NAME,
                        new String[]{MovieContract.MovieEntry.COLUMN_ID},
                        MovieContract.MovieEntry.COLUMN_ID + " = " + values.getAsString(MovieContract.MovieEntry.COLUMN_ID),
                        null, null, null, null);
                if (c.moveToFirst()) {
                    assert c.getCount() == 1;
                    _id = c.getLong(0);
                }
                else {
                    _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, values);
                }
                if (_id > 0)
                    returnUri = MovieContract.MovieEntry.buildMovieUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if (null == selection) selection = "1";
        switch (match) {
            case MOVIES:
                rowsDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case MOVIE:
                selection = getSelectionWithIdFromUri(uri, selection);
                rowsDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }


    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case MOVIES:
                rowsUpdated = db.update(MovieContract.MovieEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case MOVIE:
                // We are updating a single row.
                selection = getSelectionWithIdFromUri(uri, selection);
                rowsUpdated = db.update(MovieContract.MovieEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                Log.v(LOG_TAG, String.format("Number of rows updated: %d", rowsUpdated));
                break;
            case TOGGLE_FAVORITE:
                Log.d(LOG_TAG, "Toggling favorite status...");
                selection = getSelectionWithIdFromUri(uri, selection);
                String toggleQuery = "UPDATE " + MovieContract.MovieEntry.TABLE_NAME +
                        " SET " + MovieContract.MovieEntry.COLUMN_IS_FAVORITE + " = (" +
                        MovieContract.MovieEntry.COLUMN_IS_FAVORITE + " + 1) % 2 " +
                        "WHERE " + selection;
                Log.d(LOG_TAG, "Toggling favorite query: " + toggleQuery);
                rowsUpdated = db.rawQuery(toggleQuery, null).getCount();
                Log.d(LOG_TAG, "RowsUpdated: " + rowsUpdated);
                // TODO: How can we do this query (a workaround would be a select followed by an update) AND get the count of updated rows?
                // For now we'll be using this great hack.
                rowsUpdated = 1;
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }


    private String getSelectionWithIdFromUri(Uri uri, String selection) {
        Log.d(LOG_TAG, "in getSelectionWithIdFromUri");
        long _id = MovieContract.MovieEntry.getIdFromUri(uri);
        String selectionWithId = String.format("%s.%s = %d",
                MovieContract.MovieEntry.TABLE_NAME,
                MovieContract.MovieEntry.COLUMN_ID,
                _id);
        if (selection != null && !selection.isEmpty()) {
            selectionWithId = selection + " AND " + selectionWithId;
        }
        return selectionWithId;
    }


    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
