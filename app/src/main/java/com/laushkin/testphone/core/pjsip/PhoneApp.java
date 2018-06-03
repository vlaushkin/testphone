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

    static {
        System.loadLibrary("pjsua2");
    }

    public Endpoint ep = new Endpoint();
    public PhoneEventListener eventListener;
    public ArrayList<PhoneAccount> accList = new ArrayList<>();

    private ArrayList<PhoneAccountConfig> accCfgs =
            new ArrayList<>();
    private EpConfig epConfig = new EpConfig();
    private TransportConfig sipTpConfig = new TransportConfig();
    private String appDir;

    /* Maintain reference to log writer to avoid premature cleanup by GC */
    private LogWriter logWriter;

    private final String configName = "pjsua2.json";
    private final int SIP_PORT  = 6000;
    private final int LOG_LEVEL = 4;

    public void init(PhoneEventListener obs, String app_dir)
    {
        init(obs, app_dir, false);
    }

    public void init(PhoneEventListener obs, String app_dir,
                     boolean own_worker_thread)
    {
        eventListener = obs;
        appDir = app_dir;

        /* Create endpoint */
        try {
            ep.libCreate();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        /* Load config */
        String configPath = appDir + "/" + configName;
        File f = new File(configPath);
        if (f.exists()) {
            loadConfig(configPath);
        } else {
            /* Set 'default' values */
            sipTpConfig.setPort(SIP_PORT);
        }

        /* Override log level setting */
        epConfig.getLogConfig().setLevel(LOG_LEVEL);
        epConfig.getLogConfig().setConsoleLevel(LOG_LEVEL);

        /* Set log config. */
        LogConfig log_cfg = epConfig.getLogConfig();
        logWriter = new LogWriter();
        log_cfg.setWriter(logWriter);
        log_cfg.setDecor(log_cfg.getDecor() &
                ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue()));

        /* Write log to file (just uncomment whenever needed) */
        //String log_path = android.os.Environment.getExternalStorageDirectory().toString();
        //log_cfg.setFilename(log_path + "/pjsip.log");

        /* Set ua config. */
        UaConfig ua_cfg = epConfig.getUaConfig();
        ua_cfg.setUserAgent("Pjsua2 Android " + ep.libVersion().getFull());

        /* STUN server. */
        //StringVector stun_servers = new StringVector();
        //stun_servers.add("stun.pjsip.org");
        //ua_cfg.setStunServer(stun_servers);

        /* No worker thread */
        if (own_worker_thread) {
            ua_cfg.setThreadCnt(0);
            ua_cfg.setMainThreadOnly(true);
        }

        /* Init endpoint */
        try {
            ep.libInit(epConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        /* Create transports. */
        try {
            ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                    sipTpConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Set SIP port back to default for JSON saved config */
        sipTpConfig.setPort(SIP_PORT);

        /* Create accounts. */
        for (int i = 0; i < accCfgs.size(); i++) {
            PhoneAccountConfig my_cfg = accCfgs.get(i);

            /* Customize account config */
            my_cfg.accCfg.getNatConfig().setIceEnabled(true);
            my_cfg.accCfg.getVideoConfig().setAutoTransmitOutgoing(false);
            my_cfg.accCfg.getVideoConfig().setAutoShowIncoming(false);

            PhoneAccount acc = addAcc(my_cfg.accCfg);
            if (acc == null)
                continue;
        }

        /* Start. */
        try {
            ep.libStart();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public PhoneAccount addAcc(AccountConfig cfg)
    {
        PhoneAccount acc = new PhoneAccount(cfg, this);
        try {
            acc.create(cfg);
        } catch (Exception e) {
            e.printStackTrace();
            acc = null;
            return null;
        }

        accList.add(acc);
        return acc;
    }

    public void delAcc(PhoneAccount acc)
    {
        accList.remove(acc);
    }

    private void loadConfig(String filename)
    {
        JsonDocument json = new JsonDocument();

        try {
            /* Load file */
            json.loadFile(filename);
            ContainerNode root = json.getRootContainer();

            /* Read endpoint config */
            epConfig.readObject(root);

            /* Read transport config */
            ContainerNode tp_node = root.readContainer("SipTransport");
            sipTpConfig.readObject(tp_node);

            /* Read account configs */
            accCfgs.clear();
            ContainerNode accs_node = root.readArray("accounts");
            while (accs_node.hasUnread()) {
                PhoneAccountConfig acc_cfg = new PhoneAccountConfig();
                acc_cfg.readObject(accs_node);
                accCfgs.add(acc_cfg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Force delete json now, as I found that Java somehow destroys it
         * after lib has been destroyed and from non-registered thread.
         */
        json.delete();
    }

    private void buildAccConfigs()
    {
        /* Sync accCfgs from accList */
        accCfgs.clear();
        for (int i = 0; i < accList.size(); i++) {
            PhoneAccount acc = accList.get(i);
            PhoneAccountConfig my_acc_cfg = new PhoneAccountConfig();
            my_acc_cfg.accCfg = acc.config;


            accCfgs.add(my_acc_cfg);
        }
    }

    private void saveConfig(String filename)
    {
        JsonDocument json = new JsonDocument();

        try {
            /* Write endpoint config */
            json.writeObject(epConfig);

            /* Write transport config */
            ContainerNode tp_node = json.writeNewContainer("SipTransport");
            sipTpConfig.writeObject(tp_node);

            /* Write account configs */
            buildAccConfigs();
            ContainerNode accs_node = json.writeNewArray("accounts");
            for (int i = 0; i < accCfgs.size(); i++) {
                accCfgs.get(i).writeObject(accs_node);
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

    public void handleNetworkChange()
    {
        try{
            Log.d(TAG,"Network change detected");
            IpChangeParam changeParam = new IpChangeParam();
            ep.handleIpChange(changeParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deinit()
    {
        Log.d("dbg", "deinit");
        String configPath = appDir + "/" + configName;
        saveConfig(configPath);

        /* Try force GC to avoid late destroy of PJ objects as they should be
         * deleted before lib is destroyed.
         */
        Runtime.getRuntime().gc();

        /* Shutdown pjsua. Note that Endpoint destructor will also invoke
         * libDestroy(), so this will be a test of double libDestroy().
         */
        try {
            ep.libDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Force delete Endpoint here, to avoid deletion from a non-
         * registered thread (by GC?).
         */
        ep.delete();
        ep = null;
    }

    public void exit() {
        accCfgs.clear();
        accList.clear();
        deinit();

    }

    class LogWriter extends org.pjsip.pjsua2.LogWriter {
        @Override
        public void write(LogEntry entry) {
            Log.d(TAG, entry.getMsg());
        }
    }
}
