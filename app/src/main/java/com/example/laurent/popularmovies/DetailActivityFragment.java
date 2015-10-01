package com.example.laurent.popularmovies;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.laurent.popularmovies.data.MovieContract;
import com.facebook.drawee.view.SimpleDraweeView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    // We need a unique id for each loader.
    public int DETAIL_LOADER = 0;

    private final static String[] DETAIL_COLUMNS = {
            MovieContract.MovieEntry.COLUMN_IMAGE_URI,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_SYNOPSIS,
            MovieContract.MovieEntry.COLUMN_TITLE,
//            MovieContract.MovieEntry.COLUMN_POPULARITY,
//            MovieContract.MovieEntry.COLUMN_INSERT_ORDER,
    };


    private static final int COL_MOVIE_IMAGE_URI = 0;
    private static final int COL_MOVIE_RATING = 1;
    private static final int COL_MOVIE_RELEASE_DATE = 2;
    private static final int COL_MOVIE_SYNOPSIS = 3;
    private static final int COL_MOVIE_TITLE = 4;
//    private static final int COL_MOVIE_POPULARITY = 5;
//    private static final int COL_MOVIE_INSERT_ORDER = 6;

    public DetailActivityFragment() {
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        view.setTag(new ViewHolder(view));
        return view;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        Intent intent = getActivity().getIntent();
        Log.v(LOG_TAG, "Movie URI: " + intent.getDataString());

        return new CursorLoader(
                getActivity(),
                intent.getData(),
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

            viewHolder.title_view.getWidth();
            viewHolder.title_view.getLineHeight();
            viewHolder.title_view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    findRightTextSize(viewHolder.title_view));

        }
    }

    /**
     * Dichotomic search to find a text size that fits.
     * @param view
     * @return A text size that works (in pixels).
     */
    private float findRightTextSize(TextView view) {
        // http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview
        String text = (String) view.getText();
        int textWidth = view.getWidth();
        int targetWidth = textWidth - view.getPaddingLeft() - view.getPaddingRight();
        float hi = view.getTextSize();
        float lo = 10;
        final float threshold = 0.5f; // How close we have to be

        TextPaint testPaint = new TextPaint();
        testPaint.set(view.getPaint());

        while((hi - lo) > threshold) {
            float size = (hi + lo) / 2;
            testPaint.setTextSize(size);
            if(testPaint.measureText(text) >= targetWidth)
                hi = size; // too big
            else
                lo = size; // too small
        }
        // Use lo so that we undershoot rather than overshoot
        return lo;
    }


    /**
     * The dates we get from the movie database are formatted as yyyy-mm-dd.
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
//        TextView popularity_view;
//        TextView insert_order_view;

        public ViewHolder(View view) {
            this.rating_view = ((TextView) view.findViewById(R.id.detail_rating));
            this.title_view = ((TextView) view.findViewById(R.id.detail_title));
            this.release_date_view = ((TextView) view.findViewById(R.id.detail_release_date));
            this.synopsis_view = ((TextView) view.findViewById(R.id.detail_synopsis));
            this.poster_view = (SimpleDraweeView) view.findViewById(R.id.detail_poster);
//            this.popularity_view = ((TextView) view.findViewById(R.id.detail_popularity));
//            this.insert_order_view = ((TextView) view.findViewById(R.id.detail_insert_order));
        }
    }
}
