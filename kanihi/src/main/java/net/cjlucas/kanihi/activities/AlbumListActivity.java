package net.cjlucas.kanihi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.model.Album;
import net.cjlucas.kanihi.model.AlbumArtist;

/**
 * Created by chris on 3/11/14.
 */
public class AlbumListActivity extends ModelListActivity<Album> {

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        ApiHttpClient.setApiEndpoint("home.cjlucas.net", 34232);
//        DataStore.getInstance().update();
    }

    @Override
    int executeDefaultQuery() {
        return DataStore.getInstance().getAlbumArtists();
    }

    public View getRowView(Album album, View reusableView, ViewGroup viewGroup) {
        View view = reusableView;
        if (view == null) {
            view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(android.R.id.text1);
        textView.setText(album.getTitle());

        return view;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Album album = (Album)getListAdapter().getItem(position);
        int token = DataStore.getInstance().getTracks(album);
        Intent intent = new Intent(this, TrackListActivity.class);
        intent.putExtra("token", token);
        startActivity(intent);
    }
}
