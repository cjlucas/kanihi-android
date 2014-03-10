package net.cjlucas.kanihi.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.parser.JsonTrackArrayParser;
import net.cjlucas.kanihi.model.*;
import net.minidev.json.JSONArray;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DataStore extends Thread implements Handler.Callback {
    private static final String TAG = "DataStore";

    private static DataStore mSharedDataStore;

    private Context mContext;
    private Handler mHandler;
    private DatabaseHelper mDatabaseHelper;
    private AsyncQueryMonitor mQueryMonitor;
    private HashSet<Object> mModelCache;

    enum MessageType {
        UPDATE_DB(1),
        TRACK_DATA_RECEIVED(1 << 1);

        public static MessageType forValue(int value) {
            for (MessageType type : MessageType.values()) {
                if (type.value == value)
                    return type;
            }
            return null;
        }

        public final int value;

        MessageType(int value) {
            this.value = value;
        }
    }

    private DataStore(Context context) {
        mContext = context;
        mDatabaseHelper = new DatabaseHelper(mContext);
        mQueryMonitor = new AsyncQueryMonitor();
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
            mSharedDataStore.start();
            // noop until handler is initialized
            while (mSharedDataStore.mHandler == null) {

            }
        }

        return mSharedDataStore;
    }

    public static synchronized DataStore getInstance() {
        return mSharedDataStore;
    }

    public void registerQueryMonitorListener(int token,
                                             AsyncQueryMonitor.Listener<?> listener) {
        mQueryMonitor.registerListener(listener, token);
    }

    public int getTracks() {
        Dao<Track, ?> dao = mDatabaseHelper.getTrackDao();
        try {
            PreparedQuery<Track> query = dao.queryBuilder().prepare();
            return mDatabaseHelper.submit(dao, query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update() {
        mHandler.sendEmptyMessage(MessageType.UPDATE_DB.value);
    }

    public void run() {
        Looper.prepare();
        mHandler = new Handler(this);
        Looper.loop();
    }

    public boolean handleMessage(Message message) {
        switch (MessageType.forValue(message.what)) {
            case UPDATE_DB:
                handleUpdateDb(message);
                break;
            case TRACK_DATA_RECEIVED:
                handleTrackDataReceived(message);
                break;
            default:
                throw new RuntimeException("Unknown message received: " + message);
        }
        return true;
    }

    private void handleUpdateDb(Message message) {
        if (mModelCache == null) mModelCache = new HashSet<>(5000); // TODO: figure out when to delete this

        ApiHttpClient.Callback<JSONArray> callback = new ApiHttpClient.Callback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data) {
                Log.v(TAG, "received data of size " + data.size());
                Message msg = mHandler.obtainMessage(MessageType.TRACK_DATA_RECEIVED.value);
                msg.obj = data;
                mHandler.sendMessage(msg);
            }
            @Override
            public void onFailure() {
                Log.e(TAG, "failure with ApiHttpClient.getTracks");
            }
        };

        for (int offset = 0; offset <= 5000; offset += 1000) {
            ApiHttpClient.getTracks(offset, 1000, null, callback);
        }
    }

    private <T> boolean isInCache(T object) {
        if (mModelCache.contains(object)) return true;

        mModelCache.add(object);
        return false;
    }

    private void handleTrackDataReceived(Message message) {
        JSONArray data = (JSONArray)message.obj;

        final List<Track> tracks = JsonTrackArrayParser.getTracks(data);
        mDatabaseHelper.transaction(new Callable<Void>(){
            @Override
            public Void call() throws Exception{
                for (Track track : tracks) {
                    Genre genre = track.getGenre();
                    if (genre != null && !isInCache(genre.getUuid())) {
                        mDatabaseHelper.createOrUpdate(mDatabaseHelper.getGenreDao(), genre);
                    }

                    TrackArtist trackArtist = track.getTrackArtist();
                    if (trackArtist != null && !isInCache(trackArtist.getUuid())) {
                        mDatabaseHelper.createOrUpdate(mDatabaseHelper.getTrackArtistDao(), trackArtist);
                    }

                    Disc disc = track.getDisc();
                    if (disc != null && !isInCache(disc.getUuid())) {
                        mDatabaseHelper.createOrUpdate(mDatabaseHelper.getDiscDao(), disc);

                        Album album = disc.getAlbum();
                        if (album != null && !isInCache(album.getUuid())) {
                            mDatabaseHelper.createOrUpdate(mDatabaseHelper.getAlbumDao(), album);

                            AlbumArtist albumArtist = album.getAlbumArtist();
                            if (albumArtist != null && !isInCache(albumArtist.getUuid())) {
                                mDatabaseHelper.createOrUpdate(mDatabaseHelper.getAlbumArtistDao(), albumArtist);
                            }
                        }
                    }

//                    mDatabaseHelper.createOrUpdate(mDatabaseHelper.getTrackDao(), track);
                    mDatabaseHelper.getTrackDao().create(track);
                }
                return null;
            }
        });
    }

    private class DatabaseHelper extends OrmLiteSqliteOpenHelper {
        private Dao<Track, String> mTrackDao;
        private Dao<Genre, String> mGenreDao;
        private Dao<Disc, String> mDiscDao;
        private Dao<Album, String> mAlbumDao;
        private Dao<TrackArtist, String> mTrackArtistDao;
        private Dao<AlbumArtist, String> mAlbumArtistDao;
        private ThreadPoolExecutor mExecutor;
        private int createOrUpdateCount;

        public DatabaseHelper(Context context) {
            super(context, "kanihi.sqlite", null, 1);
            setWriteAheadLoggingEnabled(true);

            mExecutor = new ThreadPoolExecutor(
                    4, 10, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20));
        }

        @Override
        public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
            Log.v(TAG, "onCreate");
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

        public <T> int submit(final Dao<T, ?> dao, final PreparedQuery<T> query) {
            int id;
            do {
                id = new Random().nextInt();
            } while (mQueryMonitor.isTokenUsed(id));

            final int token = id;

            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mQueryMonitor.putIterator(token, dao.iterator(query));
                    } catch (SQLException e) {
                    }
                }
            });

            return token;
        }

        public <V> void transaction(final Callable<V> callable) {
            try {
                long start = System.currentTimeMillis();
                TransactionManager.callInTransaction(getConnectionSource(), callable);
                long duration = System.currentTimeMillis() - start;
                Log.i(TAG, "Transaction took " + (duration / 1000.0) + " seconds");
                Log.i(TAG, "createOrUpdateCount: " + createOrUpdateCount);
            } catch (SQLException e) {
                Log.e(TAG, "caught SQLException", e);
            }
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
            createOrUpdateCount++;
            try {
                dao.createOrUpdate(o);
            } catch (SQLException e) {
                Log.e(TAG, "error with createOrUpdate");
                throw new RuntimeException(e);
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