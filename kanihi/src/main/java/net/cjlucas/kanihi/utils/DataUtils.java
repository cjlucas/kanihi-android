package net.cjlucas.kanihi.utils;

import com.j256.ormlite.dao.CloseableIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataUtils {
    public static <T> List<T> getList(CloseableIterator<T> iterator) {
        ArrayList<T> list = new ArrayList<>();

        while (iterator.hasNext()) {
            iterator.next();
            try {
                list.add(iterator.current());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        iterator.closeQuietly();
        return list;
    }
}
