package com.laushkin.testphone;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Pair;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 03/06/2018.
 */
class Utils {
    private static final String APP_PREFS = "com.laushkin.testphone.app_prefs";
    private static final String USERNAME = "extra_username";
    private static final String PASSWORD = "extra_password";

    public static void saveCreds(Context context, String userName, String password) {
        SharedPreferences preferences = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(USERNAME, userName);
        editor.putString(PASSWORD, password);
        editor.apply();
    }

    public static Pair<String, String> getCreds(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        String username = preferences.getString(USERNAME, null);
        String password = preferences.getString(PASSWORD, null);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) return null;
        return new Pair<>(username, password);
    }
}
