package net.cjlucas.kanihi.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.fragments.AlbumListFragment;
import net.cjlucas.kanihi.fragments.ArtistListFragment;
import net.cjlucas.kanihi.fragments.ModelListFragment;
import net.cjlucas.kanihi.fragments.TrackListFragment;

/**
 * Created by chris on 3/11/14.
 */
public class MainNavigationActivity extends Activity implements ListView.OnItemClickListener{
    private Fragment mCurrentFragment;
    private ListView mMenuListView;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_navigation);

        ApiHttpClient.setApiEndpoint("home.cjlucas.net", 34232);
        DataStore.setupInstance(this);
        ImageStore.setupInstance(getApplicationContext());

        String[] items = {"Artists", "Albums", "Tracks"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);

        mMenuListView = (ListView)findViewById(R.id.menu_list);
        mMenuListView.setAdapter(adapter);
        mMenuListView.setOnItemClickListener(this);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        addFragment(new ArtistListFragment());
    }

    private ModelListFragment fragmentForSelection(int position) {
        switch(position) {
            case 0:
                return new ArtistListFragment();
            case 1:
                return new AlbumListFragment();
            case 2:
                return new TrackListFragment();
            default:
                throw new RuntimeException("unknown menu item selected");
        }
    }

    private void addFragment(ModelListFragment fragment) {
        int token = fragment.executeDefaultQuery();
        Bundle args = new Bundle();
        args.putInt(ModelListFragment.ARG_TOKEN, token);
        fragment.setArguments(args);

        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();

        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        addFragment(fragmentForSelection(i));
    }
}