package com.example.laurent.popularmovies;

import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import com.example.laurent.popularmovies.data.MovieContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
    private ArrayAdapter<String> mMovieListAdapter;

    public MainActivityFragment() {
        // TODO: Remove this once it's clear WHY THE HELL IT DOES NOT WORK.
        try {
//            String filePath = new File("").getAbsolutePath();
//            File homedir = new File(System.getProperty("user.home"));
//            Log.e(LOG_TAG, filePath);
//            Log.e(LOG_TAG, homedir.getAbsolutePath());
//            File file = new File("tmdb_api_key.txt");
//            Log.e(LOG_TAG, file.getAbsolutePath());
            FileReader reader = new FileReader("/home/laurent/AndroidStudioProjects/PopularMovies/tmdb_api_key.txt");
//            FileReader reader = new FileReader("/tmdb_api_key");
//            API_KEY = reader.toString();
        }
        catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "No API key found for the movie database!!");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        GridView gridView = (GridView)inflater.inflate(R.layout.fragment_main, container, false);
        mImageListAdapter = new ImageListAdapter(getActivity(), R.layout.grid_item, new ArrayList<String>());
        // The task populates the adapter with image urls.
        new FetchMovieDataTask().execute();

        gridView.setAdapter(mImageListAdapter);
        return gridView;
    }


    public class FetchMovieDataTask extends AsyncTask<Void, Void, Vector<ContentValues>> {

        private final String LOG_TAG = this.getClass().getSimpleName();
        private String baseUri = "https://api.themoviedb.org/3/discover/movie";

        /**
         * Take the String representing the movies in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
    //    http://api.themoviedb.org/3/discover/movie
        private Vector<ContentValues> getMovieDataFromJson(String moviesJsonStr)
                throws JSONException {
            final String TMDB_MOVIES_LIST = "results";
            final String TMDB_TITLE = "title";
            final String TMDB_POSTER_PATH = "poster_path";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray moviesArray = moviesJson.getJSONArray(TMDB_MOVIES_LIST);

            // Insert the new movie information into the database
    //            Vector<ContentValues> cVVector = new Vector<ContentValues>(moviesArray.length());
            ArrayList<String> movies = new ArrayList<>();


            Vector<ContentValues> cVVector = new Vector<>(moviesArray.length());

            for (int i = 0; i < moviesArray.length(); i++) {
                // These are the values that will be collected.

                String title;
                String imagePath;
                int movieId;

                // Get the JSON object representing the day
                JSONObject movie = moviesArray.getJSONObject(i);
                title = movie.getString(TMDB_TITLE);
                imagePath = movie.getString(TMDB_POSTER_PATH);
                movies.add(title);
                movies.add(imagePath);
    //            new FetchMoviePosterTask().execute(imagePath);
                ContentValues movieValues = new ContentValues();
                movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
                movieValues.put(MovieContract.MovieEntry.COLUMN_IMAGE_PATH, imagePath);

                cVVector.add(movieValues);
            }
            return cVVector;
        }

        @Override
        protected Vector<ContentValues> doInBackground(Void... params) {
            Uri movieUri = Uri.parse(baseUri).buildUpon()
                    .appendQueryParameter("api_key", MainActivity.API_KEY)
                    .build();

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

                // TODO:
                movies = getMovieDataFromJson(moviesJsonStr);
    //            Save to db?


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
                mImageListAdapter.add(movie.getAsString(MovieContract.MovieEntry.COLUMN_IMAGE_PATH));
            }
        }
    }
}
