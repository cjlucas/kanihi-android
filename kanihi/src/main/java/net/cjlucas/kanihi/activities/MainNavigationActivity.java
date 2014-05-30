package net.cjlucas.kanihi.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.DataService;
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

    private ApiHttpClient mApiHttpClient;
    private DataService mDataService;
    private ImageStore mImageStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_navigation);

        mApiHttpClient = new ApiHttpClient();
        mApiHttpClient.setApiEndpoint("home.cjlucas.net", 34232);

        mImageStore = new ImageStore(getApplicationContext(), mApiHttpClient);

        String[] items = {"Artists", "Albums", "Tracks", "Update"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);

        mMenuListView = (ListView)findViewById(R.id.menu_list);
        mMenuListView.setAdapter(adapter);
        mMenuListView.setOnItemClickListener(this);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        addFragment(new ArtistListFragment(mImageStore, mDataStore));
        startService(new Intent(this, DataService.class));

        bindService(new Intent(getApplicationContext(), DataService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mDataService = ((DataService.LocalBinder)service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mDataService = null;
            }
        }, 0);

        addFragment(new ArtistListFragment());

    }

    private ModelListFragment fragmentForSelection(int position) {
        switch(position) {
            case 0:
                return new ArtistListFragment(mImageStore, mDataStore);
            case 1:
                return new AlbumListFragment(mImageStore, mDataStore);
            case 2:
                return new TrackListFragment(mImageStore, mDataStore);
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
        if (i == 3) {
            mDataService.update();
        } else {
            addFragment(fragmentForSelection(i));
        }
    }
}