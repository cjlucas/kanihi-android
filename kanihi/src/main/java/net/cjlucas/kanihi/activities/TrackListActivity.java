package net.cjlucas.kanihi.activities;

import android.app.ListActivity;
import android.os.Bundle;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.data.AsyncQueryMonitor;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.model.Track;

/**
 * Created by chris on 3/10/14.
 */
public class TrackListActivity extends ListActivity implements AsyncQueryMonitor.Listener<Track>{
    @Override
    protected void onStart() {
        super.onStart();
        DataStore dataStore = DataStore.setupInstance(this);
        dataStore.registerQueryMonitorListener(dataStore.getTracks(), this);
    }

    @Override
    public void onQueryComplete(CloseableIterator<Track> iterator) {
        ModelAdapter<Track> adapter = new ModelAdapter<>(null, iterator);
        setListAdapter(adapter);
    }
}
