package com.example.laurent.popularmovies;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

/**
 * Using Fresco we just need to set a URI for a SimpleDraweeView to have the image downloaded,
 * cached...
 */
public class ImageListAdapter extends ArrayAdapter<Uri>{

    //TODO: look at which size actually is appropriate.
    private static final String size = "w500";
    private final String LOG_TAG = this.getClass().getSimpleName();

    public ImageListAdapter(Context context, int resource, ArrayList<Uri> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleDraweeView draweeView;
        if (convertView != null) {
            draweeView = (SimpleDraweeView) convertView;
        }
        else {
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            draweeView = (SimpleDraweeView) inflater.inflate(R.layout.grid_item, parent, false);
        }
        draweeView.setImageURI(this.getItem(position));
        return draweeView;
    }



}
