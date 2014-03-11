package net.cjlucas.kanihi.activities;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.data.AsyncQueryMonitor;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.data.adapters.RowViewAdapter;

import java.sql.SQLException;

public abstract class ModelListActivity<E> extends ListActivity
        implements AsyncQueryMonitor.Listener<E>, RowViewAdapter<E> {

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(android.R.layout.list_content);

        DataStore dataStore = DataStore.setupInstance(this);

        Intent intent = getIntent();
        int token;

        if (intent.hasExtra("token")) {
            token = intent.getIntExtra("token", -1);
        } else {
            token = executeDefaultQuery();
        }

        dataStore.registerQueryMonitorListener(token, this);
    }

    @Override
    public void onQueryComplete(final CloseableIterator<E> iterator) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setListAdapter(new ModelAdapter<>(ModelListActivity.this, iterator));
            }
        });
    }

    abstract int executeDefaultQuery();
}
