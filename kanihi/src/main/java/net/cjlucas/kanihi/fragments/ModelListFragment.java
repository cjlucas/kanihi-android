package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.BoomboxService;
import net.cjlucas.kanihi.data.DataService;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.data.adapters.RowViewAdapter;
import net.cjlucas.kanihi.data.connectors.BoomboxServiceConnector;
import net.cjlucas.kanihi.data.connectors.DataServiceConnector;
import net.cjlucas.kanihi.data.connectors.ImageServiceConnector;
import net.cjlucas.kanihi.data.loaders.CloseableIteratorAsyncLoader;

import java.util.concurrent.Callable;

public abstract class ModelListFragment<E> extends ListFragment
        implements RowViewAdapter<E>, LoaderManager.LoaderCallbacks<CloseableIterator<E>>,
        DataServiceConnector.Listener, ImageServiceConnector.Listener,
        BoomboxServiceConnector.Listener {
    protected static final String ARG_ANCESTOR_CLASS    = "ANCESTOR_CLASS";
    protected static final String ARG_ANCESTOR_UUID     = "ANCESTOR_UUID";
    public static final String ARG_ALBUM_UUID = "ALBUM_UUID";

    protected DataService mDataService;
    protected ImageStore mImageStore;
    protected BoomboxService mBoomboxService;

    @Override
    public Loader<CloseableIterator<E>> onCreateLoader(int id, Bundle args) {
        Log.d("whatever", "onCreate Loader dataStore: " + mDataService);

        Bundle fragmentArgs = getArguments();

        final Class ancestorClazz = fragmentArgs != null
                ? (Class)fragmentArgs.getSerializable(ARG_ANCESTOR_CLASS) : null;
        final String ancestorUuid = fragmentArgs != null
                ? fragmentArgs.getString(ARG_ANCESTOR_UUID) : null;

        return new CloseableIteratorAsyncLoader<>(getActivity(), mDataService,
                new Callable<CloseableIterator<E>>() {
                    @Override
                    public CloseableIterator<E> call() throws Exception {
                        return mDataService.executePreparedQuery(getGenericClass(),
                                getDefaultQuery(ancestorClazz, ancestorUuid));
                    }
                });
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
        if (appContext != null) {
            DataServiceConnector.connect(appContext, this);
            ImageServiceConnector.connect(appContext, this);
            BoomboxServiceConnector.connect(appContext, this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDataServiceConnected(DataService dataService) {
        mDataService = dataService;
        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public void onDataServiceDisconnected() {
        mDataService = null;
    }

    @Override
    public void onImageServiceConnected(ImageStore dataService) {
        mImageStore = dataService;
    }

    @Override
    public void onImageServiceDisconnected() {
        mImageStore = null;
    }

    @Override
    public void onBoomboxServiceConnected(BoomboxService boomboxService) {
        mBoomboxService = boomboxService;
    }

    @Override
    public void onBoomboxServiceDisconnected() {
        mBoomboxService = null;

    }

    protected void onActionMenuButtonClicked() {
        Bundle args = new Bundle();
        args.putInt(MenuDialogFragment.ARG_ITEM_COUNT, 3);
        int[] blah = {R.drawable.jack, R.drawable.jack, R.drawable.jack};
        String[] omg = {"First Item", "Much Longer Item Than the Last One", "Third Item"};
        args.putIntArray(MenuDialogFragment.ARG_MENU_ITEM_RES_ID_ARRAY,blah);
        args.putStringArray(MenuDialogFragment.ARG_MENU_ITEM_TEXT_ARRAY, omg);

        DialogFragment dialogFragment = new MenuDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), null);
    }

    protected void blah(Fragment fragment) {

        getFragmentManager().beginTransaction()
                .addToBackStack(null)
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(getId(), fragment)
                .commit();
    }

    public abstract Class<E> getGenericClass();
    public abstract PreparedQuery<E> getDefaultQuery(Class ancestorClazz, String ancestorUuid);
}