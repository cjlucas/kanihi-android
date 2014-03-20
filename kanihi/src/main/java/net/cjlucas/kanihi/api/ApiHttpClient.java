package net.cjlucas.kanihi.api;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ApiHttpClient {
    private static final String TAG = "ApiHttpClient";

    private AsyncHttpClient mAsyncClient;
    private String mApiHost;
    private int mApiPort;

    public interface Callback<T> {
        public void onSuccess(T data);
        public void onFailure();
    }

    public ApiHttpClient() {
        mAsyncClient = new AsyncHttpClient();
        mAsyncClient.setThreadPool((ThreadPoolExecutor)Executors.newFixedThreadPool(1));
        mAsyncClient.setMaxConnections(1);
    }

    public void setApiEndpoint(String host, int port) {
        mApiHost = host;
        mApiPort = port;
    }

    private String getUrl(String path) {
        String host = mApiHost;
        int port = mApiPort;

        if (host == null || port < 1) {
            throw new RuntimeException("API endpoint info is not set");
        }

        try {
            return new URL("http", host, port, path == null ? "/" : path).toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    public void getTracks(long offset, long limit, Date lastUpdatedAt,
                                 final Callback<JSONArray> callback) {
        Header[] headers = {
                new BasicHeader("SQL-Offset", String.valueOf(offset)),
                new BasicHeader("SQL-Limit", String.valueOf(limit)),
                /* new BasicHeader("Last-Updated-At", "") */
        };

        mAsyncClient.get(null, getUrl("/tracks.json"), headers, null, new JsonArrayHttpResponseHandler() {
            public void onSuccess(int i, Header[] headers, String s, JSONArray objects) {
                callback.onSuccess(objects);
            }
            public void onFailure(int statusCode, Header[] headers, Throwable e,
                                  String rawData, JSONArray errorResponse) {
                callback.onFailure();
            }
        });
    }

    public void getTrackCount(final Callback<Integer> callback) {
        mAsyncClient.get(getUrl("/tracks/count.json"), new JsonObjectHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  String s, JSONObject response) {
                callback.onSuccess((Integer) response.get("track_count"));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  String s, JSONObject jsonObject) {
                callback.onFailure();
            }
        });
    }

    public void getDeletedTracks(List<String> uuids,
                                 final Callback<List<String>> callback) {

        JSONArray jsonArray = new JSONArray();
        for (String uuid : uuids) jsonArray.add(uuid);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current_tracks", jsonArray);

        HttpEntity postEntity;
        try {
            postEntity = new StringEntity(jsonObject.toJSONString());
        } catch (UnsupportedEncodingException e) {
            callback.onFailure();
            return;
        }

        mAsyncClient.post(null, getUrl("/tracks/deleted.json"),
                postEntity, "application/json", new JsonObjectHttpResponseHandler() {
            @SuppressWarnings("unchecked")
            @Override
            public void onSuccess(int statusCode, Header[] headers, String s, JSONObject jsonObject) {
                callback.onSuccess((List<String>)jsonObject.get("deleted_tracks"));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String s, JSONObject jsonObject) {
                callback.onFailure();
            }
        }
        );
    }

    public RequestHandle getImage(String imageId, int width, final Callback<byte[]> callback) {
        String url = getUrl("/images") + "/" + imageId;

        Log.v(TAG, "image width: " + width);

        Header[] headers = new Header[1];
        if (width > 0) {
           headers[0] = new BasicHeader("Image-Resize-Width", String.valueOf(width));
        } else {
            headers = new Header[0];
        }

        return mAsyncClient.get(null, url, headers, null, new BinaryHttpResponseHandler() {
            @Override
            public void onSuccess(byte[] binaryData) {
                callback.onSuccess(binaryData);
            }
        }
        );
    }

    public RequestHandle getImage(String imageId, final Callback<byte[]> callback) {
        return getImage(imageId, -1, callback);
    }
}
