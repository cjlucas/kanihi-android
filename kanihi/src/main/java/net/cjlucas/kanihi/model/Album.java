package net.cjlucas.kanihi.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "albums")
public class Album {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TOTAL_DISCS = "total_discs";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_TITLE)
    private String mTitle;

    @DatabaseField(columnName = COLUMN_TOTAL_DISCS)
    private int mTotalDiscs;

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public int getTotalDiscs() {
        return mTotalDiscs;
    }

    public void setTotalDiscs(int totalDiscs) {
        mTotalDiscs = totalDiscs;
    }
}
