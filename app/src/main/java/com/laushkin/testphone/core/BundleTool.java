package com.laushkin.testphone.core;

import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class BundleTool {
    private final Bundle mBundle;

    private BundleTool(Bundle bundle) {
        mBundle = bundle;
    }

    public static BundleTool newBundle() {
        return new BundleTool(new Bundle());
    }

    public Bundle build() {
        return mBundle;
    }

    public BundleTool withString(String key, String data) {
        mBundle.putString(key, data);
        return this;
    }

    public BundleTool withInt(String key, int data) {
        mBundle.putInt(key, data);
        return this;
    }

    public BundleTool withLong(String key, long data) {
        mBundle.putLong(key, data);
        return this;
    }

    public BundleTool withStringArrayList(String key, ArrayList<String> data) {
        mBundle.putStringArrayList(key, data);
        return this;
    }

    public BundleTool withStringArray(String key, String[] data) {
        mBundle.putStringArray(key, data);
        return this;
    }

    public BundleTool withParcelable(String key, Parcelable data) {
        mBundle.putParcelable(key, data);
        return this;
    }

    public BundleTool withParcelableArrayList(String key, ArrayList<Parcelable> data) {
        mBundle.putParcelableArrayList(key, data);
        return this;
    }

    public BundleTool withParcelableArray(String key, Parcelable[] data) {
        mBundle.putParcelableArray(key, data);
        return this;
    }

    public BundleTool withBundle(String key, Bundle data) {
        mBundle.putBundle(key, data);
        return this;
    }

    public BundleTool withBoolean(String key, boolean data) {
        mBundle.putBoolean(key, data);
        return this;
    }
}