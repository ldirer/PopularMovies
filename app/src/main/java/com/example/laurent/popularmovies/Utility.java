package com.example.laurent.popularmovies;

import android.net.Uri;
import android.support.v4.app.Fragment;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;

public class Utility {

    static Uri buildYoutubeUriFromKey(String key) {
        return Uri.parse(String.format("https://www.youtube.com/watch?v=%s", key));
    }

    public static Trailer getDummyTrailer() {
        return new Trailer("name_bouga", "je73b_9JdR0");
    }

    /**
     * Dichotomic search to find a text size that fits.
     * Adapted from:
      http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview
     *
     * @param fragment
     * @param view
     * @return A text size that works (in pixels).
     */
    static float findRightTextSize(Fragment fragment, TextView view) {
        String text = (String) view.getText();
        int textWidth = view.getWidth();
        int targetWidth = textWidth - view.getPaddingLeft() - view.getPaddingRight();
        // We don't want to use view.getTextSize() because repeated calls would gradually shrink the text!
        // float hi = view.getTextSize();

        // Get view height in dp:  http://stackoverflow.com/questions/4605527/converting-pixels-to-dp
        DisplayMetrics metrics = fragment.getResources().getDisplayMetrics();
        float dpHeight = view.getHeight() / (metrics.densityDpi / 160f);
        // We take the max so that the text fits on one line and does not overflow vertically.
        float hi = Math.min(fragment.getResources().getInteger(R.integer.detail_max_title_font), dpHeight);

        float lo = 10;
        final float threshold = 2f; // How close we have to be

        TextPaint testPaint = new TextPaint();
        testPaint.set(view.getPaint());

        while ((hi - lo) > threshold) {
            float size = (hi + lo) / 2;
            testPaint.setTextSize(size);
            // For some reason if we target exactly targetWidth the text still does not fit on a line.
            if (testPaint.measureText(text) >= targetWidth / 1.5)
                hi = size; // too big
            else
                lo = size; // too small
        }
        // Use lo so that we undershoot rather than overshoot
        return lo;
    }
}
