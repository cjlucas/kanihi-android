package net.cjlucas.kanihi.fragments;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.models.Track;

/**
 * Created by chris on 3/10/14.
 */
public class TrackListFragment extends ModelListFragment<Track> {
    private static final String TAG = "TrackListFragment";

    @Override
    public Class<Track> getGenericClass() {
        return Track.class;
    }

    @Override
    public PreparedQuery<Track> getDefaultQuery(Class ancestorClazz, String ancestorUuid) {
        return mDataService.getTracksQuery(ancestorClazz, ancestorUuid, Track.COLUMN_TITLE, true);
    }

    public View getRowView(Track track, View reusableView, ViewGroup viewGroup) {
        long start = System.currentTimeMillis();
        View view = reusableView;
        if (view == null && getActivity() != null) {
            view = getActivity().getLayoutInflater()
                    .inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(android.R.id.text1);
        textView.setText(track.getTitle());

        Log.d(TAG, "getRowView took (in ms): " + (System.currentTimeMillis() - start));
        return view;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int pos, long id) {
        Log.d("OMGG", "" + listView.getFirstVisiblePosition());
        Log.d("OMGG", "" + listView.getLastVisiblePosition());
    }
}
