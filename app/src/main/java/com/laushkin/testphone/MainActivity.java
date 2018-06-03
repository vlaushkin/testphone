package com.laushkin.testphone;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;

import com.laushkin.testphone.core.PhoneClient;

public class MainActivity extends AppCompatActivity implements PhoneClient.ConnectionListener, PhoneClient.EventListener {

    private PhoneClient phoneClient;
    private Pair<String, String> mCreds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCreds = Utils.getCreds(this);

        phoneClient = new PhoneClient(this.toString());
    }

    @Override
    public void onStart() {
        super.onStart();
        phoneClient.setConnectionListener(this);
        phoneClient.setEventListener(this);
        phoneClient.connect(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        phoneClient.setConnectionListener(null);
        phoneClient.setEventListener(null);
        phoneClient.disconnect(this);
    }

    @Override
    public void connected() {
        phoneClient.register("sip:" + mCreds.first + "@sip.zadarma.com", "sip:sip.zadarma.com",
                "sip:sip.zadarma.com:5060", mCreds.first, mCreds.second);
    }

    @Override
    public void disconnected() {

    }

    @Override
    public void onCallInit() {

    }

    @Override
    public void onRegistrationSuccessful() {

    }

    @Override
    public void onRegistrationFailed() {

    }

    @Override
    public void onIncomingCall(String remoteUri) {

    }

    @Override
    public void onCallState(String remoteUri, int state, int role) {

    }
}
