package net.cjlucas.kanihi.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.loopj.android.http.RequestHandle;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.model.Image;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageStore {
    private static ImageStore mSharedImageStore;
    private static final String LARGE_DIR = "images";
    private static final String THUMB_DIR = "thumbs";
    private static final String TAG = "ImageStore";
    private static final int THUMBNAIL_CACHE_SIZE = 128 * 1024;

    private Context mContext;
    private Map<ImageView, RequestHandle> mRequestHandleMap;
    private LruCache<String, Drawable> mThumbnailCache;

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
        mThumbnailCache = new LruCache<>(THUMBNAIL_CACHE_SIZE);

        File[] dirs = { getImagesDir(), getThumbsDir() };
        for (File dir : dirs) {
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Could not mkdirs: " + dir);
            }
        }
    }

    private static ImageStore getInstance() {
        return mSharedImageStore;
    }

    private File getImagesDir() {
        return new File(mContext.getCacheDir(), LARGE_DIR);
    }

    private File getThumbsDir() {
        return new File(mContext.getCacheDir(), THUMB_DIR);
    }

    private File getPath(Image image, boolean thumbnail) {
        File dir = thumbnail ? getThumbsDir() : getImagesDir();
        return new File(dir, image.getId());
    }

    private boolean writeImage(Image image, byte[] data, boolean thumbnail) {
        try {
            FileOutputStream fos = new FileOutputStream(getPath(image, thumbnail));
            fos.write(data);
            fos.close();

            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not write e to disk", e);
        } catch(IOException e) {
            Log.e(TAG, "Could not write data to disk", e);
        }

        return false;
    }

    private Drawable getDrawable(File f) {
        if (!f.exists()) throw new RuntimeException("getDrawable: File doesn't exist");

        return Drawable.createFromPath(f.getAbsolutePath());
    }

    private void cancelRequest(ImageView key) {
        RequestHandle req = mRequestHandleMap.get(key);
        if (req != null) {
            Log.v(TAG, "cancelling pending image request");
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

    public static void loadImage(final Image image,
                                 final ImageView imageView,
                                 final boolean thumbnail,
                                 final Callback callback) {
        ImageStore imageStore = getInstance();

        // cancel image request on this view if one is pending
        imageStore.cancelRequest(imageView);

        final File imagePath = imageStore.getPath(image, thumbnail);
        if (imagePath.exists()) {
            callback.onImageAvailable(imageView, imageStore.getDrawable(imagePath));
            return;
        }

        RequestHandle req = ApiHttpClient.getImage(image.getId(), thumbnail ? imageView.getWidth() : -1,
                new ApiHttpClient.Callback<byte[]>() {
                    @Override
                    public void onSuccess(byte[] data) {
                        ImageStore imageStore = getInstance();
                        imageStore.writeImage(image, data, thumbnail);
                        callback.onImageAvailable(imageView, imageStore.getDrawable(imagePath));
                    }

                    @Override
                    public void onFailure() {

                    }
                });

        imageStore.mRequestHandleMap.put(imageView, req);
    }
}
