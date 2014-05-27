package net.cjlucas.kanihi.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.cjlucas.kanihi.api.ApiHttpClient;
import net.cjlucas.kanihi.data.parser.JsonTrackArrayParser;
import net.cjlucas.kanihi.models.Album;
import net.cjlucas.kanihi.models.AlbumArtist;
import net.cjlucas.kanihi.models.Disc;
import net.cjlucas.kanihi.models.Genre;
import net.cjlucas.kanihi.models.Image;
import net.cjlucas.kanihi.models.interfaces.ImageRepresentation;
import net.cjlucas.kanihi.models.Track;
import net.cjlucas.kanihi.models.TrackArtist;
import net.cjlucas.kanihi.models.TrackImage;
import net.cjlucas.kanihi.prefs.GlobalPrefs;
import net.minidev.json.JSONArray;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DataStore extends Thread implements Handler.Callback {
    private static final String TAG = "DataStore";
    private static final int TRACK_LIMIT = 500;

    private Context mContext;
    private ApiHttpClient mApiHttpClient;
    private Handler mHandler;
    private DatabaseHelper mDbHelper;
    private AsyncQueryMonitor mQueryMonitor;
    private HashSet<Object> mModelCache;
    private UpdateDbProgress mUpdateDbProgress;

    public class UpdateDbProgress {
        public int mCurrentTrack;
        public int mTotalTracks;
        public int mLoadTrackApiCallsRemaining;
        public String mLastUpdatedAt;
        public String mServerTime; // at start of update

        private boolean hasLoadTrackApiCallsRemaining() {
            return mLoadTrackApiCallsRemaining > 0;
        }
    }

    enum MessageType {
        UPDATE_DB(1),
        TRACK_DATA_RECEIVED(1 << 1),
        ASSOCIATE_IMAGES(1 << 2),
        UPDATE_COUNTS(1 << 3),
        FINALIZE_UPDATE(1 << 4);

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

    private DataStore(Context context, ApiHttpClient apiHttpClient) {
        mContext = context;
        mApiHttpClient = apiHttpClient;
        mDbHelper = new DatabaseHelper(mContext);
        mQueryMonitor = new AsyncQueryMonitor();
        try {
            mDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {}
    }

    public void close() {
        mDbHelper.close();
    }

    public static synchronized DataStore newInstance(Context context,
                                                     ApiHttpClient apiHttpClient) {
        DataStore dataStore = new DataStore(context, apiHttpClient);
        dataStore.start();
        // noop until handler is initialized
        while (dataStore.mHandler == null) {

        }
        return dataStore;
    }

    public void registerQueryMonitorListener(int token,
                                             AsyncQueryMonitor.Listener<?> listener) {
        mQueryMonitor.registerListener(listener, token);
    }

    public void unregisterQueryMonitorListener(int token) {
        mQueryMonitor.unregisterListener(token);
    }

    public void closeQuery(int token) {
        mQueryMonitor.closeQuery(token);
    }

    private int getTracks(Where<Track, String> where) throws SQLException {
        return mDbHelper.submit(Track.class,
                mDbHelper.preparedQuery(Track.class, where, Track.COLUMN_TITLE, true));
    }

    public int getTracks() {
        try {
            return mDbHelper.submit(Track.class,
                    mDbHelper.preparedQuery(Track.class, null, Track.COLUMN_TITLE, true));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Where<Track, String> tracksWhere(TrackArtist artist){
        try {
            return mDbHelper.where(Track.class).in(Track.COLUMN_TRACK_ARTIST, artist);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Where<Track, String> tracksWhere(Disc disc) {
        try {
            return mDbHelper.where(Track.class).in(Track.COLUMN_DISC, disc);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Where<Track, String> tracksWhere(Genre genre) {
        try {
            return mDbHelper.where(Track.class).in(Track.COLUMN_GENRE, genre);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Where<Track, String> tracksWhere(Album album) {
        try {
            // get the discs from the album
            QueryBuilder<Disc, String> discsQb = mDbHelper.qb(Disc.class);
            Where<Disc, String> discsWhere = discsQb.where().eq(Disc.COLUMN_ALBUM, album);
            discsQb.selectColumns(Disc.COLUMN_UUID);
            discsQb.setWhere(discsWhere);

            return mDbHelper.where(Track.class).in(Track.COLUMN_DISC, discsQb);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Where<Track, String> tracksWhere(AlbumArtist artist) {
        try {
            // get the albums from the album artist
            QueryBuilder<Album, String> albumsQb = mDbHelper.qb(Album.class);
            Where<Album, String> albumsWhere = albumsQb.where().eq(Album.COLUMN_ALBUM_ARTIST, artist);
            albumsQb.selectColumns(Album.COLUMN_UUID);
            albumsQb.setWhere(albumsWhere);

            // get the discs from the albums
            QueryBuilder<Disc, String> discsQb = mDbHelper.qb(Disc.class);
            Where<Disc, String> discsWhere = discsQb.where().in(Disc.COLUMN_ALBUM, albumsQb);
            discsQb.selectColumns(Disc.COLUMN_UUID);
            discsQb.setWhere(discsWhere);

            // get the tracks from the discs
            return mDbHelper.where(Track.class).in(Track.COLUMN_DISC, discsQb);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Where<Track, String> tracksWhereForObject(Object object) {
        if (object instanceof TrackArtist) return tracksWhere((TrackArtist) object);
        if (object instanceof Genre) return tracksWhere((Genre) object);
        if (object instanceof Disc) return tracksWhere((Disc) object);
        if (object instanceof Album) return tracksWhere((Album) object);
        if (object instanceof AlbumArtist) return tracksWhere((AlbumArtist) object);

        throw new IllegalArgumentException(
                "Unexpected class given: " + object.getClass().getName());
    }

    public int getTracks(Object object) {
        try {
            return getTracks(tracksWhereForObject(object));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Track> getTracksSync(Object object) {
        try {
            return tracksWhereForObject(object).query();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getAlbumArtists() {
        return mDbHelper.submit(AlbumArtist.class, null, AlbumArtist.COLUMN_NAME, true);
    }

    public int getAlbums() {
        return mDbHelper.submit(Album.class, null, Album.COLUMN_TITLE, true);
    }

    public int getAlbums(AlbumArtist artist) {
        try {
            return mDbHelper.submit(Album.class,
                    mDbHelper.where(Album.class).eq(Album.COLUMN_ALBUM_ARTIST, artist),
                    Album.COLUMN_TITLE, true);
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
            case ASSOCIATE_IMAGES:
                handleAssociateImages(message);
                break;
            case UPDATE_COUNTS:
                handleUpdateCounts(message);
                break;
            case FINALIZE_UPDATE:
                handleFinalizeUpdate(message);
                break;
            default:
                throw new RuntimeException("Unknown message received: " + message);
        }
        return true;
    }

    private final ApiHttpClient.Callback<JSONArray> TRACK_DATA_CALLBACK = new ApiHttpClient.Callback<JSONArray>() {
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

    private void handleUpdateDb(Message message) {
        if (mModelCache == null) mModelCache = new HashSet<>(5000); // TODO: figure out when to delete this

        // don't initiate another update if one is already in progress
        if (mUpdateDbProgress != null) return;

        mUpdateDbProgress = new UpdateDbProgress();

        mUpdateDbProgress.mLastUpdatedAt = new GlobalPrefs(mContext).getLastUpdated();

        mApiHttpClient.getServerTime(new ApiHttpClient.Callback<String>() {
            @Override
            public void onSuccess(String serverTime) {
                Log.v(TAG, "Got server time: " + serverTime);
                mUpdateDbProgress.mServerTime = serverTime;
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "error with getServerTime");
            }
        });

        mApiHttpClient.getTrackCount(mUpdateDbProgress.mLastUpdatedAt,
                new ApiHttpClient.Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer totalTracks) {
                        Log.i(TAG, "getTrackCount callback returned totalTracks = " + totalTracks);
                        mUpdateDbProgress.mTotalTracks = totalTracks;
                        mUpdateDbProgress.mLoadTrackApiCallsRemaining =
                                (int) Math.ceil((totalTracks * 1.0) / TRACK_LIMIT);

                        if (mUpdateDbProgress.hasLoadTrackApiCallsRemaining()) {
                            loadTracks();
                        }
                    }

            @Override
            public void onFailure() {
                Log.e(TAG, "getTrackCount failed");
            }
        });
    }

    private void loadTracks() {
        if (mUpdateDbProgress == null) throw new RuntimeException();

        if (!mUpdateDbProgress.hasLoadTrackApiCallsRemaining()) return;

        Log.d(TAG, String.format(Locale.getDefault(), "offset: %d, limit: %d",
                mUpdateDbProgress.mCurrentTrack, TRACK_LIMIT));
        mApiHttpClient.getTracks(mUpdateDbProgress.mCurrentTrack, TRACK_LIMIT,
                mUpdateDbProgress.mLastUpdatedAt, TRACK_DATA_CALLBACK);
        mUpdateDbProgress.mLoadTrackApiCallsRemaining--;
    }

    private <T> boolean isInCache(T object) {
        if (mModelCache.contains(object)) return true;

        mModelCache.add(object);
        return false;
    }

    private void handleTrackDataReceived(Message message) {
        JSONArray data = (JSONArray)message.obj;

        final List<Track> tracks = JsonTrackArrayParser.getTracks(data);
        final Map<String, List<Image>> trackImages = JsonTrackArrayParser.getTrackImages(data);
        mDbHelper.transaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (Track track : tracks) {
                    mUpdateDbProgress.mCurrentTrack++;
                    Genre genre = track.getGenre();
                    if (genre != null && !isInCache(genre.getUuid())) {
                        mDbHelper.createOrUpdate(Genre.class, genre);
                    }

                    TrackArtist trackArtist = track.getTrackArtist();
                    if (trackArtist != null && !isInCache(trackArtist.getUuid())) {
                        mDbHelper.createOrUpdate(TrackArtist.class, trackArtist);
                    }

                    Disc disc = track.getDisc();
                    if (disc != null && !isInCache(disc.getUuid())) {
                        mDbHelper.createOrUpdate(Disc.class, disc);

                        Album album = disc.getAlbum();
                        if (album != null && !isInCache(album.getUuid())) {
                            mDbHelper.createOrUpdate(Album.class, album);

                            AlbumArtist albumArtist = album.getAlbumArtist();
                            if (albumArtist != null && !isInCache(albumArtist.getUuid())) {
                                mDbHelper.createOrUpdate(AlbumArtist.class, albumArtist);
                            }
                        }
                    }

                    mDbHelper.createOrUpdate(Track.class, track);
//                    mDbHelper.getTrackDao().create(track);
                }

                for (String trackId : trackImages.keySet()) {
                    for (Image image : trackImages.get(trackId)) {
                        mDbHelper.createOrUpdate(Image.class, image);

                        // skip if track-image entry already exists
                        Where<TrackImage, String> where = mDbHelper.where(TrackImage.class);
                        where.eq(TrackImage.COLUMN_TRACK_ID, trackId)
                                .eq(TrackImage.COLUMN_IMAGE_ID, image.getId())
                                .and(2 /* number of clauses added */);
                        if (mDbHelper.countOf(TrackImage.class, where) > 0) continue;

                        TrackImage trackImage = new TrackImage();
                        trackImage.setTrackId(trackId);
                        trackImage.setImageId(image.getId());
                        mDbHelper.create(TrackImage.class, trackImage);
                    }
                }

                tracks.clear();
                trackImages.clear();

                if (mUpdateDbProgress.hasLoadTrackApiCallsRemaining()) {
                    loadTracks();
                } else {
                    // send loop messages for cleanup
                    mHandler.sendEmptyMessage(MessageType.UPDATE_COUNTS.value);
                    mHandler.sendEmptyMessage(MessageType.ASSOCIATE_IMAGES.value);
                    mHandler.sendEmptyMessage(MessageType.FINALIZE_UPDATE.value);
                }
                return null;
            }
        });
    }

    private List<Image> getImages(Iterable<Track> tracks) {
        ArrayList<String> trackIds = new ArrayList<>();
        for (Track track : tracks) trackIds.add(track.getUuid());

        try {
            QueryBuilder<TrackImage, String> trackImageQb = mDbHelper.qb(TrackImage.class);
            trackImageQb.setWhere(trackImageQb.where().in(TrackImage.COLUMN_TRACK_ID, trackIds));
            trackImageQb.selectColumns(TrackImage.COLUMN_IMAGE_ID);

            return mDbHelper.where(Image.class).in(Image.COLUMN_ID, trackImageQb).query();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleAssociateImages(Message message) {
        Log.v(TAG, "handleAssociateImages");
        Map<Class, String> classColumnMap = new HashMap<>();
        classColumnMap.put(AlbumArtist.class, AlbumArtist.COLUMN_IMAGE);
        classColumnMap.put(Album.class, Album.COLUMN_IMAGE);
        classColumnMap.put(Genre.class, Genre.COLUMN_IMAGE);

        for (Class clazz : classColumnMap.keySet()) {
            fillMissingAssociatedImages(clazz, classColumnMap.get(clazz));
        }
    }

    private <T extends ImageRepresentation> void fillMissingAssociatedImages(
            final Class<T> clazz, final String nullColumn) {
        mDbHelper.transaction(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                PreparedQuery<T> query = mDbHelper.where(clazz).isNull(nullColumn).prepare();
                CloseableIterator<T> iter = mDbHelper.dao(clazz).iterator(query);

                while (iter.hasNext()) {
                    T item = iter.next();
                    List<Track> tracks = getTracksSync(item);

                    List<Image> images = getImages(tracks);
                    if (images.size() > 0) {
                        item.setImage(images.get(0));
                        mDbHelper.dao(clazz).update(item);
                    }
                }

                iter.close();
                return null;
            }
        });
    }

    private void handleUpdateCounts(Message message) {
        mDbHelper.transaction(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Dao<Genre, String> genreDao = mDbHelper.dao(Genre.class);
                for (Genre genre : genreDao) {
                    genre.setTrackCount(mDbHelper.countOf(Track.class, tracksWhere(genre)));

                    genreDao.update(genre);
                }

                Dao<Disc, String> discDao = mDbHelper.dao(Disc.class);
                for (Disc disc : discDao) {
                    disc.setTrackCount(mDbHelper.countOf(Track.class, tracksWhere(disc)));

                    discDao.update(disc);
                }

                Dao<Album, String> albumDao = mDbHelper.dao(Album.class);
                for (Album album : albumDao) {
                    album.setTrackCount(mDbHelper.countOf(Track.class, tracksWhere(album)));

                    albumDao.update(album);
                }

                Dao<AlbumArtist, String> albumArtistDao = mDbHelper.dao(AlbumArtist.class);
                for (AlbumArtist artist : albumArtistDao) {
                    artist.setTrackCount(mDbHelper.countOf(Track.class, tracksWhere(artist)));
                    artist.setAlbumCount(artist.getAlbums().size());

                    albumArtistDao.update(artist);
                }

                Dao<TrackArtist, String> trackArtistDao = mDbHelper.dao(TrackArtist.class);
                for (TrackArtist artist : mDbHelper.dao(TrackArtist.class)) {
                    artist.setTrackCount(mDbHelper.countOf(Track.class, tracksWhere(artist)));

                    trackArtistDao.update(artist);
                }
                return null;
            }
        });
    }

    private void handleFinalizeUpdate(Message message) {
        Log.d(TAG, "handleFinalizeUpdate");

        if (mUpdateDbProgress.mServerTime != null) {
            new GlobalPrefs(mContext).setLastUpdated(mUpdateDbProgress.mServerTime);
        }

        mUpdateDbProgress = null;
        Log.d(TAG, "update complete");
    }

    private class DatabaseHelper extends OrmLiteSqliteOpenHelper {
        private Map<Class, Dao> mDaoMap;
        private ThreadPoolExecutor mExecutor;

        public DatabaseHelper(Context context) {
            super(context, "kanihi.sqlite", null, 1);
            setWriteAheadLoggingEnabled(true);

            mDaoMap = new ConcurrentHashMap<>();
            mExecutor = new ThreadPoolExecutor(
                    4, 10, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20));
        }

        @Override
        public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
            Log.v(TAG, "onCreate");
            try {
                Class[] clazzes = { Track.class, Genre.class, Disc.class, Album.class,
                        TrackArtist.class, AlbumArtist.class, Image.class, TrackImage.class};

                for (Class clazz : clazzes) {
                    TableUtils.createTable(connectionSource, clazz);
                }
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

        public <T> int submit(Class<T> clazz, PreparedQuery<T> query) {
            return submit(dao(clazz), query);
        } 
        public <T> int submit(Class<T> clazz, Where<T, String> where,
                                 String orderColumn, boolean ascending) {
            try {
                return submit(clazz, preparedQuery(clazz, where, orderColumn, ascending));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public <V> void transaction(final Callable<V> callable) {
            try {
                long start = System.currentTimeMillis();
                TransactionManager.callInTransaction(getConnectionSource(), callable);
                long duration = System.currentTimeMillis() - start;
                Log.i(TAG, "Transaction took " + (duration / 1000.0) + " seconds");
            } catch (SQLException e) {
                Log.e(TAG, "caught SQLException", e);
            }
        }

        public <T> void create(Class<T> clazz, T object) throws SQLException {
            dao(clazz).create(object);
        }

        public <T> void createOrUpdate(Class<T> clazz, T object) throws SQLException {
            dao(clazz).createOrUpdate(object);
        }

        @SuppressWarnings("unchecked")
        public synchronized <T> Dao<T, String> dao(Class<T> clazz) {
            if (mDaoMap.containsKey(clazz)) {
                return mDaoMap.get(clazz);
            }

            try {
                mDaoMap.put(clazz, getDao(clazz));
                return mDaoMap.get(clazz);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> QueryBuilder<T, String> qb(Class<T> clazz) {
            return dao(clazz).queryBuilder();
        }

        public <T> Where<T, String> where(Class<T> clazz) {
            return qb(clazz).where();
        }

        private <T> PreparedQuery<T> preparedQuery(Class<T> clazz,
                                                   Where<T, String> where,
                                                   String orderColumn,
                                                   boolean ascending) throws SQLException {
            QueryBuilder<T, String> qb = qb(clazz);
            if (where != null) qb.setWhere(where);
            qb.orderBy(orderColumn, ascending);

            return qb.prepare();
        }

        public <T> List<T> query(Class<T> clazz, Where<T, String> where,
                                 String orderColumn, boolean ascending) {
            try {
                return dao(clazz).query(preparedQuery(clazz, where, orderColumn, ascending));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> CloseableIterator<T> iterator(Class<T> clazz, Where<T, String> where,
                                                 String orderColumn, boolean ascending) {
            try {
                return dao(clazz).iterator(preparedQuery(clazz, where, orderColumn, ascending));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> long countOf(Class<T> clazz, Where<T, String> where) {
            try {
                QueryBuilder<T, String> qb = qb(clazz).setCountOf(true);
                qb.setWhere(where);
                return dao(clazz).countOf(qb.prepare());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}