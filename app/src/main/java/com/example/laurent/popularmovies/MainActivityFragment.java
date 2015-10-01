package com.example.laurent.popularmovies;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.Spinner;

import com.example.laurent.popularmovies.data.MovieContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private final String LOG_TAG = this.getClass().getSimpleName();
    public ImageListAdapter mImageListAdapter;

    private static final int MOVIE_LOADER = 0;

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry.COLUMN_ID,
            MovieContract.MovieEntry.COLUMN_INSERT_ORDER,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_IMAGE_URI,
            MovieContract.MovieEntry.COLUMN_SYNOPSIS,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_POPULARITY,
            MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY,
            MovieContract.MovieEntry.COLUMN_IN_LIST_RATING,
    };
    static final int COL_MOVIE_ID = 0;
    static final int COL_MOVIE_INSERT_ORDER = 1;
    static final int COL_MOVIE_RELEASE_DATE = 2;
    static final int COL_MOVIE_IMAGE_URI = 3;
    static final int COL_MOVIE_SYNOPSIS = 4;
    static final int COL_MOVIE_TITLE = 5;
    static final int COL_MOVIE_RATING = 6;
    static final int COL_MOVIE_POPULARITY = 7;
    static final int COL_MOVIE_IN_LIST_POPULARITY = 8;
    static final int COL_MOVIE_IN_LIST_RATING = 9;



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(MOVIE_LOADER, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        GridView gridView = (GridView)inflater.inflate(R.layout.fragment_main, container, false);

        mImageListAdapter = new ImageListAdapter(getActivity(), null, 0);
        // The task populates the adapter with image urls.
        gridView.setAdapter(mImageListAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    startActivity(new Intent(getActivity(), DetailActivity.class)
                            .setData(MovieContract.MovieEntry.buildMovieUri(cursor.getLong(COL_MOVIE_ID))));
                }
            }
        });

        // We want to have an 'endless scrolling' effect.
        gridView.setOnScrollListener(new infiniteOnScrollListener(0));
        gridView.scrollTo(0, 0);
        return gridView;
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

                // TODO: This is fragile: we depend on the order of the resources arrays being consistent...
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
        new FetchMovieDataTask(sortByTmdb).execute();
        getLoaderManager().restartLoader(MOVIE_LOADER, null, this);
        //TODO: reset scroll position to the top.
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri moviesUri = MovieContract.MovieEntry.buildMoviesUri();
        String sortBy = null;
        String selection;

        Log.d(LOG_TAG, "onCreateLoader");

        String sortByPreference = getSortByPreference();
        if (sortByPreference.equals(getString(R.string.sort_order_popularity))) {
            // Don't sort here! It would give a different order compared to the (broken) tmdb api order.
            // ... And cause inconsistent order of movies: poor user experience.
            // Better have an approximate order by popularity as provided by the tmdb api.
//            sortBy = MovieContract.MovieEntry.COLUMN_POPULARITY + " DESC";
            selection = MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY + " = 1";
        }
        else {
            sortBy = MovieContract.MovieEntry.COLUMN_RATING + " DESC";
            selection = MovieContract.MovieEntry.COLUMN_IN_LIST_RATING + " = 1";
        }
        if (sortBy == null) {
            sortBy = MovieContract.MovieEntry.COLUMN_INSERT_ORDER + " ASC";
        }
        else {
            sortBy += ", " + MovieContract.MovieEntry.COLUMN_INSERT_ORDER + " ASC";
        }

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

    public class FetchMovieDataTask extends AsyncTask<Void, Void, Vector<ContentValues>> {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private int page;
        private String baseUri = "https://api.themoviedb.org/3/discover/movie";

        final int TMDB_ITEMS_PER_PAGE = 20;

        // Parameters to get images from tmdb.
        private String size = "w185";
        private String baseImageUri;
        private String sortBy;

        public FetchMovieDataTask() {
            this.sortBy = getSortByPreference();
        }

        public FetchMovieDataTask(String sortBy) {
            this.sortBy = sortBy;
        }

        /**
         * Take the String representing the movies in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private Vector<ContentValues> getMovieDataFromJson(String moviesJsonStr)
                throws JSONException {
            final String TMDB_MOVIES_LIST = "results";
            final String TMDB_TITLE = "original_title";
            final String TMDB_POSTER_PATH = "poster_path";
            final String TMDB_SYNOPSIS = "overview";
            final String TMDB_RATING = "vote_average";
            final String TMDB_RELEASE_DATE = "release_date";
            final String TMDB_POPULARITY = "popularity";
            final String TMDB_ID = "id";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(TMDB_MOVIES_LIST);
            Vector<ContentValues> cVVector = new Vector<>(moviesArray.length());

            for (int i = 0; i < moviesArray.length(); i++) {

                JSONObject movie = moviesArray.getJSONObject(i);
                int movieId = movie.getInt(TMDB_ID);
                String title = movie.getString(TMDB_TITLE);
                String imagePath = movie.getString(TMDB_POSTER_PATH);
                String synopsis = movie.getString(TMDB_SYNOPSIS);
                String rating = movie.getString(TMDB_RATING);
                String releaseDate = movie.getString(TMDB_RELEASE_DATE);
                String popularity = movie.getString(TMDB_POPULARITY);

                ContentValues movieValues = new ContentValues();
                movieValues.put(MovieContract.MovieEntry.COLUMN_ID, movieId);
                movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
                movieValues.put(MovieContract.MovieEntry.COLUMN_IMAGE_URI,  getImageUriFromPath(imagePath));
                movieValues.put(MovieContract.MovieEntry.COLUMN_SYNOPSIS, synopsis);
                movieValues.put(MovieContract.MovieEntry.COLUMN_RATING, rating);
                movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
                movieValues.put(MovieContract.MovieEntry.COLUMN_POPULARITY, popularity);

                cVVector.add(movieValues);
            }
            return cVVector;
        }

        @Override
        protected Vector<ContentValues> doInBackground(Void... params) {

            if (baseImageUri == null) {
                baseImageUri = new FetchMovieConfiguration().getImageBaseUri();
            }

            long itemsInDatabase = mImageListAdapter.getCount();
            // We use floor instead of ceil in case we somehow did not load a page in its entirety.
            this.page = (int) (Math.floor((float)itemsInDatabase / TMDB_ITEMS_PER_PAGE) + 1);

            Log.d(LOG_TAG, String.format("Number of items already in database: %d", itemsInDatabase));
            Log.d(LOG_TAG, String.format("Page number we want to fetch: %d", this.page));

            Uri movieUri = Uri.parse(baseUri).buildUpon()
                    .appendQueryParameter("sort_by", this.sortBy)
                    .appendQueryParameter("page", String.valueOf(this.page))
                    .appendQueryParameter("api_key", MainActivity.API_KEY)
                    .build();

            Log.d(LOG_TAG, movieUri.toString());

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String moviesJsonStr = null;
            Vector<ContentValues> movies = null;
            try {
                URL url = new URL(movieUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();

                // Save to database
                movies = getMovieDataFromJson(moviesJsonStr);

                // We want to add an insert id so that user get a consistent order new movies are added.
                // For that we basically want an AUTOINCREMENT field, which sqlite does not provide for non primary key fields.
                Cursor maxCursor = getContext().getContentResolver().query(
                        MovieContract.MovieEntry.CONTENT_URI,
                        new String[]{String.format("MAX(%s) AS max", MovieContract.MovieEntry.COLUMN_INSERT_ORDER)},
                        null,
                        null,
                        null
                );
                maxCursor.moveToFirst();
                long maxInsertId = maxCursor.getLong(0);
                maxCursor.close();

                for (int i = 0; i < movies.toArray().length; i++) {
                    ContentValues movie = movies.get(i);
                    movie.put(MovieContract.MovieEntry.COLUMN_INSERT_ORDER, maxInsertId + 1 + i);
                    Uri insertedMovie = getContext().getContentResolver().insert(
                            MovieContract.MovieEntry.CONTENT_URI, movie);
                    Log.v(LOG_TAG, "Inserted movie uri: " + insertedMovie.toString());

                    // We update the fields to say in which list(s) the movie should show up.
                    ContentValues inListValue = new ContentValues();
                    if (this.sortBy.equals(getString(R.string.sort_order_popularity))){
                        inListValue.put(MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY, 1);
                    }
                    else {
                        inListValue.put(MovieContract.MovieEntry.COLUMN_IN_LIST_RATING, 1);
                    }
                    getContext().getContentResolver().update(insertedMovie, inListValue, null, null);
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return movies;
        }

        @Override
        protected void onPostExecute(Vector<ContentValues> movieValues) {
            super.onPostExecute(movieValues);
        }

        public String getImageUriFromPath(String posterHash) {
            posterHash = posterHash.replaceAll("^/", "");
            Uri posterUri = Uri.parse(baseImageUri).buildUpon()
                    .appendPath(size)
                    .appendEncodedPath(posterHash)
                    .build();
            Log.d(LOG_TAG, String.format("Poster Uri: %s", posterUri.toString()));
            return posterUri.toString();
        }
    }

    private String getSortByPreference() {
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


        // This happens many times a second during a scroll, so be wary of the code you place here.
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//            Log.v(LOG_TAG, String.format("firstVisibleItem: %d", firstVisibleItem));
//            Log.v(LOG_TAG, String.format("VisibleItemCount: %d", visibleItemCount));
//            Log.v(LOG_TAG, String.format("totalItemCount: %d", totalItemCount));
//            Log.v(LOG_TAG, String.format("previousTotalItemCount: %d", this.previousTotalItemCount));
//            Log.v(LOG_TAG, String.format("totalItemCount - (firstVisibleItem + visibleItemCount) <= this.bufferItemCount %b", totalItemCount - (firstVisibleItem + visibleItemCount) <= this.bufferItemCount));

            if(this.previousTotalItemCount > totalItemCount) {
                // That happens when we change the sort order: we need to reset the previous count.
                this.previousTotalItemCount = 0;
            }

            // If we don't have more items than last time, we're still loading smt.
            if (totalItemCount > this.previousTotalItemCount && visibleItemCount != 0 &&
                    totalItemCount - (firstVisibleItem + visibleItemCount) <= this.bufferItemCount) {
                this.previousTotalItemCount = totalItemCount;
                new FetchMovieDataTask().execute();
            }
        }


    }
}
