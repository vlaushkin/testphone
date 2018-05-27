package com.laushkin.testphone.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Set;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class Communicator {
    private static final String ACTION = "org.satel.rtu.im.communicator.Communicator.ACTION";
    private static final String EXTRA_COMMAND = "org.satel.rtu.im.communicator.Communicator.EXTRA_COMMAND";
    private static final String EXTRA_NODE = "org.satel.rtu.im.communicator.Communicator.EXTRA_NODE";

    private static final int COMMAND_INCORRECT = -1;
    private static final int COMMAND_NEW_CONNECTION = 1001;

    private static final int MESSAGE_INCORRECT = -1;
    private static final int MESSAGE_CONNECTING_REPLY = 101;
    private static final int MESSAGE_DISCONNECTING = 102;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null || !intent.getAction().equals(mAction)) return;

            int command = intent.getIntExtra(EXTRA_COMMAND, COMMAND_INCORRECT);
            if (command == COMMAND_INCORRECT) return;

            if (command == COMMAND_NEW_CONNECTION) {
                Node newNode = intent.getParcelableExtra(EXTRA_NODE);
                if (!newNode.name().equals(selfName())) {
                    connectWith(newNode);
                }
            }
        }
    };

    private final Node mSelfNode;
    private final HashMap<String, Node> mContacts;
    private final MessageHandler mHandler;
    private boolean mIsConnected;
    private final boolean mExternal;

    private String mAction;

    private InternalHandler publicHandler;
    private InternalHandler privateHandler;

    public Communicator(final MessageHandler handler, final String selfName) {
        this(handler, selfName, false);
    }

    public Communicator(final MessageHandler handler, final String selfName, final boolean external) {
        publicHandler = new InternalHandler(new OnMessageListener() {
            @Override
            public void onMessage(Message msg) {
                handler.handleMessage(getNode(msg).name(), msg);
            }
        });

        privateHandler = new InternalHandler(new OnMessageListener() {
            @Override
            public void onMessage(Message msg) {
                handleInternalMessage(msg);
            }
        });

        Messenger messenger = new Messenger(publicHandler);
        Messenger privateMessenger = new Messenger(privateHandler);

        this.mSelfNode = new Node(messenger, privateMessenger, selfName);
        this.mContacts = new HashMap<>();
        this.mHandler = handler;
        this.mExternal = external;
    }

    public String selfName() {
        return mSelfNode.name();
    }

    public void connect(Context context) {
        if (!mIsConnected) {
            mIsConnected = true;
            mContacts.clear();
            mAction = buildAction(context);
            registerReceiver(context, mReceiver, new IntentFilter(mAction));
            sendConnectionBroadcast(context);
        }
    }

    private String buildAction(Context context) {
        return Utils.getAppId(context) + "." + Communicator.ACTION;
    }

    private void registerReceiver(Context context, BroadcastReceiver receiver, IntentFilter intentFilter) {
        if (mExternal) {
            context.registerReceiver(receiver, intentFilter);
        } else {
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);
        }
    }

    public void disconnect(Context context) {
        if (mIsConnected) {
            unregisterReceiver(context, mReceiver);
            try {
                internalSendPrivateBroadcats(obtainMessage(MESSAGE_DISCONNECTING));
            } catch (RemoteException e) {
                // TODO
                e.printStackTrace();
            }
            mContacts.clear();
            mIsConnected = false;
        }
    }

    private void unregisterReceiver(Context context, BroadcastReceiver receiver) {
        if (mExternal) {
            context.unregisterReceiver(receiver);
        } else {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        }
    }

    public Set<String> contacts() {
        return mContacts.keySet();
    }

    public void sendMessage(String to, Message msg) throws RemoteException {
        Node node = mContacts.get(to);
        if (node != null) {
            sendMessage(node, msg);
        }
    }

    public void sendBroadcast(Message msg) throws RemoteException {
        internalSendBroadcast(msg);
    }

    private void sendMessage(Node node, Message msg) throws RemoteException {
        internalSendMessage(node, msg);
    }

    private void internalSendMessage(Node node, Message msg) throws RemoteException {
        addNode(msg);
        node.messenger().send(msg);
    }

    private void internalSendPrivateMessage(Node node, Message msg) throws RemoteException {
        addNode(msg);
        node.privateMessenger().send(msg);
    }

    private void internalSendBroadcast(Message msg) throws RemoteException {
        for (Node node: mContacts.values()) {
            internalSendMessage(node, Message.obtain(msg));
        }
    }

    private void internalSendPrivateBroadcats(Message msg) throws RemoteException {
        for (Node node: mContacts.values()) {
            internalSendPrivateMessage(node, Message.obtain(msg));
        }
    }

    private Message obtainMessage(int what) {
        Message message = Message.obtain();
        message.what = what;
        return message;
    }

    private boolean handleInternalMessage(Message message) {
        if (message.what < 1000) {

            if (message.what == MESSAGE_CONNECTING_REPLY) {
                Node node = getNode(message);
                if (node != null && !node.name().equals(selfName())) {
                    mContacts.put(node.name(), node);
                    mHandler.onNewConnection(node.name());
                }
            } else if (message.what == MESSAGE_DISCONNECTING) {
                Node node = getNode(message);
                if (node != null && !node.name().equals(selfName())) {
                    mContacts.remove(node.name());
                    mHandler.onLostConnection(node.name());
                }
            }

            return true;
        }

        return false;
    }

    private Node getNode(Message message) {
        Bundle bundle = message.getData();
        bundle.setClassLoader(Communicator.class.getClassLoader());
        return bundle.getParcelable(EXTRA_NODE);
    }

    private void addNode(Message message) {
        Bundle bundle = message.getData();
        if (bundle == null) bundle = new Bundle();

        bundle.putParcelable(EXTRA_NODE, mSelfNode);
        message.setData(bundle);
    }

    private void sendConnectionBroadcast(Context context) {
        Intent intent = new Intent(mAction);
        intent.putExtra(EXTRA_COMMAND, COMMAND_NEW_CONNECTION);
        intent.putExtra(EXTRA_NODE, mSelfNode);
        sendBroadcast(context, intent);
    }

    private void sendBroadcast(Context context, Intent intent) {
        if (mExternal) {
            context.sendBroadcast(intent);
        } else {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    private void connectWith(Node node) {
        mContacts.put(node.name(), node);
        try {
            internalSendPrivateMessage(node, obtainMessage(MESSAGE_CONNECTING_REPLY));
        } catch (RemoteException e) {
            // TODO:
            e.printStackTrace();
        }

        mHandler.onNewConnection(node.name());
    }

    public interface MessageHandler {
        void handleMessage(String from, Message msg);

        void onNewConnection(String from);

        void onLostConnection(String from);
    }

    private static class Node implements Parcelable {
        private final Messenger mMessenger;
        private final Messenger mPrivateMessenger;
        private final String mName;

        Node(Messenger messenger, Messenger privateMessenger, String name) {
            this.mMessenger = messenger;
            this.mPrivateMessenger = privateMessenger;
            this.mName = name;
        }

        public Messenger messenger() {
            return mMessenger;
        }

        public Messenger privateMessenger() {
            return mPrivateMessenger;
        }

        public String name() {
            return mName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(this.mMessenger, flags);
            dest.writeParcelable(this.mPrivateMessenger, flags);
            dest.writeString(this.mName);
        }

        protected Node(Parcel in) {
            this.mMessenger = in.readParcelable(Messenger.class.getClassLoader());
            this.mPrivateMessenger = in.readParcelable(Messenger.class.getClassLoader());
            this.mName = in.readString();
        }

        public static final Creator<Node> CREATOR = new Creator<Node>() {
            @Override
            public Node createFromParcel(Parcel source) {
                return new Node(source);
            }

            @Override
            public Node[] newArray(int size) {
                return new Node[size];
            }
        };
    }

    private interface OnMessageListener {
        void onMessage(Message msg);
    }

    private static class InternalHandler extends Handler {
        private final OnMessageListener mListener;

        public InternalHandler(final OnMessageListener listener) {
            this.mListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (mListener != null) {
                mListener.onMessage(msg);
            }
        }
    }
}
