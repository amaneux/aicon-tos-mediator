package com.aicon.tos.connect.cdc;

import com.aicon.tos.connect.web.callback.ProgressCallback;
import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.kafka.DynamicTestKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resends CDC-topic messages to a topic based on the CSV input with the following format:
 * Send, #topic-name, element, offset, topic-ts, ... all message columns ...
 *     , tos.apex.dbo.inv_wi,before,87820,2025-10-08 14:21:51.756, ....
 *     , tos.apex.dbo.inv_wi,after,87820,2025-10-08 14:21:51.756, ....
 * The Send column needs to be skipped, is used for the feedback to the user.
 * The row for before and after can be reversed or missing (a single before or after record), all these situations gets
 * detected and reacted upon.
 * Execution is done in its own thread and can be controlled via stop, pause and resume methods.
 */
public class CDCReplayer implements Runnable {

    private static final String NO_DATA_MARKER = "no data";

    public enum IntervalType {
        RealTime,
        Fixed,
        Stepping;
    }

    public static final String ELM_BEFORE = "before";
    public static final String ELM_AFTER = "after";
    public static final String REPORT_IDX = "REPORT_IDX";
    public static final int FIXED_ITV_MS = 1000;
    public static final int MAX_RT_MS = 10 * FIXED_ITV_MS;

    private static final Logger LOG = LoggerFactory.getLogger(CDCReplayer.class);
    private static final String COMMENT_PREFIX = "#";
    private static final String THREAD_PFX = "CDC-replay-";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final DynamicTestKafkaProducer dynamicTestKafkaProducer;
    private Thread thread;
    private final Object lock = new Object();
    private boolean running = true;
    private boolean paused = false;

    private int fixedInterval = FIXED_ITV_MS;
    private int maxRtInterval = MAX_RT_MS;

    private List<String[]> csv;
    private int messageStartsAt;
    private int rowIdx = -1;
    private IntervalType intervalType;
    private String topicSuffix;
    private ProgressCallback callback;
    private Map<String, Object> reportMap;

    public CDCReplayer(ProgressCallback callback) {
        this.callback = callback;
        dynamicTestKafkaProducer = new DynamicTestKafkaProducer();
    }

    /**
     * Start processing the csv data record for record in the interval time given.
     * @param csv the csv dataset with the first <>messageStartsAt</> columns not to be send out as message.
     * @param messageStartsAt the actual message columns to send starts here in the csv dataset (so 4 means, skip the 1st 4 columns)
     * @param interval defines the interval between messages, realTime keeps the original speed, OnePerSecond sends 1 message per second
     * @param topicSuffix a suffix to be added to the topicName (null will ignore this).
     */
    public void start(List<String[]> csv, int messageStartsAt, IntervalType interval, String topicSuffix) {
        if (csv == null) {
            return;
        }
        if (thread != null) {
            stop();
        }
        rowIdx = -1;
        this.csv = csv;
        this.messageStartsAt = messageStartsAt;
        this.intervalType = interval;
        this.topicSuffix = topicSuffix;
        reportMap = new HashMap<>();

        thread = new Thread(this);
        thread.setName(THREAD_PFX + thread.getName());
        thread.start();
    }

    /**
     * Stops the execution thread.
     */
    public void stop() {
        LOG.info("Stopping {}...", thread.getName());
        resume();
        thread.interrupt();
        running = false;
    }

    /**
     * Pauses the execution thread until it gets resumed or stopped.
     */
    public void pause() {
        LOG.info("Pausing {}...", thread.getName());
        paused = true;
    }

    /**
     * Resumes a paused execution thread.
     */
    public void resume() {
        synchronized (lock) {
            if (paused) {
                if (intervalType != IntervalType.Stepping) {
                    LOG.info("Resuming {}...", thread.getName());
                    paused = false;
                }

                lock.notifyAll(); // Resume the thread

                if (intervalType != IntervalType.Stepping) {
                    if (callback != null) {callback.reportProgress(ResultLevel.OK, "Resumed", reportMap);}
                    LOG.info("Resumed {}", thread.getName());
                }
            }
        }
    }

    /**
     * @return true when paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * @return true when running (and not paused)
     */
    public boolean isRunning() {
        return !paused && running;
    }

    /**
     * Allows change of the intervalType even during thread execution.
     * @param intervalType the interval type
     */
    public void setIntervalType(IntervalType intervalType) {
        this.intervalType = intervalType;
    }

