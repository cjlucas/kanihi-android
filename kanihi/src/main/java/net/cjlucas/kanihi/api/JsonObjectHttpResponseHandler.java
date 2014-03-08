package net.cjlucas.kanihi.api;

import com.loopj.android.http.BaseJsonHttpResponseHandler;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.Header;

public class JsonObjectHttpResponseHandler extends BaseJsonHttpResponseHandler<JSONObject>{
    @Override
    public void onSuccess(int statusCode, Header[] headers, String s, JSONObject jsonObject) {

    }

    @Override
    public void onFailure(int statusCode, Header[] headers,
                          Throwable throwable, String s, JSONObject jsonObject) {

    }

    @Override
    protected JSONObject parseResponse(String s) throws Throwable {
        System.err.println("here: " + s);
        return (JSONObject)JSONValue.parse(s);
    }
}
