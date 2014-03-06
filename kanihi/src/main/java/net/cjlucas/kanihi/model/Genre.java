package net.cjlucas.kanihi.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "genres")
public class Genre {
    @DatabaseField(id = true, columnName = "uuid")
    private String uuid;

    @DatabaseField(columnName = "name")
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
