package com.aicon.tos.connect.web;

import ch.qos.logback.classic.LoggerContext;
import com.aicon.tos.connect.web.pages.AboutView;
import com.aicon.tos.connect.web.pages.logview.VaadinLogAppender;
import com.aicon.tos.interceptor.decide.InterceptorDecide;
import com.aicon.tos.shared.kafka.LogbackDebugger;
import com.sun.mail.iap.ConnectionException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(InitServlet.class);

    static private final String CTX_ATT_TEMPDIR = "javax.servlet.context.tempdir";
    private AppComposer composer;

    @Override
    public void init() throws ServletException {
        // Place to retrieve and store data at web-application level
        final ServletContext ctx = getServletContext();

        super.init();
        composer = AppComposer.getInstance();
        ctx.setAttribute("webappName", composer.getName());
        String atcBase = System.getProperty("atc.base");

        LOG.info("\n------------------------------------------------------------");
        LOG.info(composer.getVersion());
        LOG.info("------------------------------------------------------------");
        LOG.info("Webapp deployed name    = {}", composer.getName());
        LOG.info("Webapp version          = {}", AboutView.VERSION_NUMBER);
        LOG.info("ATC base                = {}", atcBase);
        LOG.info("TERMINAL                = {}", AppComposer.getAppSetting(AppComposer.APP_VAR_TERMINAL));
        LOG.info("ENVIROMENT              = {}", AppComposer.getAppSetting(AppComposer.APP_VAR_ENVIRONMENT));
        LOG.info("Servlet context name    = {}", ctx.getServletContextName());
        LOG.info("Servlet server info     = {}", ctx.getServerInfo());
        LOG.info("Servlet context tempdir = {}", ctx.getAttribute(CTX_ATT_TEMPDIR));
        LOG.info("------------------------------------------------------------");
        LOG.info("------------------------------------------------------------\n");

        // Initialize Composer
        composer.init();
        try {
            composer.start();
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }

        // Register a proper shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown hook triggered. Cleaning up resources.");
            cleanupResources();
        }));
    }

    private void resetLoggingContext() {
        // Reset logger context to clear any stale configurations
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        // Debugging appenders for verification
        LogbackDebugger.debugLogAppenders();

        // Ensure VaadinLogAppender is attached only to com.aicon
        Logger appLogger = loggerContext.getLogger("com.aicon");
        VaadinLogAppender vaadinLogAppender = VaadinLogAppender.createLogAppender();


    }

    @Override
    public void destroy() {
        super.destroy();

        LOG.info("InitServlet is destroyed, stopping Composer and Interceptor.");

        // Cleanup Composer and Interceptor
        cleanupResources();
    }

    // Helper to properly stop both resources
    private void cleanupResources() {
        // Stop Composer
        if (composer != null) {
            composer.stop();
        }
    }
}
