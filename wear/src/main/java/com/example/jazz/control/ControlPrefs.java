package com.example.jazz.control;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by CAP-ONE on 30/12/2016.
 */

public class ControlPrefs {

    private SharedPreferences.Editor prefsEditor;
    private SharedPreferences prefs;
    public static final String PREFS_NAME = "controlPrefs";
    public static final String DB_VERSION = "dbversion";

    private Context context;

    public ControlPrefs(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
    }

    public int getIntPreference(String key) {
        return prefs.getInt(key, 0);
    }

    public void saveIntPreference(String key, int value) {
        prefsEditor.putInt(key, value);
        prefsEditor.commit();
    }
}
