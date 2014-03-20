package net.cjlucas.kanihi.models;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "tracks")
public class Track {
    public static final String COLUMN_UUID          = "uuid";
    public static final String COLUMN_TITLE         = "title";
    public static final String COLUMN_SUBTITLE      = "subtitle";
    public static final String COLUMN_TRACK_NUM     = "track_num";
    public static final String COLUMN_DURATION      = "duration";
    public static final String COLUMN_GENRE         = "genre_id";
    public static final String COLUMN_DISC          = "disc_id";
    public static final String COLUMN_TRACK_ARTIST  = "track_artist_id";
    public static final String COLUMN_DATE          = "date";
    public static final String COLUMN_ORIGINAL_DATE = "orig_date";

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

    @DatabaseField(dataType = DataType.DATE, columnName = COLUMN_DATE)
    private Date mDate;

    @DatabaseField(dataType = DataType.DATE, columnName = COLUMN_ORIGINAL_DATE)
    private Date mOriginalDate;

    @DatabaseField(foreign = true, index = true, columnName = COLUMN_GENRE)
    private Genre genre;

    @DatabaseField(foreign = true, index = true, columnName = COLUMN_DISC)
    private Disc mDisc;

    @DatabaseField(foreign = true, index = true, columnName = COLUMN_TRACK_ARTIST)
    private TrackArtist mTrackArtist;


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

    public Disc getDisc() {
        return mDisc;
    }

    public void setDisc(Disc disc) {
        mDisc = disc;
    }

    public TrackArtist getTrackArtist() {
        return mTrackArtist;
    }

    public void setTrackArtist(TrackArtist trackArtist) {
        mTrackArtist = trackArtist;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public Date getOriginalDate() {
        return mOriginalDate;
    }

    public void setOriginalDate(Date originalDate) {
        mOriginalDate = originalDate;
    }
}