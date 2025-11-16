package com.aicon.tos.connect.flows;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowMain {

    private static final Logger LOG = LoggerFactory.getLogger(FlowMain.class.getName());

    public static void main(String[] args) {

        // Instantieer de FlowManager
        FlowManager flowManager = FlowManager.getInstance();

        // Initialiseer de FlowManager om de FlowControllers te laden
        flowManager.init();

        // Start de FlowControllers
        flowManager.start();

        // Simpel logbericht om aan te geven dat de flows gestart zijn
        LOG.info("FlowManager started:\n" + flowManager);

        try {
            while (true) {

            }
        }
            catch (Exception e) {
            e.printStackTrace();
            }
        finally {
            // Stop de FlowControllers
            flowManager.stop();
            LOG.info("FlowManager stopped.");
        }
    }
}
