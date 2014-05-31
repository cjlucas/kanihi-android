package net.cjlucas.kanihi.api;

import com.loopj.android.http.BaseJsonHttpResponseHandler;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONValue;

import org.apache.http.Header;

public class JsonArrayHttpResponseHandler extends BaseJsonHttpResponseHandler<JSONArray> {
    @Override
    public void onSuccess(int i, Header[] headers, String s, JSONArray objects) {

    }

    @Override
    public void onFailure(int i, Header[] headers, Throwable throwable, String s, JSONArray objects) {

    }

    @Override
    protected JSONArray parseResponse(String s, boolean isFailure) throws Throwable {
        return (JSONArray)JSONValue.parse(s);
    }
}
