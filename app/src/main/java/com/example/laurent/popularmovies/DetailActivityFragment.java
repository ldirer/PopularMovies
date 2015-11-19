package com.example.laurent.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
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
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ViewTreeObserver.OnScrollChangedListener {

    // Fragment Tags are useful on orientation change when activity is destroyed + re-created.
    // If the fragment has already been created and it has a tag, it can be retrieved.
    public static final String DETAIL_FRAGMENT_TAG = "DF_TAG";
    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    // We need a unique id for each loader.
    public int DETAIL_LOADER = 0;
    public Integer mId = null;
    public Uri mMovieUri;
    // the title is used to give a link to search for trailers on youtube if no trailer.
    private String mTitle;

    private MenuItem mShareMenuItem;

    private TextView mRatingView;
    private RatingBar mRatingBarView;
    private TextView mTitleView;
    private TextView mReleaseDateView;
    private TextView mSynopsysView;
    private SimpleDraweeView mPosterView;
    private Button mFavoriteButton;
    private FloatingActionButton mFavoriteFloatingActionButton;
    private LinearLayout mReviewLinearLayout;
    private LinearLayout mTrailerLinearLayout;
    private TextView mReviewLinearLayoutEmpty;
    private TextView mTrailerLinearLayoutEmpty;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;

    private final static String[] DETAIL_COLUMNS = {
            MovieContract.MovieEntry.COLUMN_IMAGE_URI,
            MovieContract.MovieEntry.COLUMN_IS_FAVORITE,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_SYNOPSIS,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_IMAGE_BACKDROP_URI,
//            MovieContract.MovieEntry.COLUMN_POPULARITY,
//            MovieContract.MovieEntry.COLUMN_INSERT_ORDER,
    };


    private static final int COL_MOVIE_IMAGE_URI = 0;
    private static final int COL_MOVIE_IS_FAVORITE = 1;
    private static final int COL_MOVIE_RATING = 2;
    private static final int COL_MOVIE_RELEASE_DATE = 3;
    private static final int COL_MOVIE_SYNOPSIS = 4;
    private static final int COL_MOVIE_TITLE = 5;
    private static final int COL_MOVIE_IMAGE_BACKDROP_URI = 6;
//    private static final int COL_MOVIE_POPULARITY = 5;
//    private static final int COL_MOVIE_INSERT_ORDER = 6;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    private void initInstances() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(LOG_TAG, "in onCreateView");
        Bundle arguments = getArguments();

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);


        if (arguments != null) {
            mId = arguments.getInt(MainActivity.DETAIL_EXTRAS_ID);
            Bundle extras = new Bundle();
            extras.putInt(MainActivity.DETAIL_EXTRAS_ID, mId);
            // Fetching task populates the linear layouts.
            new FetchReviewsDataTask(extras).execute();
            new FetchTrailersDataTask(extras).execute();
        }
        mRatingView = (TextView) rootView.findViewById(R.id.detail_rating);
        mRatingBarView = (RatingBar) rootView.findViewById(R.id.detail_rating_bar);
        mTitleView = (TextView) rootView.findViewById(R.id.detail_title);
//            release_date_view = ((TextView) rootView.findViewById(R.id.detail_release_date));
        mSynopsysView = ((TextView) rootView.findViewById(R.id.detail_synopsis));
