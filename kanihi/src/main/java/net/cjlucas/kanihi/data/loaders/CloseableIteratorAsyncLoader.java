package net.cjlucas.kanihi.data.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.data.DataService;

import java.io.Closeable;
import java.util.concurrent.Callable;

/**
 * Created by chris on 5/29/14.
 */
public class CloseableIteratorAsyncLoader<T> extends DataServiceLoader<CloseableIterator<T>> {

    public CloseableIteratorAsyncLoader(Context context, DataService dataService,
                                        Callable<CloseableIterator<T>> callable) {
        super(context, dataService, callable);
    }

    @Override
    protected void onReset() {
        super.onReset();

        if (mCache != null)
            mCache.closeQuietly();
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();

        if (mCache != null)
            mCache.closeQuietly();
    }
}

