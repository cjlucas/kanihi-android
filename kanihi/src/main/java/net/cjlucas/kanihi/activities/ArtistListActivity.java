package net.cjlucas.kanihi.activities;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.data.AsyncQueryMonitor;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.model.Album;
import net.cjlucas.kanihi.model.AlbumArtist;

/**
 * Created by chris on 3/10/14.
 */
public class ArtistListActivity extends ModelListActivity<AlbumArtist> {

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
    }

    @Override
    int executeDefaultQuery() {
        return DataStore.getInstance().getAlbumArtists();
    }

    @Override
    public View getRowView(AlbumArtist artist, View reusableView, ViewGroup viewGroup) {
        View view = reusableView;
        if (view == null) {
            view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(android.R.id.text1);
        textView.setText(artist.getName());

        return view;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        int token = DataStore.getInstance().getTracks(new Album());
        Intent intent = new Intent(this, TrackListActivity.class);
        intent.putExtra("token", token);
        startActivity(intent);
    }
}
