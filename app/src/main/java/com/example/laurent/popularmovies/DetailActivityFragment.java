package com.example.laurent.popularmovies;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.laurent.popularmovies.data.MovieContract;

import org.w3c.dom.Text;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    // We need a unique id for each loader.
    public int DETAIL_LOADER = 0;

    private final static String[] DETAIL_COLUMNS = {
            MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry.COLUMN_ID,
            MovieContract.MovieEntry.COLUMN_IMAGE_URI,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_SYNOPSIS,
            MovieContract.MovieEntry.COLUMN_TITLE,
    };


    private static final int COL_MOVIE_ID = 0;
    private static final int COL_MOVIE_IMAGE_URI = 1;
    private static final int COL_MOVIE_RATING = 2;
    private static final int COL_MOVIE_RELEASE_DATE = 3;
    private static final int COL_MOVIE_SYNOPSIS = 4;
    private static final int COL_MOVIE_TITLE = 5;

    public DetailActivityFragment() {
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        Intent intent = getActivity().getIntent();
        Log.v(LOG_TAG, "Movie URI: " + intent.getDataString());

        // TODO: I did not find a light way to map uri movies/13412 to the right movie so it's done here.
        Uri uri = intent.getData();
        long movieId = MovieContract.MovieEntry.getIdFromUri(uri);

        String[] selectionArgs = new String[] {
                (String.format("%d", movieId))
        };

        return new CursorLoader(
                getActivity(),
                intent.getData(),
                DETAIL_COLUMNS,
                MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry.COLUMN_ID + " = ?",
                selectionArgs,
                null
                );
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        View view = getView();
        if (data.moveToFirst()) {
            ((TextView)view.findViewById(R.id.detail_id)).setText(String.format("id: %d", data.getInt(COL_MOVIE_ID)));
            ((TextView)view.findViewById(R.id.detail_rating)).setText(String.format("%.2f", data.getDouble(COL_MOVIE_RATING)));
            ((TextView)view.findViewById(R.id.detail_title)).setText(data.getString(COL_MOVIE_TITLE));
            ((TextView)view.findViewById(R.id.detail_release_date)).setText(data.getString(COL_MOVIE_RELEASE_DATE));
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
