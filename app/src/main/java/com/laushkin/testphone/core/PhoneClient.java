package com.laushkin.testphone.core;

import android.content.Context;
import android.os.Message;
import android.text.TextUtils;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class PhoneClient implements Communicator.MessageHandler {
    private final Communicator mCommunicator;

    public PhoneClient(String name) {
        String communicatorName = (!TextUtils.isEmpty(name))?name:generateName();

        mCommunicator = new Communicator(this, name);
    }

    private String generateName() {
        return toString();
    }

    public void disconnect(Context context) {
        if (isConnected()) {
            mCommunicator.disconnect(context);
        }
    }

    private boolean isConnected() {
        return mCommunicator.isConnected();
    }

    @Override
    public void handleMessage(String from, Message msg) {

    }

    @Override
    public void onNewConnection(String from) {

    }

    @Override
    public void onLostConnection(String from) {

    }
}
