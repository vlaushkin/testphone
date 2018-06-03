package com.laushkin.testphone.core.pjsip;

import android.util.Log;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.ContainerNode;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.IpChangeParam;
import org.pjsip.pjsua2.JsonDocument;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class PhoneApp {
    private static final String TAG = "PhoneApp";
    private static final int SIP_PORT  = 6000;
    private static final int LOG_LEVEL = 4;

    static {
        System.loadLibrary("pjsua2");
    }

    public Endpoint mEndPoint = new Endpoint();
    public PhoneEventListener mEventListener;
    public ArrayList<PhoneAccount> mAccList = new ArrayList<>();

    private ArrayList<PhoneAccountConfig> mAccCfgs =
            new ArrayList<>();
    private EpConfig mEpConfig = new EpConfig();
    private TransportConfig mSipTpConfig = new TransportConfig();
    private String mAppDir;

    private final String configName = "pjsua2.json";
    private LogWriter mLogWriter;

    public void init(PhoneEventListener obs, String app_dir) {
        init(obs, app_dir, false);
    }

    public void init(PhoneEventListener obs, String app_dir,
                     boolean own_worker_thread) {
        mEventListener = obs;
        mAppDir = app_dir;

        /* Create endpoint */
        try {
            mEndPoint.libCreate();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        /* Load config */
        String configPath = mAppDir + "/" + configName;
        File f = new File(configPath);
        if (f.exists()) {
            loadConfig(configPath);
        } else {
            /* Set 'default' values */
            mSipTpConfig.setPort(SIP_PORT);
        }

        /* Override log level setting */
        mEpConfig.getLogConfig().setLevel(LOG_LEVEL);
        mEpConfig.getLogConfig().setConsoleLevel(LOG_LEVEL);

        /* Set log config. */
        LogConfig log_cfg = mEpConfig.getLogConfig();
        mLogWriter = new LogWriter();
        log_cfg.setWriter(mLogWriter);
        log_cfg.setDecor(log_cfg.getDecor() &
                ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()));

        /* Set ua config. */
        UaConfig ua_cfg = mEpConfig.getUaConfig();
        ua_cfg.setUserAgent("Pjsua2 Android " + mEndPoint.libVersion().getFull());

        /* No worker thread */
        if (own_worker_thread) {
            ua_cfg.setThreadCnt(0);
            ua_cfg.setMainThreadOnly(true);
        }

        /* Init endpoint */
        try {
            mEndPoint.libInit(mEpConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        /* Create transports. */
        try {
            mEndPoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                    mSipTpConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Set SIP port back to default for JSON saved config */
        mSipTpConfig.setPort(SIP_PORT);

        /* Create accounts. */
        for (int i = 0; i < mAccCfgs.size(); i++) {
            PhoneAccountConfig my_cfg = mAccCfgs.get(i);

            /* Customize mAccount config */
            my_cfg.accCfg.getNatConfig().setIceEnabled(true);
            my_cfg.accCfg.getVideoConfig().setAutoTransmitOutgoing(false);
            my_cfg.accCfg.getVideoConfig().setAutoShowIncoming(false);

            PhoneAccount acc = addAcc(my_cfg.accCfg);
            if (acc == null)
                continue;
        }

        /* Start. */
        try {
            mEndPoint.libStart();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public PhoneAccount addAcc(AccountConfig cfg) {
        PhoneAccount acc = new PhoneAccount(cfg, this);
        try {
            acc.create(cfg);
        } catch (Exception e) {
            e.printStackTrace();
            acc = null;
            return null;
        }

        mAccList.add(acc);
        return acc;
    }

    public void delAcc(PhoneAccount acc)
    {
        mAccList.remove(acc);
    }

    private void loadConfig(String filename) {
        JsonDocument json = new JsonDocument();

        try {
            /* Load file */
            json.loadFile(filename);
            ContainerNode root = json.getRootContainer();

            /* Read endpoint config */
            mEpConfig.readObject(root);

            /* Read transport config */
            ContainerNode tp_node = root.readContainer("SipTransport");
            mSipTpConfig.readObject(tp_node);

            /* Read mAccount configs */
            mAccCfgs.clear();
            ContainerNode accs_node = root.readArray("accounts");
            while (accs_node.hasUnread()) {
                PhoneAccountConfig acc_cfg = new PhoneAccountConfig();
                acc_cfg.readObject(accs_node);
                mAccCfgs.add(acc_cfg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Force delete json now, as I found that Java somehow destroys it
         * after lib has been destroyed and from non-registered thread.
         */
        json.delete();
    }

    private void buildAccConfigs() {
        /* Sync mAccCfgs from mAccList */
        mAccCfgs.clear();
        for (int i = 0; i < mAccList.size(); i++) {
            PhoneAccount acc = mAccList.get(i);
            PhoneAccountConfig my_acc_cfg = new PhoneAccountConfig();
            my_acc_cfg.accCfg = acc.config;


            mAccCfgs.add(my_acc_cfg);
        }
    }

    private void saveConfig(String filename) {
        JsonDocument json = new JsonDocument();

        try {
            /* Write endpoint config */
            json.writeObject(mEpConfig);

            /* Write transport config */
            ContainerNode tp_node = json.writeNewContainer("SipTransport");
            mSipTpConfig.writeObject(tp_node);

            /* Write mAccount configs */
            buildAccConfigs();
            ContainerNode accs_node = json.writeNewArray("accounts");
            for (int i = 0; i < mAccCfgs.size(); i++) {
                mAccCfgs.get(i).writeObject(accs_node);
            }

            /* Save file */
            json.saveFile(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Force delete json now, as I found that Java somehow destroys it
         * after lib has been destroyed and from non-registered thread.
         */
        json.delete();
    }

    public void handleNetworkChange() {
        try{
            Log.d(TAG,"Network change detected");
            IpChangeParam changeParam = new IpChangeParam();
            mEndPoint.handleIpChange(changeParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deinit() {
        Log.d("dbg", "deinit");
        String configPath = mAppDir + "/" + configName;
        saveConfig(configPath);

        /* Try force GC to avoid late destroy of PJ objects as they should be
         * deleted before lib is destroyed.
         */
        Runtime.getRuntime().gc();

        /* Shutdown pjsua. Note that Endpoint destructor will also invoke
         * libDestroy(), so this will be a test of double libDestroy().
         */
        try {
            mEndPoint.libDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Force delete Endpoint here, to avoid deletion from a non-
         * registered thread (by GC?).
         */
        mEndPoint.delete();
        mEndPoint = null;
    }

    public void exit() {
        mAccCfgs.clear();
        mAccList.clear();
        deinit();

    }

    class LogWriter extends org.pjsip.pjsua2.LogWriter {
        @Override
        public void write(LogEntry entry) {
            Log.d(TAG, entry.getMsg());
        }
    }
}
