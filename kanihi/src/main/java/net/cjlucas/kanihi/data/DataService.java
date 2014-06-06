package net.cjlucas.kanihi.data;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
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
import net.cjlucas.kanihi.models.interfaces.UniqueModel;
import net.cjlucas.kanihi.prefs.GlobalPrefs;
import net.cjlucas.kanihi.utils.DataUtils;
import net.minidev.json.JSONArray;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class DataService extends Service {
    private static final String TAG = "DataService";
    private static final int TRACK_LIMIT = 2;

    private ApiHttpClient mApiHttpClient;
    private DatabaseHelper mDbHelper;
    private HashSet<Object> mModelCache;
    private UpdateDbProgress mUpdateDbProgress;
    private Observer mObserver;
    private IBinder mBinder = new LocalBinder();

    public interface Observer {
        public void onDatabaseUpdated();
    }

    public class UpdateDbProgress {
        private UpdateDbStages mCurrentStage;
        public int mCurrentTrack;
        public int mTotalTracks;
        public String mLastUpdatedAt;
        public String mServerTime; // at start of update
    }

    public enum MessageTypes {
        BEGIN_UPDATE(0),
        FETCH_DELETED_TRACKS(1),
        DELETED_TRACKS_RECEIVED(2),
        CLEANUP_ORPHANED_RELATIONSHIPS(3),
        FETCH_TRACK_DATA(4),
        TRACK_DATA_RECEIVED(5),
        ASSOCIATES_IMAGES(6),
        UPDATE_COUNTS(7),
        FINISH_UPDATE(8);

        private int value;

        MessageTypes(int value) {
            this.value = value;
        }

        public static MessageTypes forValue(int value) {
            for (MessageTypes type : MessageTypes.values())
                if (type.value == value) return type;
            return null;
        }
    }

    enum UpdateDbStages {
        FETCH_DELETED_TRACKS,
        CLEANUP_ORPHANED_RELATIONSHIPS,
        FETCH_TRACK_DATA,
        ASSOCIATE_IMAGES,
        UPDATE_COUNTS,
        UPDATE_COMPLETE
    }

    public class LocalBinder extends Binder {
        public DataService getService () {
            return DataService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mDbHelper = new DatabaseHelper(this);
        mApiHttpClient = new ApiHttpClient();
        mApiHttpClient.setApiEndpoint("192.168.0.2", 8080);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        mDbHelper.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void registerObserver(Observer observer) {
        mObserver = observer;
    }

    public void unregisterObserver(Observer observer) {
        mObserver = null;
    }

    private void notifyObserver() {
        if (mObserver != null)
            mObserver.onDatabaseUpdated();
    }

    /**
     *
     * @param ancestorClazz the class of the ancestor
     * @param ancestor either the UUID of the ancestor, or an instance of ancestorClazz
     * @return the Where object
     */
    private Where<Track, String> tracksWhere(Class ancestorClazz, Object ancestor) {
        if (ancestorClazz == null) {
            return null;
        }

        Where<Track, String> where = mDbHelper.where(Track.class);
        try {
            if (ancestorClazz == TrackArtist.class) {
                return where.eq(Track.COLUMN_TRACK_ARTIST, ancestor);

            } else if (ancestorClazz == Disc.class) {
                return where.eq(Track.COLUMN_DISC, ancestor);

            } else if (ancestorClazz == Genre.class) {
                return where.eq(Track.COLUMN_DISC, ancestor);

            } else if (ancestorClazz == Album.class) {
                QueryBuilder<Disc, String> discsQb = mDbHelper.qb(Disc.class);
                Where<Disc, String> discsWhere = discsQb.where().eq(Disc.COLUMN_ALBUM, ancestor);
                discsQb.selectColumns(Disc.COLUMN_UUID);
                discsQb.setWhere(discsWhere);

                return where.in(Track.COLUMN_DISC, discsQb);

            } else if (ancestorClazz == AlbumArtist.class) {
                // get the albums from the album artist
                QueryBuilder<Album, String> albumsQb = mDbHelper.qb(Album.class);
                Where<Album, String> albumsWhere =
                        albumsQb.where().eq(Album.COLUMN_ALBUM_ARTIST, ancestor);
                albumsQb.selectColumns(Album.COLUMN_UUID);
                albumsQb.setWhere(albumsWhere);

                // get the discs from the albums
                QueryBuilder<Disc, String> discsQb = mDbHelper.qb(Disc.class);
                Where<Disc, String> discsWhere = discsQb.where().in(Disc.COLUMN_ALBUM, albumsQb);
                discsQb.selectColumns(Disc.COLUMN_UUID);
                discsQb.setWhere(discsWhere);

                // get the tracks from the discs
                return where.in(Track.COLUMN_DISC, discsQb);

            } else {
                throw new RuntimeException("Unexpected ancestor class received");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see #tracksWhere(Class, Object)
     */
    private Where<Album, String> albumsWhere(Class ancestorClazz, Object ancestor) {
        if (ancestorClazz == null) {
            return null;
        }

        Where<Album, String> where = mDbHelper.where(Album.class);

        try {
            if (ancestorClazz == AlbumArtist.class) {
                return where.eq(Album.COLUMN_ALBUM_ARTIST, ancestor);
            } else if (ancestorClazz == Album.class) {
                return where.eq(Album.COLUMN_UUID, ancestor);
            } else {
                throw new RuntimeException("Unexpected ancestor class received");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see #tracksWhere(Class, Object)
     */
    private Where<AlbumArtist, String> albumArtistsWhere(Class ancestorClazz, Object ancestor) {
        return null;
    }

    /**
     * @see #tracksWhere(Class, Object)
     */
    private Where<Disc, String> discsWhere(Class ancestorClazz, Object ancestor) {
        if (ancestorClazz == null) {
            return null;
        }

        Where<Disc, String> where = mDbHelper.where(Disc.class);

        try {
            if (ancestorClazz == Album.class) {
                return where.eq(Disc.COLUMN_ALBUM, ancestor);
            } else {
                throw new RuntimeException("Unexpected ancestor class received");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> CloseableIterator<T> executePreparedQuery(Class<T> clazz,
                                                         PreparedQuery<T> preparedQuery) {
        try {
            return mDbHelper.dao(clazz).iterator(preparedQuery);
        } catch (SQLException e) {
            throw new RuntimeException("Could not execute prepared query");
        }
    }

    public PreparedQuery<Track> getTracksQuery(Class ancestorClazz, String ancestorUuid,
                                               String sortColumn, boolean sortAscending) {
        return mDbHelper.preparedQuery(Track.class,
                tracksWhere(ancestorClazz, ancestorUuid), sortColumn, sortAscending);
    }

    public PreparedQuery<Album> getAlbumsQuery(Class ancestorClazz, String ancestorUuid,
                                              String sortColumn, boolean sortAscending) {
        return mDbHelper.preparedQuery(Album.class,
                albumsWhere(ancestorClazz, ancestorUuid), sortColumn, sortAscending);
    }

    public PreparedQuery<AlbumArtist> getAlbumArtistsQuery(Class ancestorClazz, String ancestorUuid,
                                                          String sortColumn, boolean sortAscending) {
        return mDbHelper.preparedQuery(AlbumArtist.class,
                albumArtistsWhere(ancestorClazz, ancestorUuid), sortColumn, sortAscending);

    }

    public PreparedQuery<Disc> getDiscsQuery(Class ancestorClazz, String ancestorUuid,
                                             String sortColumn, boolean sortAscending) {
        return mDbHelper.preparedQuery(Disc.class,
                discsWhere(ancestorClazz, ancestorUuid), sortColumn, sortAscending);
    }

    private String getIdField(Class clazz) {
        if (clazz == Track.class) {
            return Track.COLUMN_UUID;
        } else if (clazz == TrackArtist.class) {
            return TrackArtist.COLUMN_UUID;
        } else if (clazz == Genre.class) {
            return Genre.COLUMN_UUID;
        } else if (clazz == Disc.class) {
            return Disc.COLUMN_UUID;
        } else if (clazz == AlbumArtist.class) {
            return AlbumArtist.COLUMN_UUID;
        } else if (clazz == Image.class) {
            return Image.COLUMN_ID;
        }

        throw new IllegalArgumentException("Unknown class: " + clazz.getName());
    }

    public <T> List<T> getModels(Class<T> clazz, List<String> ids) {
        T[] modelArray = (T[])Array.newInstance(clazz, ids.size());

        Dao <T, String> dao = mDbHelper.dao(clazz);
        try {
            CloseableIterator<T> iterator = mDbHelper.iterator(
                    clazz, mDbHelper.where(clazz).in(getIdField(clazz), ids), null, false);

            while (iterator.hasNext()) {
                iterator.next();
                T data = iterator.current();
                modelArray[ids.indexOf(dao.extractId(data))] = data;
            }

            iterator.closeQuietly();
            return Arrays.asList(modelArray);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMe(Iterable<Track> tracks) {
        Set<String> ids = new HashSet<>();
        for (Track track : tracks) {
            ids.add(track.getTrackArtist().getUuid());
        }

        try {
            for (TrackArtist ta :
                    mDbHelper.where(TrackArtist.class).in(TrackArtist.COLUMN_UUID, ids).query()) {
                for (Track track : tracks) {
                    if (track.getTrackArtist().getUuid().equals(ta.getUuid())) {
                        track.setTrackArtist(ta);
                    }
                }
            }
        }catch (Exception e) {}
    }

    // TODO: try implementing this in single album list fragment
    public <T> void refresh(Class<T> clazz, T data) {
        try {
            mDbHelper.dao(clazz).refresh(data);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void refreshTrack(Track track) {
        refresh(Genre.class, track.getGenre());
        refresh(TrackArtist.class, track.getTrackArtist());
        refresh(Disc.class, track.getDisc());
        refresh(Album.class, track.getDisc().getAlbum());
        refresh(AlbumArtist.class, track.getDisc().getAlbum().getAlbumArtist());
    }

    public UpdateDbProgress update() {
        if (mUpdateDbProgress != null) {
            return mUpdateDbProgress;
        }

        mUpdateDbProgress = new UpdateDbProgress();
        new UpdateDbThread(mUpdateDbProgress).start();
        return mUpdateDbProgress;
    }

    private void onUpdateComplete() {
        mUpdateDbProgress = null;
        notifyObserver();
    }

    private <T> boolean isInCache(T object) {
        if (mModelCache.contains(object)) return true;

        mModelCache.add(object);
        return false;
    }


    private <T extends UniqueModel> void deleteOrphanedObjects(Class<T> clazz) {
        List<String> uuids = new ArrayList<>();
        for (T object : mDbHelper.dao(clazz)) {
            if (mDbHelper.countOf(Track.class, tracksWhere(clazz, object)) == 0)
                uuids.add(object.getUuid());
        }
        mDbHelper.deleteIds(clazz, uuids);
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

    public Map<Track, List<Image>> getTrackImagesMap(Iterable<Track> tracks) {
        Map<Track, List<Image>> map = new HashMap<>();

        try {
            List<String> trackIdsList = new ArrayList<>();
            for (Track track : tracks) {
                trackIdsList.add(track.getUuid());
            }
            List<TrackImage> trackImageList = mDbHelper.query(TrackImage.class,
                    mDbHelper.where(TrackImage.class).in(TrackImage.COLUMN_TRACK_ID, trackIdsList),
                    null, false);

            List<String> imageIdsList = new ArrayList<>();
            for (TrackImage trackImage : trackImageList) {
                imageIdsList.add(trackImage.getImageId());
            }

            List<Image> imagesList = mDbHelper.query(Image.class,
                    mDbHelper.where(Image.class).in(Image.COLUMN_ID, imageIdsList), null, false);

            for (Track track : tracks) {
                List<Image> imageList = new ArrayList<>();

                for (TrackImage trackImage : trackImageList) {
                    // final all TrackImage objects that reference the current track
                    if (track.getUuid().equals(trackImage.getTrackId())) {
                        // iterate imagesList until the Image that trackImage references is found
                        for (Image image : imagesList) {
                            if (trackImage.getImageId().equals(image.getId())) {
                                imageList.add(image);
                                break;
                            }
                        }
                    }
                }

                map.put(track, imageList);
            }
            return map;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Image> getTrackImages(Track track) {
        List<Track> list = new ArrayList<>();
        list.add(track);
        return getTrackImagesMap(list).get(track);
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
                    List<Track> tracks =
                            mDbHelper.query(Track.class, tracksWhere(clazz, item), Track.COLUMN_TITLE, true);

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

    private class DatabaseHelper extends OrmLiteSqliteOpenHelper {
        private Map<Class, Dao> mDaoMap;
        private final Class[] DAO_CLAZZES = { Track.class, Genre.class, Disc.class, Album.class,
                TrackArtist.class, AlbumArtist.class, Image.class, TrackImage.class};

        public DatabaseHelper(Context context) {
            super(context, "kanihi.sqlite", null, 1);
            setWriteAheadLoggingEnabled(true);

            mDaoMap = new ConcurrentHashMap<>();
        }

        @Override
        public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
            Log.v(TAG, "onCreate");
            try {
                for (Class clazz : DAO_CLAZZES) {
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

        public <T> void deleteIds(Class<T> clazz, Collection<String> ids) {
            try {
                dao(clazz).deleteIds(ids);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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

        private <T> PreparedQuery<T> preparedQuery(Class<T> clazz, Where<T, String> where,
                                                   String sortColumn, boolean sortAscending) {
            QueryBuilder<T, String> qb = qb(clazz);
            if (where != null)
                qb.setWhere(where);
            if (sortColumn != null)
                qb.orderBy(sortColumn, sortAscending);

            try {
                return qb.prepare();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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

    private class UpdateDbThread extends Thread implements Handler.Callback {
        private Handler mHandler;
        private UpdateDbProgress mUpdateDbProgress;
        private ApiCursor mApiCursor;

        private class ApiCursor {
            public long mCurrentOffset;
            public long mLastLimitUsed;
        }

        private final ApiHttpClient.Callback<JSONArray> TRACK_DATA_CALLBACK = new ApiHttpClient.Callback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data) {
                Log.v(TAG, "received data of size " + data.size());
                Message msg = obtainMessage(MessageTypes.TRACK_DATA_RECEIVED);
                msg.obj = data;
                mHandler.sendMessage(msg);
            }
            @Override
            public void onFailure() {
                Log.e(TAG, "failure with ApiHttpClient.getTracks");
            }
        };

        private final ApiHttpClient.Callback<JSONArray> DELETED_TRACKS_CALLBACK
                = new ApiHttpClient.Callback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data) {
                Log.v(TAG, "DELETED_TRACKS_CALLBACK: json array size: " + data.size());
                Message msg = obtainMessage(MessageTypes.DELETED_TRACKS_RECEIVED);
                msg.obj = data;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "ApiHttpClient.fetchDeletedTracks failed");
            }
        };

        public UpdateDbThread(UpdateDbProgress dbProgress) {
            mUpdateDbProgress = dbProgress;
        }

        private void sendEmptyMessage(MessageTypes messageType) {
            mHandler.sendEmptyMessage(messageType.value);
        }

        private Message obtainMessage(MessageTypes messageType) {
            return mHandler.obtainMessage(messageType.value);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (MessageTypes.forValue(msg.what)) {
                case BEGIN_UPDATE:
                    handleBeginUpdate(msg);
                    break;
                case FETCH_DELETED_TRACKS:
                    handleFetchDeletedTracks(msg);
                    break;
                case CLEANUP_ORPHANED_RELATIONSHIPS:
                    handleCleanupOrphanedRelationships(msg);
                    break;
                case DELETED_TRACKS_RECEIVED:
                    handleDeletedTracksReceived(msg);
                    break;
                case FETCH_TRACK_DATA:
                    handleFetchTrackData(msg);
                    break;
                case TRACK_DATA_RECEIVED:
                    handleTrackDataReceived(msg);
                    break;
                case ASSOCIATES_IMAGES:
                    handleAssociateImages(msg);
                    break;
                case UPDATE_COUNTS:
                    handleUpdateCounts(msg);
                    break;
                case FINISH_UPDATE:
                    handleFinishUpdate();
                    break;
                default:
                    break;
            }
            return true;
        }

        private void handleBeginUpdate(Message message) {
            if (mModelCache == null) mModelCache = new HashSet<>(5000); // TODO: figure out when to delete this

            mUpdateDbProgress.mLastUpdatedAt = new GlobalPrefs(DataService.this).getLastUpdated();

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
                        }

                        @Override
                        public void onFailure() {
                            Log.e(TAG, "getTrackCount failed");
                        }
                    }
            );

            if (mDbHelper.countOf(Track.class, null) > 0) {
                fetchDeletedTracks(0, TRACK_LIMIT);
            } else {
                Log.v(TAG, "handleBeginUpdate: first run, skipped fetching deleted tracks");
                fetchTrackData(0, TRACK_LIMIT);
            }

        }

        private void handleFetchDeletedTracks(Message message) {
            fetchDeletedTracks(0, TRACK_LIMIT);
        }

        private void handleDeletedTracksReceived(Message message) {
            JSONArray data = (JSONArray)message.obj;
            List<String> uuids = new ArrayList<>();
            for (Object o : data) uuids.add((String)o);

            mDbHelper.deleteIds(Track.class, uuids);

            if (data.size() < mApiCursor.mLastLimitUsed) {
                mApiCursor = null;
                fetchTrackData(0, TRACK_LIMIT);
            } else {
                fetchDeletedTracks(mApiCursor.mCurrentOffset + data.size(), TRACK_LIMIT);
            }
        }
        private void handleCleanupOrphanedRelationships(Message message) {
            mUpdateDbProgress.mCurrentStage = UpdateDbStages.CLEANUP_ORPHANED_RELATIONSHIPS;

            deleteOrphanedObjects(TrackArtist.class);
            deleteOrphanedObjects(Genre.class);
            deleteOrphanedObjects(Disc.class);
            deleteOrphanedObjects(Album.class);
            deleteOrphanedObjects(AlbumArtist.class);
        }

        private void handleFetchTrackData(Message message) {
            fetchTrackData(0, TRACK_LIMIT);
        }

        private void handleTrackDataReceived(Message message) {
            JSONArray data = (JSONArray)message.obj;
            final List<Track> tracks = JsonTrackArrayParser.getTracks(data);
            final int tracksSize = tracks.size();
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

                    return null;
                }
            });

            if (tracksSize < mApiCursor.mLastLimitUsed) {
                mApiCursor = null;
                sendEmptyMessage(MessageTypes.CLEANUP_ORPHANED_RELATIONSHIPS);
                sendEmptyMessage(MessageTypes.UPDATE_COUNTS);
                sendEmptyMessage(MessageTypes.ASSOCIATES_IMAGES);
                sendEmptyMessage(MessageTypes.FINISH_UPDATE);
            } else {
                fetchTrackData(mApiCursor.mCurrentOffset + tracksSize, TRACK_LIMIT);
            }
        }

        private void handleAssociateImages(Message message) {
            Log.v(TAG, "handleAssociateImages");
            mUpdateDbProgress.mCurrentStage = UpdateDbStages.ASSOCIATE_IMAGES;

            Map<Class, String> classColumnMap = new HashMap<>();
            classColumnMap.put(AlbumArtist.class, AlbumArtist.COLUMN_IMAGE);
            classColumnMap.put(Album.class, Album.COLUMN_IMAGE);
            classColumnMap.put(Genre.class, Genre.COLUMN_IMAGE);

            for (Class clazz : classColumnMap.keySet()) {
                fillMissingAssociatedImages(clazz, classColumnMap.get(clazz));
            }
        }

        // TODO: rename to handleCalculateProperties
        private void handleUpdateCounts(Message message) {
            mUpdateDbProgress.mCurrentStage = UpdateDbStages.UPDATE_COUNTS;

            mDbHelper.transaction(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    Dao<Genre, String> genreDao = mDbHelper.dao(Genre.class);
                    for (Genre genre : genreDao) {
                        genre.setTrackCount(
                                mDbHelper.countOf(Track.class, tracksWhere(Genre.class, genre)));

                        genreDao.update(genre);
                    }

                    Dao<Disc, String> discDao = mDbHelper.dao(Disc.class);
                    for (Disc disc : discDao) {
                        disc.setTrackCount(
                                mDbHelper.countOf(Track.class, tracksWhere(Disc.class, disc)));

                        discDao.update(disc);
                    }

                    Dao<Album, String> albumDao = mDbHelper.dao(Album.class);
                    for (Album album : albumDao) {
                        Where<Track, String> where = tracksWhere(Album.class, album);
                        album.setTrackCount(mDbHelper.countOf(Track.class, where));

                        QueryBuilder<Track, String> qb = mDbHelper.qb(Track.class);
                        qb.selectRaw("SUM(" + Track.COLUMN_DURATION + ")")
                                .setWhere(where);
                        album.setAlbumDuration(
                                albumDao.queryRawValue(qb.prepare().getStatement()));

                        albumDao.update(album);
                    }

                    Dao<AlbumArtist, String> albumArtistDao = mDbHelper.dao(AlbumArtist.class);
                    for (AlbumArtist artist : albumArtistDao) {
                        artist.setTrackCount(
                                mDbHelper.countOf(Track.class, tracksWhere(AlbumArtist.class, artist)));
                        artist.setAlbumCount(artist.getAlbums().size());

                        albumArtistDao.update(artist);
                    }

                    Dao<TrackArtist, String> trackArtistDao = mDbHelper.dao(TrackArtist.class);
                    for (TrackArtist artist : mDbHelper.dao(TrackArtist.class)) {
                        artist.setTrackCount(
                                mDbHelper.countOf(Track.class, tracksWhere(TrackArtist.class, artist)));

                        trackArtistDao.update(artist);
                    }
                    return null;
                }
            });
        }

        private void handleFinishUpdate() {
            Log.d(TAG, "handleFinishUpdate");

            if (mUpdateDbProgress.mServerTime != null) {
                new GlobalPrefs(DataService.this).setLastUpdated(mUpdateDbProgress.mServerTime);
            }

            mUpdateDbProgress.mCurrentStage = UpdateDbStages.UPDATE_COMPLETE;
            mUpdateDbProgress = null;
            Log.d(TAG, "update complete");
            onUpdateComplete();
        }

        private void fetchDeletedTracks(long offset, long limit) {
            if (mApiCursor == null)
                mApiCursor = new ApiCursor();

            mUpdateDbProgress.mCurrentStage = UpdateDbStages.FETCH_DELETED_TRACKS;

            mApiHttpClient.getDeletedTracks(offset, limit,
                    new GlobalPrefs(DataService.this).getLastUpdated(), DELETED_TRACKS_CALLBACK);

            mApiCursor.mCurrentOffset = offset;
            mApiCursor.mLastLimitUsed = limit;
        }

        private void fetchTrackData(long offset, long limit) {
            if (mApiCursor == null)
                mApiCursor = new ApiCursor();

            mUpdateDbProgress.mCurrentStage = UpdateDbStages.FETCH_TRACK_DATA;

            Log.d(TAG, "here1");
            mApiHttpClient.getTracks(offset, limit,
                    new GlobalPrefs(DataService.this).getLastUpdated(), TRACK_DATA_CALLBACK);
            Log.d(TAG, "here2");

            mApiCursor.mCurrentOffset = offset;
            mApiCursor.mLastLimitUsed = limit;
        }


        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler(this);
            sendEmptyMessage(MessageTypes.BEGIN_UPDATE);
            Looper.loop();
        }
    }
}