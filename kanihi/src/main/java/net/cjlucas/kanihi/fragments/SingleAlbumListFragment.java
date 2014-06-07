package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.loaders.CloseableIteratorAsyncLoader;
import net.cjlucas.kanihi.data.DataService;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.data.connectors.DataServiceConnector;
import net.cjlucas.kanihi.data.connectors.ImageServiceConnector;
import net.cjlucas.kanihi.data.loaders.DataServiceLoader;
import net.cjlucas.kanihi.data.loaders.ListAsyncLoader;
import net.cjlucas.kanihi.models.Album;
import net.cjlucas.kanihi.models.AlbumArtist;
import net.cjlucas.kanihi.models.Disc;
import net.cjlucas.kanihi.models.Track;
import net.cjlucas.kanihi.models.TrackArtist;
import net.cjlucas.kanihi.utils.DataUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by chris on 3/21/14.
 */
public class SingleAlbumListFragment extends ListFragment
    implements DataServiceConnector.Listener, ImageServiceConnector.Listener,
        LoaderManager.LoaderCallbacks<SingleAlbumListFragment.PlaylistData>{
    public static final String ARG_ALBUM_UUID = "album_uuid";

    private ImageStore mImageStore;
    private DataService mDataService;

    public class PlaylistData {
        Album mAlbum;
        List<Disc> mDiscs;
        Map<Disc, List<Track>> mDiscTracksMap = new HashMap<>();
    }

    private class PlaylistListAdapter extends BaseAdapter {
        private final int ROW_ALBUM_HEADER = 0;
        private final int ROW_DISC_HEADER = 1;
        private final int ROW_TRACK = 2;

        private Album mAlbum;
        private List<Disc> mDiscs;
        private List<Track> mTracks;
        private Map<Disc, List<Track>> mDiscTrackMap;
        private Map<Integer, Track> mRowPositionTrackMap;
        private Map<Integer, Integer> mRowPositionRowTypeMap;
        private Bitmap mBlurredBitmap;

        public PlaylistListAdapter(Context context, int resourceId, PlaylistData data) {
            mAlbum = data.mAlbum;
            mDiscs = data.mDiscs;
            mDiscTrackMap = data.mDiscTracksMap;
            mTracks = new ArrayList<>();

            mRowPositionTrackMap = new HashMap<>();
            int rowPos = 1; // skip album header
            for (Disc disc : mDiscs) {
                mTracks.addAll(mDiscTrackMap.get(disc));

                if(useDiscHeader()) rowPos++; // skip disk header

                for (Track track : mDiscTrackMap.get(disc)) {
                    mRowPositionTrackMap.put(rowPos++, track);
                }
            }

            mRowPositionRowTypeMap = new HashMap<>();
            int pos = 0;
            mRowPositionRowTypeMap.put(pos, ROW_ALBUM_HEADER);
            for (Disc disc : mDiscs) {
               if (useDiscHeader()) mRowPositionRowTypeMap.put(++pos, ROW_DISC_HEADER);
                for (int i = 0; i < mDiscTrackMap.get(disc).size(); i++) {
                    mRowPositionRowTypeMap.put(++pos, ROW_TRACK);
                }
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
            return mRowPositionTrackMap.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return mRowPositionRowTypeMap.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        private View loadView(int layoutId, ViewGroup parent) {
            if (getActivity() == null) throw new RuntimeException("activity is null");

            return getActivity().getLayoutInflater().inflate(layoutId, parent, false);
        }

        private Bitmap renderBlurredBitmap(Bitmap image) {
            Bitmap src = Bitmap.createScaledBitmap(image, 500, 500, true);

            Bitmap outBitmap = src.copy(src.getConfig(), true);

            final RenderScript rs = RenderScript.create(getActivity());
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

        private void configureRowAlbumHeader(final View view) {
            TextView albumNameView = (TextView)view.findViewById(R.id.album_name);
            albumNameView.setText(mAlbum.getTitle());

            TextView albumArtistView = (TextView)view.findViewById(R.id.album_artist);
            albumArtistView.setText(mAlbum.getAlbumArtist().getName());

            TextView albumDateView = (TextView)view.findViewById(R.id.album_date);
            SimpleDateFormat df = new SimpleDateFormat("MMMM d, yyyy");
            Date date = mTracks.get(0).getOriginalDate();
            albumDateView.setText(df.format(date));

            if (mAlbum.getImage() == null)
                return;

            mImageStore.getBitmap(mAlbum.getImage(), new ImageStore.NewCallback<Bitmap>() {
                @Override
                public void onImageAvailable(Bitmap image) {
                    RelativeLayout albumBg = (RelativeLayout) view.findViewById(R.id.album_background);
                    if (mBlurredBitmap == null)
                        mBlurredBitmap = renderBlurredBitmap(image);
                    albumBg.setBackground(new BitmapDrawable(mBlurredBitmap));

                    ImageView imageView = (ImageView)view.findViewById(R.id.imageView);
                    imageView.setImageBitmap(image);
                }
            });
        }

        private void configureRowDiscHeader(int position, View view) {
            String discSubtitle = null;
            Disc disc = null;

            for (int i = 0, count = 1; i < mDiscs.size(); i++) {
               disc = mDiscs.get(i);
               if (position == count) {
                   discSubtitle = disc.getSubtitle();
                   break;
               }

                count += mDiscTrackMap.get(disc).size() + 1; // +1 is for the current disc row
            }

            int[] discNumResIds = { R.string.disc_one, R.string.disc_two, R.string.disc_three,
                    R.string.disc_four, R.string.disc_five, R.string.disc_six, R.string.disc_seven,
                    R.string.disc_eight, R.string.disc_nine, R.string.disc_ten };

            int discNum = disc.getDiscNum();
            int discNumResId = (discNum > 0 && discNum <= 10) ? discNumResIds[discNum - 1] : -1;

            Resources res = getResources();
            String discNumStr = res.getString(R.string.disc) + " "
                    + (discNumResId != -1
                    ? res.getString(discNumResId) : String.valueOf(disc.getDiscNum()));

            CharSequence discRowText;
            if (disc.getSubtitle() != null) {
                SpannableString spannable
                        = new SpannableString(disc.getSubtitle() + " (" + discNumStr + ")");

                int start = disc.getSubtitle().length() + 1;
                int end = spannable.length();
                spannable.setSpan(new RelativeSizeSpan(.7f), start, end, 0);

                discRowText = spannable;
            } else {
                discRowText = discNumStr;
            }

            ((TextView)view.findViewById(R.id.disc_title)).setText(discRowText);
        }

        private void configureRowDiscFooter(int position, View view) {
            // the footer is the same as row_track except for the bottom padding
            configureRowTrack(position, view);
        }

        private void configureRowTrack(int position, View view) {
            Track track = getItem(position);
            TrackArtist trackArtist = track.getTrackArtist();

            TextView trackNumber = (TextView)view.findViewById(R.id.track_number);
            trackNumber.setText(String.valueOf(track.getNum()));

            TextView subtitle = (TextView)view.findViewById(R.id.subtitle);
            if (trackArtist == null || trackArtist.getName() == null) {
                subtitle.setVisibility(View.GONE);
            } else {
                subtitle.setText(track.getTrackArtist().getName());
            }

            TextView leftView = (TextView)view.findViewById(R.id.left_text);
            leftView.setText(track.getTitle());
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            int viewType = getItemViewType(position);

            if (view == null) {
                int resourceId;
                switch(viewType) {
                    case ROW_ALBUM_HEADER:
                        resourceId = R.layout.single_album_list_header_view;
                        break;
                    case ROW_DISC_HEADER:
                        resourceId = R.layout.playlist_row_disc_header;
                        break;
                    case ROW_TRACK:
                        resourceId = R.layout.playlist_row_track;
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
            }

            return view;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Context appContext = activity.getApplicationContext();
        if (appContext != null) {
            DataServiceConnector.connect(appContext, this);
            ImageServiceConnector.connect(appContext, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_list_view, container, false);
    }

    @Override
    public Loader<PlaylistData> onCreateLoader(int id, Bundle args) {
        return new DataServiceLoader<PlaylistData>(getActivity(), mDataService,
                new Callable<PlaylistData>() {
                    @Override
                    public PlaylistData call() throws Exception {
                        PlaylistData data = new PlaylistData();
                        long now = System.currentTimeMillis();
                        long start = now;
                        List<Album> albums =
                                DataUtils.getList(mDataService.executePreparedQuery(Album.class,
                                mDataService.getAlbumsQuery(Album.class,
                                        getArguments().getString(ARG_ALBUM_UUID),
                                        Album.COLUMN_TITLE, true)));

                        Log.d("wee", "stage 1: " + (System.currentTimeMillis() - now));
                        now = System.currentTimeMillis();

                        if (albums.size() > 0) {
                            Album album = albums.get(0);
                            data.mAlbum = album;
                            mDataService.refresh(AlbumArtist.class, album.getAlbumArtist());

                            Log.d("wee", "start stage 2");
                            List<Disc> discs =
                                    DataUtils.getList(mDataService.executePreparedQuery(Disc.class,
                                            mDataService.getDiscsQuery(Album.class, album.getUuid(),
                                                    Disc.COLUMN_DISC_NUM, true)));
                            Log.d("wee", "stage 2: " + (System.currentTimeMillis() - now));
                            now = System.currentTimeMillis();
                            data.mDiscs = discs;

                            for (Disc disc : discs) {
                                now = System.currentTimeMillis();
                                List<Track> tracks = DataUtils.getList(
                                        disc.getTracks().closeableIterator());



                                mDataService.deleteMe(tracks);
                                Log.d("wee", "stage 3: " + (System.currentTimeMillis() - now));
                                now = System.currentTimeMillis();

                                data.mDiscTracksMap.put(disc, tracks);
                            }
                        }

                        Log.d("wee", "final: " + (System.currentTimeMillis() - start));

                        return data;
                    }
                });
    }

    @Override
    public void onLoadFinished(Loader<PlaylistData> loader, PlaylistData data) {
        setListAdapter(new PlaylistListAdapter(getActivity(), -1, data));
    }

    @Override
    public void onLoaderReset(Loader<PlaylistData> loader) {

    }

    @Override
    public void onDataServiceConnected(DataService dataService) {
        mDataService = dataService;
        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public void onDataServiceDisconnected() {

    }

    @Override
    public void onImageServiceConnected(ImageStore imageStore) {
        mImageStore = imageStore;
    }

    @Override
    public void onImageServiceDisconnected() {

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Fragment fragment = new MusicPlayerFragment();
        PlaylistListAdapter adapter = (PlaylistListAdapter)getListAdapter();

        Bundle args = new Bundle();
        ArrayList<String> uuids = new ArrayList<>();
        for (Disc disc : adapter.mDiscs) {
            for (Track track : adapter.mDiscTrackMap.get(disc)) {
                uuids.add(track.getUuid());
            }
        }

        args.putStringArrayList(MusicPlayerFragment.ARG_ADD_TRACKS, uuids);

        fragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(getId(), fragment).commit();
    }
}
