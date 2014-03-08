package net.cjlucas.kanihi.api;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class ApiHttpClient {
    private static ApiHttpClient mApiClient;
    private AsyncHttpClient mAsyncClient;
    private String mApiHost;
    private int mApiPort;

    public interface Callback<T> {
        public void onSuccess(T data);
        public void onFailure();
    }

    private static synchronized ApiHttpClient getInstance() {
        if (mApiClient == null) {
            mApiClient = new ApiHttpClient();
        }

        return mApiClient;
    }

    public ApiHttpClient() {
        mAsyncClient = new AsyncHttpClient();
        mAsyncClient.setMaxConnections(1);
    }

    public static void setApiEndpoint(String host, int port) {
        getInstance().mApiHost = host;
        getInstance().mApiPort = port;
    }

    private static String getUrl(String path) {

        try {
            path = path == null ? "/" : path;
            URL url = new URL("http", getInstance().mApiHost, getInstance().mApiPort, path);
            return url.toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    public static void getTracks(long offset, long limit, Date lastUpdatedAt,
                                 final Callback<JSONArray> callback) {
        Header[] headers = {
                new BasicHeader("SQL-Offset", String.valueOf(offset)),
                new BasicHeader("SQL-Limit", String.valueOf(limit)),
                /* new BasicHeader("Last-Updated-At", "") */
        };

        getInstance().mAsyncClient.get(null, getUrl("/tracks.json"), headers, null,
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

    public static void getTrackCount(final Callback<Integer> callback) {
        getInstance().mAsyncClient.get(getUrl("/tracks/count.json"),
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
}
