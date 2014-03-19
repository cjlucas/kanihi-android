package net.cjlucas.kanihi.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "images")
public class Image {
    public static final String COLUMN_ID = "id";

    @DatabaseField(id = true, columnName = COLUMN_ID)
    private String mId;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }
}