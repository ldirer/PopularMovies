package com.example.laurent.popularmovies.data;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;


public class MovieDbHelperTest extends AndroidTestCase {

    public void testCreateDb() throws Exception {
        final HashSet<String> tableNameHashSet = new HashSet<>();
        tableNameHashSet.add(MovieContract.MovieEntry.TABLE_NAME);

        mContext.deleteDatabase(MovieDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new MovieDbHelper(this.mContext).getWritableDatabase();

        assertTrue(db.isOpen());

        // The database has been create correctly.
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // We created the tables we want.
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain all entry tables
        assertTrue("Error: Your database was created without all the required tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + MovieContract.MovieEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<>();
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_ID);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_IN_LIST_RATING);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_INSERT_ORDER);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_IMAGE_URI);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_POPULARITY);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_RATING);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_RELEASE_DATE);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_SYNOPSIS);
        locationColumnHashSet.add(MovieContract.MovieEntry.COLUMN_TITLE);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }
}