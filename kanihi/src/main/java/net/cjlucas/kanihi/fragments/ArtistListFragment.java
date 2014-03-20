package net.cjlucas.kanihi.fragments;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.DataStore;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.models.AlbumArtist;
import net.cjlucas.kanihi.models.Image;

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
    public int executeDefaultQuery() {
        return mDataStore.getAlbumArtists();
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
        int token = mDataStore.getAlbums(artist);

        blah(new AlbumListFragment(mImageStore, mDataStore), token);
    }
}
