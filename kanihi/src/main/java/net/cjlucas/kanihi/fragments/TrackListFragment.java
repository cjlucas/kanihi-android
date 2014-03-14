package net.cjlucas.kanihi.fragments;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.model.Track;

/**
 * Created by chris on 3/10/14.
 */
public class TrackListFragment extends ModelListFragment<Track> {
    @Override
    public int executeDefaultQuery() {
        return DataStore.getInstance().getTracks();
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
}
