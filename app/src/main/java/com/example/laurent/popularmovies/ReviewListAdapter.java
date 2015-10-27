package com.example.laurent.popularmovies;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ReviewListAdapter extends BaseAdapter {

    private static final String LOG_TAG = ReviewListAdapter.class.getSimpleName();
    private ArrayList<Review> reviewList;
    private Context mContext;

    public ReviewListAdapter(Context context, ArrayList<Review> reviewList) {
        super();
        this.mContext = context;
        this.reviewList = reviewList;
    }

    @Override
    public int getCount() {
        Log.d(LOG_TAG, String.format("in getCount: result is %d", reviewList.size()));
        return reviewList.size();
    }

    @Override
    public Object getItem(int position) {
        Log.d(LOG_TAG, "in getItem");
        return reviewList.get(position);
    }

    @Override
    public long getItemId(int position) {
        Log.d(LOG_TAG, "in getItemId");
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(LOG_TAG, "in getView");
        View reviewView = convertView;
        ViewHolder viewHolder;
        if (reviewView == null) {
            // We need to create the view.
            viewHolder = new ViewHolder();
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            reviewView = li.inflate(R.layout.detail_review_item, parent, false);
            viewHolder.author_view = (TextView) reviewView.findViewById(R.id.detail_review_author);
            viewHolder.body_view = (TextView) reviewView.findViewById(R.id.detail_review_body);
            reviewView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) reviewView.getTag();
        }
        viewHolder.author_view.setText(reviewList.get(position).author);
        viewHolder.body_view.setText(reviewList.get(position).body);
        return reviewView;
    }

    public void addAll(List<Review> result) {
        Log.d(LOG_TAG, "in addAll");
        this.reviewList.addAll(result);
        this.notifyDataSetChanged();
    }

    private class ViewHolder {
        public TextView author_view;
        public TextView body_view;
    }
}
