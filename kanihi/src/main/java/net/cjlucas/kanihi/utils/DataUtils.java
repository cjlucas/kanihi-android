package net.cjlucas.kanihi.utils;

import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataUtils {
    private static final String TAG = "DataUtils";

    public static <T> List<T> getList(CloseableIterator<T> iterator) {
        ArrayList<T> list = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                iterator.next();
                list.add(iterator.current());
            }
        } catch (SQLException e) {
            Log.e(TAG, "SQLException thrown while iterating", e);
        } finally {
            iterator.closeQuietly();
            return list;
        }
    }
}
