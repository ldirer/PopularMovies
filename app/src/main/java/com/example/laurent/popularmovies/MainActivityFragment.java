package com.example.laurent.popularmovies;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;
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
import java.util.ArrayList;
import java.util.Vector;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private final String LOG_TAG = this.getClass().getSimpleName();
    public ImageListAdapter mImageListAdapter;
    public CursorAdapter mMovieDetailAdapter;

    private static final int MOVIE_LOADER = 0;

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry.COLUMN_ID,
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
    static final int COL_MOVIE_RELEASE_DATE = 1;
    static final int COL_MOVIE_IMAGE_URI = 2;
    static final int COL_MOVIE_SYNOPSIS = 3;
    static final int COL_MOVIE_TITLE = 4;
    static final int COL_MOVIE_RATING = 5;
    static final int COL_MOVIE_POPULARITY = 6;
    static final int COL_MOVIE_IN_LIST_POPULARITY = 7;
    static final int COL_MOVIE_IN_LIST_RATING = 8;



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
        // TODO: remove this, it's done in onItemSelected for the sort by button.
//        new FetchMovieDataTask().execute();
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

                // TODO: This is super fragile: we depend on the order of the resources arrays being consistent...
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
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri moviesUri = MovieContract.MovieEntry.buildMoviesUri();
        String sortBy;
        String selection;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        String[] sortByValues = getResources().getStringArray(R.array.sort_by_values);
        if (sortByValues[prefs.getInt(getString(R.string.pref_sort_by_key), 0)]
                .equals(getString(R.string.sort_order_popularity))) {
            sortBy = MovieContract.MovieEntry.COLUMN_POPULARITY + " DESC";
            selection = MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY + " = 1";
        }
        else {
            sortBy = MovieContract.MovieEntry.COLUMN_RATING + " DESC";
            selection = MovieContract.MovieEntry.COLUMN_IN_LIST_RATING + " = 1";
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
        mImageListAdapter.swapCursor(null);
    }

    public class FetchMovieDataTask extends AsyncTask<Void, Void, Vector<ContentValues>> {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private String baseUri = "https://api.themoviedb.org/3/discover/movie";


        // Parameters to get images from tmdb.
        private String size = "w500";
        private String baseImageUri;
        private String sortBy;

//        popularity.asc
//        popularity.desc
//        release_date.asc
//        release_date.desc
//        revenue.asc
//        revenue.desc
//        primary_release_date.asc
//        primary_release_date.desc
//        original_title.asc
//        original_title.desc
//        vote_average.asc
//        vote_average.desc
//        vote_count.asc
//        vote_count.desc

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

            Uri movieUri = Uri.parse(baseUri).buildUpon()
                    .appendQueryParameter("sort_by", sortBy)
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
                for (int i = 0; i < movies.toArray().length; i++) {
                    ContentValues movie = movies.get(i);
                    Uri insertedMovie = getContext().getContentResolver().insert(
                            MovieContract.MovieEntry.CONTENT_URI, movie);
                    Log.v(LOG_TAG, "Inserted movie uri: " + insertedMovie.toString());

                    // We update the fields to say in which list(s) the movie should show up.
                    ContentValues inListValue = new ContentValues();
                    if (this.sortBy.equals(getString(R.string.sort_order_popularity))){
                        inListValue.put(MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY, 1);
                        getContext().getContentResolver().update(insertedMovie, inListValue, null, null);
                    }
                    else {
                        inListValue.put(MovieContract.MovieEntry.COLUMN_IN_LIST_RATING, 1);
                        getContext().getContentResolver().update(insertedMovie, inListValue, null, null);
                    }
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
//            if (movieValues != null) {
//                mImageListAdapter.clear();
//            }
//            for (ContentValues movie : movieValues) {
//                Uri imageUri = Uri.parse(movie.getAsString(MovieContract.MovieEntry.COLUMN_IMAGE_URI));
//                mImageListAdapter.add(imageUri);
//            }
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
}
