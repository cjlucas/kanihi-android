package net.cjlucas.kanihi.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.List;

@DatabaseTable(tableName = "tracks")
public class Track {
    public static final String COLUMN_UUID         = "uuid";
    public static final String COLUMN_TITLE        = "title";
    public static final String COLUMN_SUBTITLE     = "subtitle";
    public static final String COLUMN_TRACK_NUM    = "track_num";
    public static final String COLUMN_DURATION     = "duration";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_TITLE)
    private String mTitle;

    @DatabaseField(columnName = COLUMN_SUBTITLE)
    private String subtitle;

    @DatabaseField(columnName = COLUMN_TRACK_NUM)
    private int mNum;

    @DatabaseField(columnName = COLUMN_DURATION)
    private int duration;

    @DatabaseField(foreign = true)
    private Genre genre;

    public Track() {

    }

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        mUuid = uuid;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String name) {
        mTitle = name;
    }

    public int getNum() {
        return mNum;
    }

    public void setNum(int num) {
        mNum = num;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }
}