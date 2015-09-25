package com.example.laurent.popularmovies;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class FetchMoviePosterTask extends AsyncTask<String, Void, Bitmap> {
    private ImageView bmImage;
    private final String LOG_TAG = this.getClass().getSimpleName();
    private final String size = "w500";

    public FetchMoviePosterTask(ImageView imageView) {
        bmImage = imageView;
    }
//    private final String baseUri = "http://image.tmdb.org/t/p/w500";

    @Override
    protected Bitmap doInBackground(String... params) {
        String baseUri = new FetchMovieConfiguration().getImageBaseUri();
        String posterHash = params[0];
        posterHash = posterHash.replaceAll("^/", "");
//        String posterHash = "8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg";
        Uri posterUri = Uri.parse(baseUri).buildUpon()
                .appendPath(size)
                .appendEncodedPath(posterHash)
                .build();
        Log.d(LOG_TAG, String.format("Poster Uri: %s", posterUri.toString()));

        HttpURLConnection urlConnection = null;
//        BufferedReader reader = null;
        Bitmap posterBitmap = null;
        try {
            URL posterUrl = new URL(posterUri.toString());
            urlConnection = (HttpURLConnection)posterUrl.openConnection();
            urlConnection.setRequestMethod("GET");

            InputStream in = posterUrl.openStream();
            posterBitmap = BitmapFactory.decodeStream(in);

//            ByteArrayInputStream inputStream = (ByteArrayInputStream) urlConnection.getInputStream();
//            BufferedImage blah = new BufferedImage();
//            ByteBuffer buffer = new ByteBu
//            StringBuffer buffer = new StringBuffer();

//            reader = new BufferedReader(new InputStreamReader(inputStream));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return posterBitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        bmImage.setImageBitmap(bitmap);
    }
}
