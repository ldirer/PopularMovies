package com.example.laurent.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

/**
 * Using Fresco we just need to set a URI for a SimpleDraweeView to have the image downloaded,
 * cached...
 * We still need a cursorAdapter if/since we're fetching image urls from a database.
 */
public class ImageListAdapter extends CursorAdapter {

    private final String LOG_TAG = this.getClass().getSimpleName();

    public ImageListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        SimpleDraweeView draweeView;
        draweeView = (SimpleDraweeView) LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        draweeView.setTag(new ViewHolder(draweeView));
        return draweeView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.posterView.setImageURI(Uri.parse(cursor.getString(MainActivityFragment.COL_MOVIE_IMAGE_URI)));
    }

    /**
     ViewHolder pattern: we 'cache' the views so that we don't need to use findViewById on recycled
     views (views that we've seen before). VERY verbose imho.
     */
    private class ViewHolder {
        public SimpleDraweeView posterView;

        public ViewHolder(View view) {
            this.posterView = (SimpleDraweeView) view.findViewById(R.id.grid_item_image);
        }

    }

}
