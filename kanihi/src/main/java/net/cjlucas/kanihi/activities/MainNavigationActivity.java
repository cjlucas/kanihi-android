package net.cjlucas.kanihi.activities;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.cjlucas.kanihi.R;

/**
 * Created by chris on 3/11/14.
 */
public class MainNavigationActivity extends Activity {
    private Fragment mCurrentFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_navigation);

        String[] items = {"Artists", "Albums", "Tracks"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);

        ((ListView)findViewById(R.id.menu_list)).setAdapter(adapter);
    }
}