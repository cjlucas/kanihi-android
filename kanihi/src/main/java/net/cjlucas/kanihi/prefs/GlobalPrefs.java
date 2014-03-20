package net.cjlucas.kanihi.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class GlobalPrefs {
    private static final String PREF_NAME = "kanihi";
    private static final String KEY_API_HOST = "api_host";
    private static final String KEY_API_PORT = "api_port";
    private static final String KEY_LAST_UPDATED = "last_updated";

    private SharedPreferences mSharedPreferences;

    public GlobalPrefs(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setApiInfo(String host, int port) {
        mSharedPreferences.edit()
                .putString(KEY_API_HOST, host)
                .putInt(KEY_API_PORT, port).commit();
    }

    public String getApiHost() {
        return mSharedPreferences.getString(KEY_API_HOST, null);
    }

    public int getApiPort() {
        return mSharedPreferences.getInt(KEY_API_PORT, 0);
    }

    public void setLastUpdated(String lastUpdated) {
        mSharedPreferences.edit().putString(KEY_LAST_UPDATED, lastUpdated).commit();
    }

    public String getLastUpdated() {
        return mSharedPreferences.getString(KEY_LAST_UPDATED, null);
    }
}
