package com.example.laurent.popularmovies;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String DETAIL_EXTRAS_ID = "DETAIL_ID";
    public static String API_KEY;
    public boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        API_KEY = this.getString(R.string.api_key_tmdb);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, String.format("API KEY IS: %s", API_KEY));

        // Check if we have a 2-pane layout
        if (findViewById(R.id.fragment_detail_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_detail_container, new DetailActivityFragment(), DetailActivityFragment.DETAIL_FRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            // Other specific tweaks we may want to have here.
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // CursorAdapter returns a cursor at the correct position for getItem(), or null
        // if it cannot seek to that position.
        Log.v(LOG_TAG, "in onItemClick");
        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        if (cursor != null) {
            Bundle args = new Bundle();
            args.putInt(DETAIL_EXTRAS_ID, cursor.getInt(MainActivityFragment.COL_MOVIE_ID));
            if (mTwoPane) {
                // We replace the fragment using a fragment transaction.
                // We can't reuse our previous fragment (we could find it using its tag) since arguments can be set only on non-active fragments.
//                Fragment fragmentDetail = getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT_TAG);
                Fragment fragmentDetail = new DetailActivityFragment();
                fragmentDetail.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_detail_container, fragmentDetail, DetailActivityFragment.DETAIL_FRAGMENT_TAG)
                        .commit();
            } else {
                startActivity(new Intent(this, DetailActivity.class).putExtras(args));
            }
        }
    }
}

