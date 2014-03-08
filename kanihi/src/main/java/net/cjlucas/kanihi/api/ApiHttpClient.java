package net.cjlucas.kanihi.api;

import com.loopj.android.http.AsyncHttpClient;

import net.minidev.json.JSONArray;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.Date;

public class ApiHttpClient {
    private static ApiHttpClient mApiClient;
    private AsyncHttpClient mAsyncClient;

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

    public static void getTracks(long offset, long limit, Date lastUpdatedAt,
                                 final Callback<JSONArray> callback) {
        Header[] headers = {
                new BasicHeader("SQL-Offset", String.valueOf(offset)),
                new BasicHeader("SQL-Limit", String.valueOf(limit)),
                /* new BasicHeader("Last-Updated-At", "") */
        };

        getInstance().mAsyncClient.get(
                null, "http://home.cjlucas.net:34232/tracks.json", headers, null,
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
}
