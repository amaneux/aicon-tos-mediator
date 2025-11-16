package com.aicon.tos.connect.web.pages;

import com.aicon.tos.connect.cdc.CDCReplayer;
import com.aicon.tos.connect.cdc.CDCReplayer.IntervalType;
import com.aicon.tos.connect.web.callback.ProgressCallback;
import com.aicon.tos.shared.ResultLevel;
import com.avlino.common.utils.CsvParser;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PageTitle("Replay CDC streams")
@Route(value = "cdc-replay-view", layout = MainLayout.class)
public class CDCReplayView extends VerticalLayout implements Receiver, ProgressCallback {

    private static final Logger LOG = LoggerFactory.getLogger(CDCReplayView.class);

    private static final String CSV_DIR = System.getProperty("java.io.tmpdir");
    private static final String EXT_CSV = ".csv";
    private static final String CSV_FILE_FORMAT_S = CSV_DIR + File.separator + "%s";

    private final Grid<String[]> grid;
    private Button startButton;
    private Button pauseButton;
    private Button stopButton;
    private RadioButtonGroup<IntervalType> radioInterval;
    private TextField topicSuffix;
    private final TextField info;
    private TextField fixedItvMs;
    private TextField maxRtMs;

    private File file = null;
    private CDCReplayer replay = null;
    private List<String[]> gridRecords = null;

    private UI callbackUI;

    public CDCReplayView() {
        setSizeFull();

        // Create the Upload component
        Upload upload = new Upload(this);
        upload.setAcceptedFileTypes(EXT_CSV);

        // Handle successful upload
        upload.addSucceededListener(event -> {
            ViewHelper.notifyInfo(String.format("Upload saved to: %s.", file.getAbsolutePath()));
            parseFile();
            upload.clearFileList();
            startButton.setEnabled(true);
        });

        // Handle failed upload
        upload.addFailedListener(event ->
                ViewHelper.notifyError("Upload failed: " + event.getReason().getMessage())
        );

        Paragraph uploadHelp = ViewHelper.createParagraph(
                "Requirements for the CSV-file:\n" +
                        "1st column=#topic-name, 2nd-column=record-offset, " +
                        "3rd-column=topic-ts, 4+-columns=content of the CDC capture.\n" +
                        "Note: a line starting with #, like #topic-name will be skipped.",
                true);

        grid = ViewHelper.createStringArrayGrid(null, false, false, true);
        grid.setHeightFull();

        startButton = ViewHelper.createButton("", new Icon(VaadinIcon.PLAY), Key.KEY_P, ButtonVariant.LUMO_PRIMARY, null);
        startButton.setEnabled(false);
        startButton.addClickListener(event -> {
            boolean isStepping = radioInterval.getValue() == IntervalType.Stepping;

            if (replay != null && replay.isPaused()) {
                replay.resume();
                if (!isStepping) {
                    ViewHelper.notifyInfo("Resumed");
                }
            } else {
                for (String[] csvRec : gridRecords) {
                    csvRec[0] = "";
                }
                grid.getDataProvider().refreshAll();

                callbackUI = UI.getCurrent();       // we have to remember the current UI now, because when called from the callback thread it will be null.
                replay = new CDCReplayer(this);
                try {
                    replay.setFixedInterval(Integer.parseInt(fixedItvMs.getValue()));
                    replay.setMaxRtInterval(Integer.parseInt(maxRtMs.getValue()));
                } catch (NumberFormatException e) {
                    LOG.warn("Setting not applied, reason: {}", e.getMessage());
                }
                replay.start(gridRecords, 5, radioInterval.getValue(), topicSuffix.getValue());

                ViewHelper.notifyInfo(String.format("Started with interval: %s.", radioInterval.getValue()));
                if (!isStepping) {
                    startButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                }
                stopButton.setEnabled(true);
            }
        });

        pauseButton = ViewHelper.createButton("", new Icon(VaadinIcon.PAUSE), Key.KEY_S, ButtonVariant.LUMO_PRIMARY, null);
        pauseButton.setEnabled(false);
        pauseButton.addClickListener(event -> {
            replay.pause();
            ViewHelper.notifyInfo("Paused (use Start to resume or Stop).");
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(true);
        });

        stopButton = ViewHelper.createButton("", new Icon(VaadinIcon.STOP), Key.KEY_X, ButtonVariant.LUMO_PRIMARY, null);
        stopButton.setEnabled(false);
        stopButton.addClickListener(event -> {
            replay.stop();
            ViewHelper.notifyInfo("Stopped (use Start to start all-over again).");
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        });

        radioInterval = new RadioButtonGroup<>("Replay interval", IntervalType.values());
        radioInterval.setValue(IntervalType.RealTime);
        radioInterval.addValueChangeListener(event -> {
            if (replay != null) {
                replay.setIntervalType(event.getValue());
            }
        });

        fixedItvMs = ViewHelper.createTextField("Fixed interval (ms)", String.valueOf(CDCReplayer.FIXED_ITV_MS), "The fixed interval in ms.", null);
        fixedItvMs.addValueChangeListener(event -> {
            try {
                if (replay != null) {
                    replay.setFixedInterval(Integer.parseInt(event.getValue()));
                }
            } catch (Exception e) {
                // don't execute any wrong value
            }
        });
        maxRtMs = ViewHelper.createTextField("RealTime max interval (ms)", String.valueOf(CDCReplayer.MAX_RT_MS), "The max RealTime interval in ms.", null);
        maxRtMs.addValueChangeListener(event -> {
            try {
                if (replay != null) {
                    replay.setMaxRtInterval(Integer.parseInt(event.getValue()));
                }
            } catch (Exception e) {
                // don't execute any wrong value
            }
        });
        topicSuffix = ViewHelper.createTextField("Topic Suffix", "_eli_test", "Will be aded to the original topic name to send to", null);

        HorizontalLayout hlEntries = new HorizontalLayout();
        hlEntries.add(radioInterval, fixedItvMs, maxRtMs, topicSuffix);

        info = ViewHelper.createTextField(null, null);
        info.setWidthFull();
        info.setReadOnly(true);

        HorizontalLayout hlButtons = new HorizontalLayout();
        hlButtons.setWidthFull();
        hlButtons.add(startButton, pauseButton, stopButton, info);

        add(uploadHelp, upload, hlEntries, hlButtons, grid);
    }

