package net.cjlucas.kanihi.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.cjlucas.kanihi.data.parser.JsonTrackArrayParser;
import net.cjlucas.kanihi.model.*;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public class DataStore {
    private static final String TAG = "DataStore";

    private static DataStore mSharedDataStore;

    private Context mContext;
    private DatabaseHelper mDatabaseHelper;

    public DataStore(Context context) {
        mContext = context;
        mDatabaseHelper = new DatabaseHelper(mContext);
        try {
            mDatabaseHelper.getWritableDatabase();
        } catch (SQLiteException e) {}
    }

    public void close() {
        mDatabaseHelper.close();
    }

    public static synchronized DataStore setupInstance(Context context) {
        if (mSharedDataStore == null) {
            mSharedDataStore = new DataStore(context);
        }

        return mSharedDataStore;
    }

    public static synchronized DataStore getInstance() {
        return mSharedDataStore;
    }

    public void update(InputStream in) {
        JsonTrackArrayParser parser = new JsonTrackArrayParser(in);

        List<Track> tracks = parser.getTracks();
        for (Track track : tracks) {
            Genre genre = track.getGenre();
            mDatabaseHelper.createOrUpdate(mDatabaseHelper.getGenreDao(), genre);

            TrackArtist trackArtist = track.getTrackArtist();
            if (trackArtist != null) mDatabaseHelper.createOrUpdate(
                    mDatabaseHelper.getTrackArtistDao(), trackArtist);

            Disc disc = track.getDisc();
            if (disc != null) {
                mDatabaseHelper.createOrUpdate(mDatabaseHelper.getDiscDao(), disc);

                Album album = disc.getAlbum();
                if (album != null) {
                    mDatabaseHelper.createOrUpdate(mDatabaseHelper.getAlbumDao(), album);

                    AlbumArtist albumArtist = album.getAlbumArtist();
                    if (albumArtist != null) mDatabaseHelper.createOrUpdate(
                            mDatabaseHelper.getAlbumArtistDao(), albumArtist);
                }
            }

            mDatabaseHelper.createOrUpdate(mDatabaseHelper.getTrackDao(), track);
        }

    }

    public void addTrack(Track track) {
        try {
            mDatabaseHelper.getTrackDao().createOrUpdate(track);
        } catch (SQLException e) { Log.e(TAG, "addTrack error"); }
    }

    private class DatabaseHelper extends OrmLiteSqliteOpenHelper {
        private Dao<Track, String> mTrackDao;
        private Dao<Genre, String> mGenreDao;
        private Dao<Disc, String> mDiscDao;
        private Dao<Album, String> mAlbumDao;
        private Dao<TrackArtist, String> mTrackArtistDao;
        private Dao<AlbumArtist, String> mAlbumArtistDao;

        public DatabaseHelper(Context context) {
            super(context, "kanihi.sqlite", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
            Log.v(TAG, "onCreate");
//            db.enableWriteAheadLogging();
            try {
                TableUtils.createTable(connectionSource, Track.class);
                TableUtils.createTable(connectionSource, Genre.class);
                TableUtils.createTable(connectionSource, Disc.class);
                TableUtils.createTable(connectionSource, Album.class);
                TableUtils.createTable(connectionSource, TrackArtist.class);
                TableUtils.createTable(connectionSource, AlbumArtist.class);
            } catch (SQLException e) {
                Log.e(TAG, "Couldn't create table");
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int i, int i2) {

        }

        private Dao<?, ?> getDaoCatch(Class clazz) {
            try {
                return getDao(clazz);
            } catch (SQLException e) {
                Log.e(TAG, "Could not create DAO for " + clazz.getName());
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public void createOrUpdate(Dao dao, Object o) {
            try {
                dao.createOrUpdate(o);
            } catch (SQLException e) {
                Log.e(TAG, "error with createOrUpdate");
            }
        }

        @SuppressWarnings("unchecked")
        public synchronized Dao<Track, String> getTrackDao() {
            if (mTrackDao == null) {
                mTrackDao = (Dao<Track, String>)getDaoCatch(Track.class);
            }

            return mTrackDao;
        }

        @SuppressWarnings("unchecked")
        public synchronized Dao<Genre, String> getGenreDao() {
            if (mGenreDao == null) {
                mGenreDao = (Dao<Genre, String>)getDaoCatch(Genre.class);
            }

            return mGenreDao;
        }

        @SuppressWarnings("unchecked")
        public synchronized Dao<Disc, String> getDiscDao() {
            if (mDiscDao == null) {
                mDiscDao = (Dao<Disc, String>)getDaoCatch(Disc.class);
            }

            return mDiscDao;
        }

        @SuppressWarnings("unchecked")
        public synchronized Dao<Album, String> getAlbumDao() {
            if (mAlbumDao == null) {
                mAlbumDao = (Dao<Album, String>)getDaoCatch(Album.class);
            }

            return mAlbumDao;
        }

        @SuppressWarnings("unchecked")
        public synchronized Dao<TrackArtist, String> getTrackArtistDao() {
            if (mTrackArtistDao == null) {
                mTrackArtistDao = (Dao<TrackArtist, String>)getDaoCatch(TrackArtist.class);
            }

            return mTrackArtistDao;
        }

        @SuppressWarnings("unchecked")
        public synchronized Dao<AlbumArtist, String> getAlbumArtistDao() {
            if (mAlbumArtistDao == null) {
                mAlbumArtistDao = (Dao<AlbumArtist, String>)getDaoCatch(AlbumArtist.class);
            }

            return mAlbumArtistDao;
        }
    }
}