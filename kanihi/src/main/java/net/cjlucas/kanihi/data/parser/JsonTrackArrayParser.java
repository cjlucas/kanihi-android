package net.cjlucas.kanihi.data.parser;

import net.cjlucas.kanihi.models.Album;
import net.cjlucas.kanihi.models.AlbumArtist;
import net.cjlucas.kanihi.models.Disc;
import net.cjlucas.kanihi.models.Genre;
import net.cjlucas.kanihi.models.Image;
import net.cjlucas.kanihi.models.Track;
import net.cjlucas.kanihi.models.TrackArtist;
import net.cjlucas.kanihi.utils.DateUtils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonTrackArrayParser {
    private static final String KEY_TRACK = "track";
    private static final String KEY_GENRE = "genre";
    private static final String KEY_DISC = "disc";
    private static final String KEY_ALBUM = "album";
    private static final String KEY_TRACK_ARTIST = "track_artist";
    private static final String KEY_ALBUM_ARTIST = "album_artist";
    private static final String KEY_IMAGES = "images";

    private static abstract class JsonModelParser {

        public static Object getObject(JSONObject o, String key, Object fallback) {
            Object val = o.get(key);
            return val == null ? fallback : val;
        }

        public static String getString(JSONObject o, String key) {
            return (String)getObject(o, key, null);
        }

        public static int getInt(JSONObject o, String key) {
            return (int)getObject(o, key, 0);
        }

        public static Date getDate(JSONObject o, String key) {
            Object obj = getObject(o, key, null);
            if (obj == null) return null;

            Date date = null;
            try {
                date = DateUtils.RAILS_DATE_FORMAT.parse((String) obj);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return date;
        }
    }

    public static class JsonTrackParser extends JsonModelParser {
        private static final String KEY_TRACK_UUID          = "uuid";
        private static final String KEY_TRACK_TITLE         = "name";
        private static final String KEY_TRACK_SUBTITLE      = "subtitle";
        private static final String KEY_TRACK_NUM           = "num";
        private static final String KEY_TRACK_DURATION      = "duration";
        private static final String KEY_TRACK_DATE          = "date";
        private static final String KEY_TRACK_ORIGINAL_DATE = "original_date";

        public static Track parse(JSONObject jsonObject) {
            Track track = new Track();

            track.setUuid(getString(jsonObject, KEY_TRACK_UUID));
            track.setTitle(getString(jsonObject, KEY_TRACK_TITLE));
            track.setSubtitle(getString(jsonObject, KEY_TRACK_SUBTITLE));
            track.setNum(getInt(jsonObject, KEY_TRACK_NUM));
            track.setDuration(getInt(jsonObject, KEY_TRACK_DURATION));
            track.setDate(getDate(jsonObject, KEY_TRACK_DATE));
            track.setOriginalDate(getDate(jsonObject, KEY_TRACK_ORIGINAL_DATE));

            JSONObject genreObject = (JSONObject)jsonObject.get(KEY_GENRE);
            if (genreObject != null) track.setGenre(JsonGenreParser.parse(genreObject));

            JSONObject discObject = (JSONObject)jsonObject.get(KEY_DISC);
            if (discObject != null) track.setDisc(JsonDiscParser.parse(discObject));

            JSONObject trackArtistObject = (JSONObject)jsonObject.get(KEY_TRACK_ARTIST);
            if (trackArtistObject != null) track.setTrackArtist(
                    JsonTrackArtistParser.parse(trackArtistObject));

            return track;
        }
    }

    public static class JsonGenreParser extends JsonModelParser {
        private static final String KEY_GENRE_UUID = "uuid";
        private static final String KEY_GENRE_NAME = "name";

        public static Genre parse(JSONObject jsonObject) {
            Genre genre = new Genre();

            genre.setUuid(getString(jsonObject, KEY_GENRE_UUID));
            genre.setName(getString(jsonObject, KEY_GENRE_NAME));

            return genre;
        }
    }

    public static class JsonDiscParser extends JsonModelParser {
        private static final String KEY_DISC_UUID           = "uuid";
        private static final String KEY_DISC_NUM            = "num";
        private static final String KEY_DISC_SUBTITLE       = "subtitle";
        private static final String KEY_DISC_TOTAL_TRACKS   = "total_tracks";

        public static Disc parse(JSONObject jsonObject) {
            Disc disc = new Disc();

            disc.setUuid(getString(jsonObject, KEY_DISC_UUID));
            disc.setDiscNum(getInt(jsonObject, KEY_DISC_NUM));
            disc.setSubtitle(getString(jsonObject, KEY_DISC_SUBTITLE));
            disc.setTotalTracks(getInt(jsonObject, KEY_DISC_TOTAL_TRACKS));

            JSONObject albumObject = (JSONObject)jsonObject.get(KEY_ALBUM);
            if (albumObject != null) disc.setAlbum(JsonAlbumParser.parse(albumObject));

            return disc;
        }
    }

    public static class JsonAlbumParser extends JsonModelParser {
        private static final String KEY_ALBUM_UUID = "uuid";
        private static final String KEY_ALBUM_TITLE = "name";
        private static final String KEY_ALBUM_TOTAL_DISCS = "total_discs";

        public static Album parse(JSONObject jsonObject) {
            Album album = new Album();

            album.setUuid(getString(jsonObject, KEY_ALBUM_UUID));
            album.setTitle(getString(jsonObject, KEY_ALBUM_TITLE));
            album.setTotalDiscs(getInt(jsonObject, KEY_ALBUM_TOTAL_DISCS));

            JSONObject albumArtistObject = (JSONObject)jsonObject.get(KEY_ALBUM_ARTIST);
            if (albumArtistObject != null) album.setAlbumArtist(
                    JsonAlbumArtistParser.parse(albumArtistObject));

            return album;
        }
    }

    public static class JsonTrackArtistParser extends JsonModelParser {
        private static final String KEY_TRACK_ARTIST_UUID = "uuid";
        private static final String KEY_TRACK_ARTIST_NAME = "name";
        private static final String KEY_TRACK_ARTIST_SORT_NAME = "sort_name";

        public static TrackArtist parse(JSONObject jsonObject) {
            TrackArtist artist = new TrackArtist();

            artist.setUuid(getString(jsonObject, KEY_TRACK_ARTIST_UUID));
            artist.setName(getString(jsonObject, KEY_TRACK_ARTIST_NAME));
            artist.setSortName(getString(jsonObject, KEY_TRACK_ARTIST_SORT_NAME));

            return artist;
        }
    }

    public static class JsonAlbumArtistParser extends JsonModelParser {
        private static final String KEY_ALBUM_ARTIST_UUID = "uuid";
        private static final String KEY_ALBUM_ARTIST_NAME = "name";
        private static final String KEY_ALBUM_ARTIST_SORT_NAME = "sort_name";

        public static AlbumArtist parse(JSONObject jsonObject) {
            AlbumArtist artist = new AlbumArtist();

            artist.setUuid(getString(jsonObject, KEY_ALBUM_ARTIST_UUID));
            artist.setName(getString(jsonObject, KEY_ALBUM_ARTIST_NAME));
            artist.setSortName(getString(jsonObject, KEY_ALBUM_ARTIST_SORT_NAME));

            return artist;
        }
    }

    public static class JsonImageParser extends JsonModelParser {
        private static final String KEY_CHECKSUM = "checksum";

        public static Image parse(JSONObject jsonObject) {
            Image image = new Image();
            image.setId(getString(jsonObject, KEY_CHECKSUM));

            return image;
        }
    }

    public static List<Track> getTracks(JSONArray jsonArray) {
        ArrayList<Track> tracks = new ArrayList<>();

        for (Object o : jsonArray) {
            Object trackJsonObject = ((JSONObject)o).get(KEY_TRACK);
            tracks.add(JsonTrackParser.parse((JSONObject) trackJsonObject));
        }

        return tracks;
    }

    public static Map<String, List<Image>> getTrackImages(JSONArray jsonArray) {
        Map<String, List<Image>> imagesMap = new HashMap<>();

        for (Object o : jsonArray) {
            ArrayList<Image> images = new ArrayList<>();

            Object trackJsonObject = ((JSONObject)o).get(KEY_TRACK);
            Track t = JsonTrackParser.parse((JSONObject)trackJsonObject);

            Object trackImagesJsonArray = ((JSONObject)trackJsonObject).get(KEY_IMAGES);
            for (Object imageJsonObject : (JSONArray)trackImagesJsonArray) {
                images.add(JsonImageParser.parse((JSONObject) imageJsonObject));
            }

            imagesMap.put(t.getUuid(), images);
        }

        return imagesMap;
    }
}
