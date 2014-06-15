package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import net.cjlucas.boombox.Boombox;
import net.cjlucas.boombox.BoomboxInfoListener;
import net.cjlucas.boombox.provider.AudioDataProvider;
import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.BoomboxService;
import net.cjlucas.kanihi.data.DataService;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.data.connectors.BoomboxServiceConnector;
import net.cjlucas.kanihi.data.connectors.DataServiceConnector;
import net.cjlucas.kanihi.data.connectors.ImageServiceConnector;
import net.cjlucas.kanihi.data.loaders.DataServiceLoader;
import net.cjlucas.kanihi.models.Image;
import net.cjlucas.kanihi.models.Track;
import net.cjlucas.kanihi.utils.TextUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

/**
 * Created by chris on 6/3/14.
 */
public class MusicPlayerFragment extends Fragment
    implements  DataServiceConnector.Listener,
                ImageServiceConnector.Listener,
                BoomboxServiceConnector.Listener,
                View.OnClickListener,
                BoomboxInfoListener {
    private static final String TAG = "MusicPlayerFragment";

    private BoomboxService mBoomboxService;
    private ImageStore mImageService;
    private DataService mDataService;

    private ImageView mImageView;

    private ImageView mPrevButton;
    private ImageView mPlayPauseButton;
    private ImageView mNextButton;
    private TextView mTimeElapsedView;
    private TextView mTimeRemainingView;
    private SeekBar mTimeProgressBar;

    private Timer mTimer;

    private class UpdatePlayerInfoUiTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePlayerInfoUi();
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");

        Context appContext = activity.getApplicationContext();
        if (appContext != null) {
            DataServiceConnector.connect(appContext, this);
            ImageServiceConnector.connect(appContext, this);
            BoomboxServiceConnector.connect(appContext, this);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.music_player, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        mImageView = (ImageView)view.findViewById(R.id.imageView);

        mPrevButton = (ImageView)view.findViewById(R.id.prev);
        mPrevButton.setOnClickListener(this);

        mNextButton = (ImageView)view.findViewById(R.id.next);
        mNextButton.setOnClickListener(this);

        mPlayPauseButton = (ImageView)view.findViewById(R.id.toggle_play_pause);
        mPlayPauseButton.setOnClickListener(this);

        mTimeElapsedView = (TextView)view.findViewById(R.id.time_elapsed);
        mTimeRemainingView = (TextView)view.findViewById(R.id.time_remaining);
        mTimeProgressBar = (SeekBar)view.findViewById(R.id.time_progress_bar);

        if (mBoomboxService != null && getBoombox().getPlaylist().size() > 0) {
            updateTrackInfoUi(mBoomboxService.getTrack(getBoombox().getCurrentProvider()));
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        mTimer = new Timer();
        mTimer.schedule(new UpdatePlayerInfoUiTask(), 0, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTimer.cancel();
        mTimer = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.music_player_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.up_next:
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                        .addToBackStack(null)
                        .replace(getId(), new UpNextListFragment())
                        .commit();
                break;
            default:
                break;
        }

        return true;
    }

    private Boombox getBoombox() {
        return mBoomboxService.getBoombox();
    }

    @Override
    public void onBoomboxServiceConnected(BoomboxService boomboxService) {
        Log.d(TAG, "onBoomboxServiceConnected");
        mBoomboxService = boomboxService;
        getBoombox().registerInfoListener(this);

        // TODO: Boombox.getCurrentProvider doesn't check if the playlist is empty
        if (getBoombox().getPlaylist().size() > 0) {
            updateTrackInfoUi(mBoomboxService.getTrack(getBoombox().getCurrentProvider()));
        }
    }

    @Override
    public void onBoomboxServiceDisconnected() {
        getBoombox().unregisterInfoListener(this);
        mBoomboxService = null;
    }

    @Override
    public void onImageServiceConnected(ImageStore dataService) {
        mImageService = dataService;
    }

    @Override
    public void onImageServiceDisconnected() {
        mImageService = null;
    }

    @Override
    public void onDataServiceConnected(DataService dataService) {
        Log.d(TAG, "onDataServiceConnected");
        mDataService = dataService;
    }

    @Override
    public void onDataServiceDisconnected() {
        mDataService = null;
    }

    @Override
    public void onClick(View v) {
        if (v == mPlayPauseButton) {
            // update icon to reflect new play/pause state
            mPlayPauseButton.setImageResource(getBoombox().isPlaying()
                    ? R.drawable.pause : R.drawable.play);
            getBoombox().togglePlayPause();
        } else if (v == mNextButton) {
            getBoombox().playNext();
        } else if (v == mPrevButton) {
            getBoombox().playPrevious();
        }
    }

    @Override
    public void onPlaybackStart(Boombox boombox, final AudioDataProvider audioDataProvider) {
        Log.d(TAG, "onPlaybackStart");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateTrackInfoUi(mBoomboxService.getTrack(audioDataProvider));
            }
        });
    }

    @Override
    public void onPlaybackCompletion(Boombox boombox, AudioDataProvider audioDataProvider, AudioDataProvider audioDataProvider2) {

    }

    @Override
    public void onPlaylistCompletion(Boombox boombox) {

    }

    @Override
    public void onBufferingStart(Boombox boombox, AudioDataProvider audioDataProvider) {

    }

    @Override
    public void onBufferingEnd(Boombox boombox, AudioDataProvider audioDataProvider) {

    }

    @Override
    public void onBufferingUpdate(Boombox boombox, AudioDataProvider audioDataProvider, int i) {

    }

    private void updatePlayerInfoUi() {
        Boombox boombox = getBoombox();
        if (boombox == null)
            return;

        mPlayPauseButton.setImageResource(boombox.isPlaying() ? R.drawable.pause : R.drawable.play);
        mPrevButton.setEnabled(boombox.hasPrevious());
        mNextButton.setEnabled(boombox.hasNext());

        double timeElapsed = boombox.getCurrentPosition() / 1000.0;
        mTimeElapsedView.setText(TextUtils.getTimeCode((int)timeElapsed));

        double timeRemaining = (boombox.getDuration() / 1000.0) - timeElapsed;
        mTimeRemainingView.setText("-" + TextUtils.getTimeCode((int)timeRemaining));

        double trackDuration = boombox.getDuration() / 1000.0;
        mTimeProgressBar.setProgress((int)(timeElapsed * 100 / trackDuration));
    }

    private void updateTrackInfoUi(Track track) {
        if (getActivity() == null || getView() == null) {
            return;
        }

        ((TextView)getView().findViewById(R.id.track_title)).setText(track.getTitle());
        ((TextView)getView().findViewById(R.id.track_artist_name)).setText(track.getTrackArtist().getName());
        ((TextView)getView().findViewById(R.id.album_name)).setText(track.getDisc().getAlbum().getTitle());

        loadTrackImage(track);
    }

    private void loadTrackImage(Track track) {
        if (mDataService == null || mImageService == null) {
            return;
        }

        // TODO: call getTrackImages async, (via loader?)
        final List<Image> images = mDataService.getTrackImages(track);
        if (images.size() == 0) {
            return;
        }

        mImageService.loadImage(images.get(0), mImageView, ImageStore.ImageType.FULL_SIZE,
                new ImageStore.Callback() {
                    @Override
                    public void onImageAvailable(final ImageView imageView, final Drawable drawable) {
                        if (drawable.getIntrinsicHeight() != drawable.getIntrinsicWidth()) {
                            mImageService.loadBlurredImage(images.get(0), new ImageStore.Callback() {
                                @Override
                                public void onImageAvailable(ImageView ignore, final Drawable blurredDrawable) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            getView().findViewById(R.id.image_frame).setBackground(blurredDrawable);
                                            imageView.setImageDrawable(drawable);
                                        }
                                    });
                                }
                            });
                        } else {
                            ModelListFragment.ImageAttacher.attach(getActivity(), imageView, drawable);
                        }
                    }
                });
    }

    private void runOnUiThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        } else {
            Log.d(TAG, "runOnUiThread: getActivity() returned null");
        }
    }
}
