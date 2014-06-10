package net.cjlucas.kanihi.data;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.boombox.Boombox;
import net.cjlucas.boombox.BoomboxInfoListener;
import net.cjlucas.boombox.provider.AudioDataProvider;
import net.cjlucas.boombox.provider.HttpAudioDataProvider;
import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.connectors.DataServiceConnector;
import net.cjlucas.kanihi.data.connectors.ImageServiceConnector;
import net.cjlucas.kanihi.models.Image;
import net.cjlucas.kanihi.models.Track;
import net.cjlucas.kanihi.utils.DataUtils;

import java.util.ArrayList;
import java.util.List;

public class BoomboxService extends Service
        implements AudioManager.OnAudioFocusChangeListener, BoomboxInfoListener,
        DataServiceConnector.Listener, ImageServiceConnector.Listener {
    private static final String TAG = "BoomboxService";
    private static final int NOTIFICATION_ID = 542355243;
    private static final String NOTIFICATION_PREV_ACTION = "net.cjlucas.kanihi.notification.prev";
    private static final String NOTIFICATION_NEXT_ACTION = "net.cjlucas.kanihi.notification.next";
    private static final String NOTIFICATION_PLAY_PAUSE_ACTION = "net.cjlucas.kanihi.notification.play_pause";

    private Boombox mBoombox;
    private ApiHttpClient mApiHttpClient;
    private DataService mDataService;
    private ImageStore mImageService;
    private NotificationManager mNotificationManager;
    private BroadcastReceiver mBroadcastReceiver;
    private IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BoomboxService getService() {
            return BoomboxService.this;
        }
    }

    private class BoomboxBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case NOTIFICATION_PLAY_PAUSE_ACTION:
                    Track track = getTrack(mBoombox.getCurrentProvider());
                    updateNotification(track, null, mBoombox.hasPrevious(),
                            mBoombox.hasNext(), !mBoombox.isPlaying());
                    mBoombox.togglePlayPause();
                    break;
                case NOTIFICATION_PREV_ACTION:
                    mBoombox.playPrevious();
                    break;
                case NOTIFICATION_NEXT_ACTION:
                    mBoombox.playNext();
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        mApiHttpClient = new ApiHttpClient();
        mApiHttpClient.setApiEndpoint("192.168.0.2", 8080);

        mBoombox = new Boombox();
        mBoombox.registerInfoListener(this);
        mBoombox.start();

        mBroadcastReceiver = new BoomboxBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_NEXT_ACTION);
        filter.addAction(NOTIFICATION_PREV_ACTION);
        filter.addAction(NOTIFICATION_PLAY_PAUSE_ACTION);
        registerReceiver(mBroadcastReceiver, filter);


        DataServiceConnector.connect(getApplicationContext(), this);
        ImageServiceConnector.connect(getApplicationContext(), this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mBoombox.release();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange: " + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            mBoombox.pause();
        }
    }

    @Override
    public void onPlaybackStart(Boombox boombox, AudioDataProvider provider) {
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        updateNotification(getTrack(provider), mBoombox.hasPrevious(),
                mBoombox.hasNext(), mBoombox.isPlaying());
    }

    @Override
    public void onPlaybackCompletion(Boombox boombox, AudioDataProvider completedProvider, AudioDataProvider nextProvider) {

    }

    @Override
    public void onPlaylistCompletion(Boombox boombox) {
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        am.abandonAudioFocus(this);

        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onBufferingStart(Boombox boombox, AudioDataProvider provider) {

    }

    @Override
    public void onBufferingEnd(Boombox boombox, AudioDataProvider provider) {

    }

    @Override
    public void onBufferingUpdate(Boombox boombox, AudioDataProvider provider, int percentComplete) {

    }

    @Override
    public void onDataServiceConnected(DataService dataService) {
        mDataService = dataService;
    }

    @Override
    public void onDataServiceDisconnected() {
        mDataService = null;
    }

    @Override
    public void onImageServiceConnected(ImageStore dataService) {
        mImageService = dataService;
    }

    @Override
    public void onImageServiceDisconnected() {
        mImageService = null;
    }

    public Boombox getBoombox() {
        return mBoombox;
    }

    private void addTrack(Track track, boolean startPlaying) {
        AudioDataProvider provider = new HttpAudioDataProvider(
                mApiHttpClient.getStreamUrl(track), track);

        mDataService.refreshTrack(track);
        mBoombox.addProvider(provider);
        if (startPlaying) {
            mBoombox.play();
        }
    }

    public void addTracks(final List<Track> tracks, final boolean startPlaying) {
        mBoombox.reset();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for (Track track : tracks.subList(0, tracks.size() - 2)) {
                    addTrack(track, false);
                }

                addTrack(tracks.get(tracks.size() - 1), startPlaying);
                return null;
            }
        }.execute();

    }

    public void addTracks(final CloseableIterator<Track> tracks, final boolean startPlaying) {
        addTracks(DataUtils.getList(tracks), startPlaying);
    }

    public Track getTrack(AudioDataProvider provider) {
        return (Track)provider.getId();
    }

    public List<Track> getPlaylist() {
        List<Track> playlist = new ArrayList<>();
        for (AudioDataProvider provider : mBoombox.getPlaylist()) {
            playlist.add((Track)provider.getId());
        }

        return playlist;
    }

    private void updateNotification(Track track, final Bitmap image, boolean hasPrev,
                                    boolean hasNext, boolean isPlaying) {
        Notification.Builder builder = new Notification.Builder(BoomboxService.this)
                .setSmallIcon(R.drawable.microphone)
                .setContentTitle(track.getTitle())
                .setContentText(track.getTrackArtist().getName())
                .setSubText(track.getDisc().getAlbum().getTitle())
                .setOngoing(true);

        PendingIntent pendingIntent;

        if (hasPrev) {
            pendingIntent = PendingIntent.getBroadcast(BoomboxService.this, 0,
                    new Intent(NOTIFICATION_PREV_ACTION), 0);
            builder.addAction(R.drawable.prev, "Previous", pendingIntent);
        }

        int actionIcon;
        String actionText;
        if (isPlaying) {
            actionIcon = R.drawable.pause;
            actionText = "Pause";
        } else {
            actionIcon = R.drawable.play;
            actionText = "Play";
        }
        pendingIntent = PendingIntent.getBroadcast(BoomboxService.this, 0,
                new Intent(NOTIFICATION_PLAY_PAUSE_ACTION), 0);
        builder.addAction(actionIcon, actionText, pendingIntent);

        if (hasNext) {
            pendingIntent = PendingIntent.getBroadcast(BoomboxService.this, 0,
                    new Intent(NOTIFICATION_NEXT_ACTION), 0);
            builder.addAction(R.drawable.next, "Next", pendingIntent);
        }

        if (image != null)
            builder.setLargeIcon(image);

        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void updateNotification(final Track track, final boolean hasPrev,
                                    final boolean hasNext, final boolean isPlaying) {
        // update notification immediately w/o image,
        // then update notification again after async image fetch
        updateNotification(track, null, hasPrev, hasNext, isPlaying);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<Image> images = mDataService.getTrackImages(track);
                if (images.size() > 0) {
                    mImageService.loadImage(images.get(0), null, ImageStore.ImageType.THUMBNAIL,
                            new ImageStore.Callback() {
                                @Override
                                public void onImageAvailable(ImageView imageView, Drawable drawable) {
                                    Bitmap scaled = mImageService.resizeBitmap(
                                            ((BitmapDrawable)drawable).getBitmap(),
                                            getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                            getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height));
                                    updateNotification(track, scaled, hasPrev, hasNext, isPlaying);
                                }
                            });
                }
                return null;
            }
        }.execute();
    }
}