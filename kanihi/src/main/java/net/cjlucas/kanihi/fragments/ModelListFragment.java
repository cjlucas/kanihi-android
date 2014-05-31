package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.CloseableIteratorAsyncLoader;
import net.cjlucas.kanihi.data.DataService;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.data.adapters.RowViewAdapter;

public abstract class ModelListFragment<E> extends ListFragment
        implements RowViewAdapter<E>, LoaderManager.LoaderCallbacks<CloseableIterator<E>> {
    protected static final String ARG_ANCESTOR_CLASS    = "ANCESTOR_CLASS";
    protected static final String ARG_ANCESTOR_UUID     = "ANCESTOR_UUID";

    protected DataService mDataService;
    protected ImageStore mImageStore;

    private final ServiceConnection mImageStoreConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("whatever", "imageStore bound");
            mImageStore = ((ImageStore.LocalBinder)service).getService();
//            onImageServiceConnection();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mImageStore = null;
        }
    };

    private final ServiceConnection mDataStoreConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("whatever", "dataStore bound");
            mDataService = ((DataService.LocalBinder)service).getService();
//            onDataServiceConnection();

            if (getLoaderManager() != null) {
                getLoaderManager().initLoader(1, null, ModelListFragment.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDataService = null;
        }
    };

    @Override
    public Loader<CloseableIterator<E>> onCreateLoader(int id, Bundle args) {
        Log.d("whatever", "onCreate Loader dataStore: " + mDataService);

        Class ancestorClazz = null;
        String ancestorUuid = null;
        Bundle fragmentArgs = getArguments();

        if (fragmentArgs != null) {
            ancestorClazz = (Class)fragmentArgs.getSerializable(ARG_ANCESTOR_CLASS);
            ancestorUuid = fragmentArgs.getString(ARG_ANCESTOR_UUID);
        }

        return new CloseableIteratorAsyncLoader<>(getActivity(), mDataService,
                getGenericClass(), getDefaultQuery(ancestorClazz, ancestorUuid));
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

        Context appContext = activity.getApplicationContext();
        Log.d("whatever", "here");
        if (appContext != null) {
            appContext.bindService(new Intent(appContext, ImageStore.class),
                    mImageStoreConnection, 0);
            appContext.bindService(new Intent(appContext, DataService.class),
                    mDataStoreConnection, 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void blah(Fragment fragment) {

        getFragmentManager().beginTransaction()
                .addToBackStack(null).replace(getId(), fragment).commit();
    }

//    public abstract void onImageServiceConnection();
//    public abstract void onDataServiceConnection();
    public abstract Class<E> getGenericClass();
    public abstract PreparedQuery<E> getDefaultQuery(Class ancestorClazz, String ancestorUuid);
}