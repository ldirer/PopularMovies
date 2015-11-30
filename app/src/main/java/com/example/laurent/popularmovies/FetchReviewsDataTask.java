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

public class FetchReviewsDataTask extends AsyncTask<Void, Void, List<Review>> {
    private static final String[] REVIEWS_COLUMNS = {
            MovieContract.ReviewEntry.COLUMN_REVIEW_BODY,
            MovieContract.ReviewEntry.COLUMN_REVIEW_AUTHOR,
    };
    private static final int COL_REVIEW_BODY = 0;
    private static final int COL_REVIEW_AUTHOR = 1;

    private DetailActivityFragment detailActivityFragment;
    private String baseUriStr;
    private Uri baseProviderUri;
    private String LOG_TAG = FetchReviewsDataTask.class.getSimpleName();

    public FetchReviewsDataTask(DetailActivityFragment detailActivityFragment, Bundle bundle) {
        super();
        this.detailActivityFragment = detailActivityFragment;
        long _id = bundle.getInt(MainActivity.DETAIL_EXTRAS_ID);
        this.baseUriStr = String.format("https://api.themoviedb.org/3/movie/%d/reviews", _id);
        baseProviderUri = MovieContract.MovieEntry.buildMovieUri(_id);
    }


    @Override
    protected List<Review> doInBackground(Void... params) {
//            publishProgress(0);

        Uri reviewsUri = Uri.parse(baseUriStr).buildUpon()
                .appendQueryParameter("api_key", MainActivity.API_KEY)
                .build();

        Log.d(LOG_TAG, reviewsUri.toString());
        List<Review> reviews;
        if (detailActivityFragment.mIsFavorite == 1) {
            Log.d(LOG_TAG, "Using database to fetch favorite details...");
            reviews = new ArrayList<Review>();
            Uri reviewsProviderUri = baseProviderUri.buildUpon().appendPath("reviews")
                    .build();
            Cursor reviewsCursor = detailActivityFragment.getContext().getContentResolver().query(reviewsProviderUri, REVIEWS_COLUMNS, null,
                    null, null);

            if (reviewsCursor.moveToFirst()) {
                do {
                    String reviewAuthor = reviewsCursor.getString(COL_REVIEW_AUTHOR);
                    String reviewBody = reviewsCursor.getString(COL_REVIEW_BODY);
                    reviews.add(new Review(reviewAuthor, reviewBody));
                } while (reviewsCursor.moveToNext());
            } else {
                Log.d(LOG_TAG, "No reviews found in DB!");
            }
            reviewsCursor.close();
        } else {
            reviews = getReviewsFromNetwork(reviewsUri);
        }
        return reviews;
    }


    protected List<Review> getReviewsFromNetwork(Uri reviewsUri) {
        Log.d(LOG_TAG, "in getReviewsFromNetwork");
        Log.d(LOG_TAG, "in getReviewsFromNetwork with mIsFavorite=" + detailActivityFragment.mIsFavorite);
        List<Review> reviews = null;
        String reviewsJsonStr = null;
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(reviewsUri.toString());
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
            reviewsJsonStr = buffer.toString();
            reviews = getReviewDataFromJson(reviewsJsonStr);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return reviews;
    }


    @Override
    protected void onPostExecute(List<Review> result) {
        super.onPostExecute(result);
        detailActivityFragment.mReviewsFetched = true;
        if (result != null) {
            detailActivityFragment.mReviews = result;
            Log.d(LOG_TAG, String.format("Number of reviews fetched: %s", result.size()));
            detailActivityFragment.updateReviewsUI(detailActivityFragment.mReviews);
        }
    }


    private List<Review> getReviewDataFromJson(String reviewsJsonStr) throws JSONException {
        final String TMDB_REVIEWS_LIST = "results";
        final String TMDB_REVIEW_AUTHOR = "author";
        final String TMDB_REVIEW_BODY = "content";

        JSONObject reviewsJson = new JSONObject(reviewsJsonStr);
        JSONArray reviewsArray = reviewsJson.getJSONArray(TMDB_REVIEWS_LIST);

        List<Review> reviewList = new ArrayList<>();

        for (int i = 0; i < reviewsArray.length(); i++) {
            // TODO: is this efficient? Fetching the ith element each time does not seem so.
            // TODO: No map method on JSONArray?..
            JSONObject reviewJson = reviewsArray.getJSONObject(i);
            String author = reviewJson.getString(TMDB_REVIEW_AUTHOR);
            String body = reviewJson.getString(TMDB_REVIEW_BODY);

            Review review = new Review();
            review.author = author;
            review.body = body;
            reviewList.add(review);
        }
        return reviewList;
    }
}
