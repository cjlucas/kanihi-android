package net.cjlucas.kanihi.fragments;

import android.app.Fragment;
import android.app.ListActivity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.AsyncQueryMonitor;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.data.adapters.RowViewAdapter;

public abstract class ModelListFragment<E> extends ListFragment
        implements AsyncQueryMonitor.Listener<E>, RowViewAdapter<E> {

    public static final String ARG_TOKEN = "token";
    private int mToken;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(android.R.layout.list_content, container, false);

        DataStore dataStore = DataStore.setupInstance(getActivity());

        Bundle args = getArguments();
        mToken = args != null && args.containsKey(ARG_TOKEN)
                ? args.getInt(ARG_TOKEN) : executeDefaultQuery();

        dataStore.registerQueryMonitorListener(mToken, this);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
    }

    @Override
    public void onQueryComplete(final CloseableIterator<E> iterator) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setListAdapter(new ModelAdapter<>(ModelListFragment.this, iterator));
            }
        });
    }

    protected Bundle bundleWithToken(int token) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_TOKEN, token);

        return bundle;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DataStore.getInstance().unregisterQueryMonitorListener(mToken);
        DataStore.getInstance().closeQuery(mToken);
    }

    protected void blah(Fragment fragment, int token) {
        fragment.setArguments(bundleWithToken(token));

        getFragmentManager().beginTransaction()
                .addToBackStack(null).replace(getId(), fragment).commit();
    }

    public abstract int executeDefaultQuery();
}