package net.cjlucas.kanihi.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "discs")
public class Disc {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_DISC_NUM = "disc_num";
    public static final String COLUMN_SUBTITLE = "subtitle";
    public static final String COLUMN_TOTAL_TRACKS = "total_tracks";
    public static final String COLUMN_ALBUM = "album_id";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_DISC_NUM)
    private int mDiscNum;

    @DatabaseField(columnName = COLUMN_SUBTITLE)
    private String mSubtitle;

    @DatabaseField(columnName = COLUMN_TOTAL_TRACKS)
    private int mTotalTracks;

    @DatabaseField(foreign = true, index = true, columnName = COLUMN_ALBUM)
    private Album mAlbum;

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public int getDiscNum() {
        return mDiscNum;
    }

    public void setDiscNum(int discNum) {
        mDiscNum = discNum;
    }

    public String getSubtitle() {
        return mSubtitle;
    }

    public void setSubtitle(String subtitle) {
        mSubtitle = subtitle;
    }

    public int getTotalTracks() {
        return mTotalTracks;
    }

    public void setTotalTracks(int totalTracks) {
        mTotalTracks = totalTracks;
    }

    public Album getAlbum() {
        return mAlbum;
    }

    public void setAlbum(Album album) {
        mAlbum = album;
    }
}
