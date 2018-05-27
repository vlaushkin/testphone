package com.laushkin.testphone.core;

import android.content.Context; /**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
class Utils {
    public static String getAppId(Context context) {
        return context.getApplicationContext().getPackageName();
    }
}
