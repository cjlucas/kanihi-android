package net.cjlucas.kanihi.data.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.model.Track;

import java.sql.SQLException;

public class ModelAdapter<E> extends BaseAdapter {
    private Activity mActivity;
    private CloseableIterator<E> mIterator;
    private AndroidDatabaseResults mDbResults;

    public ModelAdapter(Activity activity, CloseableIterator<E> iterator) {
        super();
        mActivity = activity;
        mIterator = iterator;
        mDbResults = (AndroidDatabaseResults)mIterator.getRawResults();
    }

    @Override
    public int getCount() {
        return mDbResults.getCount();
    }

    @Override
    public Object getItem(int position) {
        mDbResults.moveAbsolute(position);
        try {
            return mIterator.current();
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }

        Track t = (Track)getItem(position);
        ((TextView)view.findViewById(android.R.id.text1)).setText(t.getTitle());
        return view;
    }
}
