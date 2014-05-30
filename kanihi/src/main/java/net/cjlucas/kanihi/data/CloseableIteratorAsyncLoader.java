package net.cjlucas.kanihi.data;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

/**
 * Created by chris on 5/29/14.
 */
public class CloseableIteratorAsyncLoader<T> extends AsyncTaskLoader<CloseableIterator<T>>
    implements DataService.Observer {
    private static final String TAG = "CloseableIteratorAsyncLoader";

    private Class<T> mClazz;
    private PreparedQuery<T> mPreparedQuery;
    private DataService mDataService;
    private CloseableIterator<T> mIterator;

    public CloseableIteratorAsyncLoader(Context context, DataService dataService,
                                        Class<T> clazz, PreparedQuery<T> preparedQuery) {
        super(context);
        mDataService = dataService;
        mDataService.registerObserver(this);
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
        mIterator = mDataService.executePreparedQuery(mClazz, mPreparedQuery);
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
