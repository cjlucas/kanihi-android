package net.cjlucas.kanihi.fragments;

import android.app.ListActivity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.data.AsyncQueryMonitor;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.data.adapters.RowViewAdapter;

public abstract class ModelListFragment<E> extends ListFragment
        implements AsyncQueryMonitor.Listener<E>, RowViewAdapter<E> {

    public static final String ARG_TOKEN = "token";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(android.R.layout.list_content, container, false);

        DataStore dataStore = DataStore.setupInstance(getActivity());

        Bundle args = getArguments();
        int token;
        if (args.containsKey(ARG_TOKEN)) {
            token = args.getInt(ARG_TOKEN);
        } else {
            token = executeDefaultQuery();
        }

        dataStore.registerQueryMonitorListener(token, this);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
    }

    @Override
    public void onQueryComplete(final CloseableIterator<E> iterator) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setListAdapter(new ModelAdapter<>(ModelListFragment.this, iterator));
            }
        });
    }

    abstract int executeDefaultQuery();

}