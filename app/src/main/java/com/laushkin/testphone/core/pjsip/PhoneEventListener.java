package com.laushkin.testphone.core.pjsip;

import org.pjsip.pjsua2.pjsip_status_code;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 02/06/2018.
 */
public interface PhoneEventListener {
    void notifyRegState(pjsip_status_code code, String reason,
                        int expiration);

    void notifyIncomingCall(PhoneCall call);

    void notifyCallState(PhoneCall call);

    void notifyCallMediaState(PhoneCall call);

    void notifyBuddyState(PhoneCall buddy);

    void notifyChangeNetwork();
}
