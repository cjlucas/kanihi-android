package net.cjlucas.kanihi.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.DataService;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.listeners.PaddingShiftOnTouchListener;
import net.cjlucas.kanihi.models.Album;
import net.cjlucas.kanihi.models.Disc;
import net.cjlucas.kanihi.models.Track;
import net.cjlucas.kanihi.utils.DataUtils;
import net.cjlucas.kanihi.utils.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by chris on 3/21/14.
 */
public class SingleAlbumListFragment extends ListFragment {
    private ImageStore mImageStore;
    private DataService mDataService;
    private Album mAlbum;

    private class PlaylistListAdapter extends BaseAdapter {
        private final int ROW_ALBUM_HEADER = 0;
        private final int ROW_DISC_HEADER = 1;
        private final int ROW_TRACK = 2;
        private final int ROW_DISC_FOOTER = 3;

        private Album mAlbum;
        private List<Disc> mDiscs;
        private Map<Disc, List<Track>> mDiscTrackMap;
        private Map<Integer, Track> mRowPositionTrackMap;
        private Map<Integer, Integer> mRowPositionRowTypeMap;

        public PlaylistListAdapter(Context context, int resourceId, Album album) {
            mAlbum = album;
            mDiscs = DataUtils.getList(album.getDiscs().closeableIterator());
            mDiscTrackMap = new HashMap<>();
            for (Disc disc : mDiscs) {
                mDiscTrackMap.put(disc, DataUtils.getList(disc.getTracks().closeableIterator()));
            }

            mRowPositionTrackMap = new HashMap<>();
            int rowPos = 1; // skip album header
            for (Disc disc : mDiscs) {
                if(useDiscHeader()) rowPos++; // skip disk header

                for (Track track : mDiscTrackMap.get(disc)) {
                    mRowPositionTrackMap.put(rowPos++, track);
                }
            }

            Log.d("blah", mRowPositionTrackMap.toString());

            mRowPositionRowTypeMap = new HashMap<>();
            int pos = 0;
            mRowPositionRowTypeMap.put(pos, ROW_ALBUM_HEADER);
            for (Disc disc : mDiscs) {
               if (useDiscHeader()) mRowPositionRowTypeMap.put(++pos, ROW_DISC_HEADER);
                for (int i = 0; i < mDiscTrackMap.get(disc).size() - 1; i++) {
                    mRowPositionRowTypeMap.put(++pos, ROW_TRACK);
                }

                mRowPositionRowTypeMap.put(++pos, ROW_DISC_FOOTER);
            }
        }

        private boolean useDiscHeader() {
            return mDiscs.size() > 1;
        }

        @Override
        public long getItemId(int position) {
            Log.i("blah", "getItemId start");
            int itemId = 1;
            int offset = position;
            for (Disc disc : mDiscs) {
                itemId += 1;

                List<Track> tracks = mDiscTrackMap.get(disc);
                if (offset < tracks.size()) {
                    itemId += offset;
                    break;
                }
                offset -= tracks.size();
            }

            Log.i("blah", "getItemId is returning " + itemId);
            return itemId;
        }

        @Override
        public int getCount() {
            return mRowPositionRowTypeMap.size();
        }

        @Override
        public Track getItem(int position) {
            Log.d("blah", "getItem position = " + position);
            return mRowPositionTrackMap.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return mRowPositionRowTypeMap.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        private View loadView(int layoutId, ViewGroup parent) {
            if (getActivity() == null) throw new RuntimeException("activity is null");

            return getActivity().getLayoutInflater().inflate(layoutId, parent, false);
        }

        private void configureRowAlbumHeader(View view) {
            view.setOnTouchListener(new PaddingShiftOnTouchListener());
        }

        private void configureRowDiscHeader(int position, View view) {
        }

        private void configureRowDiscFooter(int position, View view) {
            // the footer is the same as row_track except for the bottom padding
            configureRowTrack(position, view);
        }

        private void configureRowTrack(int position, View view) {
            Track track = getItem(position);

            TextView leftView = (TextView)view.findViewById(R.id.left_text);
            TextView rightView = (TextView)view.findViewById(R.id.right_text);

            leftView.setText(track.getTitle());
            rightView.setText(TextUtils.getTimeCode(track.getDuration()));
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.i("blah", String.valueOf(position));
            View view = convertView;
            int viewType = getItemViewType(position);

            if (view == null) {
                int resourceId;
                switch(viewType) {
                    case ROW_ALBUM_HEADER:
                        resourceId = R.layout.playlist_row_album_header;
                        break;
                    case ROW_DISC_HEADER:
                        resourceId = R.layout.playlist_row_disc_header;
                        break;
                    case ROW_TRACK:
                        resourceId = R.layout.playlist_row_track;
                        break;
                    case ROW_DISC_FOOTER:
                        resourceId = R.layout.playlist_row_disc_footer;
                        break;
                    default:
                        throw new RuntimeException("unhandled viewType");
                }

                view = loadView(resourceId, parent);
            }

            switch (viewType) {
                case ROW_ALBUM_HEADER:
                    configureRowAlbumHeader(view);
                    break;
                case ROW_DISC_HEADER:
                    configureRowDiscHeader(position, view);
                    break;
                case ROW_TRACK:
                    configureRowTrack(position, view);
                    break;
                case ROW_DISC_FOOTER:
                    configureRowDiscFooter(position, view);
                    break;
            }

            return view;
        }
    }

    public SingleAlbumListFragment(ImageStore imageStore, DataService dataService, Album album) {
        mImageStore = imageStore;
        mDataService = dataService;
        mAlbum = album;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.playlist_list_view, container, false);

        PlaylistListAdapter adapter = new PlaylistListAdapter(getActivity(), R.layout.playlist_row_track, mAlbum);
        setListAdapter(adapter);
        return view;
    }
}
