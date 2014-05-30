package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.data.CloseableIteratorAsyncLoader;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.models.Track;

/**
 * Created by chris on 3/10/14.
 */
public class TrackListFragment extends ModelListFragment<Track> {
    private ImageStore mImageStore;

    public TrackListFragment(ImageStore imageStore, DataStore dataStore) {
        super(dataStore);
        mImageStore = imageStore;
    }

    @Override
    public Class<Track> getGenericClass() {
        return Track.class;
    }

    @Override
    public PreparedQuery<Track> getDefaultQuery() {
        return mDataStore.getTracksQuery(Track.COLUMN_TITLE, true);
    }

    public View getRowView(Track track, View reusableView, ViewGroup viewGroup) {
        View view = reusableView;
        if (view == null && getActivity() != null) {
            view = getActivity().getLayoutInflater()
                    .inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(android.R.id.text1);
        textView.setText(track.getTitle());

        return view;
    }
    @Override
    public void onAttach(Activity activity) {
        Log.d("ArtistListFragment", "onAttached");
        super.onAttach(activity);
        getLoaderManager().initLoader(1, null, this);
    }
}
