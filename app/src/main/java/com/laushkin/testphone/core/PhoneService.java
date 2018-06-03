package com.laushkin.testphone.core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.laushkin.testphone.core.pjsip.PhoneAccount;
import com.laushkin.testphone.core.pjsip.PhoneApp;
import com.laushkin.testphone.core.pjsip.PhoneCall;
import com.laushkin.testphone.core.pjsip.PhoneEventListener;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.StringVector;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.LinkedList;

public class PhoneService extends Service implements Communicator.MessageHandler, PhoneEventListener {
    public static final String COMMUNICATOR_NAME = "SuperPhoneServiceEver";
    private static final String TAG = "PhoneService";

    static {
        System.loadLibrary("pjsua2");
    }

    private Communicator mCommunicator;
    private LinkedList<String> mRoster;
    private PhoneApp mPhoneApp;

    public PhoneCall mCurrentCall = null;
    public PhoneAccount mAccount = null;
    public AccountConfig mAccCfg = null;

    private NetworkBroadcastReceiver mNetworkReceiver;

    public PhoneService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Context getContext() {
        return this;
    }

    @Override
    public void onCreate() {
        mCommunicator = new Communicator(this, COMMUNICATOR_NAME, false);
        mRoster = new LinkedList<>();

        mCommunicator.connect(getContext());

        mPhoneApp = new PhoneApp();

        registerNetworkChangeReceiver();
    }

