package net.cjlucas.kanihi.data;

import android.app.Service;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.boombox.Boombox;
import net.cjlucas.boombox.BoomboxInfoListener;
import net.cjlucas.boombox.provider.AudioDataProvider;
import net.cjlucas.boombox.provider.HttpAudioDataProvider;
import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.connectors.DataServiceConnector;
import net.cjlucas.kanihi.models.Track;
import net.cjlucas.kanihi.utils.DataUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BoomboxService extends Service
        implements AudioManager.OnAudioFocusChangeListener, BoomboxInfoListener,
        DataServiceConnector.Listener {
    private static final String TAG = "BoomboxService";

    private Boombox mBoombox;
    private ApiHttpClient mApiHttpClient;
    private DataService mDataService;
    private IBinder mBinder = new LocalBinder();



    public class LocalBinder extends Binder {
        public BoomboxService getService() {
            return BoomboxService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mApiHttpClient = new ApiHttpClient();
        mApiHttpClient.setApiEndpoint("192.168.0.2", 8080);

        mBoombox = new Boombox();
        mBoombox.registerInfoListener(this);
        mBoombox.start();

        DataServiceConnector.connect(getApplicationContext(), this);

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
    }

    @Override
    public void onPlaybackCompletion(Boombox boombox, AudioDataProvider completedProvider, AudioDataProvider nextProvider) {

    }

    @Override
    public void onPlaylistCompletion(Boombox boombox) {
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        am.abandonAudioFocus(this);
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
}