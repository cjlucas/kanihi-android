package net.cjlucas.kanihi.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.model.AlbumArtist;

/**
 * Created by chris on 3/10/14.
 */
public class ArtistListFragment extends ModelListFragment<AlbumArtist> {

    @Override
    public int executeDefaultQuery() {
        return DataStore.getInstance().getAlbumArtists();
    }

    public View getRowView(AlbumArtist artist, View reusableView, ViewGroup viewGroup) {
        View view = reusableView;
        if (view == null && getActivity() != null) {
            view = getActivity().getLayoutInflater()
                    .inflate(R.layout.model_list_row, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(R.id.text1);
        textView.setText(artist.getName());

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        AlbumArtist artist = (AlbumArtist)getListAdapter().getItem(position);
        int token = DataStore.getInstance().getAlbums(artist);

        blah(new AlbumListFragment(), token);
    }
}
