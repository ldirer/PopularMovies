package com.example.laurent.popularmovies;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

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

public class FetchMovieDataTask extends AsyncTask<Void, Integer, Vector<ContentValues>> {

    private MainActivityFragment fragment;
    private final String LOG_TAG = this.getClass().getSimpleName();
    private int page;
    private String baseUri = "https://api.themoviedb.org/3/discover/movie";

    final int TMDB_ITEMS_PER_PAGE = 20;

    // Parameters to get images from tmdb.
    private String size = "w185";
    private String baseImageUri;
    private String sortBy;

    public FetchMovieDataTask(MainActivityFragment fragment) {
        this.fragment = fragment;
        this.sortBy = fragment.getSortByPreference();
    }

    public FetchMovieDataTask(MainActivityFragment fragment, String sortBy) {
        this.fragment = fragment;
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
            movieValues.put(MovieContract.MovieEntry.COLUMN_IMAGE_URI, getImageUriFromPath(imagePath));
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
        // We tell our progress bar/spinning wheel to show up.
        publishProgress(0);

        if (baseImageUri == null) {
            baseImageUri = new FetchMovieConfiguration().getImageBaseUri();
        }

        long itemsInDatabase = fragment.mImageListAdapter.getCount();
        // We use floor instead of ceil in case we somehow did not load a page in its entirety.
        this.page = (int) (Math.floor((float) itemsInDatabase / TMDB_ITEMS_PER_PAGE) + 1);

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
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            moviesJsonStr = buffer.toString();

            // Save to database
            movies = getMovieDataFromJson(moviesJsonStr);

            // We want to add an insert id so that user get a consistent order as new movies are added.
            // For that we basically want an AUTOINCREMENT field, which sqlite does not provide for non primary key fields.
            long maxInsertId;
            Cursor maxCursor = fragment.getContext().getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    new String[]{String.format("MAX(%s) AS max", MovieContract.MovieEntry.COLUMN_INSERT_ORDER)},
                    null,
                    null,
                    null
            );
            if (maxCursor != null) {
                maxCursor.moveToFirst();
                maxInsertId = maxCursor.getLong(0);
                maxCursor.close();
            } else {
                maxInsertId = 0;
            }

            for (int i = 0; i < movies.toArray().length; i++) {
                ContentValues movie = movies.get(i);
                movie.put(MovieContract.MovieEntry.COLUMN_INSERT_ORDER, maxInsertId + 1 + i);
                Uri insertedMovie = fragment.getContext().getContentResolver().insert(
                        MovieContract.MovieEntry.CONTENT_URI, movie);
                Log.v(LOG_TAG, "Inserted movie uri: " + insertedMovie.toString());

                // We update the fields to say in which list(s) the movie should show up.
                ContentValues inListValue = new ContentValues();
                if (this.sortBy.equals(fragment.getString(R.string.sort_order_popularity))) {
                    inListValue.put(MovieContract.MovieEntry.COLUMN_IN_LIST_POPULARITY, 1);
                } else {
                    inListValue.put(MovieContract.MovieEntry.COLUMN_IN_LIST_RATING, 1);
                }
                fragment.getContext().getContentResolver().update(insertedMovie, inListValue, null, null);
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        // Our progress bar is a spinning wheel, we just need to tell it when we're done.
        publishProgress(100);
        return movies;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int completion = values[0];
//            Log.d(LOG_TAG, String.format("Progress: %d%%", completion));
        if (completion == 100) {
            fragment.mProgress.setVisibility(View.GONE);
        } else {
            fragment.mProgress.setVisibility(View.VISIBLE);
            fragment.mProgress.setProgress(completion);
        }

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
