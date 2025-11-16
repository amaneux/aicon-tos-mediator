package com.aicon.tos.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.abs;

/**
 * A utility class to determine the time synchronization between AICON and TOS.
 */
public final class TimeSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSyncService.class);

    // Private constructor to prevent instantiation of this utility class
    private TimeSyncService() {
    }

    /**
     * Determines the synchronization time between AICON and TOS based on the provided timestamps.
     *
     * @param reqSendAicon     The timestamp when the request was sent from AICON.
     * @param reqReceivedTos   The timestamp when the request was received by TOS.
     * @param resSendTos       The timestamp when the response was sent from TOS.
     * @param resReceivedAicon The timestamp when the response was received by AICON.
     * @return The calculated synchronization time in milliseconds.
     * @throws IllegalArgumentException If any of the arguments are null.
     */
    public static long determineSyncTime(Long reqSendAicon,
                                         Long reqReceivedTos,
                                         Long resSendTos,
                                         Long resReceivedAicon) {

        if (reqSendAicon == null || reqReceivedTos == null || resSendTos == null || resReceivedAicon == null) {
            throw new IllegalArgumentException("All arguments must be non-null.");
        }

        // @formatter:off
        // NUMBER_OF_DIFFS = 2
        // tDiff1 = reqReceivedTos - reqSendAicon    : transmission time + syncTime
        // tDiff2 = resReceivedAicon - resSendTos    : transmission time + syncTime
        // =>
        // tDiff1 = transmissionTime + syncTime
        // tDiff2 = transmissionTime - syncTime
        // =>
        // tDiff1 = tDiff2 + 2 x syncTime
        // =>
        // syncTime = (tDiff1 - tDiff2) / NUMBER_OF_DIFFS
        // transmissionTime = (tDiff1 + tDiff2)/2
        //
        // @formatter:on

        long tDiff1 = reqReceivedTos - reqSendAicon;
        long tDiff2 = resReceivedAicon - resSendTos;

        long transmissionTime = (tDiff1 + tDiff2) / 2;
        long syncTime = tDiff1 - transmissionTime;

        LOG.debug("Transmission time = {}, AICON is {} msec {} of TOS", transmissionTime, abs(syncTime),
                (syncTime > 0 ? "behind" : "ahead"));

        return syncTime;
    }
}
