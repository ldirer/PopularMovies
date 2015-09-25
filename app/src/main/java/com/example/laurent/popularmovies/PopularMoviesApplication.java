package com.example.laurent.popularmovies;

import android.app.Application;
import com.facebook.drawee.backends.pipeline.Fresco;

/**
 * Created by laurent on 9/25/15.
 */
public class PopularMoviesApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(getApplicationContext());
    }
}
