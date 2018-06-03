package com.laushkin.testphone;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.laushkin.testphone.core.PhoneClient;

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_role_e;

public class CallActivity extends AppCompatActivity implements PhoneClient.ConnectionListener, PhoneClient.EventListener {

    private static final String EXTRA_REMOTE_URI = "extra_remote_uri";
    private static final String EXTRA_IS_OUTGOING = "extra_outgoing";
    private static final String TAG = "CallActivity";

    private View mIncomingLayout;
    private View mCallControls;
    private PhoneClient mPhoneClient;
    private TextView mTvStatus;
    private String mRemoteUri;
    private boolean mIsOutgoing;

    public static void startCall(Context context, String remoteUri, boolean outgoing) {
        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra(EXTRA_REMOTE_URI, remoteUri);
        intent.putExtra(EXTRA_IS_OUTGOING, outgoing);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        if (getIntent() == null ||
                !getIntent().hasExtra(EXTRA_REMOTE_URI) ||
                !getIntent().hasExtra(EXTRA_IS_OUTGOING)) {
            finish();
        }

        mRemoteUri = getIntent().getStringExtra(EXTRA_REMOTE_URI);
        mIsOutgoing = getIntent().getBooleanExtra(EXTRA_IS_OUTGOING, true);

        mPhoneClient = new PhoneClient(this.toString());

        TextView tvRemoteUri = findViewById(R.id.tvRemoteUri);
        mTvStatus = findViewById(R.id.tvStatus);

        mIncomingLayout = findViewById(R.id.incoming_layout);
        mCallControls = findViewById(R.id.call_controls_layout);

        tvRemoteUri.setText(getNumberFromUri(mRemoteUri));

        if (mIsOutgoing) {
            Utils.gone(mIncomingLayout);
            Utils.visible(mCallControls);

            setStateConnecting();
        } else {
            Utils.visible(mIncomingLayout);
            Utils.gone(mCallControls);

            setStateIncoming();
        }

    }

    private String getNumberFromUri(String uri) {
        return uri
                .replace("sip:", "")
                .replace("@sip.zadarma.com", "")
                .replace("\"SIP\" ", "")
                .replace("<", "")
                .replace(">", "");
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

    public void onClick(View view) {
        if (view.getId() == R.id.accept) {
            mPhoneClient.acceptCall();

            Utils.gone(mIncomingLayout);
            Utils.visible(mCallControls);
        } else if (view.getId() == R.id.decline || view.getId() == R.id.hangup) {
            mPhoneClient.declineCall();

            finish();
        }
    }

    @Override
    public void connected() {
        if (mIsOutgoing) {
            mPhoneClient.makeCall(mRemoteUri);
        }
    }

    @Override
    public void disconnected() {
        // do nothing
    }

    @Override
    public void onCallInit() {
        // do nothing
    }

    @Override
    public void onRegistrationSuccessful() {
        // do nothing
    }

    @Override
    public void onRegistrationFailed() {
        // do nothing
    }

    @Override
    public void onIncomingCall(String remoteUri) {
        // do nothing
    }

    @Override
    public void onCallState(String remoteUri, int state, int role) {
        // хорошо бы сделать собственные обёртки над стейтами.
        pjsip_inv_state pjState = pjsip_inv_state.swigToEnum(state);
        pjsip_role_e pjRole = pjsip_role_e.swigToEnum(role);
        Log.d(TAG, "remoteUri: " + remoteUri + ", state: " + pjState.toString() + ", role: " + pjRole.toString());

        if (state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED.swigValue()) {
            finish();
        } else if (state == pjsip_inv_state.PJSIP_INV_STATE_CALLING.swigValue() ||
                state == pjsip_inv_state.PJSIP_INV_STATE_EARLY.swigValue() ||
                state == pjsip_inv_state.PJSIP_INV_STATE_CONNECTING.swigValue()) {
            setStateConnecting();
        } else if (state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED.swigValue()) {
            setStateConnected();
        }


    }

    private void setStateConnected() {
        Utils.visible(mTvStatus);
        mTvStatus.setText(getString(R.string.connected));
    }

    private void setStateConnecting() {
        Utils.visible(mTvStatus);
        mTvStatus.setText(getString(R.string.connecting));
    }

    private void setStateIncoming() {
        Utils.visible(mTvStatus);
        mTvStatus.setText(getString(R.string.incoming));
    }
}
