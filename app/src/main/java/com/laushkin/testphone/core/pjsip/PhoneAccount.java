package com.laushkin.testphone.core.pjsip;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class PhoneAccount extends Account {
    public AccountConfig config;
    private PhoneApp app;

    public PhoneAccount(AccountConfig config, PhoneApp app) {
        this.config = config;
        this.app = app;
    }

    @Override
    public void onRegState(OnRegStateParam prm) {
        app.mEventListener.notifyRegState(prm.getCode(), prm.getReason(),
                prm.getExpiration());
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {
        PhoneCall call = new PhoneCall(this, prm.getCallId(), app);
        app.mEventListener.notifyIncomingCall(call);
    }


}
