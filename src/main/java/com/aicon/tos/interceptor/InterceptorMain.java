package com.aicon.tos.interceptor;

import com.aicon.tos.interceptor.decide.InterceptorDecide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class InterceptorMain {
    private static final Logger LOG = LoggerFactory.getLogger(InterceptorMain.class);

    public static void main(String[] args) {
        CountDownLatch shutdownSignal = new CountDownLatch(1);

        InterceptorDecide interceptorDecide = new InterceptorDecide();
        LOG.info("[Main] InterceptorDecide started.");
        interceptorDecide.start();

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("[Main] Shutdown detected, stopping InterceptorDecide...");
            interceptorDecide.shutdown();
            shutdownSignal.countDown();
            LOG.info("[Main] >>> InterceptorMain shutdown complete.");
        }));

        // Wait until shutdown hook signals
        try {
            shutdownSignal.await();
        } catch (InterruptedException e) {
            LOG.warn("[Main] Main thread interrupted while waiting for shutdown signal.");
            Thread.currentThread().interrupt();
        }

        LOG.info("[Main] Main method exiting.");
    }

    // Optional: use only for diagnostics
    private static void showThreads() {
        Thread.getAllStackTraces().keySet().forEach(t ->
            LOG.info("Thread: {}, daemon={}, alive={}, state={}",
                    t.getName(), t.isDaemon(), t.isAlive(), t.getState())
        );
    }
}
