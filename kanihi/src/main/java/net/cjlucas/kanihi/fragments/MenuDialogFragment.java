package net.cjlucas.kanihi.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import net.cjlucas.kanihi.R;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.ConcurrentModificationException;

/**
 * Created by chris on 6/5/14.
 */
public class MenuDialogFragment extends DialogFragment {
    private final static String TAG = "MenuDialogFragment";
    public final static String ARG_ITEM_COUNT = "item_count";
    public final static String ARG_MENU_ITEM_RES_ID_ARRAY = "res_id_array";
    public final static String ARG_MENU_ITEM_TEXT_ARRAY = "menu_item_text_array";

    private GridView mGridView;

    public static class MenuDialogItem {
        private int mResourceId;
        private String mText;

        public MenuDialogItem(int resId, String text) {
            mResourceId = resId;
            mText = text;
        }
    }

    private class MenuDialogAdapter extends BaseAdapter {
        private MenuDialogItem[] mItems;

        public MenuDialogAdapter(MenuDialogItem[] items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.length;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mItems[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = getActivity().getLayoutInflater()
                    .inflate(R.layout.menu_dialog_item, parent, false);
            ImageView imageView = (ImageView)view.findViewById(R.id.menu_item_image);
            TextView textView = (TextView)view.findViewById(R.id.menu_item_text);
            MenuDialogItem item = (MenuDialogItem)getItem(position);

            imageView.setImageResource(item.mResourceId);
            textView.setText(item.mText);

            return view;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        return new Dialog(getActivity()) {
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setTitle("Select an Action");

                setContentView(R.layout.menu_dialog);

                mGridView = (GridView)findViewById(R.id.grid_view);


                if (getActivity() != null && getArguments() != null) {
                    int count = getArguments().getInt(ARG_ITEM_COUNT, -1);
                    if (count == -1) {
                        throw new IllegalArgumentException("Bundle is missing ARG_ITEM_COUNT");
                    }

                    MenuDialogItem[] items = new MenuDialogItem[count];
                    getMenuDialogItemArray(items);
                    mGridView.setAdapter(new MenuDialogAdapter(items));
                }
            }
        };
    }

    private void getMenuDialogItemArray(MenuDialogItem[] items) {
        Bundle args = getArguments();

        if (args == null) {
            throw new IllegalArgumentException("Bundle can't be null");
        }

        int[] resIds = args.getIntArray(ARG_MENU_ITEM_RES_ID_ARRAY);
        String[] texts = args.getStringArray(ARG_MENU_ITEM_TEXT_ARRAY);

        if (resIds == null || texts == null) {
            throw new IllegalArgumentException("Bundle is missing required data");
        }

        for (int i = 0; i < items.length; i++) {
            Log.d(TAG, "i am here yo");
            items[i] = new MenuDialogItem(resIds[i], texts[i]);
        }

    }
}
