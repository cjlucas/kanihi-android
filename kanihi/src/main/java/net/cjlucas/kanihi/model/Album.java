package net.cjlucas.kanihi.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "albums")
public class Album {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TOTAL_DISCS = "total_discs";
    public static final String COLUMN_ALBUM_ARTIST = "album_artist_id";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_TITLE)
    private String mTitle;

    @DatabaseField(columnName = COLUMN_TOTAL_DISCS)
    private int mTotalDiscs;

    @DatabaseField(foreign = true, index = true, columnName = COLUMN_ALBUM_ARTIST)
    private AlbumArtist mAlbumArtist;

    @ForeignCollectionField
    private ForeignCollection<Disc> mDiscs;

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

    public AlbumArtist getAlbumArtist() {
        return mAlbumArtist;
    }

    public void setAlbumArtist(AlbumArtist albumArtist) {
        mAlbumArtist = albumArtist;
    }

    public ForeignCollection<Disc> getDiscs() {
        return mDiscs;
    }
}
