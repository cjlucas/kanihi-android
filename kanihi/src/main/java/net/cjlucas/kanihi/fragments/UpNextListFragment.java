package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import net.cjlucas.boombox.Boombox;
import net.cjlucas.boombox.BoomboxInfoListener;
import net.cjlucas.boombox.provider.AudioDataProvider;
import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.BoomboxService;
import net.cjlucas.kanihi.data.connectors.BoomboxServiceConnector;
import net.cjlucas.kanihi.models.Track;

import java.util.List;

/**
 * Created by chris on 6/7/14.
 */
public class UpNextListFragment extends ListFragment
    implements BoomboxServiceConnector.Listener, BoomboxInfoListener {

    private BoomboxService mBoomboxService;
    private PlaylistAdapter mPlaylistAdapter;

    private class PlaylistAdapter extends ArrayAdapter<Track> {
        private Track mCurrentTrack;

        public PlaylistAdapter(Context context, List<Track> list) {
           super(context, R.layout.playlist_row_track, list);
        }

        public void setCurrentTrack(Track currentTrack) {
            mCurrentTrack = currentTrack;
            if (getActivity() != null)
                getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getListView() != null)
                        getListView().invalidateViews();
                }
            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Track track = getItem(position);
            View view = convertView;

            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.playlist_row_track, parent, false);
            }

            ((TextView)view.findViewById(R.id.track_number)).setText(String.valueOf(position + 1));

            CheckedTextView titleView = (CheckedTextView)view.findViewById(R.id.left_text);
            if (track == mCurrentTrack) {
                titleView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                titleView.setChecked(true);
            } else {
                titleView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                titleView.setChecked(false);
            }
            titleView.setChecked(track == mCurrentTrack);
            titleView.setText(track.getTitle());
            ((TextView)view.findViewById(R.id.subtitle)).setText(track.getTrackArtist().getName());

            return view;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        BoomboxServiceConnector.connect(activity.getApplicationContext(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_list_view, container, false);
    }

    @Override
    public void onBoomboxServiceConnected(BoomboxService boomboxService) {
        mBoomboxService = boomboxService;
        mBoomboxService.getBoombox().registerInfoListener(this);

        Track track = mBoomboxService.getTrack(mBoomboxService.getBoombox().getCurrentProvider());

        mPlaylistAdapter = new PlaylistAdapter(getActivity(), mBoomboxService.getPlaylist());
        mPlaylistAdapter.setCurrentTrack(track);
        setListAdapter(mPlaylistAdapter);
        setSelection(mBoomboxService.getPlaylist().indexOf(track));
    }

    @Override
    public void onBoomboxServiceDisconnected() {
        mBoomboxService.getBoombox().unregisterInfoListener(this);
        mBoomboxService = null;
    }

    @Override
    public void onPlaybackStart(Boombox boombox, AudioDataProvider audioDataProvider) {
        mPlaylistAdapter.setCurrentTrack(mBoomboxService.getTrack(audioDataProvider));
    }

    @Override
    public void onPlaybackCompletion(Boombox boombox, AudioDataProvider audioDataProvider, AudioDataProvider audioDataProvider2) {

    }

    @Override
    public void onPlaylistCompletion(Boombox boombox) {
        getFragmentManager().popBackStack();
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Track track = mPlaylistAdapter.getItem(position);
        mPlaylistAdapter.setCurrentTrack(track);
        mBoomboxService.getBoombox().play(track);
    }
}
