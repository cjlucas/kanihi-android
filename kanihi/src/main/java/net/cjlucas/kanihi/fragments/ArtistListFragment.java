package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.CloseableIteratorAsyncLoader;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.data.adapters.ModelAdapter;
import net.cjlucas.kanihi.models.AlbumArtist;
import net.cjlucas.kanihi.models.Image;
import net.cjlucas.kanihi.models.Track;

/**
 * Created by chris on 3/10/14.
 */
public class ArtistListFragment extends ModelListFragment<AlbumArtist> {

    private ImageStore mImageStore;

    public ArtistListFragment(ImageStore imageStore, DataStore dataStore) {
        super(dataStore);
        mImageStore = imageStore;

    }

    @Override
    public Class<AlbumArtist> getGenericClass() {
        return AlbumArtist.class;
    }

    @Override
    public PreparedQuery<AlbumArtist> getDefaultQuery() {
        return mDataStore.getAlbumArtistsQuery(AlbumArtist.COLUMN_NAME, true);
    }

    public View getRowView(AlbumArtist artist, View reusableView, ViewGroup viewGroup) {
        View view = reusableView;
        if (view == null && getActivity() != null) {
            view = getActivity().getLayoutInflater()
                    .inflate(R.layout.model_list_row, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(R.id.text1);
        textView.setText(artist.getName());

        ImageView imageView = (ImageView)view.findViewById(R.id.image_view);
        Image image = artist.getImage();
        if (image != null) {
            mImageStore.loadImage(image, imageView, true /* thumbnail */,
                    new ImageStore.Callback() {
                        @Override
                        public void onImageAvailable(final ImageView imageView,
                                                     final Drawable drawable) {
                            ImageAttacher.attach(getActivity(), imageView, drawable);
                        }
                    });
        }

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        AlbumArtist artist = (AlbumArtist)getListAdapter().getItem(position);

        blah(new AlbumListFragment(mImageStore, mDataStore));
    }

}
