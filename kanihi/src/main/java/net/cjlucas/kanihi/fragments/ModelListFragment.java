package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.CloseableIteratorAsyncLoader;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.data.adapters.RowViewAdapter;

public abstract class ModelListFragment<E> extends ListFragment
        implements RowViewAdapter<E>, LoaderManager.LoaderCallbacks<CloseableIterator<E>> {

    public static final String ARG_TOKEN = "token";
    private int mToken;

    protected DataStore mDataStore;

    @Override
    public Loader<CloseableIterator<E>> onCreateLoader(int id, Bundle args) {
        return new CloseableIteratorAsyncLoader<>(getActivity(), mDataStore,
                getGenericClass(), getDefaultQuery());
    }

    @Override
    public void onLoadFinished(Loader<CloseableIterator<E>> loader,
                               final CloseableIterator<E> iterator) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setListAdapter(new ModelAdapter<>(ModelListFragment.this, iterator));
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<CloseableIterator<E>> loader) {
        // TODO: figure out what to do here
    }

    protected static class ImageAttacher {
        public static void attach(Activity activity,
                                  final ImageView imageView, final Drawable drawable) {
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageDrawable(drawable);
                    }
                });
            }
        }
    }

    public ModelListFragment(DataStore dataStore) {
        mDataStore = dataStore;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.model_list_view, container, false);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (getLoaderManager() == null) return;
        getLoaderManager().initLoader(1, null ,this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void blah(Fragment fragment) {

        getFragmentManager().beginTransaction()
                .addToBackStack(null).replace(getId(), fragment).commit();
    }

    public abstract Class<E> getGenericClass();
    public abstract PreparedQuery<E> getDefaultQuery();
}