package com.example.laurent.popularmovies;

        import android.content.Context;
        import android.content.Intent;
        import android.net.Uri;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.BaseAdapter;
        import android.widget.ListAdapter;
        import android.widget.TextView;

        import java.util.ArrayList;
        import java.util.List;

public class TrailerListAdapter extends BaseAdapter {

    private static final String LOG_TAG = TrailerListAdapter.class.getSimpleName();
    private ArrayList<Trailer> trailerList;
    private Context mContext;

    public TrailerListAdapter(Context context, ArrayList<Trailer> trailerList) {
        super();
        this.mContext = context;
        this.trailerList = trailerList;
    }

    @Override
    public int getCount() {
        Log.d(LOG_TAG, String.format("in getCount: result is %d", trailerList.size()));
        return trailerList.size();
    }

    @Override
    public Object getItem(int position) {
        Log.d(LOG_TAG, "in getItem");
        return trailerList.get(position);
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
        View trailerView = convertView;
        ViewHolder viewHolder;
        if (trailerView == null) {
            // We need to create the view.
            viewHolder = new ViewHolder();
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            trailerView = li.inflate(R.layout.detail_trailer_item, parent, false);
            viewHolder.name_view = (TextView) trailerView.findViewById(R.id.detail_trailer_name);
            trailerView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) trailerView.getTag();
        }
        viewHolder.name_view.setText(trailerList.get(position).name);

        return trailerView;
    }

    public void addAll(List<Trailer> result) {
        Log.d(LOG_TAG, "in addAll");
        this.trailerList.addAll(result);
        this.notifyDataSetChanged();
    }

    private class ViewHolder {
        public TextView name_view;
    }

}
