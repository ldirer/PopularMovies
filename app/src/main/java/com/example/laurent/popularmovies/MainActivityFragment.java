package com.example.laurent.popularmovies;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.Vector;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private final String LOG_TAG = this.getClass().getSimpleName();
    public ImageListAdapter mImageListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        GridView gridView = (GridView)inflater.inflate(R.layout.fragment_main, container, false);
        mImageListAdapter = new ImageListAdapter(getActivity(), R.layout.grid_item, new ArrayList<Uri>());

        // The task populates the adapter with image urls.
        new FetchMovieDataTask().execute();
        gridView.setAdapter(mImageListAdapter);
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
                SharedPreferences.Editor editor = getActivity().getPreferences(0).edit();

                editor.putInt(getString(R.string.pref_sort_by_key), position);
                editor.apply();

                // TODO: This is super fragile: we depend on the order of the resources arrays being consistent...
                String[] sortByValues = getResources().getStringArray(R.array.sort_by_values);

                // Trigger data reload.
                new FetchMovieDataTask(sortByValues[position]).execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public class FetchMovieDataTask extends AsyncTask<Void, Void, Vector<ContentValues>> {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private String baseUri = "https://api.themoviedb.org/3/discover/movie";


        // Parameters to get images from tmdb.
        private String size = "w500";
        private String baseImageUri;
        private String sortBy = "popularity.desc";

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


        public FetchMovieDataTask() {
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

                ContentValues movieValues = new ContentValues();
                movieValues.put(MovieContract.MovieEntry.COLUMN_ID, movieId);
                movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
                movieValues.put(MovieContract.MovieEntry.COLUMN_IMAGE_URI,  getImageUriFromPath(imagePath));
                movieValues.put(MovieContract.MovieEntry.COLUMN_SYNOPSIS, synopsis);
                movieValues.put(MovieContract.MovieEntry.COLUMN_RATING, rating);
                movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);

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

                // TODO:Save to db?
                movies = getMovieDataFromJson(moviesJsonStr);

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
            if (movieValues != null) {
                mImageListAdapter.clear();
            }
            for (ContentValues movie : movieValues) {
                Uri imageUri = Uri.parse(movie.getAsString(MovieContract.MovieEntry.COLUMN_IMAGE_URI));
                mImageListAdapter.add(imageUri);
            }
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
