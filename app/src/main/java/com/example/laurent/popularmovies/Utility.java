package com.example.laurent.popularmovies;

import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URL;

public class Utility {

    static Uri buildYoutubeUriFromKey(String key) {
        return Uri.parse(String.format("https://www.youtube.com/watch?v=%s", key));
    }

    public static Trailer getDummyTrailer() {
        return new Trailer("name_bouga", "je73b_9JdR0");
    }
}
