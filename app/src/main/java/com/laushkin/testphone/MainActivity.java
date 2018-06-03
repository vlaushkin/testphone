package com.laushkin.testphone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.laushkin.testphone.core.PhoneClient;

public class MainActivity extends AppCompatActivity implements PhoneClient.ConnectionListener, PhoneClient.EventListener {

    private static final String TAG = "MainActivity";
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
    }

    public void onClick(View view) {
        int id = view.getId();
        Log.d(TAG, "onClick!: " + id);
        String text = mEdNumber.getText().toString();

        if (id == R.id.btErase) {
            Log.d(TAG, "bterase: " + text);
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

    }

    @Override
    public void onStart() {
        super.onStart();
        mPhoneClient.setConnectionListener(this);
        mPhoneClient.setEventListener(this);
        mPhoneClient.connect(this);
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
        mPhoneClient.register("sip:" + mCreds.first + "@sip.zadarma.com", "sip:sip.zadarma.com",
                "sip:sip.zadarma.com:5060", mCreds.first, mCreds.second);
    }

    @Override
    public void disconnected() {
        Log.d(TAG, "disconnected");
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
    }

    @Override
    public void onCallState(String remoteUri, int state, int role) {
        Log.d(TAG, "remoteUri: " + remoteUri + ", state: " + state + ", role: " + role);
    }
}
