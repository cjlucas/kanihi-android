package net.cjlucas.kanihi.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import net.cjlucas.kanihi.models.interfaces.ImageRepresentation;
import net.cjlucas.kanihi.models.interfaces.UniqueModel;

@DatabaseTable(tableName = "album_artists")
public class AlbumArtist implements UniqueModel, ImageRepresentation {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SORT_NAME = "sort_name";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_ALBUM_COUNT = "album_count";
    public static final String COLUMN_TRACK_COUNT = "track_count";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_NAME)
    private String mName;

    @DatabaseField(columnName = COLUMN_SORT_NAME)
    private String mSortName;

    @DatabaseField(columnName = COLUMN_ALBUM_COUNT)
    private long mAlbumCount;
    
    @DatabaseField(columnName = COLUMN_TRACK_COUNT)
    private long mTrackCount;

    @DatabaseField(foreign = true, columnName = COLUMN_IMAGE)
    private Image mImage;

    @ForeignCollectionField
    private ForeignCollection<Album> mAlbums;

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

    public long getAlbumCount() {
        return mAlbumCount;
    }

    public void setAlbumCount(long albumCount) {
        mAlbumCount = albumCount;
    }

    public long getTrackCount() {
        return mTrackCount;
    }

    public void setTrackCount(long trackCount) {
        mTrackCount = trackCount;
    }

    public ForeignCollection<Album> getAlbums() {
        return mAlbums;
    }

    public Image getImage() {
        return mImage;
    }

    public void setImage(Image image) {
        mImage = image;
    }
}
