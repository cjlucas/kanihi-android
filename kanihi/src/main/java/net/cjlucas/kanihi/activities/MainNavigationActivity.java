package net.cjlucas.kanihi.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.BoomboxService;
import net.cjlucas.kanihi.data.DataService;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.data.connectors.DataServiceConnector;
import net.cjlucas.kanihi.data.loaders.DataServiceLoader;
import net.cjlucas.kanihi.fragments.AlbumListFragment;
import net.cjlucas.kanihi.fragments.ArtistListFragment;
import net.cjlucas.kanihi.fragments.ModelListFragment;
import net.cjlucas.kanihi.fragments.TrackListFragment;

import java.util.concurrent.Callable;

/**
 * Created by chris on 3/11/14.
 */
public class MainNavigationActivity extends Activity
        implements ListView.OnItemClickListener, DataServiceConnector.Listener,
        LoaderManager.LoaderCallbacks<DataService.ModelCounts> {
    private Fragment mCurrentFragment;
    private ListView mMenuListView;
    private DrawerLayout mDrawerLayout;

    private ApiHttpClient mApiHttpClient;
    private DataService mDataService;
    private ImageStore mImageStore;

    private static class DrawerAdapter extends BaseAdapter {
        private static final int NUM_ENTRIES = 5;
        private Context mContext;
        private Entry[] mEntries;

        private class Entry {
            private int text;
            private int icon;
            private long count;

            public Entry(int text, int icon, long count) {
                this.text = text;
                this.icon = icon;
                this.count = count;
            }
        }

        public DrawerAdapter(Context context, DataService.ModelCounts counts) {
            super();
            mContext = context;
            mEntries = new Entry[NUM_ENTRIES];
            updateCounts(counts);
        }

        @Override
        public int getCount() {
            return NUM_ENTRIES;
        }

        @Override
        public Entry getItem(int position) {
            return mEntries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void updateCounts(DataService.ModelCounts counter) {
            mEntries[0] = new Entry(R.string.artists, R.drawable.note, counter.artistCount);
            mEntries[1] = new Entry(R.string.albums, R.drawable.note, counter.albumCount);
            mEntries[2] = new Entry(R.string.genres, R.drawable.note, counter.genreCount);
            mEntries[3] = new Entry(R.string.tracks, R.drawable.note, counter.tracksCount);
            mEntries[4] = new Entry(R.string.update, R.drawable.note, -1);

            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Entry entry = getItem(position);

            View view = convertView;
            if (view == null) {
                LayoutInflater layoutInflater
                        = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

                view = layoutInflater.inflate(R.layout.main_navigation_row, parent, false);
            }

            ((ImageView)view.findViewById(R.id.image)).setImageResource(entry.icon);
            ((TextView)view.findViewById(R.id.main)).setText(entry.text);

            TextView counterView = (TextView)view.findViewById(R.id.counter);
            if (entry.count >= 0) {
                counterView.setVisibility(View.VISIBLE);
                counterView.setText(String.valueOf(entry.count));
            } else {
                counterView.setVisibility(View.INVISIBLE);
            }

            return view;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_navigation);

        mApiHttpClient = new ApiHttpClient();
        mApiHttpClient.setApiEndpoint("192.168.0.2", 8080);

        mMenuListView = (ListView)findViewById(R.id.menu_list);
        mMenuListView.setOnItemClickListener(this);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        startService(new Intent(this, DataService.class));
        startService(new Intent(this, ImageStore.class));
        startService(new Intent(this, BoomboxService.class));

        DataServiceConnector.connect(getApplicationContext(), this);

        addFragment(new ArtistListFragment());
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private ModelListFragment fragmentForSelection(int position) {
        switch(position) {
            case 0:
                return new ArtistListFragment();
            case 1:
                return new AlbumListFragment();
            case 2:
                return null;
            case 3:
                return new TrackListFragment();
            case 4:
                return null;
            default:
                throw new RuntimeException("unknown menu item selected");
        }
    }

    private void addFragment(ModelListFragment fragment) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.fragment_placeholder, fragment).commit();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (i == 2) {
        } else if (i == 4) {
            mDataService.update();
        } else {
            addFragment(fragmentForSelection(i));
        }

        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    @Override
    public void onDataServiceConnected(DataService dataService) {
        mDataService = dataService;
        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public void onDataServiceDisconnected() {
        mDataService = null;
    }

    @Override
    public Loader<DataService.ModelCounts> onCreateLoader(int id, Bundle args) {
        return new DataServiceLoader<>(this, mDataService, new Callable<DataService.ModelCounts>() {
            @Override
            public DataService.ModelCounts call() throws Exception {
                return mDataService.getModelCounts();
            }
        });
    }

    @Override
    public void onLoadFinished(Loader<DataService.ModelCounts> loader, DataService.ModelCounts data) {
        mMenuListView.setAdapter(new DrawerAdapter(this, data));
    }

    @Override
    public void onLoaderReset(Loader<DataService.ModelCounts> loader) {

    }
}