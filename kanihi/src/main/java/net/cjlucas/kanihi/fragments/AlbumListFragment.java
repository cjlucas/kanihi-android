package net.cjlucas.kanihi.fragments;

import android.app.Fragment;
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
import net.cjlucas.kanihi.models.Album;
import net.cjlucas.kanihi.models.Image;
import net.cjlucas.kanihi.utils.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chris on 3/11/14.
 */
public class AlbumListFragment extends ModelListFragment<Album> {
    private static final String TAG = "AlbumListFragment";

    private Map<View, RowViewHolder> mRowViewHolders = new HashMap<>();

    private static class RowViewHolder {
        private ImageView primaryImageView;
        private TextView primaryTextView;

        private ImageView detailOneImageView;
        private TextView detailOneTextView;

        private ImageView detailTwoImageView;
        private TextView detailTwoTextView;
    }

    @Override
    public Class<Album> getGenericClass() {
        return Album.class;
    }

    @Override
    public PreparedQuery<Album> getDefaultQuery(Class ancestorClazz, String ancestorUuid) {
        return mDataService.getAlbumsQuery(ancestorClazz, ancestorUuid, Album.COLUMN_TITLE, true);
    }

    public View getRowView(Album album, View reusableView, ViewGroup viewGroup) {
        long start = System.currentTimeMillis();
        View view = reusableView;
        if (view == null && getActivity() != null) {
            view = getActivity().getLayoutInflater()
                    .inflate(R.layout.model_list_row, viewGroup, false);

            RowViewHolder holder = new RowViewHolder();
            holder.primaryImageView = (ImageView)view.findViewById(R.id.image_view);
            holder.primaryTextView = (TextView)view.findViewById(R.id.text1);
            holder.detailOneImageView = (ImageView)view.findViewById(R.id.detail_one_image);
            holder.detailOneTextView = (TextView)view.findViewById(R.id.detail_one_text);
            holder.detailTwoImageView = (ImageView)view.findViewById(R.id.detail_two_image);
            holder.detailTwoTextView = (TextView)view.findViewById(R.id.detail_two_text);

            holder.detailOneImageView.setImageResource(R.drawable.note);
            holder.detailTwoImageView.setImageResource(R.drawable.clock);

            mRowViewHolders.put(view, holder);
        }

        RowViewHolder holder = mRowViewHolders.get(view);

        holder.primaryTextView.setText(album.getTitle());
        holder.detailOneTextView.setText(String.valueOf(album.getTrackCount()));
        holder.detailTwoTextView.setText(TextUtils.getTimeCode(album.getAlbumDuration()));

        Image image = album.getImage();
        if (image != null) {
            mImageStore.loadImage(image, holder.primaryImageView, true, new ImageStore.Callback() {
                @Override
                public void onImageAvailable(final ImageView imageView, final Drawable drawable) {
                    ImageAttacher.attach(getActivity(), imageView, drawable);
                }
            });
        }

        Log.d(TAG, "getRowView took (in ms): " + (System.currentTimeMillis() - start));
        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Album album = (Album)getListAdapter().getItem(position);

        Bundle args = new Bundle();
        args.putString(SingleAlbumListFragment.ARG_ALBUM_UUID, album.getUuid());
        Fragment fragment = new SingleAlbumListFragment();
        fragment.setArguments(args);
        blah(fragment);
    }
}
