package net.cjlucas.kanihi.data.parser;

import net.cjlucas.kanihi.model.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonTrackArrayParser {
    private static final String KEY_TRACK = "track";
    private static final String KEY_GENRE = "genre";
    private static final String KEY_DISC = "disc";

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
    }

    public static class JsonTrackParser extends JsonModelParser {
        private static final String KEY_TRACK_UUID = "uuid";
        private static final String KEY_TRACK_TITLE = "name";
        private static final String KEY_TRACK_SUBTITLE = "subtitle";
        private static final String KEY_TRACK_NUM = "num";
        private static final String KEY_TRACK_DURATION = "duration";

        public static Track parse(JSONObject jsonObject) {
            Track track = new Track();

            track.setUuid(getString(jsonObject, KEY_TRACK_UUID));
            track.setTitle(getString(jsonObject, KEY_TRACK_TITLE));
            track.setSubtitle(getString(jsonObject, KEY_TRACK_SUBTITLE));
            track.setNum(getInt(jsonObject, KEY_TRACK_NUM));
            track.setDuration(getInt(jsonObject, KEY_TRACK_DURATION));

            JSONObject genreObject = (JSONObject)jsonObject.get(KEY_GENRE);
            if (genreObject != null) track.setGenre(JsonGenreParser.parse(genreObject));

            JSONObject discObject = (JSONObject)jsonObject.get(KEY_DISC);
            if (discObject != null) track.setDisc(JsonDiscParser.parse(discObject));

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
        private static final String KEY_DISC_UUID = "uuid";
        private static final String KEY_DISC_NUM = "num";
        private static final String KEY_DISC_SUBTITLE = "subtitle";
        private static final String KEY_DISC_TOTAL_TRACKS = "total_tracks";

        public static Disc parse(JSONObject jsonObject) {
            Disc disc = new Disc();

            disc.setUuid(getString(jsonObject, KEY_DISC_UUID));
            disc.setDiscNum(getInt(jsonObject, KEY_DISC_NUM));
            disc.setSubtitle(getString(jsonObject, KEY_DISC_SUBTITLE));
            disc.setTotalTracks(getInt(jsonObject, KEY_DISC_TOTAL_TRACKS));

            return disc;
        }
    }

    private JSONArray mJsonArray;

    public JsonTrackArrayParser(InputStream in) {
        mJsonArray = (JSONArray)JSONValue.parse(in);
    }

    public List<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<>();

        for (Object o : mJsonArray) {
            Object trackJsonObject = ((JSONObject)o).get(KEY_TRACK);
            tracks.add(JsonTrackParser.parse((JSONObject)trackJsonObject));
        }

        return tracks;
    }

}
