package com.example.laurent.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.laurent.popularmovies.data.MovieContract;
import com.facebook.drawee.view.SimpleDraweeView;

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
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    // We need a unique id for each loader.
    public int DETAIL_LOADER = 0;
    public Uri movieUri;

    private MenuItem mShareMenuItem;

    private final static String[] DETAIL_COLUMNS = {
            MovieContract.MovieEntry.COLUMN_IMAGE_URI,
            MovieContract.MovieEntry.COLUMN_IS_FAVORITE,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_SYNOPSIS,
            MovieContract.MovieEntry.COLUMN_TITLE,
//            MovieContract.MovieEntry.COLUMN_POPULARITY,
//            MovieContract.MovieEntry.COLUMN_INSERT_ORDER,
    };


    private static final int COL_MOVIE_IMAGE_URI = 0;
    private static final int COL_MOVIE_IS_FAVORITE = 1;
    private static final int COL_MOVIE_RATING = 2;
    private static final int COL_MOVIE_RELEASE_DATE = 3;
    private static final int COL_MOVIE_SYNOPSIS = 4;
    private static final int COL_MOVIE_TITLE = 5;
//    private static final int COL_MOVIE_POPULARITY = 5;
//    private static final int COL_MOVIE_INSERT_ORDER = 6;

    public DetailActivityFragment() {
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        Button favoriteButton = (Button) view.findViewById(R.id.detail_favorite_button);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markMovieAsFavorite(v);
            }
        });
        view.setTag(new ViewHolder(view));



        // Fetching task populates the linear layouts.
        new FetchReviewsDataTask().execute();
        new FetchTrailersDataTask().execute();

        return view;
    }

    public Void addReviewToLinearLayout(LinearLayout parent, Review review) {
        Log.d(LOG_TAG, "in addReviewToLinearLayout");
        LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View reviewView = li.inflate(R.layout.detail_review_item, parent, false);
        TextView authorView = (TextView) reviewView.findViewById(R.id.detail_review_author);
        TextView bodyView = (TextView) reviewView.findViewById(R.id.detail_review_body);
        authorView.setText(review.author);
        bodyView.setText(review.body);
        parent.addView(reviewView);
        return null;
    }

    public Void addTrailerToLinearLayout(LinearLayout parent, final Trailer trailer) {
        Log.d(LOG_TAG, "in addTrailerToLinearLayout");
        LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View trailerView = li.inflate(R.layout.detail_trailer_item, parent, false);
        TextView nameView = (TextView) trailerView.findViewById(R.id.detail_trailer_name);
        nameView.setText(trailer.name);
        trailerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Uri youtubeUri = trailer.uri;
                Intent videoIntent = new Intent(Intent.ACTION_VIEW, youtubeUri);
                startActivity(videoIntent);
            }
        });
        parent.addView(trailerView);
        return null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mShareMenuItem = menu.findItem(R.id.action_share);
        Uri uri = Utility.getDummyTrailer().uri;
        Intent shareIntent = getShareIntent(uri);
        mShareMenuItem.setIntent(shareIntent);
    }

    public Intent getShareIntent(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, uri.toString());
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        Intent intent = getActivity().getIntent();
        Log.v(LOG_TAG, "Movie URI: " + intent.getDataString());
        this.movieUri = intent.getData();

        return new CursorLoader(
                getActivity(),
                this.movieUri,
                DETAIL_COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        View view = getView();
        if (data.moveToFirst()) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();

            viewHolder.rating_view.setText(String.format("%.2f/10", data.getDouble(COL_MOVIE_RATING)));
            viewHolder.title_view.setText(data.getString(COL_MOVIE_TITLE));
            viewHolder.release_date_view.setText(getYearFromDate(data.getString(COL_MOVIE_RELEASE_DATE)));
            viewHolder.synopsis_view.setText(data.getString(COL_MOVIE_SYNOPSIS));
            viewHolder.poster_view.setImageURI(Uri.parse(data.getString(COL_MOVIE_IMAGE_URI)));
//            viewHolder.popularity_view.setText(String.format("Popularity score: %.2f", data.getDouble(COL_MOVIE_POPULARITY)));
//            viewHolder.insert_order_view.setText(String.format("Insert order: %d", data.getInt(COL_MOVIE_INSERT_ORDER)));

            Log.d(LOG_TAG, String.format("movie is favorite value: %d", data.getInt(COL_MOVIE_IS_FAVORITE)));
            if (data.getInt(COL_MOVIE_IS_FAVORITE) == 0) {
                viewHolder.favorite_button.setText(R.string.favorite_add_to_button_text);
            } else {
                viewHolder.favorite_button.setText(R.string.favorite_remove_button_text);
            }

            viewHolder.title_view.getWidth();
            viewHolder.title_view.getLineHeight();
            viewHolder.title_view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    findRightTextSize(viewHolder.title_view));

        }
    }

    /**
     * Dichotomic search to find a text size that fits.
     *
     * @param view
     * @return A text size that works (in pixels).
     */
    private float findRightTextSize(TextView view) {
        // http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview
        String text = (String) view.getText();
        int textWidth = view.getWidth();
        int targetWidth = textWidth - view.getPaddingLeft() - view.getPaddingRight();
        // We don't want to use view.getTextSize() because repeated calls will gradually shrink the text!!
//        float hi = view.getTextSize();
        float hi = getResources().getInteger(R.integer.detail_max_title_font);
        float lo = 10;
        final float threshold = 0.5f; // How close we have to be

        TextPaint testPaint = new TextPaint();
        testPaint.set(view.getPaint());

        while ((hi - lo) > threshold) {
            float size = (hi + lo) / 2;
            testPaint.setTextSize(size);
            if (testPaint.measureText(text) >= targetWidth)
                hi = size; // too big
            else
                lo = size; // too small
        }
        // Use lo so that we undershoot rather than overshoot
        return lo;
    }


    /**
     * The dates we get from the movie database are formatted as yyyy-mm-dd.
     *
     * @param string
     * @return
     */
    private String getYearFromDate(String string) {
        return string.split("-")[0];
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private class ViewHolder {

        TextView rating_view;
        TextView title_view;
        TextView release_date_view;
        TextView synopsis_view;
        SimpleDraweeView poster_view;
        Button favorite_button;
        LinearLayout review_linear_layout;
        LinearLayout trailer_linear_layout;


        public ViewHolder(View view) {
            this.rating_view = (TextView) view.findViewById(R.id.detail_rating);
            this.title_view = (TextView) view.findViewById(R.id.detail_title);
            this.release_date_view = ((TextView) view.findViewById(R.id.detail_release_date));
            this.synopsis_view = ((TextView) view.findViewById(R.id.detail_synopsis));
            this.poster_view = (SimpleDraweeView) view.findViewById(R.id.detail_poster);
            this.favorite_button = (Button) view.findViewById(R.id.detail_favorite_button);
            this.review_linear_layout = (LinearLayout)view.findViewById(R.id.detail_reviews);
            this.trailer_linear_layout = (LinearLayout)view.findViewById(R.id.detail_trailers);
        }
    }


    /**
     * We don't need to modify the view here: it's done by the loader when it notices data has
     * changed.
     *
     * @param view
     */
    public void markMovieAsFavorite(View view) {
        // Update id with favorite = 1 if it was 0, 0 if it was 1.
        Uri toggleFavoriteUri = this.movieUri.buildUpon()
                .appendPath("toggle_favorite")
                .build();

        Log.d(LOG_TAG, "in markMovieAsFavorite, toggle fav URI: " + toggleFavoriteUri.toString());

        // TODO: check that this is a proper use of ContentProvider (it probably isn't).
        int rowsUpdated = getContext().getContentResolver().update(toggleFavoriteUri, null, null, null);
        Log.d(LOG_TAG, String.format("rows updated: %d", rowsUpdated));
    }

    public class FetchTrailersDataTask extends AsyncTask<Void, Void, List<Trailer>>{

        private String baseUri;
        private String LOG_TAG = FetchTrailersDataTask.class.getSimpleName();

        public FetchTrailersDataTask() {
            super();
            Intent intent = getActivity().getIntent();
            long _id = Long.parseLong(intent.getData().getLastPathSegment());
            this.baseUri = String.format("https://api.themoviedb.org/3/movie/%d/videos", _id);
        }

        @Override
        protected List<Trailer> doInBackground(Void... params) {
            Uri trailersUri = Uri.parse(baseUri).buildUpon()
                    .appendQueryParameter("api_key", MainActivity.API_KEY)
                    .build();

            Log.d(LOG_TAG, trailersUri.toString());
            List<Trailer> trailers = null;
            String trailersJsonStr = null;
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
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

        private List<Trailer> getTrailerDataFromJson(String trailersJsonStr) throws JSONException {
            final String TMDB_TRAILERS_LIST = "results";
            final String TMDB_TRAILER_NAME = "name";
            final String TMDB_TRAILER_KEY = "key";

            JSONObject trailersJson = new JSONObject(trailersJsonStr);
            JSONArray trailersArray = trailersJson.getJSONArray(TMDB_TRAILERS_LIST);


            Trailer t = Utility.getDummyTrailer();
            List<Trailer> trailerList = new ArrayList<>(Arrays.asList(t));

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
            if (result != null) {
                Log.d(LOG_TAG, String.format("Number of trailers fetched: %s",
                        Integer.toString(result.size())));
                for (Trailer trailer:result) {
                    addTrailerToLinearLayout(((ViewHolder)getView().getTag()).trailer_linear_layout, trailer);
                }
                Intent shareIntent = getShareIntent(result.get(0).uri);
                mShareMenuItem.setIntent(shareIntent);
            }
        }
    }

    public class FetchReviewsDataTask extends AsyncTask<Void, Void, List<Review>> {
        private String baseUri;
        private String LOG_TAG = FetchReviewsDataTask.class.getSimpleName();

        public FetchReviewsDataTask() {
            super();
            Intent intent = getActivity().getIntent();
            long _id = Long.parseLong(intent.getData().getLastPathSegment());
            this.baseUri = String.format("https://api.themoviedb.org/3/movie/%d/reviews", _id);
        }


        @Override
        protected List<Review> doInBackground(Void... params) {

//            publishProgress(0);

            Uri reviewsUri = Uri.parse(baseUri).buildUpon()
                    .appendQueryParameter("api_key", MainActivity.API_KEY)
                    .build();

            Log.d(LOG_TAG, reviewsUri.toString());

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
            // Our progress bar is a spinning wheel, we just need to tell it when we're done.
//            publishProgress(100);
            return reviews;
        }

        @Override
        protected void onPostExecute(List<Review> result) {
            super.onPostExecute(result);
            if (result != null) {
                for (Review review:result) {
                    addReviewToLinearLayout(((ViewHolder)getView().getTag()).review_linear_layout, review);
                }
            }
        }

        private List<Review> getReviewDataFromJson(String reviewsJsonStr) throws JSONException {
            final String TMDB_REVIEWS_LIST = "results";
            final String TMDB_REVIEW_AUTHOR = "author";
            final String TMDB_REVIEW_BODY = "content";

            JSONObject reviewsJson = new JSONObject(reviewsJsonStr);
            JSONArray reviewsArray = reviewsJson.getJSONArray(TMDB_REVIEWS_LIST);


            Review r = new Review("Laurent", "This movie is freakin' awesome");
            List<Review> reviewList = new ArrayList<>(Arrays.asList(r));

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
}

