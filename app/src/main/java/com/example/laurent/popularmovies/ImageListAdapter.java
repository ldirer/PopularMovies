package com.example.laurent.popularmovies;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.zip.Inflater;

/**
 * I originally wanted to have an ArrayAdapter<Integer> here, taking the resource id of the image.
 * However we cant modify the resources folder at runtime (they are compiled).
 * Workaround: pass a url to the ArrayAdapter, launch a task to download and assign the image to the
 * view passed to the constructor of the task.
 */
public class ImageListAdapter extends ArrayAdapter<String>{

//    http://stackoverflow.com/questions/2752924/android-images-from-assets-folder-in-a-gridview?rq=1
    // Ultimately discarded, it feels wrong to be loading images here and not just assigning resources.

    public ImageListAdapter(Context context, int resource, ArrayList<String> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // The super method already recycles the view or creates a new one using the `resource` parameter from the constructor.
        // Conclusion: no need to inflate layouts.
        // Turns out we need to do it ourselves as the ArrayAdapter only handles TextViews (!!).
//        ImageView imageView = (ImageView)super.getView(position, convertView, parent);
        ImageView imageView;
        if (convertView != null) {
            imageView = (ImageView) convertView;
        }
        else {
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            imageView = (ImageView) inflater.inflate(R.layout.grid_item, parent, false);
        }
        new FetchMoviePosterTask(imageView).execute(this.getItem(position));
//        imageView.setImageResource(this.getItem(position));
        return imageView;
    }
}