//        mPosterView = (SimpleDraweeView) rootView.findViewById(R.id.detail_poster);
        mFavoriteButton = (Button) rootView.findViewById(R.id.detail_favorite_button);
        mReviewLinearLayout = (LinearLayout)rootView.findViewById(R.id.detail_reviews);
        mReviewLinearLayoutEmpty = (TextView)rootView.findViewById(R.id.detail_reviews_empty);
        mTrailerLinearLayout = (LinearLayout)rootView.findViewById(R.id.detail_trailers);
        mTrailerLinearLayoutEmpty = (TextView)rootView.findViewById(R.id.detail_trailers_empty);

        // Toolbar-related
        mPosterView = (SimpleDraweeView) getActivity().findViewById(R.id.toolbar_image);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) getActivity().findViewById(R.id.collapsingToolbarLayout);
        mFavoriteFloatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.detail_favorite_floating_action_button);

        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markMovieAsFavorite();
            }
        });

        mFavoriteFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markMovieAsFavorite();
            }
        });


        return rootView;
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
//        mShareMenuItem = menu.findItem(R.id.action_share);
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
        Log.v(LOG_TAG, "in onCreateLoader, mId:" + mId);
        if (mId != null) {
            mMovieUri = MovieContract.MovieEntry.buildMovieUri(mId);
            Log.v(LOG_TAG, "Movie URI: " + mMovieUri.toString());

            return new CursorLoader(
                    getActivity(),
                    mMovieUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            Log.v(LOG_TAG, "in onLoadFinished with non null, non empty data");
            mRatingView.setText(String.format("%.2f/10", data.getDouble(COL_MOVIE_RATING)));
            // Using a 5-star rating with ratings from tmdb on a 0-10 scale.
            mRatingBarView.setRating((float) data.getDouble(COL_MOVIE_RATING) / 2);
            mTitle = data.getString(COL_MOVIE_TITLE);
            mTitleView.setText(data.getString(COL_MOVIE_TITLE));
            mCollapsingToolbarLayout.setTitle(data.getString(COL_MOVIE_TITLE));
//            release_date_view.setText(getYearFromDate(data.getString(COL_MOVIE_RELEASE_DATE)));
            mSynopsysView.setText(data.getString(COL_MOVIE_SYNOPSIS));
            mPosterView.setImageURI(Uri.parse(data.getString(COL_MOVIE_IMAGE_BACKDROP_URI)));

            Log.d(LOG_TAG, String.format("movie is favorite value: %d", data.getInt(COL_MOVIE_IS_FAVORITE)));
            if (data.getInt(COL_MOVIE_IS_FAVORITE) == 0) {
                mFavoriteButton.setText(R.string.favorite_add_to_button_text);
                mFavoriteFloatingActionButton.setImageDrawable(ContextCompat.getDrawable(getContext(), android.R.drawable.star_off));
            } else {
                mFavoriteButton.setText(R.string.favorite_remove_button_text);
                mFavoriteFloatingActionButton.setImageDrawable(ContextCompat.getDrawable(getContext(), android.R.drawable.star_on));
            }

            mTitleView.getWidth();
            mTitleView.getLineHeight();
            mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    findRightTextSize(mTitleView));
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



    /**
     * We don't need to modify the view here: it's done by the loader when it notices data has
     * changed.
     *
     */
    public void markMovieAsFavorite() {
        // Update id with favorite = 1 if it was 0, 0 if it was 1.
        Uri toggleFavoriteUri = mMovieUri.buildUpon()
                .appendPath("toggle_favorite")
                .build();

        Log.d(LOG_TAG, "in markMovieAsFavorite, toggle fav URI: " + toggleFavoriteUri.toString());

        // TODO: check that this is a proper use of ContentProvider (it probably isn't).
        int rowsUpdated = getContext().getContentResolver().update(toggleFavoriteUri, null, null, null);
        Log.d(LOG_TAG, String.format("rows updated: %d", rowsUpdated));
    }

    @Override
    public void onScrollChanged() {

    }


    public class FetchTrailersDataTask extends AsyncTask<Void, Void, List<Trailer>>{

        private String baseUri;
        private String LOG_TAG = FetchTrailersDataTask.class.getSimpleName();

        public FetchTrailersDataTask(Bundle extras) {
            super();
            Integer movieId = extras.getInt(MainActivity.DETAIL_EXTRAS_ID);
            this.baseUri = String.format("https://api.themoviedb.org/3/movie/%d/videos", movieId);
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
            if (result != null) {
                Log.d(LOG_TAG, String.format("Number of trailers fetched: %s",
                        Integer.toString(result.size())));

                if (result.size() == 0) {
                    mTrailerLinearLayout.setVisibility(View.GONE);
                    mTrailerLinearLayoutEmpty.setVisibility(View.VISIBLE);
                    // Here we're not sure that mTitle is not null... But we can hope for the best!
                    mTrailerLinearLayoutEmpty.setText(Html.fromHtml(getString(R.string.trailer_list_empty, mTitle)));
                    mTrailerLinearLayoutEmpty.setMovementMethod(LinkMovementMethod.getInstance());
                }
                else {
                    for (Trailer trailer:result) {
                        addTrailerToLinearLayout(mTrailerLinearLayout, trailer);
                    }
                    Intent shareIntent = getShareIntent(result.get(0).uri);
//                    mShareMenuItem.setIntent(shareIntent);
                }
            }
            }
    }

    public class FetchReviewsDataTask extends AsyncTask<Void, Void, List<Review>> {
        private String baseUri;
        private String LOG_TAG = FetchReviewsDataTask.class.getSimpleName();

        public FetchReviewsDataTask(Bundle bundle) {
            super();
            long _id = bundle.getInt(MainActivity.DETAIL_EXTRAS_ID);
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
                Log.d(LOG_TAG, String.format("Number of reviews fetched: %s", result.size()));
                for (Review review:result) {
                    addReviewToLinearLayout(mReviewLinearLayout, review);
                }
                if (result.size() == 0) {
                    mReviewLinearLayout.setVisibility(View.GONE);
                    mReviewLinearLayoutEmpty.setVisibility(View.VISIBLE);
                }
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
}

