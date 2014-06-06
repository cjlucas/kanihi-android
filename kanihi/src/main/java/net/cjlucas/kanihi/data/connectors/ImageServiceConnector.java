package net.cjlucas.kanihi.data.connectors;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import net.cjlucas.kanihi.data.DataService;
import net.cjlucas.kanihi.data.ImageStore;

/**
 * Created by chris on 6/2/14.
 */
public class ImageServiceConnector {
    public interface Listener {
        void onImageServiceConnected(ImageStore dataService);
        void onImageServiceDisconnected();
    }

    private Context mBindingContext;
    private Listener mListener;

    private final ServiceConnection mDataStoreConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mListener.onImageServiceConnected(((ImageStore.LocalBinder) service).getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mListener.onImageServiceDisconnected();
        }
    };

    public static void connect(Context bindingContext, Listener listener) {
        new ImageServiceConnector(bindingContext, listener).connect();
    }

    public ImageServiceConnector(Context bindingContext, Listener listener) {
        mBindingContext = bindingContext;
        mListener = listener;
    }

    public void connect() {
        mBindingContext.bindService(new Intent(mBindingContext, ImageStore.class),
                mDataStoreConnection, 0);
    }

}
