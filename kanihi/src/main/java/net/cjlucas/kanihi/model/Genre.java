package net.cjlucas.kanihi.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "genres")
public class Genre {
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_NAME = "name";

    @DatabaseField(id = true, columnName = COLUMN_UUID)
    private String uuid;

    @DatabaseField(columnName = COLUMN_NAME)
    private String name;

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

    public ForeignCollection<Track> getTracks() {
        return mTracks;
    }
}
