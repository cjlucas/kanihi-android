package net.cjlucas.kanihi.data.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.data.DataService;

import java.util.concurrent.Callable;

/**
 * Created by chris on 6/2/14.
 */
public class DataServiceLoader<T> extends AsyncTaskLoader<T>
    implements DataService.Observer {
    private static final String TAG = "DataServiceLoader";

    protected DataService mDataService;
    protected T mCache;
    protected Callable<T> mCallable;

    public DataServiceLoader(Context context, DataService dataService,
                             Callable<T> callable) {
        super(context);
        mDataService = dataService;
        mDataService.registerObserver(this);
        mCallable = callable;
    }

    @Override
    public void onCanceled(T data) {
        super.onCanceled(data);
        Log.d(TAG, "onCanceled");
    }

    @Override
    public T loadInBackground() {
        Log.d(TAG, "loadInBackground");

        try {
            long start = System.currentTimeMillis();
            mCache = mCallable.call();
            Log.d(TAG, "Callable took: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
            return mCache;
        } catch (Exception e) {
            Log.e(TAG, "Callable executed unsuccessfully", e);
            return null;
        }
    }

    @Override
    public void deliverResult(T data) {
        Log.d(TAG, "deliverResult");
        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {
        Log.d(TAG, "onStartLoading");
        super.onStartLoading();
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        Log.d(TAG, "onStopLoading");
        super.onStopLoading();
        mDataService.unregisterObserver(this);
    }

    @Override
    protected void onReset() {
        Log.d(TAG, "onReset");
        super.onReset();
    }

    @Override
    public void onDatabaseUpdated() {
        Log.d(TAG, "onDatabaseUpdated");
        onContentChanged();
    }
}
