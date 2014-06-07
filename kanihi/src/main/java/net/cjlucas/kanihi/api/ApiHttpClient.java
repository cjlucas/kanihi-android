package net.cjlucas.kanihi.api;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import net.cjlucas.kanihi.models.Track;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

    public void getTracks(long offset, long limit, String lastUpdated,
                                 final Callback<JSONArray> callback) {

        ArrayList<Header> headersList = new ArrayList<>();
        headersList.add(new BasicHeader("SQL-Offset", String.valueOf(offset)));
        headersList.add(new BasicHeader("SQL-Limit", String.valueOf(limit)));

        if (lastUpdated != null) headersList.add(new BasicHeader("Last-Updated-At", lastUpdated));

        Header[] headers = headersList.toArray(new Header[0]);

        mAsyncClient.get(null, getUrl("/tracks.json"), headers, null,
                new JsonArrayHttpResponseHandler() {
                    public void onSuccess(int i, Header[] headers, String s, JSONArray objects) {
                        callback.onSuccess(objects);
                    }

            public void onFailure(int statusCode, Header[] headers, Throwable e,
                                  String rawData, JSONArray errorResponse) {
                callback.onFailure();
            }
        });
    }

    public void getTrackCount(String lastUpdated, final Callback<Integer> callback) {
        ArrayList<Header> headersList = new ArrayList<>();
        if (lastUpdated != null) headersList.add(new BasicHeader("Last-Updated-At", lastUpdated));

        Header[] headers = headersList.toArray(new Header[0]);

        mAsyncClient.get(null, getUrl("/tracks/count.json"), headers, null,
                new JsonObjectHttpResponseHandler() {
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

    public void getDeletedTracks(long offset, long limit, String lastUpdated,
                                 final Callback<JSONArray> callback) {
        ArrayList<Header> headersList = new ArrayList<>();
        headersList.add(new BasicHeader("SQL-Offset", String.valueOf(offset)));
        headersList.add(new BasicHeader("SQL-Limit", String.valueOf(limit)));

        if (lastUpdated != null) headersList.add(new BasicHeader("Last-Updated-At", lastUpdated));

        Header[] headers = headersList.toArray(new Header[0]);

        mAsyncClient.get(null, getUrl("/tracks/deleted.json"), headers, null,
                new JsonArrayHttpResponseHandler()  {
                    @Override
                    public void onSuccess(int i, Header[] headers, String s, JSONArray objects) {
                        callback.onSuccess(objects);
                    }

                    @Override
                    public void onFailure(int i, Header[] headers, Throwable throwable,
                                          String s, JSONArray objects) {
                        callback.onFailure();
                    }
                }
        );
    }

    public void getServerTime(final Callback<String> callback) {
        mAsyncClient.get(getUrl("/info.json"), new JsonObjectHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  String s, JSONObject jsonObject) {
                JSONObject serverInfo = (JSONObject)jsonObject.get("server_info");
                callback.onSuccess((String)serverInfo.get("server_time"));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  Throwable throwable, String s, JSONObject jsonObject) {
                callback.onFailure();
            }
        });
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
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                callback.onFailure();
            }

           @Override
            public void onSuccess(int i, Header[] headers, byte[] binaryData) {
                callback.onSuccess(binaryData);
            }
        });
    }

    public RequestHandle getImage(String imageId, final Callback<byte[]> callback) {
        return getImage(imageId, -1, callback);
    }

    public URL getStreamUrl(Track track) {
        try {
            return new URL(getUrl("/tracks") + "/" + track.getUuid() + "/stream");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
