package net.cjlucas.kanihi.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "track_artists")
public class TrackArtist {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SORT_NAME = "sort_name";
    public static final String COLUMN_TRACK_COUNT = "track_count";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_NAME)
    private String mName;

    @DatabaseField(columnName = COLUMN_SORT_NAME)
    private String mSortName;

    @DatabaseField(columnName = COLUMN_TRACK_COUNT)
    private long mTrackCount;

    @ForeignCollectionField
    private ForeignCollection<Track> mTracks;

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getSortName() {
        return mSortName;
    }

    public void setSortName(String sortName) {
        mSortName = sortName;
    }

    public long getTrackCount() {
        return mTrackCount;
    }

    public void setTrackCount(long trackCount) {
        mTrackCount = trackCount;
    }

    public ForeignCollection<Track> getTracks() {
        return mTracks;
    }
}
