package com.example.laurent.popularmovies;

import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchMovieConfiguration {

    private final String baseUri = "http://api.themoviedb.org/3/configuration";

    private String getConfigFromJson(String configJsonStr) throws JSONException {


        final String TMDB_IMAGE_CONFIG = "images";
        final String TMDB_BASE_URI = "secure_base_url";
        final String TMDB_POSTER_SIZE = "poster_sizes";

        JSONObject configJson = new JSONObject(configJsonStr);

        // Insert the new movie information into the database
//            Vector<ContentValues> cVVector = new Vector<ContentValues>(moviesArray.length());

        JSONObject imageConfig = configJson.getJSONObject(TMDB_IMAGE_CONFIG);
        String baseImageUri = imageConfig.getString(TMDB_BASE_URI);
        // TODO: mb get available poster sizes.
        return baseImageUri;
    }

    public String getImageBaseUri(Void... params) {
        Uri uri = Uri.parse(baseUri).buildUpon()
                .appendQueryParameter("api_key", MainActivity.API_KEY)
                .build();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String configJsonStr = null;
        String imageBaseUri = null;

        try {
            URL url = new URL(uri.toString());
            urlConnection = (HttpURLConnection)url.openConnection();
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
            configJsonStr = buffer.toString();

            // TODO: getConfigFromJson
//            Save to db?
            imageBaseUri = getConfigFromJson(configJsonStr);


        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return imageBaseUri;
    }
}
