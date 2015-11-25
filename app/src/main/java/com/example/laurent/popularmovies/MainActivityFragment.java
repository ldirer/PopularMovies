package com.example.laurent.popularmovies;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.example.laurent.popularmovies.data.MovieContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = this.getClass().getSimpleName();
    public ImageListAdapter mImageListAdapter;
    protected ProgressBar mProgress;
    public FetchMovieDataTask mFetchMoviesDataTask;

    private static final int MOVIE_LOADER = 0;

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry.COLUMN_ID,
            MovieContract.MovieEntry.COLUMN_IMAGE_URI,
    };
    static final int COL_MOVIE_ID = 0;
    static final int COL_MOVIE_IMAGE_URI = 1;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        getLoaderManager().initLoader(MOVIE_LOADER, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (null != mFetchMoviesDataTask) {
            // Prevents onPostExecute from being called with a 'loose' fragment (no activity attached)
            mFetchMoviesDataTask.cancel(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.fragment_main, container, false);
        GridView gridView = (GridView) rootView.findViewById(R.id.main_gridview);
        mProgress = (ProgressBar) rootView.findViewById(R.id.main_progressbar);

        mImageListAdapter = new ImageListAdapter(getActivity(), null, 0);
        gridView.setAdapter(mImageListAdapter);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());

        // We want to have an 'endless scrolling' effect.
        gridView.setOnScrollListener(new infiniteOnScrollListener(0));
        // Scrolling triggers data fetching.
        gridView.scrollTo(0, 0);

        if(null != mFetchMoviesDataTask) {
            // The task populates the adapter with image urls.
            mFetchMoviesDataTask = (FetchMovieDataTask) new FetchMovieDataTask(this).execute();
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem sortByItem = menu.findItem(R.id.spinner_sort_by);
        Spinner sortBySpinner = (Spinner) MenuItemCompat.getActionView(sortByItem);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this.getContext(), R.array.sort_by_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sortBySpinner.setAdapter(adapter);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        sortBySpinner.setSelection(sharedPrefs.getInt(getString(R.string.pref_sort_by_key), 0));

        sortBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Using 0 to specify permissions, static constants are deprecated. NOT IDEAL.
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();

                editor.putInt(getString(R.string.pref_sort_by_key), position);
                editor.apply();

                // This is fragile: we depend on the order of the resources arrays being consistent...
                String[] sortByValues = getResources().getStringArray(R.array.sort_by_values);
                onSortByChanged(sortByValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void onSortByChanged(String sortByTmdb) {
        // Trigger data reload.
        if (!getSortByPreference().equals(getString(R.string.sort_order_filter_favorites))) {
            new FetchMovieDataTask(this, sortByTmdb).execute();
        }
        getLoaderManager().restartLoader(MOVIE_LOADER, null, this);
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri moviesUri = MovieContract.MovieEntry.buildMoviesUri();
        String sortBy;
        String selection;

        Log.d(LOG_TAG, "onCreateLoader");

        String sortByPreference = getSortByPreference();
        if (sortByPreference.equals(getString(R.string.sort_order_filter_favorites))) {
            selection = MovieContract.MovieEntry.COLUMN_IS_FAVORITE + " = 1";
        } else if (sortByPreference.equals(getString(R.string.sort_order_popularity))) {
            // Don't sort here! It would give a different order compared to the (broken) tmdb api order.
            // ... And cause inconsistent order of movies: poor user experience.
            // Better have an approximate order by popularity as provided by the tmdb api.
//            sortBy = MovieContract.MovieEntry.COLUMN_POPULARITY + " DESC";
            selection = MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY + " = 1";
        } else if (sortByPreference.equals(getString(R.string.sort_order_rating))) {
//            sortBy = MovieContract.MovieEntry.COLUMN_RATING + " DESC";
            selection = MovieContract.MovieEntry.COLUMN_IN_LIST_RATING + " = 1";
        } else {
            Log.e(LOG_TAG, String.format("Unexpected sortByPreference value! Got: %s", sortByPreference));
            selection = MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY + " = 1";
        }

        sortBy = MovieContract.MovieEntry.COLUMN_INSERT_ORDER + " ASC";

        return new CursorLoader(
                getContext(),
                moviesUri,
                MOVIE_COLUMNS,
                selection,
                null,
                sortBy
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "onLoadFinished");
        mImageListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        Log.d(LOG_TAG, "onLoaderReset");
        mImageListAdapter.swapCursor(null);
    }

    protected String getSortByPreference() {
        String[] sortByValues = getResources().getStringArray(R.array.sort_by_values);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sortByValues[prefs.getInt(getString(R.string.pref_sort_by_key), 0)];
    }

    private class infiniteOnScrollListener implements AbsListView.OnScrollListener {
        // Good resource: https://github.com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews
        // If we have less items than the number we want in buffer, we need to fetch more data.
        private int bufferItemCount = 4;
        private int previousTotalItemCount = 0;
        private final String LOG_TAG = AbsListView.OnScrollListener.class.getSimpleName();

        public infiniteOnScrollListener(int previousTotalItemCount) {
            super();
            this.previousTotalItemCount = previousTotalItemCount;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }


        // This happens many times a second during a scroll, so we must be wary of the code placed here.
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            Log.v(LOG_TAG, String.format("firstVisibleItem: %d", firstVisibleItem));
            Log.v(LOG_TAG, String.format("VisibleItemCount: %d", visibleItemCount));
            Log.v(LOG_TAG, String.format("totalItemCount: %d", totalItemCount));
            Log.v(LOG_TAG, String.format("previousTotalItemCount: %d", this.previousTotalItemCount));
            Log.v(LOG_TAG, String.format("totalItemCount - (firstVisibleItem + visibleItemCount) <= this.bufferItemCount %b", totalItemCount - (firstVisibleItem + visibleItemCount) <= this.bufferItemCount));

            if (this.previousTotalItemCount > totalItemCount) {
                // That happens when we change the sort order: we need to reset the previous count.
                this.previousTotalItemCount = 0;
            }

            // If we don't have more items than last time, we're still loading smt.
            if (totalItemCount > this.previousTotalItemCount && visibleItemCount != 0 &&
                    totalItemCount - (firstVisibleItem + visibleItemCount) <= this.bufferItemCount) {
                this.previousTotalItemCount = totalItemCount;
                new FetchMovieDataTask(MainActivityFragment.this).execute();
            }
        }


    }
}
