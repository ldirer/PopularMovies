package com.example.laurent.popularmovies;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.example.laurent.popularmovies.data.MovieContract;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;


public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

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
    private FloatingActionButton mShareFloatingActionButton;

    private TextView mRatingView;
    private RatingBar mRatingBarView;
    private TextView mTitleView;
    private TextView mSynopsysView;
    private SimpleDraweeView mPosterView;
    private FloatingActionButton mFavoriteFloatingActionButton;
    private LinearLayout mReviewLinearLayout;
    private LinearLayout mTrailerLinearLayout;
    private TextView mReviewLinearLayoutEmpty;
    private TextView mTrailerLinearLayoutEmpty;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    public int mIsFavorite;

    public List<Review> mReviews = null;
    public List<Trailer> mTrailers = null;
    public boolean mTrailersFetched;
    public boolean mReviewsFetched;

    public final static String[] DETAIL_COLUMNS = {
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


    public static final int COL_MOVIE_IMAGE_URI = 0;
    public static final int COL_MOVIE_IS_FAVORITE = 1;
    public static final int COL_MOVIE_RATING = 2;
    public static final int COL_MOVIE_RELEASE_DATE = 3;
    public static final int COL_MOVIE_SYNOPSIS = 4;
    public static final int COL_MOVIE_TITLE = 5;
    public static final int COL_MOVIE_IMAGE_BACKDROP_URI = 6;
    private FetchReviewsDataTask mFetchReviewsDataTask = null;
    private FetchTrailersDataTask mFetchTrailersDataTask = null;
//    private static final int COL_MOVIE_POPULARITY = 5;
//    private static final int COL_MOVIE_INSERT_ORDER = 6;


    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        // This prevents the AsyncTask from using detached fragments (avoids some NPE).
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(LOG_TAG, "in onCreateView");
        Bundle arguments = getArguments();

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        if (arguments == null) {
            return inflater.inflate(R.layout.fragment_detail_placeholder, container, false);
        }


        mId = arguments.getInt(MainActivity.DETAIL_EXTRAS_ID);

        mRatingView = (TextView) rootView.findViewById(R.id.detail_rating);
        mRatingBarView = (RatingBar) rootView.findViewById(R.id.detail_rating_bar);
        mTitleView = (TextView) rootView.findViewById(R.id.detail_title);
        mSynopsysView = ((TextView) rootView.findViewById(R.id.detail_synopsis));
        mReviewLinearLayout = (LinearLayout) rootView.findViewById(R.id.detail_reviews);
        mReviewLinearLayoutEmpty = (TextView) rootView.findViewById(R.id.detail_reviews_empty);
        mTrailerLinearLayout = (LinearLayout) rootView.findViewById(R.id.detail_trailers);
        mTrailerLinearLayoutEmpty = (TextView) rootView.findViewById(R.id.detail_trailers_empty);

        // Toolbar-related
        mPosterView = (SimpleDraweeView) getActivity().findViewById(R.id.toolbar_image);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) getActivity().findViewById(R.id.collapsingToolbarLayout);
        mFavoriteFloatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.detail_favorite_floating_action_button);

        if (null == getActivity().findViewById(R.id.toolbar_image)) {
            // Maybe we could move the code for setActionBar in the fragment. See how Sunshine did it.
            // It would prevent this boilerplate code.
            mPosterView = (SimpleDraweeView) rootView.findViewById(R.id.toolbar_image);
            mCollapsingToolbarLayout = (CollapsingToolbarLayout) rootView.findViewById(R.id.collapsingToolbarLayout);
            mFavoriteFloatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.detail_favorite_floating_action_button);
            mShareFloatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.action_share);
        }

        if (mTrailersFetched) {
            updateTrailersUI(mTrailers);
        }
        if (mReviewsFetched) {
            updateReviewsUI(mReviews);
        }

        // Fixing floating action button margin for <20 api versions
        if (Build.VERSION.SDK_INT < 55) { //Build.VERSION_CODES.LOLLIPOP) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) mFavoriteFloatingActionButton.getLayoutParams();
            marginParams.setMargins(0, 0, 0, 0);
            mFavoriteFloatingActionButton.setLayoutParams(marginParams);
        }

        mFavoriteFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markMovieAsFavorite();
            }
        });


        return rootView;
    }


    /**
     * Update the reviews UI by populating a linear layout with a list of the reviews.
     * @param reviews
     */
    public void updateReviewsUI(List<Review> reviews) {
        for (Review review : reviews) {
            addReviewToLinearLayout(mReviewLinearLayout, review);
        }
        if (reviews.size() == 0) {
            mReviewLinearLayout.setVisibility(View.GONE);
            mReviewLinearLayoutEmpty.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Update the trailers UI by populating a linear layout with a list of the trailers.
     * @param trailers
     */
    public void updateTrailersUI(List<Trailer> trailers) {
        if (trailers.size() == 0) {
            mTrailerLinearLayout.setVisibility(View.GONE);
            mTrailerLinearLayoutEmpty.setVisibility(View.VISIBLE);
            mTrailerLinearLayoutEmpty.setText(Html.fromHtml(getString(R.string.trailer_list_empty, mTitle)));
            mTrailerLinearLayoutEmpty.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            for (Trailer trailer : trailers) {
                addTrailerToLinearLayout(mTrailerLinearLayout, trailer);
            }
            final Intent shareIntent = getShareIntent(trailers.get(0).uri);
            if (null == mShareMenuItem) {
                mShareFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(shareIntent);
                    }
                });
            } else {
                mShareMenuItem.setIntent(shareIntent);
            }
        }
    }


    public Void addReviewToLinearLayout(LinearLayout parent, Review review) {
        Log.d(LOG_TAG, "in addReviewToLinearLayout");
        // Here sometimes getActivity() returns a null object.
        // This is because the fragment that launched the task is not attached to the activity anymore.
        // In this case we prevent onPostExecute from being called by cancelling the task in onDetach.
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
            mRatingView.setText(String.format("(%.2f/5)", data.getDouble(COL_MOVIE_RATING) / 2));
            // Using a 5-star rating with ratings from tmdb on a 0-10 scale.
            mRatingBarView.setRating((float) data.getDouble(COL_MOVIE_RATING) / 2);
            mTitle = data.getString(COL_MOVIE_TITLE);
            mTitle += " (" + getYearFromDate(data.getString(COL_MOVIE_RELEASE_DATE)) + ")";
            if (null != mCollapsingToolbarLayout) {
                mCollapsingToolbarLayout.setTitle(mTitle);
            } else {
                mTitleView.setText(mTitle);
                // We cannot compute the right text size if the view is not fully rendered.
                if (mTitleView.getWidth() != 0) {
                    mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Utility.findRightTextSize(this, mTitleView));
                }
            }

            mSynopsysView.setText(data.getString(COL_MOVIE_SYNOPSIS));
            mPosterView.setImageURI(Uri.parse(data.getString(COL_MOVIE_IMAGE_BACKDROP_URI)));

            Log.d(LOG_TAG, String.format("movie is favorite value: %d", data.getInt(COL_MOVIE_IS_FAVORITE)));
            mIsFavorite = data.getInt(COL_MOVIE_IS_FAVORITE);

            if (null == mFetchReviewsDataTask || null == mFetchTrailersDataTask) {
                Bundle extras = new Bundle();
                extras.putInt(MainActivity.DETAIL_EXTRAS_ID, mId);
                // Fetching task populates the linear layouts.
                // We do this here because we want to know if the movie is a favorite to fetch from db or network.
                mFetchReviewsDataTask = (FetchReviewsDataTask) new FetchReviewsDataTask(this, extras).execute();
                mFetchTrailersDataTask = (FetchTrailersDataTask) new FetchTrailersDataTask(this, extras).execute();
            }

            if (mIsFavorite == 0) {
                mFavoriteFloatingActionButton.setImageDrawable(ContextCompat.getDrawable(getContext(), android.R.drawable.star_off));
            } else {
                mFavoriteFloatingActionButton.setImageDrawable(ContextCompat.getDrawable(getContext(), android.R.drawable.star_on));
            }

        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        // This might not be very clean: we should be checking for isCancelled() periodically in doInBackground.
        if (null != mFetchReviewsDataTask) {
            mFetchReviewsDataTask.cancel(true);
        }
        if (null != mFetchTrailersDataTask) {
            mFetchTrailersDataTask.cancel(true);
        }
    }


    /**
     * The dates we get from the movie database are formatted as yyyy-mm-dd.
     * This helper method just returns the year (yyyy).
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
     */
    public void markMovieAsFavorite() {
        // Update id with favorite = 1 if it was 0, 0 if it was 1.
        Uri toggleFavoriteUri = mMovieUri.buildUpon()
                .appendPath("toggle_favorite")
                .build();

        Log.d(LOG_TAG, "in markMovieAsFavorite, toggle fav URI: " + toggleFavoriteUri.toString());


        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);
        String snackbarText = null;
        if (mIsFavorite == 0) {
            snackbarText = "Movie marked as favorite!";
        } else {
            snackbarText = "Movie removed from favorites!";
        }

        Snackbar.make(coordinatorLayout, snackbarText, Snackbar.LENGTH_SHORT)
                .show();
        int rowsUpdated = getContext().getContentResolver().update(toggleFavoriteUri, null, null, null);
        Log.d(LOG_TAG, String.format("rows updated: %d", rowsUpdated));

        // Save reviews and trailers to db.
        for (int i = 0; i < mReviews.size(); i++) {
            Review review = mReviews.get(i);
            ContentValues cvReview = new ContentValues();
            cvReview.put(MovieContract.ReviewEntry.COLUMN_REVIEW_AUTHOR, review.author);
            cvReview.put(MovieContract.ReviewEntry.COLUMN_REVIEW_BODY, review.body);
            Uri insertedReview = getContext().getContentResolver().insert(
                    mMovieUri.buildUpon().appendPath(MovieContract.PATH_REVIEWS).build(),
                    cvReview);
            Log.d(LOG_TAG, "inserted review at uri:" + insertedReview.toString());
        }


        for (int i = 0; i < mTrailers.size(); i++) {
            Trailer trailer = mTrailers.get(i);
            ContentValues cvTrailer = new ContentValues();
            cvTrailer.put(MovieContract.TrailerEntry.COLUMN_TRAILER_NAME, trailer.name);
            cvTrailer.put(MovieContract.TrailerEntry.COLUMN_TRAILER_URL_KEY, trailer.key);
            Uri insertedTrailer = getContext().getContentResolver().insert(
                    mMovieUri.buildUpon().appendPath(MovieContract.PATH_TRAILERS).build(),
                    cvTrailer);
            Log.d(LOG_TAG, "inserted trailer at uri:" + insertedTrailer.toString());
        }
    }


}

