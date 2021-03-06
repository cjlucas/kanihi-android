package net.cjlucas.kanihi.data.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;

import java.sql.SQLException;

public class ModelAdapter<E> extends BaseAdapter {
    private RowViewAdapter<E> mRowViewAdapter;
    private CloseableIterator<E> mIterator;
    private AndroidDatabaseResults mDbResults;

    public ModelAdapter(RowViewAdapter<E> rowViewAdapter, CloseableIterator<E> iterator) {
        super();
        mRowViewAdapter = rowViewAdapter;
        mIterator = iterator;
        mDbResults = (AndroidDatabaseResults)mIterator.getRawResults();
    }

    @Override
    public int getCount() {
        return mDbResults.getCount();
    }

    @Override
    public Object getItem(int position) {
        return getModel(position);
    }

    public E getModel(int position) {
        mDbResults.moveAbsolute(position);
        try {
            return mIterator.current();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        return mRowViewAdapter.getRowView(getModel(position), view, viewGroup);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
