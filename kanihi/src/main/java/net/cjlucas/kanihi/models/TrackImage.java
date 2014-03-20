package net.cjlucas.kanihi.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "track_image")
public class TrackImage {
    public static final String COLUMN_ID    = "id";
    public static final String COLUMN_TRACK_ID = "track_id";
    public static final String COLUMN_IMAGE_ID = "image_id";

    @DatabaseField(generatedId = true, columnName = COLUMN_ID)
    private int mId;

    @DatabaseField(uniqueCombo = true, canBeNull = false, columnName = COLUMN_TRACK_ID)
    private String mTrackId;

    @DatabaseField(uniqueCombo = true, canBeNull = false, columnName = COLUMN_IMAGE_ID)
    private String mImageId;

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getTrackId() {
        return mTrackId;
    }

    public void setTrackId(String trackId) {
        mTrackId = trackId;
    }

    public String getImageId() {
        return mImageId;
    }

    public void setImageId(String imageId) {
        mImageId = imageId;
    }
}
