package net.cjlucas.kanihi.data.parser;

import net.cjlucas.kanihi.model.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonTrackArrayParser {
    private abstract class JsonModelParser {
        public String getString(JSONObject o, String key) {
            Object val = o.get(key);
            return val == null ? null : (String)val;
        }
    }
    public class JsonTrackParser extends JsonModelParser {
        private Track mTrack;

        public JsonTrackParser() {

        }

        public Track parse(JSONObject jsonObject) {
            mTrack = new Track();
            mTrack.setUuid(getString(jsonObject, "uuid"));

            return mTrack;
        }
    }

    private JSONArray mJsonArray;

    public JsonTrackArrayParser(InputStream in) {
        mJsonArray = (JSONArray)JSONValue.parse(in);
    }

    public List<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<>();

        for (Object o : mJsonArray) {
            JsonTrackParser parser = new JsonTrackParser();
            Object trackJsonObject = ((JSONObject)o).get("track");
            tracks.add(parser.parse((JSONObject)trackJsonObject));
        }

        return tracks;
    }

}
