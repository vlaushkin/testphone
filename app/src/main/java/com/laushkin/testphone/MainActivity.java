package com.laushkin.testphone;

import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.laushkin.testphone.core.PhoneClient;

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_role_e;

public class MainActivity extends AppCompatActivity implements PhoneClient.ConnectionListener, PhoneClient.EventListener {

    private static final String TAG = "MainActivity";

    // возможно это стоит вынести куда-нибудь в ресурсы
    private static final String DOMAIN = "sip.zadarma.com";
    private static final String SIP_PORT = "5060";

    private PhoneClient mPhoneClient;
    private Pair<String, String> mCreds;
    private boolean mRegistration;
    private TextView mTvStatus;
    private EditText mEdNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRegistration = false;

        mCreds = Utils.getCreds(this);

        mPhoneClient = new PhoneClient(this.toString());

        mTvStatus = findViewById(R.id.tvStatus);
        mEdNumber = findViewById(R.id.edNumber);

        findViewById(R.id.button_erase).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mEdNumber.setText("");
                return false;
            }
        });
    }

    public void onClick(View view) {
        int id = view.getId();
        String text = mEdNumber.getText().toString();

        if (id == R.id.button_erase) {
            if (!TextUtils.isEmpty(text)) {
                text = text.substring(0, text.length() - 1);
            }
        } else if (id == R.id.button_one) {
            text += "1";
        } else if (id == R.id.button_two) {
            text += "2";
        } else if (id == R.id.button_three) {
            text += "3";
        } else if (id == R.id.button_four) {
            text += "4";
        } else if (id == R.id.button_five) {
            text += "5";
        } else if (id == R.id.button_six) {
            text += "6";
        } else if (id == R.id.button_seven) {
            text += "7";
        } else if (id == R.id.button_eight) {
            text += "8";
        } else if (id == R.id.button_nine) {
            text += "9";
        } else if (id == R.id.button_zero) {
            text += "0";
        } else if (id == R.id.button_star) {
            text += "*";
        } else if (id == R.id.button_pound) {
            text += "#";
        } else if (id == R.id.button_call) {
            callTo(text);
        }

        mEdNumber.setText(text);
    }

    private void callTo(String text) {
        if (TextUtils.isEmpty(text) || !mRegistration) return;

        startOutgoingCall("sip:" + text + "@" + DOMAIN);
    }

    private void startOutgoingCall(String remoteUri) {
        CallActivity.startCall(this, remoteUri, true);
    }

    private void startIncomingCall(String remoteUri) {
        CallActivity.startCall(this, remoteUri, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        mPhoneClient.setConnectionListener(this);
        mPhoneClient.setEventListener(this);
        mPhoneClient.connect(this);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.RECORD_AUDIO)
                .withListener(new PermissionListener() {
                    @Override public void onPermissionGranted(PermissionGrantedResponse response) {

                    }
                    @Override public void onPermissionDenied(PermissionDeniedResponse response) {
                        finish();
                    }
                    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    @Override
    public void onStop() {
        super.onStop();
        mPhoneClient.setConnectionListener(null);
        mPhoneClient.setEventListener(null);
        mPhoneClient.disconnect(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            Utils.clearCreds(this);
            mPhoneClient.exit();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_exit) {
            mPhoneClient.exit();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void connected() {
        Log.d(TAG, "connected");
        mPhoneClient.register("sip:" + mCreds.first + "@" + DOMAIN, "sip:" + DOMAIN,
                "sip:" + DOMAIN + ":" + SIP_PORT, mCreds.first, mCreds.second);
    }

    @Override
    public void disconnected() {
        Log.d(TAG, "disconnected");
        mPhoneClient.connect(this);
    }

    @Override
    public void onCallInit() {
        Log.d(TAG, "onCallInit");
    }

    @Override
    public void onRegistrationSuccessful() {
        mRegistration = true;
        Log.d(TAG, "onRegistrationSuccessful");
        mTvStatus.setText(mCreds.first + " " + getString(R.string.registration_successful));
    }

    @Override
    public void onRegistrationFailed() {
        mRegistration = false;
        Log.d(TAG, "onRegistrationFailed");
        mTvStatus.setText(mCreds.first + " " + getString(R.string.registration_failed));
    }

    @Override
    public void onIncomingCall(String remoteUri) {
        Log.d(TAG, "onIncomingCall: " + remoteUri);
        startIncomingCall(remoteUri);
    }

    @Override
    public void onCallState(String remoteUri, int state, int role) {
        // do nothing
    }
}