    @Override
    public void run() {
        LOG.info("Thread {} is running...", thread.getName());

        long prevMillis = 0;
        long sleepMillis = 0;
        paused = false;

        do {    // running
            int colIdx = messageStartsAt - 4;       // looking for <result>, topic-name, element, offset, offset-ts
            synchronized (lock) {
                if (paused) {                       // only pause after the first iteration.
                    try {
                        if (!isStepping()) {
                            if (callback != null) {callback.reportProgress(ResultLevel.OK, "Paused", reportMap);}
                            LOG.info("Thread {} is paused!", thread.getName());
                        }
                        lock.wait(); // Pause the thread
                    } catch (InterruptedException e) {
                        LOG.info("Pause interrupted.");
                        running = false;
                    }
                }
            }
            paused = paused || isStepping();
            if (running) {
                try {
                    CDCRecord rec1, rec2 = null;
                    // lookup for 2 matching CDC elements
                    do {
                        rec1 = new CDCRecord(csv.get(nextRowIndex()));
                        if (rec1.comment) {
                            continue;       // skip any header/comment lines
                        }
                        if (rowIdx < csv.size() - 1) {      // peek at the next record if it belongs to this one
                            rec2 = new CDCRecord(csv.get(rowIdx + 1));
                            if (!rec2.comment && rec1.topicName.equals(rec2.topicName) && rec1.topicOffset == rec2.topicOffset) {
                                nextRowIndex();     // set rowIdx to the 2nd row for the same topic&offset
                            } else {
                                rec2 = null;        // so only 1 row found for this topic&offset
                            }
                        }
                    } while(false);     // always drop out

                    if (intervalType == IntervalType.RealTime) {
                        if (prevMillis == 0) {
                            sleepMillis = getFixedInterval();   // first messages in RealTime can be too quick, so wait 1 sec before actual sending first
                            prevMillis = rec1.dtMillis;
                        } else {
                            sleepMillis = rec1.dtMillis - prevMillis;
                            prevMillis = rec1.dtMillis;
                        }
                    } else if (intervalType == IntervalType.Fixed) {
                        sleepMillis = getFixedInterval();
                    }

                    if (sleepMillis > 0) {
                        try {
                            long actualSleepMs = Math.min(getMaxRtInterval(), sleepMillis);
                            LOG.info("Sleeping for {} ms", actualSleepMs);
                            Thread.sleep(actualSleepMs);
                        } catch (InterruptedException e) {
                            LOG.info("Interval sleep interrupted.");
                        }
                    }

                    CDCRecord cdcBefore = null;
                    CDCRecord cdcAfter = null;
                    if (rec1.matchElementName(ELM_BEFORE)) {
                        cdcBefore = rec1;
                        cdcAfter = rec2;
                    } else if (rec1.matchElementName(ELM_AFTER)) {
                        cdcAfter = rec1;
                        cdcBefore = rec2;
                    }

                    String[] before = cdcBefore == null || cdcBefore.nodata ? null: cdcBefore.getMessageRecord();
                    String[] after  = cdcAfter == null  || cdcAfter.nodata  ? null: cdcAfter.getMessageRecord();
                    LOG.info("Sending message with offset {} to topic {}{}...", rec1.topicOffset, rec1.topicName, topicSuffix);
                    ResultEntry result = send(rec1.topicName, rec1.topicName + topicSuffix, before, after);

                    String resultText = "Sent message with offset " + rec1.topicOffset;
                    if (result.getMessage() != null) {
                        resultText = result.getMessage();
                    }
                    if (callback != null) {callback.reportProgress(result.getLevel(), resultText, reportMap);}
                } catch (IndexOutOfBoundsException e) {
                    running = false;
                } catch (Exception e) {
                    if (callback != null) {callback.reportProgress(ResultLevel.ERROR, e.getMessage(), reportMap);}
                    LOG.error("Processing failed, reason: {}", e.getMessage());
                }
            }
            running = running && (rowIdx < csv.size() - 1);
        } while (running);

        if (callback != null) {callback.onDone(ResultLevel.OK, "All processed!");}
        paused = false;

        LOG.info("Thread {} has stopped!", thread.getName());
    }

    private int nextRowIndex() {
        if (rowIdx < csv.size() - 1) {
            reportMap.put(REPORT_IDX, ++rowIdx);
            return rowIdx;
        } else {
            throw new IndexOutOfBoundsException("Reached last record");
        }
    }

    private boolean isStepping() {
        return intervalType == IntervalType.Stepping;
    }

    /**
     * Creates and sends a message to given kafka topic (optionally with topicSuffix).
     * @param topicName the original topicName as used to read the data
     * @param topicNameTest the topicName to send to (can be the same as the topicName).
     * @param before the before element (can be null)
     * @param after the after element (can be null, although not both)
     * @return a report about the result
     */
    public ResultEntry send(
            String topicName,
            String topicNameTest,
            String[] before,
            String[] after
    ) {
        ResultEntry result;
        long startTs = System.currentTimeMillis();
        try {
            dynamicTestKafkaProducer.sendMessagToTestTopic(topicName, topicNameTest, before, after);
            result = new ResultEntry(ResultLevel.OK, String.format("Message sent in %s ms", System.currentTimeMillis() - startTs));
        } catch (Exception e) {
            result = new ResultEntry(ResultLevel.ERROR, String.format("Message failed in %s ms, reason: %s", System.currentTimeMillis() - startTs, e.getMessage()));
        }

        return result;
    }

    public int getFixedInterval() {
        return fixedInterval;
    }

    public void setFixedInterval(int fixedInterval) {
        this.fixedInterval = fixedInterval;
    }

    public int getMaxRtInterval() {
        return maxRtInterval;
    }

    public void setMaxRtInterval(int maxRtInterval) {
        this.maxRtInterval = maxRtInterval;
    }

    /**
     * Class storing all relevant attributes as derived from a single csv row for processing the CDC messages.
     */
    class CDCRecord {
        String[] record;
        String topicName;
        String element;
        long topicOffset;
        long dtMillis;
        boolean comment = false;
        boolean nodata = false;

        CDCRecord(String[] record) {
            this.record = record;
            int colIdx = 1;
            topicName = record[colIdx];
            if (topicName.startsWith(COMMENT_PREFIX)) {
                comment = true;
            } else {
                element = record[++colIdx];
                topicOffset = Long.valueOf(record[++colIdx]);
                LocalDateTime dateTime = LocalDateTime.parse(record[++colIdx], formatter);
                dtMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                String firstCol = record[++colIdx];
                nodata = NO_DATA_MARKER.equals(firstCol);
            }
        }

        boolean matchElementName(String elementName) {
            return element.equals(elementName);
        }

        String[] getMessageRecord() {
            String[] msg = new String[record.length - messageStartsAt];
            System.arraycopy(record, messageStartsAt, msg, 0, msg.length);
            return msg;
        }
    }
}
