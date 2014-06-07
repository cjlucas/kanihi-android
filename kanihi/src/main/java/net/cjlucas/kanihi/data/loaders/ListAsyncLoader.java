package net.cjlucas.kanihi.data.loaders;

import android.content.Context;

import net.cjlucas.kanihi.data.DataService;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by chris on 6/2/14.
 */
public class ListAsyncLoader<T> extends DataServiceLoader<List<T>> {
    public ListAsyncLoader(Context context, DataService dataService,
                                        Callable<List<T>> callable) {
        super(context, dataService, callable);
    }
}
