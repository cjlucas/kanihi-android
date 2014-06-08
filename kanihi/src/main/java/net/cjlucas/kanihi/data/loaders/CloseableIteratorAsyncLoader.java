package net.cjlucas.kanihi.data.loaders;

import android.content.Context;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.data.DataService;

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

        if (mCache != null) {
            mCache.closeQuietly();
            mCache = null;
        }
    }
}

