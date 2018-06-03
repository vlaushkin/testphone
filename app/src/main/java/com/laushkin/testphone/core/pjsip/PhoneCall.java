package com.laushkin.testphone.core.pjsip;

import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallMediaInfoVector;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsua2;
import org.pjsip.pjsua2.pjsua_call_media_status;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class PhoneCall extends Call {
    private final PhoneApp app;

    public PhoneCall(PhoneAccount acc, int callId, PhoneApp app) {
        super(acc, callId);
        this.app = app;
    }

    @Override
    public void onCallState(OnCallStateParam prm) {
        try {
            CallInfo ci = getInfo();
            if (ci.getState() ==
                    pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                app.mEndPoint.utilLogWrite(3, "MyCall", this.dump(true, ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Should not delete this call instance (self) in this context,
        // so the observer should manage this call instance deletion
        // out of this callback context.
        app.mEventListener.notifyCallState(this);
    }


    @Override
    public void onCallMediaState(OnCallMediaStateParam prm) {
        CallInfo ci;
        try {
            ci = getInfo();
        } catch (Exception e) {
            return;
        }

        CallMediaInfoVector cmiv = ci.getMedia();

        for (int i = 0; i < cmiv.size(); i++) {
            CallMediaInfo cmi = cmiv.get(i);
            if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                    (cmi.getStatus() ==
                            pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE ||
                            cmi.getStatus() ==
                                    pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD))
            {
                // unfortunately, on Java too, the returned Media cannot be
                // downcasted to AudioMedia
                Media m = getMedia(i);
                AudioMedia am = AudioMedia.typecastFromMedia(m);

                // connect ports
                try {
                    app.mEndPoint.audDevManager().getCaptureDevMedia().
                            startTransmit(am);
                    am.startTransmit(app.mEndPoint.audDevManager().
                            getPlaybackDevMedia());
                } catch (Exception e) {
                    continue;
                }
            } else if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_VIDEO &&
                    cmi.getStatus() ==
                            pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE &&
                    cmi.getVideoIncomingWindowId() != pjsua2.INVALID_ID)
            {
                // video not supported today
            }
        }

        app.mEventListener.notifyCallMediaState(this);
    }
}
