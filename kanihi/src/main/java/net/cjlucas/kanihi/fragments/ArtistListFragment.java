package net.cjlucas.kanihi.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.stmt.PreparedQuery;

import net.cjlucas.kanihi.R;
import net.cjlucas.kanihi.data.ImageStore;
import net.cjlucas.kanihi.models.AlbumArtist;
import net.cjlucas.kanihi.models.Image;

/**
 * Created by chris on 3/10/14.
 */
public class ArtistListFragment extends ModelListFragment<AlbumArtist>
    implements View.OnClickListener {

//    private ImageStore mImageStore;

    @Override
    public Class<AlbumArtist> getGenericClass() {
        return AlbumArtist.class;
    }

    @Override
    public PreparedQuery<AlbumArtist> getDefaultQuery(Class ancestorClazz, String ancestorUuid) {
        return mDataService.getAlbumArtistsQuery(ancestorClazz, ancestorUuid,
                AlbumArtist.COLUMN_NAME, true);
    }

    public View getRowView(AlbumArtist artist, View reusableView, ViewGroup viewGroup) {
        View view = reusableView;
        if (view == null && getActivity() != null) {
            view = getActivity().getLayoutInflater()
                    .inflate(R.layout.model_list_row, viewGroup, false);
        }

        TextView textView = (TextView)view.findViewById(R.id.text1);
        textView.setText(artist.getName());

        view.findViewById(R.id.options_menu_btn).setOnClickListener(this);
        ((ImageView)view.findViewById(R.id.detail_one_image)).setImageResource(R.drawable.disc);
        ((TextView)view.findViewById(R.id.detail_one_text)).
                setText(String.valueOf(artist.getAlbumCount()));

        ((ImageView)view.findViewById(R.id.detail_two_image)).setImageResource(R.drawable.note);
        ((TextView)view.findViewById(R.id.detail_two_text)).
                setText(String.valueOf(artist.getTrackCount()));

        ImageView imageView = (ImageView)view.findViewById(R.id.image_view);
        Image image = artist.getImage();
        if (image != null && mImageStore != null) {
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

        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putSerializable(ARG_ANCESTOR_CLASS, AlbumArtist.class);
        fragmentArgs.putString(ARG_ANCESTOR_UUID, artist.getUuid());

        AlbumListFragment fragment = new AlbumListFragment();
        fragment.setArguments(fragmentArgs);

        blah(fragment);
    }

//    @Override
    public void onImageServiceConnection() {
//        ListView listView = getListView();
//        ListAdapter listAdapter = getListAdapter();
//        if (listView != null && listAdapter != null) {
//            int first = listView.getFirstVisiblePosition();
//            int last = listView.getLastVisiblePosition();
//            for (int i = first; i <= last; i++) {
//                Image image = ((AlbumArtist)listAdapter.getItem(i)).getImage();
//                mImageStore.loadImage();
//
//            }
//        }
    }

//    @Override
    public void onDataServiceConnection() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.options_menu_btn:
                onActionMenuButtonClicked();
                break;
        }
    }
}
