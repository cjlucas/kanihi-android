package net.cjlucas.kanihi.data;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
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
    private static final String BLURRED_DIR = "blurred";
    private static final String TAG = "ImageStore";
    private static final int THUMBNAIL_CACHE_SIZE = 128 * 1024;

    private Context mContext;
    private ApiHttpClient mApiHttpClient;
    private Map<ImageView, RequestHandle> mRequestHandleMap;
    private LruCache<String, Drawable> mThumbnailCache;
    private IBinder mBinder = new LocalBinder();

    public enum ImageType {
        FULL_SIZE, THUMBNAIL, BLURRED
    }

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

        for (ImageType imageType : ImageType.values()) {
            createDirIfNotExists(getPath(imageType));
        }
    }

    private void createDirIfNotExists(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Could not mkdirs: " + dir);
        }
    }

    private File getPath(ImageType imageType) {
        String relDir = null;
        switch(imageType) {
            case FULL_SIZE:
                relDir = LARGE_DIR;
                break;
            case THUMBNAIL:
                relDir = THUMB_DIR;
                break;
            case BLURRED:
                relDir = BLURRED_DIR;
                break;
        }
        return new File(getCacheDir(), relDir);
    }

    private File getPath(Image image, ImageType imageType) {
        return new File(getPath(imageType), image.getId());
    }

    private boolean writeImage(Image image, byte[] data, ImageType imageType) {
        try {
            FileOutputStream fos = new FileOutputStream(getPath(image, imageType));
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

    private boolean writeImage(Image image, Bitmap bitmap, ImageType imageType) {
        try {
            FileOutputStream fos = new FileOutputStream(getPath(image, imageType));
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
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
        File imagePath = getPath(image, ImageType.FULL_SIZE);

        if (imagePath.exists()) {
            callback.onImageAvailable(BitmapFactory.decodeFile(imagePath.getAbsolutePath()));
            return;
        }

        mApiHttpClient.getImage(image.getId(), -1, new ApiHttpClient.Callback<byte[]>() {
            @Override
            public void onSuccess(byte[] data) {
                writeImage(image, data, ImageType.FULL_SIZE);
                callback.onImageAvailable(BitmapFactory.decodeByteArray(data, 0, data.length));
            }

            @Override
            public void onFailure() {

            }
        });
    }

    public void loadImage(final Image image, final boolean thumbnail) {

    }

    public void loadImage(final Image image, final ImageView imageView, final ImageType imageType,
                          final Callback callback) {
        // cancel image request on this view if one is pending
        if (imageView != null)
            cancelRequest(imageView);

        if (imageType == ImageType.THUMBNAIL && mThumbnailCache.get(image.getId()) != null) {
            Log.d(TAG, "loadImage: loading from cache");
            callback.onImageAvailable(imageView, mThumbnailCache.get(image.getId()));
            return;
        }

        final File imagePath = getPath(image, imageType);
        if (imagePath.exists()) {
            Log.d(TAG, "loadImage: loading from disk");
            doInBackground(new Runnable() {
                @Override
                public void run() {
                    Drawable drawable = getDrawable(imagePath);
                    callback.onImageAvailable(imageView, drawable);

                    if (imageType == ImageType.THUMBNAIL) {
                        mThumbnailCache.put(image.getId(), drawable);
                    }
                }
            });
            return;
        }

        Log.d(TAG, "loadImage: loading from API");

        RequestHandle req = mApiHttpClient.getImage(image.getId(),
                imageType == ImageType.THUMBNAIL ? imageView.getWidth() : -1,
                new ApiHttpClient.Callback<byte[]>() {
                    @Override
                    public void onSuccess(final byte[] data) {
                        doInBackground(new Runnable() {
                            @Override
                            public void run() {
                                Drawable drawable = new BitmapDrawable(getResources(),
                                        BitmapFactory.decodeByteArray(data, 0, data.length));
                                if (imageType == ImageType.THUMBNAIL) {
                                    mThumbnailCache.put(image.getId(), drawable);
                                }

                                writeImage(image, data, imageType);
                                callback.onImageAvailable(imageView, drawable);
                            }
                        });
                    }

                    @Override
                    public void onFailure() {

                    }
                });

        if (imageView != null)
            mRequestHandleMap.put(imageView, req);
    }

    public void loadBlurredImage(final Image image, final Callback callback) {
        doInBackground(new Runnable() {
            @Override
            public void run() {
                File blurredImagePath = getPath(image, ImageType.BLURRED);
                Bitmap bitmap;
                if (!blurredImagePath.exists()) {
                    bitmap = renderBlurredBitmap(BitmapFactory.decodeFile(
                            getPath(image, ImageType.FULL_SIZE).getAbsolutePath()));
                    writeImage(image, bitmap, ImageType.BLURRED);
                } else {
                    bitmap = BitmapFactory.decodeFile(blurredImagePath.getAbsolutePath());
                }
                callback.onImageAvailable(null, new BitmapDrawable(getResources(), bitmap));
            }
        });
    }

    private Bitmap renderBlurredBitmap(Bitmap bitmap) {
        // TODO: don't hard code size
        Bitmap src = Bitmap.createScaledBitmap(bitmap, 500, 500, true);

        Bitmap outBitmap = src.copy(src.getConfig(), true);

        final RenderScript rs = RenderScript.create(this);
        final Allocation input = Allocation.createFromBitmap(rs, src);
        final Allocation output = Allocation.createFromBitmap(rs, outBitmap);

        final ScriptIntrinsicBlur script =
                ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(25f);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(outBitmap);

        rs.destroy();

        return outBitmap;
    }

    private void doInBackground(final Runnable runnable) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                runnable.run();
                return null;
            }
        }.execute();
    }

    public Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        int width, height;

        if (maxWidth > maxHeight) {
            width = maxWidth;
            height = (int)(original.getHeight() * (width * 1.0 / original.getWidth()));
        } else {
            height = maxHeight;
            width = (int)(original.getWidth() * (height * 1.0 / original.getHeight()));
        }
        return Bitmap.createScaledBitmap(original, width, height, false);
    }
}
