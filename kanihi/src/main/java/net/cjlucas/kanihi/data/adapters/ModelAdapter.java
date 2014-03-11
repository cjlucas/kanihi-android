package net.cjlucas.kanihi.data.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.activities.ModelListActivity;
import net.cjlucas.kanihi.model.Track;

import java.sql.SQLException;

public class ModelAdapter<E> extends BaseAdapter {
    private ModelListActivity<E> mActivity;
    private CloseableIterator<E> mIterator;
    private AndroidDatabaseResults mDbResults;

    public ModelAdapter(ModelListActivity<E> activity, CloseableIterator<E> iterator) {
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
    public E getItem(int position) {
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
        return mActivity.getRowView(getItem(position), view, viewGroup);
    }
}
