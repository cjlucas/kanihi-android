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

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by chris on 6/3/14.
 */
public class MusicPlayerFragment extends Fragment
    implements  DataServiceConnector.Listener,
                ImageServiceConnector.Listener,
                BoomboxServiceConnector.Listener,
                View.OnClickListener,
                BoomboxInfoListener,
                LoaderManager.LoaderCallbacks<List<Track>> {
    private static final String TAG = "MusicPlayerFragment";
    public static final String ARG_MODE = "mode";
    public static final String ARG_ADD_TRACKS = "add_tracks";

    private BoomboxService mBoomboxService;
    private ImageStore mImageService;
    private DataService mDataService;

    private ImageView mImageView;

    private View mPrevButton;
    private View mPlayPauseButton;
    private View mNextButton;

    // temp storage if loader is executed before BoomboxService is connected
    private List<Track> mAddTracks;

    public enum PlaylistMode {
        RESET_PLAYLIST
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

        mPrevButton = view.findViewById(R.id.prev);
        mPrevButton.setOnClickListener(this);

        mNextButton = view.findViewById(R.id.next);
        mNextButton.setOnClickListener(this);

        mPlayPauseButton = view.findViewById(R.id.toggle_play_pause);
        mPlayPauseButton.setOnClickListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Context appContext = activity.getApplicationContext();
        Log.d(TAG, appContext.toString());
        if (appContext != null) {
            DataServiceConnector.connect(appContext, this);
            ImageServiceConnector.connect(appContext, this);
            BoomboxServiceConnector.connect(appContext, this);
        }

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
        Log.d(TAG, "onBoomboxServiceConnted");
        mBoomboxService = boomboxService;
        getBoombox().registerInfoListener(this);

        if (mAddTracks != null) {
            mBoomboxService.addTracks(mAddTracks, true);
            mAddTracks = null;
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

    }

    @Override
    public void onDataServiceConnected(DataService dataService) {
        Log.d(TAG, "onDataServiceConnected");
        mDataService = dataService;

        if (getArguments().getStringArrayList(ARG_ADD_TRACKS) != null)
            getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public void onDataServiceDisconnected() {
    }

    @Override
    public Loader<List<Track>> onCreateLoader(int id, Bundle args) {

        return new DataServiceLoader<>(getActivity(), mDataService, new Callable<List<Track>>() {
            @Override
            public List<Track> call() throws Exception {
                return mDataService.getModels(Track.class,
                        getArguments().getStringArrayList(ARG_ADD_TRACKS));
            }
        });
    }

    @Override
    public void onLoadFinished(Loader<List<Track>> loader, List<Track> data) {
        if (mBoomboxService != null) {
            Log.d(TAG, "yo im here");
            mBoomboxService.addTracks(data, true);
        } else {
            mAddTracks = data;
        }

    }

    @Override
    public void onLoaderReset(Loader<List<Track>> loader) {

    }

    @Override
    public void onClick(View v) {
        if (v == mPlayPauseButton) {
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

        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Track track = mBoomboxService.getTrack(audioDataProvider);

                ((TextView)getView().findViewById(R.id.track_title)).setText(track.getTitle());
                ((TextView)getView().findViewById(R.id.track_artist_name)).setText(track.getTrackArtist().getName());
                ((TextView)getView().findViewById(R.id.album_name)).setText(track.getDisc().getAlbum().getTitle());


                loadCurrentTrackImage();

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

    private void loadCurrentTrackImage() {
        if (mBoomboxService != null
                && mDataService != null
                && mImageService != null) {
            Track track = mBoomboxService.getTrack(getBoombox().getCurrentProvider());
            List<Image> images = mDataService.getTrackImages(track);
            Log.d(TAG, images.toString());
            if (images.size() > 0) {
                mImageService.loadImage(images.get(0), mImageView, false, new ImageStore.Callback() {
                    @Override
                    public void onImageAvailable(ImageView imageView, final Drawable drawable) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mImageView.setAdjustViewBounds(true);
                                    mImageView.setMinimumHeight(getView().getMeasuredWidth());
                                    mImageView.setMaxHeight(getView().getMeasuredWidth());
                                    mImageView.setImageDrawable(drawable);
                                }
                            });
                        }
                    }
                });
            }
        }
    }
}
