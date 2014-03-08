package net.cjlucas.kanihi;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.parser.JsonTrackArrayParser;
import net.cjlucas.kanihi.model.Track;
import net.cjlucas.kanihi.data.DataStore;

public class MainActivity extends Activity {
    private static DataStore mDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDataStore = new DataStore(this);

        long start = System.currentTimeMillis();
        mDataStore.update(getResources().openRawResource(R.raw.tracks));
        System.err.println("Update took: " + (System.currentTimeMillis() - start));

        mDataStore.close();

        ApiHttpClient.getTracks(0, 10, null, null);
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

}
