package com.laushkin.testphone.core;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class PhoneClient implements Communicator.MessageHandler {
    private final Communicator mCommunicator;
    private ConnectionListener mConnectionListener;
    private EventListener mEventListener;

    public PhoneClient(String name) {
        String communicatorName = (!TextUtils.isEmpty(name))?name:generateName();

        mCommunicator = new Communicator(this, name);
    }

    public void connect(Context context) {
        mCommunicator.connect(context);
    }

    public void setConnectionListener(ConnectionListener listener) {
        mConnectionListener = listener;
    }

    public void setEventListener(EventListener listener) {
        mEventListener = listener;
    }

    private String generateName() {
        return toString();
    }

    public void disconnect(Context context) {
        mCommunicator.disconnect(context);
    }

    public boolean isConnected() {
        return mCommunicator.isConnected();
    }

    public void register(String id, String registrar, String proxy, String username, String password) {
        sendCommand(PhoneService.Command.REGISTER, BundleTool.newBundle()
                .withString(PhoneService.Command.Extra.REGISTER_ID, id)
                .withString(PhoneService.Command.Extra.REGISTRAR, registrar)
                .withString(PhoneService.Command.Extra.PROXY, proxy)
                .withString(PhoneService.Command.Extra.USERNAME, username)
                .withString(PhoneService.Command.Extra.PASSWORD, password)
                .build());
    }

    public void makeCall(String uri) {
        sendCommand(PhoneService.Command.CALL_TO, BundleTool.newBundle()
                .withString(PhoneService.Command.Extra.URI, uri)
                .build());
    }

    public void acceptCall() {
        sendCommand(PhoneService.Command.CALL_ACCEPT);
    }

    public void declineCall() {
        sendCommand(PhoneService.Command.CALL_DECLINE);
    }

    public void exit() {
        sendCommand(PhoneService.Command.EXIT);
    }

    private void sendCommand(int commnand) {
        sendCommand(commnand, null);
    }

    private void sendCommand(int commnand, Bundle data) {
        if (isConnected()) {
            Message message = new Message();
            message.what = commnand;
            if (data != null) {
                message.setData(data);
            }
            try {
                mCommunicator.sendMessage(PhoneService.COMMUNICATOR_NAME, message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleMessage(String from, Message msg) {
        switch (msg.what) {
            case PhoneService.Event.CALL_INIT:
                handleCallInit();
                return;
            case PhoneService.Event.REGISTRATION_SUCCESSFUL:
                handleRegistrationSuccessful();
                return;
            case PhoneService.Event.REGISTRATION_FAILED:
                handleRegistrationFailed();
                return;
            case PhoneService.Event.INCOMING_CALL:
                handleIncomingCall(msg.getData());
                return;
            case PhoneService.Event.CALL_STATE:
                handleCallState(msg.getData());
                return;
        }
    }

    private void handleCallInit() {
        if (mEventListener != null) {
            mEventListener.onCallInit();
        }
    }

    private void handleRegistrationSuccessful() {
        if (mEventListener != null) {
            mEventListener.onRegistrationSuccessful();
        }
    }

    private void handleRegistrationFailed() {
        if (mEventListener != null) {
            mEventListener.onRegistrationFailed();
        }
    }

    private void handleIncomingCall(Bundle data) {
        String remoteUri = data.getString(PhoneService.Event.Extra.REMOTE_URI);
        if (mEventListener != null) {
            mEventListener.onIncomingCall(remoteUri);
        }
    }

    private void handleCallState(Bundle data) {
        int callState = data.getInt(PhoneService.Event.Extra.CALL_STATE);
        int role = data.getInt(PhoneService.Event.Extra.ROLE);
        String remoteUri = data.getString(PhoneService.Event.Extra.REMOTE_URI);
        if (mEventListener != null) {
            mEventListener.onCallState(remoteUri, callState, role);
        }
    }

    @Override
    public void onNewConnection(String from) {
        if (mConnectionListener != null && PhoneService.COMMUNICATOR_NAME.equals(from)) {
            mConnectionListener.connected();
        }
    }

    @Override
    public void onLostConnection(String from) {
        if (mConnectionListener != null && PhoneService.COMMUNICATOR_NAME.equals(from)) {
            mConnectionListener.disconnected();
        }
    }

    public interface ConnectionListener {
        void connected();
        void disconnected();
    }

    public interface EventListener {
        void onCallInit();
        void onRegistrationSuccessful();
        void onRegistrationFailed();
        void onIncomingCall(String remoteUri);
        void onCallState(String remoteUri, int state, int role);
    }
}
