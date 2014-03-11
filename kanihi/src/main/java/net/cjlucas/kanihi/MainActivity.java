package net.cjlucas.kanihi;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.j256.ormlite.dao.CloseableIterator;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.AsyncQueryMonitor;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.model.Track;

import java.sql.SQLException;

public class MainActivity extends Activity implements AsyncQueryMonitor.Listener<Track> {
    private static DataStore mDataStore;
    private long start;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApiHttpClient.setApiEndpoint("home.cjlucas.net", 34232);

        DataStore.setupInstance(this);
        mDataStore = DataStore.getInstance();

//        mDataStore.update();

//        for (int i = 0; i < 20; i++) {
//            int token = mDataStore.getTracks();
//            System.err.println("GOT DA TOKEN!: " + token);
//            try { Thread.sleep(500); } catch (Exception e) {}
//            start = System.currentTimeMillis();
//            mDataStore.registerQueryMonitorListener(token, this);
//        }

    }

    public void updateDb(View v) {
        DataStore.getInstance().update();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onQueryComplete(CloseableIterator<Track> iterator) {
        System.err.println("GOT DA ITERATOR");
        System.err.println(System.currentTimeMillis() - start);
        while (iterator.hasNext()) {
            try {
                System.err.println(iterator.current().getTitle());
            } catch (SQLException e) {}
        }

        iterator.closeQuietly();
    }
}
