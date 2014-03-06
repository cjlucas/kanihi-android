package net.cjlucas.kanihi.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.List;

@DatabaseTable(tableName = "tracks")
public class Track {
    @DatabaseField(id = true, columnName = "uuid")
    private String mUuid;

    @DatabaseField(columnName = "name")
    private String mTitle;

    @DatabaseField(columnName = "subtitle")
    private String subtitle;

    @DatabaseField(columnName = "num")
    private int mNum;

    @DatabaseField(columnName = "duration")
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