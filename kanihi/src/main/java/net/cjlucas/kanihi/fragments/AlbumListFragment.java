package net.cjlucas.kanihi.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.model.Album;

/**
 * Created by chris on 3/11/14.
 */
public class AlbumListFragment extends ModelListFragment<Album> {

    @Override
    public int executeDefaultQuery() {
        return DataStore.getInstance().getAlbums();
    }

    public View getRowView(Album album, View reusableView, ViewGroup viewGroup) {
        View view = reusableView;
        if (view == null && getActivity() != null) {
            view = getActivity().getLayoutInflater()
                    .inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(android.R.id.text1);
        textView.setText(album.getTitle());

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Album album = (Album)getListAdapter().getItem(position);
        int token = DataStore.getInstance().getTracks(album);

        blah(new TrackListFragment(), token);
    }
}