    private void parseFile() {
        List<String[]> csv = CsvParser.parseToList(file.getAbsolutePath(), true, String[].class);
        gridRecords = new ArrayList<>();

        // Add a Send column to the csv records list.
        String first = "Send";
        for (String[] csvRecord : csv) {        // copy the csv and add a send column
            String[] recArray = new String[csvRecord.length + 1];
            recArray[0] = first;
            first = "";
            System.arraycopy(csvRecord, 0, recArray, 1, csvRecord.length);
            gridRecords.add(recArray);
        }

        grid.removeAllColumns();
        if (gridRecords != null && !gridRecords.isEmpty()) {
            for (int i = 0; i < gridRecords.get(0).length; i++) {
                String key = gridRecords.get(0)[i];
                final int idx = i;
                grid.addColumn(str -> str[idx])
                        .setKey(key)
                        .setHeader(key)
                        .setAutoWidth(i > 0)
                        .setResizable(true);
            }
            gridRecords.remove(0);
        } else {
            gridRecords = new ArrayList<>();
        }

        ListDataProvider<String[]> ldp = new ListDataProvider<>(gridRecords);
        grid.setDataProvider(ldp);
        ldp.refreshAll();
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        try {
            file = new File(String.format(CSV_FILE_FORMAT_S, filename));
            return new FileOutputStream(file);
        } catch (Exception e) {
            String log = String.format("Upload of %s failed, reason: %s", filename, e.getMessage());
            LOG.error(log);
            ViewHelper.notifyError(log);
            return null;
        }
    }

    @Override
    public String reportProgress(ResultLevel state, String report, Map<String, Object> reportMap) {
        callbackUI.access(() -> {
            info.setValue(report);
            int idx = reportMap != null ? (Integer) reportMap.getOrDefault(CDCReplayer.REPORT_IDX, -1) : -1;
            if (idx >= 0 && idx < gridRecords.size()) {
                gridRecords.get(idx)[0] = state.toString();
            }
            grid.getDataProvider().refreshAll();
        });
        return state.toString();
    }

    @Override
    public String onDone(ResultLevel state, String report) {
        callbackUI.access(() -> {
            info.setValue(String.format("End-result: %s, %s", state, report));
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            ViewHelper.notify(state, report);
        });
        return report;
    }
}
