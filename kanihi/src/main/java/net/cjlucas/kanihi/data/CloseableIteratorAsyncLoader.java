package net.cjlucas.kanihi.data;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.models.Track;

/**
 * Created by chris on 5/29/14.
 */
public class CloseableIteratorAsyncLoader<T> extends AsyncTaskLoader<CloseableIterator<T>>
    implements DataStore.Observer {
    private static final String TAG = "CloseableIteratorAsyncLoader";

    private Class<T> mClazz;
    private PreparedQuery<T> mPreparedQuery;
    private DataStore mDataStore;
    private CloseableIterator<T> mIterator;

    public CloseableIteratorAsyncLoader(Context context, DataStore dataStore,
                                        Class<T> clazz, PreparedQuery<T> preparedQuery) {
        super(context);
        mDataStore = dataStore;
        mDataStore.registerObserver(this);
        mClazz = clazz;
        mPreparedQuery = preparedQuery;
    }

    @Override
    public void onCanceled(CloseableIterator<T> data) {
        super.onCanceled(data);
        Log.d(TAG, "onCanceled");
    }

    @Override
    public CloseableIterator<T> loadInBackground() {
        Log.d(TAG, "loadInBackground");
        mIterator = mDataStore.executePreparedQuery(mClazz, mPreparedQuery);
        return mIterator;
    }

    @Override
    public void deliverResult(CloseableIterator<T> data) {
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

        if (mIterator != null) {
            mIterator.closeQuietly();
        }

        mDataStore.unregisterObserver(this);
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
