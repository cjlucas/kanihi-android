package net.cjlucas.kanihi.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.loopj.android.http.RequestHandle;

import net.cjlucas.kanihi.api.ApiHttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageStore {
    private static ImageStore mSharedImageStore;

    private Context mContext;
    private Map<ImageView, RequestHandle> mRequestHandleMap;

    public interface Callback {
        void onImageAvailable(ImageView imageView, Drawable drawable);
    }

    public static synchronized void setupInstance(Context context) {
        if (mSharedImageStore == null) {
            mSharedImageStore = new ImageStore(context);
        }
    }

    private ImageStore(Context context) {
        mContext = context;
        mRequestHandleMap = new ConcurrentHashMap<>();
    }

    private static ImageStore getInstance() {
        return mSharedImageStore;
    }

    private void cancelRequest(ImageView key) {
        RequestHandle req = mRequestHandleMap.get(key);
        if (req != null) {
            mRequestHandleMap.remove(key);
            req.cancel(false);
        }
    }

    public static void cancelAllRequests() {
        synchronized (getInstance().mRequestHandleMap) {
            for (ImageView key : getInstance().mRequestHandleMap.keySet()) {
                getInstance().cancelRequest(key);
            }
        }
    }

    public static void getImage(ImageView imageView, final Callback callback) {
        ImageStore imageStore = getInstance();
        String imageId = "500"; // TODO: change when Image model is implemented

        RequestHandle req = ApiHttpClient.getImage(imageId,
                new ApiHttpClient.Callback<byte[]>() {
                    @Override
                    public void onSuccess(byte[] data) {
                        // TODO: write the data to disk, create the drawable, call the callback
                    }

                    @Override
                    public void onFailure() {

                    }
                });

        getInstance().mRequestHandleMap.put(imageView, req);
    }
}
