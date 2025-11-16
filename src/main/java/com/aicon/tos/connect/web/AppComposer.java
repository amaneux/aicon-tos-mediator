package com.aicon.tos.connect.web;

import com.aicon.tos.connect.flows.FlowManager;
import com.aicon.tos.interceptor.decide.InterceptorDecide;
import com.sun.mail.iap.ConnectionException;
import org.apache.kafka.common.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AppComposer {
    static private Logger LOG = LoggerFactory.getLogger(AppComposer.class.getName());
    static private AppComposer _instance = null;

    public static final String APP_VAR_TERMINAL = "AICON_TERMINAL";
    public static final String APP_VAR_ENVIRONMENT = "AICON_ENVIRONMENT";

    public static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS");
    public static final String NAME = "TOS-Mediator";

    private FlowManager flowMgr;
    private InterceptorDecide interceptorDecide;    // Interceptor reference

    /**
     * Various states a composer can have.
     */
    public enum State {
        IDLE, INITIALISED, RUNNING, STOPPED, ERROR, TEST;
    }

    private State state = State.IDLE;
    private String stateMsg = null;

    public static AppComposer getInstance() {
        synchronized (LOG) {
            if (_instance == null) {
                _instance = new AppComposer();
            }
            return _instance;
        }
    }

    private AppComposer() {}

    /**
     * Reads settings from the command line first (-D) and if not set, tries to read the same as an env. var.
     *
     * @param key the key to look for
     * @return a value or null
     */
    public static String getAppSetting(String key) {
        AppVersion.getVersionInfo();        // forces the read of the version properties first, having info about the build host

        return System.getProperty(key, System.getenv(key));
    }


    public void init() throws ConfigException {
        flowMgr = FlowManager.getInstance();
        if (flowMgr.getConfigSettings().hasStorageError()) {
            LOG.error("Startup failed, serious error: {}", flowMgr.getConfigSettings().getStorageError());
            setState(State.ERROR, "Storage error, cannot continue");
        } else {
            flowMgr.init();
            interceptorDecide = new InterceptorDecide();

            setState(State.INITIALISED, NAME + " initialised!");
        }
    }

    public void start() throws ConnectionException {

        flowMgr.start();
        interceptorDecide.start();

        setState(State.RUNNING, NAME + " started using configuration version 1.0");
    }


    public void stop() {
        setState(state, NAME + " application requested to stop...");
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        LOG.info(group.activeCount() + " threads running before stopping:");
        processThreads(group, "    Active thread: ", false);

        flowMgr.stop();
        interceptorDecide.shutdown();

        if (state != State.ERROR) {
            setState(State.STOPPED, NAME + " stopped!");
        }
        // And finally stop all threads of the webapp when they weren't already.
        LOG.info("Interrupting still running threads for this webapp...");
        processThreads(group, "    Interrupting thread: ", true);
        LOG.info("STOPPED\n\n  ===========>  " + NAME + " application STOPPED!  <===========\n");
        LOG.info(NAME + " application STOPPED");
    }


    private void processThreads(ThreadGroup group, String comment, boolean doInterrupt) {
        Thread[] list = new Thread[2 * group.activeCount()];
        group.enumerate(list, true);
        for (int i = 0; i < list.length; i++) {
            if (list[i] == null) {
                break;
            }
            // Every webapp has its on classloader, so we check if the Thread belongs to it.
            if (list[i].getContextClassLoader().getClass().getName().indexOf("Webapp") >= 0) {
                LOG.debug(comment + list[i]);
                if (doInterrupt) {
                    list[i].interrupt();
                }
            }
        }
    }


    public void restart() throws ConnectionException {
        stop();
        start();
    }

    public String getName() {
        return NAME;
    }


    public State getState() {
        return state;
    }


    public String getStateAsText() {
        return state.toString();
    }


    public String getStateMsg() {
        return stateMsg;
    }


    public void setState(State state, String stateMsg) {
        if (state == State.STOPPED || state == State.ERROR) {
            LOG.warn("State: {}  --> {} ({})", this.state.name(), state.name(), stateMsg);
        } else {
            LOG.info("State: {}  --> {} ({})", this.state.name(), state.name(), stateMsg);
        }

        this.state = state;
        if (stateMsg == null) {
            this.stateMsg = null;
        } else {
            this.stateMsg = TS_FORMAT.format(new Date()) + " - " + stateMsg;
        }
    }


    public InterceptorDecide getInterceptorDecide() {
        return interceptorDecide;
    }

    public FlowManager getFlowManager() {
        return flowMgr;
    }

    /**
     * Gets information written in the manifest file of a JAR.
     *
     * @return the implementation attributes in a single string
     */
    public String getVersion() {
        final Package pkg = getClass().getPackage();
        final String vendor = pkg.getImplementationVendor();
        final String name = pkg.getImplementationTitle();
        final String version = pkg.getImplementationVersion();

        return "(c) Copyright " + vendor + ". All rights reserved." + "\nImplementation title    = " + name
                + "\n               version  = " + version + "\nSpecification  title    = " +
                pkg.getSpecificationTitle()
                + "\n               version  = " + pkg.getSpecificationVersion();
    }


}
