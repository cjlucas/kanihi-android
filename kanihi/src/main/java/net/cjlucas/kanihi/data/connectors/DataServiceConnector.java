package net.cjlucas.kanihi.data.connectors;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import net.cjlucas.kanihi.data.DataService;

/**
 * Created by chris on 6/2/14.
 */
public class DataServiceConnector {
    public interface Listener {
        void onDataServiceConnected(DataService dataService);
        void onDataServiceDisconnected();
    }

    private Context mBindingContext;
    private Listener mListener;

    private final ServiceConnection mDataStoreConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mListener.onDataServiceConnected(((DataService.LocalBinder) service).getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mListener.onDataServiceDisconnected();
        }
    };

    public static void connect(Context bindingContext, Listener listener) {
        new DataServiceConnector(bindingContext, listener).connect();
    }

    public DataServiceConnector(Context bindingContext, Listener listener) {
        mBindingContext = bindingContext;
        mListener = listener;
    }

    public void connect() {
        mBindingContext.bindService(new Intent(mBindingContext, DataService.class),
                mDataStoreConnection, 0);
    }

}
