package net.cjlucas.kanihi.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "album_artists")
public class AlbumArtist {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SORT_NAME = "sort_name";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String mUuid;

    @DatabaseField(columnName = COLUMN_NAME)
    private String mName;

    @DatabaseField(columnName = COLUMN_SORT_NAME)
    private String mSortName;

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

    public ForeignCollection<Album> getAlbums() {
        return mAlbums;
    }
}