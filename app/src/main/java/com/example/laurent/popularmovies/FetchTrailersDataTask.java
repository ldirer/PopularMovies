package com.example.laurent.popularmovies;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

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
import java.util.List;

public class FetchTrailersDataTask extends AsyncTask<Void, Void, List<Trailer>> {

    private static final String[] TRAILERS_COLUMNS = {
            MovieContract.TrailerEntry.COLUMN_TRAILER_NAME,
            MovieContract.TrailerEntry.COLUMN_TRAILER_URL_KEY,
    };
    private static final int COL_TRAILER_NAME = 0;
    private static final int COL_TRAILER_URL_KEY = 1;
    private DetailActivityFragment detailActivityFragment;
    private String baseUri;
    private Uri baseProviderUri;
    private String LOG_TAG = FetchTrailersDataTask.class.getSimpleName();

    public FetchTrailersDataTask(DetailActivityFragment detailActivityFragment, Bundle extras) {
        super();
        this.detailActivityFragment = detailActivityFragment;
        Integer movieId = extras.getInt(MainActivity.DETAIL_EXTRAS_ID);
        this.baseUri = String.format("https://api.themoviedb.org/3/movie/%d/videos", movieId);
        baseProviderUri = MovieContract.MovieEntry.buildMovieUri(movieId);
    }

    public List<Trailer> FetchTrailersFromNetwork(Uri trailersUri) {
        List<Trailer> trailers = null;
        String trailersJsonStr;
        HttpURLConnection urlConnection = null;
        BufferedReader reader;
        try {
            URL url = new URL(trailersUri.toString());
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
            trailersJsonStr = buffer.toString();
            trailers = getTrailerDataFromJson(trailersJsonStr);

        } catch (IOException | JSONException e) {
            Log.e(LOG_TAG, "Error on my side.");
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return trailers;
    }

    @Override
    protected List<Trailer> doInBackground(Void... params) {
        Uri trailersUri = Uri.parse(baseUri).buildUpon()
                .appendQueryParameter("api_key", MainActivity.API_KEY)
                .build();

        Log.d(LOG_TAG, trailersUri.toString());
        List<Trailer> trailers = null;
        if (detailActivityFragment.mIsFavorite == 1) {
            Log.d(LOG_TAG, "Using database to fetch favorite trailers...");
            trailers = new ArrayList<Trailer>();
            Uri trailersProviderUri = baseProviderUri.buildUpon().appendPath(MovieContract.PATH_TRAILERS)
                    .build();
            Cursor trailersCursor = detailActivityFragment.getContext().getContentResolver().query(trailersProviderUri, TRAILERS_COLUMNS, null,
                    null, null);

            if (trailersCursor.moveToFirst()) {
                do {
                    String trailerName = trailersCursor.getString(COL_TRAILER_NAME);
                    String trailerKey = trailersCursor.getString(COL_TRAILER_URL_KEY);
                    trailers.add(new Trailer(trailerName, trailerKey));
                } while (trailersCursor.moveToNext());
            } else {
                Log.d(LOG_TAG, "No trailers found in DB!");
            }
            trailersCursor.close();
        }
        else {
            trailers = FetchTrailersFromNetwork(trailersUri);
        }
        return trailers;

    }

    private List<Trailer> getTrailerDataFromJson(String trailersJsonStr) throws JSONException {
        final String TMDB_TRAILERS_LIST = "results";
        final String TMDB_TRAILER_NAME = "name";
        final String TMDB_TRAILER_KEY = "key";

        JSONObject trailersJson = new JSONObject(trailersJsonStr);
        JSONArray trailersArray = trailersJson.getJSONArray(TMDB_TRAILERS_LIST);


        List<Trailer> trailerList = new ArrayList<>();

        for (int i = 0; i < trailersArray.length(); i++) {
            JSONObject trailerJson = trailersArray.getJSONObject(i);
            String name = trailerJson.getString(TMDB_TRAILER_NAME);
            String key = trailerJson.getString(TMDB_TRAILER_KEY);

            Trailer trailer = new Trailer(name, key);
            trailerList.add(trailer);
        }
        return trailerList;
    }


    @Override
    protected void onPostExecute(List<Trailer> result) {
        super.onPostExecute(result);
        detailActivityFragment.mTrailersFetched = true;
        if (result != null) {
            detailActivityFragment.mTrailers = result;
            Log.d(LOG_TAG, String.format("Number of trailers fetched: %s",
                    Integer.toString(result.size())));
            detailActivityFragment.updateTrailersUI(detailActivityFragment.mTrailers);
        }
    }
}
