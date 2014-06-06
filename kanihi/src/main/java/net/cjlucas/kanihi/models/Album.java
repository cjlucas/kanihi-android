package net.cjlucas.kanihi.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import net.cjlucas.kanihi.models.interfaces.ImageRepresentation;
import net.cjlucas.kanihi.models.interfaces.UniqueModel;

@DatabaseTable(tableName = "albums")
public class Album implements UniqueModel, ImageRepresentation {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TOTAL_DISCS = "total_discs";
    public static final String COLUMN_ALBUM_ARTIST = "album_artist_id";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_TRACK_COUNT = "track_count";
    public static final String COLUMN_ALBUM_DURATION = "album_duration";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_TITLE)
    private String mTitle;

    @DatabaseField(columnName = COLUMN_TOTAL_DISCS)
    private int mTotalDiscs;

    @DatabaseField(columnName = COLUMN_TRACK_COUNT)
    private long mTrackCount;

    @DatabaseField(columnName = COLUMN_ALBUM_DURATION)
    private long mAlbumDuration;

    @DatabaseField(foreign = true, index = true, columnName = COLUMN_ALBUM_ARTIST)
    private AlbumArtist mAlbumArtist;

    @DatabaseField(foreign = true, columnName = COLUMN_IMAGE)
    private Image mImage;

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

    public long getTrackCount() {
        return mTrackCount;
    }

    public void setTrackCount(long trackCount) {
        mTrackCount = trackCount;
    }

    public long getAlbumDuration() {
        return mAlbumDuration;
    }

    public void setAlbumDuration(long albumDuration) {
        mAlbumDuration = albumDuration;
    }

    public ForeignCollection<Disc> getDiscs() {
        return mDiscs;
    }

    public Image getImage() {
        return mImage;
    }

    public void setImage(Image image) {
        mImage = image;
    }
}
