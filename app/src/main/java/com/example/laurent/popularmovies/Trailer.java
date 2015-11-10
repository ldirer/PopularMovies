package com.example.laurent.popularmovies;

import android.net.Uri;

import java.net.URL;

public class Trailer {
    public String name;
    public Uri uri;

    public Trailer(String name, String key) {
//        TODO: super() or not super() for simple objects?
        this.name = name;
        this.uri = Utility.buildYoutubeUriFromKey(key);
    }

}
