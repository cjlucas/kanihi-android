package net.cjlucas.kanihi.data.connectors;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import net.cjlucas.kanihi.data.BoomboxService;

/**
 * Created by chris on 6/2/14.
 */
public class BoomboxServiceConnector {
    public interface Listener {
        void onBoomboxServiceConnected(BoomboxService boomboxService);
        void onBoomboxServiceDisconnected();
    }

    private Context mBindingContext;
    private Listener mListener;

    private final ServiceConnection mBoomboxServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mListener.onBoomboxServiceConnected(((BoomboxService.LocalBinder) service).getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mListener.onBoomboxServiceDisconnected();
        }
    };

    public static void connect(Context bindingContext, Listener listener) {
        new BoomboxServiceConnector(bindingContext, listener).connect();
    }

    public BoomboxServiceConnector(Context bindingContext, Listener listener) {
        mBindingContext = bindingContext;
        mListener = listener;
    }

    public void connect() {
        mBindingContext.bindService(new Intent(mBindingContext, BoomboxService.class),
                mBoomboxServiceConnection, 0);
    }

}