    private void registerNetworkChangeReceiver() {
        IntentFilter intentFilter = new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION);
        mNetworkReceiver = new NetworkBroadcastReceiver();
        registerReceiver(mNetworkReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCommunicator.disconnect(this);
        unregisterReceiver(mNetworkReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return START_STICKY;
    }

    private void init() {
        mPhoneApp.init(this, getFilesDir().getAbsolutePath());

        if (mPhoneApp.mAccList.size() == 0) {
            mAccCfg = new AccountConfig();
            mAccCfg.setIdUri("sip:localhost");
            mAccCfg.getNatConfig().setIceEnabled(true);
            mAccCfg.getVideoConfig().setAutoTransmitOutgoing(true);
            mAccCfg.getVideoConfig().setAutoShowIncoming(true);
            mAccount = mPhoneApp.addAcc(mAccCfg);
        } else {
            mAccount = mPhoneApp.mAccList.get(0);
            mAccCfg = mAccount.config;
        }


    }

    public void register(String id, String registrar, String proxy, String username, String password) {
        mAccCfg.setIdUri(id);
        mAccCfg.getRegConfig().setRegistrarUri(registrar);
        AuthCredInfoVector creds = mAccCfg.getSipConfig().
                getAuthCreds();
        creds.clear();
        if (username.length() != 0) {
            creds.add(new AuthCredInfo("Digest", "*", username, 0,
                    password));
        }
        StringVector proxies = mAccCfg.getSipConfig().getProxies();
        proxies.clear();
        if (proxy.length() != 0) {
            proxies.add(proxy);
        }

        mAccCfg.getNatConfig().setIceEnabled(true);

        try {
            mAccount.modify(mAccCfg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void makeCall(String uri) {
        PhoneCall call = new PhoneCall(mAccount, -1, mPhoneApp);
        CallOpParam prm = new CallOpParam(true);

        try {
            call.makeCall(uri, prm);
        } catch (Exception e) {
            e.printStackTrace();
            call.delete();
            return;
        }

        mCurrentCall = call;
        sendEvent(Event.CALL_INIT);
    }

    public void acceptCall() {
        if (mCurrentCall == null) return;

        CallOpParam prm = new CallOpParam();
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
        try {
            mCurrentCall.answer(prm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void declineCall() {
        if (mCurrentCall == null) return;

        CallOpParam prm = new CallOpParam();
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
        try {
            mCurrentCall.hangup(prm);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void exit() {
        mPhoneApp.exit();
        stopSelf();
    }

    private void sendEvent(int id) {
        sendEvent(id, null);
    }

    private void sendEvent(int id, Bundle data) {
        for (String contact: mRoster) {
            Message message = new Message();
            message.what = id;
            if (data != null) {
                message.setData(data);
            }
            try {
                mCommunicator.sendMessage(contact, message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleMessage(String from, Message msg) {
        switch (msg.what) {
            case Command.REGISTER:
                handleRegister(from, msg.getData());
                return;
            case Command.CALL_TO:
                handleCallTo(from, msg.getData());
                return;
            case Command.CALL_ACCEPT:
                handleCallAccept(from, msg.getData());
                return;
            case Command.CALL_DECLINE:
                handleCallDecline(from, msg.getData());
                return;
            case Command.EXIT:
                handleExit(from, msg.getData());
        }
    }

    private void handleRegister(String from, Bundle data) {
        String id = data.getString(EventCommandBase.Extra.REGISTER_ID);
        String registrar = data.getString(EventCommandBase.Extra.REGISTRAR);
        String proxy = data.getString(EventCommandBase.Extra.PROXY);
        String username = data.getString(EventCommandBase.Extra.USERNAME);
        String password = data.getString(EventCommandBase.Extra.PASSWORD);

        if (TextUtils.isEmpty(id) ||
                TextUtils.isEmpty(registrar) ||
                TextUtils.isEmpty(proxy) ||
                TextUtils.isEmpty(username) ||
                TextUtils.isEmpty(password)) {
            return;
        }

        register(id, registrar, proxy, username, password);
    }

    private void handleCallTo(String from, Bundle data) {
        String uri = data.getString(EventCommandBase.Extra.URI);

        if (TextUtils.isEmpty(uri)) return;

        makeCall(uri);
    }

    private void handleCallAccept(String from, Bundle data) {
        acceptCall();
    }

    private void handleCallDecline(String from, Bundle data) {
        declineCall();
    }

    private void handleExit(String from, Bundle data) {
        exit();
    }

    @Override
    public void onNewConnection(String from) {
        mRoster.add(from);
    }

    @Override
    public void onLostConnection(String from) {
        mRoster.remove(from);
    }

    @Override
    public void notifyRegState(pjsip_status_code code, String reason, int expiration) {
        boolean successful = code.swigValue()/100 == 2;

        if (successful) {
            sendEvent(Event.REGISTRATION_SUCCESSFUL);
        } else {
            sendEvent(Event.REGISTRATION_FAILED, BundleTool.newBundle()
                    .withString(Event.Extra.REASON, reason)
                    .build());
        }
    }

    @Override
    public void notifyIncomingCall(PhoneCall call) {
        mCurrentCall = call;

        CallOpParam prm = new CallOpParam();
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
        try {
            call.answer(prm);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        CallInfo ci;
        try {
            ci = call.getInfo();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String remoteUri = ci.getRemoteUri();

        sendEvent(Event.INCOMING_CALL, BundleTool.newBundle()
                .withString(EventCommandBase.Extra.REMOTE_URI, remoteUri)
                .build());
    }

    @Override
    public void notifyCallState(PhoneCall call) {
        CallInfo ci;
        try {
            ci = call.getInfo();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int callState = ci.getState().swigValue();
        int role = ci.getRole().swigValue();
        String remoteUri = ci.getRemoteUri();

        sendEvent(Event.CALL_STATE, BundleTool.newBundle()
                .withInt(Event.Extra.CALL_STATE, callState)
                .withInt(Event.Extra.ROLE, role)
                .withString(Event.Extra.REMOTE_URI, remoteUri)
                .build());

        if (ci.getState() == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
            call.delete();
            mCurrentCall = null;
        }
    }

    @Override
    public void notifyCallMediaState(PhoneCall call) {
        // do nothing
    }

    @Override
    public void notifyBuddyState(PhoneCall buddy) {
        // do nothing
    }

    @Override
    public void notifyChangeNetwork() {
        mPhoneApp.handleNetworkChange();
    }

    private class NetworkBroadcastReceiver extends BroadcastReceiver {
        private String connName = "";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkChange(context))
                notifyChangeNetwork();
        }

        private boolean isNetworkChange(Context context) {
            boolean networkChanged = false;
            ConnectivityManager connectivity_mgr =
                    ((ConnectivityManager)context.getSystemService(
                            Context.CONNECTIVITY_SERVICE));

            NetworkInfo netInfo = connectivity_mgr.getActiveNetworkInfo();
            if(netInfo != null && netInfo.isConnectedOrConnecting() &&
                    !connName.equalsIgnoreCase("")) {
                String newCon = netInfo.getExtraInfo();
                if (newCon != null && !newCon.equalsIgnoreCase(connName))
                    networkChanged = true;

                connName = (newCon == null)?"":newCon;
            } else {
                if (connName.equalsIgnoreCase(""))
                    connName = netInfo.getExtraInfo();
            }
            return networkChanged;
        }
    }

    private interface EventCommandBase {
        interface Extra {
            String URI = "extra_number";
            String REASON = "extra_reason";
            String CALL_STATE = "extra_state";
            String ROLE = "extra_role";
            String REMOTE_URI = "extra_remote_uri";
            String REGISTER_ID = "extra_reg_id";
            String REGISTRAR = "extra_registrar";
            String PROXY = "extra_proxy";
            String USERNAME = "extra_username";
            String PASSWORD = "extra_password";
        }
    }

    // incoming messages
    interface Command extends EventCommandBase {
        int REGISTER = 1001;
        int CALL_TO = 1002;
        int CALL_ACCEPT = 1003;
        int CALL_DECLINE = 1004;
        int EXIT = 1005;
    }

    // outgoing messages
    interface Event extends EventCommandBase {
        int CALL_INIT = 2001;
        int REGISTRATION_SUCCESSFUL = 2002;
        int REGISTRATION_FAILED = 2003;
        int INCOMING_CALL = 2004;
        int CALL_STATE = 2005;
        int CALL_MEDIA_STATE = 2006;
    }
}
