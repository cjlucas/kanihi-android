package net.cjlucas.kanihi.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import net.cjlucas.kanihi.models.interfaces.ImageRepresentation;

@DatabaseTable(tableName = "genres")
public class Genre implements ImageRepresentation {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_IMAGE = "image";
    public static final String COLUMN_TRACK_COUNT = "track_count";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String uuid;

    @DatabaseField(columnName = COLUMN_NAME)
    private String name;

    @DatabaseField(columnName = COLUMN_TRACK_COUNT)
    private long mTrackCount;

    @DatabaseField(foreign = true, columnName = COLUMN_IMAGE)
    private Image mImage;

    @ForeignCollectionField
    private ForeignCollection<Track> mTracks;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTrackCount() {
        return mTrackCount;
    }

    public void setTrackCount(long trackCount) {
        mTrackCount = trackCount;
    }

    public Image getImage() {
        return mImage;
    }

    public void setImage(Image image) {
        mImage = image;
    }

    public ForeignCollection<Track> getTracks() {
        return mTracks;
    }
}
