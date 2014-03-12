package net.cjlucas.kanihi.data;

import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncQueryMonitor {
    private static final String TAG = "AsyncQueryMonitor";

    private ThreadPoolExecutor mMonitors;
    private Map<Integer, CloseableIterator<?>> mIteratorMap;
    private Map<Integer, Listener<?>> mListenerMap;

    public interface Listener<T> {
        void onQueryComplete(CloseableIterator<T> iterator);
    }

    public AsyncQueryMonitor() {
        mMonitors = new ThreadPoolExecutor(20, 50, 100, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(100));

        mIteratorMap = new ConcurrentHashMap<>();
        mListenerMap = new ConcurrentHashMap<>();
    }

    protected boolean isTokenUsed(int token) {
        return mIteratorMap.containsKey(token);
    }
    protected void putIterator(int token, CloseableIterator<?> iterator) {
        mIteratorMap.put(token, iterator);
    }

    public <T> void registerListener(Listener<T> listener, int token) {
        mListenerMap.put(token, listener);

        // don't waste cycles if iterator is already available
        CloseableIterator<T> iterator = (CloseableIterator<T>)mIteratorMap.get(token);
        if (iterator == null) {
            mMonitors.execute(new QueryMonitor<T>(token));
        } else {
            Log.v(TAG, "iterator was complete when listener registered");
            notifyListener(token, iterator);
        }
    }

    public void unregisterListener(int token) {
        mListenerMap.remove(token);
    }

    private <T> void notifyListener(int token, CloseableIterator<T> iterator) {
        Listener<T> listener = (Listener<T>)mListenerMap.get(token);
        if (listener != null) {
            listener.onQueryComplete(iterator);
            unregisterListener(token);
        }
    }

    protected void closeQuery(int token) {
        CloseableIterator iterator = mIteratorMap.get(token);
        if (iterator != null) {
            iterator.closeQuietly();
            mIteratorMap.remove(iterator);
        }

    }

    private class QueryMonitor<T> implements Runnable {
        private static final int SLEEP_INTERVAL_MS = 5;
        private int mToken;

        public QueryMonitor(int token) {
            mToken = token;
        }

        public void run() {
            CloseableIterator<T> iterator = null;
            long start = System.currentTimeMillis();
            do {
                System.err.println("running");
               iterator = (CloseableIterator<T>)mIteratorMap.get(mToken);
               sleep(SLEEP_INTERVAL_MS);
            } while (iterator == null);

            double duration = (System.currentTimeMillis() - start) / 1000.0;
            Log.v(TAG, "QueryMonitor waited for " + duration + " seconds");

            notifyListener(mToken, iterator);
        }

        private void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
