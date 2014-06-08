package net.cjlucas.kanihi.data;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.loopj.android.http.RequestHandle;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.models.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageStore extends Service {
    private static final String LARGE_DIR = "images";
    private static final String THUMB_DIR = "thumbs";
    private static final String TAG = "ImageStore";
    private static final int THUMBNAIL_CACHE_SIZE = 128 * 1024;

    private Context mContext;
    private ApiHttpClient mApiHttpClient;
    private Map<ImageView, RequestHandle> mRequestHandleMap;
    private LruCache<String, Drawable> mThumbnailCache;
    private IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ImageStore getService() {
            return ImageStore.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    public interface Callback {
        void onImageAvailable(ImageView imageView, Drawable drawable);
    }

    public interface NewCallback<T> {
        void onImageAvailable(T image);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRequestHandleMap = new ConcurrentHashMap<>();
        mThumbnailCache = new LruCache<>(THUMBNAIL_CACHE_SIZE);
        mApiHttpClient = new ApiHttpClient();
        mApiHttpClient.setApiEndpoint("192.168.0.2", 8080);

        createDirIfNotExists(getImagesDir());
        createDirIfNotExists(getThumbsDir());
    }

    private void createDirIfNotExists(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Could not mkdirs: " + dir);
        }
    }

    private File getImagesDir() {
        return new File(getCacheDir(), LARGE_DIR);
    }

    private File getThumbsDir() {
        return new File(getCacheDir(), THUMB_DIR);
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

    private void cancelRequest(ImageView key) {
        RequestHandle req = mRequestHandleMap.get(key);
        if (req != null) {
            Log.v(TAG, "cancelling pending image request");
            mRequestHandleMap.remove(key);
            req.cancel(false);
        }
    }

    public void cancelAllRequests() {
        synchronized (mRequestHandleMap) {
            for (ImageView key : mRequestHandleMap.keySet()) {
                cancelRequest(key);
            }
        }
    }

    private Drawable getDrawable(File f) {
        if (!f.exists())
            throw new RuntimeException("getDrawable: File doesn't exist");

        return Drawable.createFromPath(f.getAbsolutePath());
    }

    public void getBitmap(final Image image, final NewCallback<Bitmap> callback) {
        File imagePath = getPath(image, false);

        if (imagePath.exists()) {
            callback.onImageAvailable(BitmapFactory.decodeFile(imagePath.getAbsolutePath()));
            return;
        }

        mApiHttpClient.getImage(image.getId(), -1, new ApiHttpClient.Callback<byte[]>() {
            @Override
            public void onSuccess(byte[] data) {
                writeImage(image, data, false);
                callback.onImageAvailable(BitmapFactory.decodeByteArray(data, 0, data.length));
            }

            @Override
            public void onFailure() {

            }
        });
    }

    public void loadImage(final Image image, final boolean thumbnail) {

    }

    public void loadImage(final Image image, final ImageView imageView, final boolean thumbnail,
                          final Callback callback) {
        // cancel image request on this view if one is pending
        cancelRequest(imageView);

        if (thumbnail && mThumbnailCache.get(image.getId()) != null) {
            callback.onImageAvailable(imageView, mThumbnailCache.get(image.getId()));
            return;
        }

        final File imagePath = getPath(image, thumbnail);
        if (imagePath.exists()) {
            Log.d(TAG, "loadImage: loading from disk");
            callback.onImageAvailable(imageView, getDrawable(imagePath));
            return;
        }

        Log.d(TAG, "loadImage: loading from API");

        RequestHandle req = mApiHttpClient.getImage(image.getId(), thumbnail ? imageView.getWidth() : -1,
                new ApiHttpClient.Callback<byte[]>() {
                    @Override
                    public void onSuccess(byte[] data) {
                        Drawable drawable = new BitmapDrawable(getResources(),
                                BitmapFactory.decodeByteArray(data, 0, data.length));
                        mThumbnailCache.put(image.getId(), drawable);
                        writeImage(image, data, thumbnail);
                        callback.onImageAvailable(imageView, drawable);
                    }

                    @Override
                    public void onFailure() {

                    }
                });

        mRequestHandleMap.put(imageView, req);
    }
}
