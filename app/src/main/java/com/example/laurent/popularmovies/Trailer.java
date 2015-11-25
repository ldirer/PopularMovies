package com.example.laurent.popularmovies;

import android.net.Uri;

import java.net.URL;

public class Trailer {
    public String name;
    public Uri uri;
    public String key;

    public Trailer(String name, String key) {
        this.name = name;
        this.key = key;
        this.uri = Utility.buildYoutubeUriFromKey(key);
    }

}
