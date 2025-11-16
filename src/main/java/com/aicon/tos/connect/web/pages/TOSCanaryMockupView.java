package com.aicon.tos.connect.web.pages;

import com.aicon.tos.connect.web.mockup.CananyMockupWebService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import generated.AiconTosCanaryRequest;
import generated.AiconTosCanaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "TOSCanaryMockup", layout = MainLayout.class)
@PageTitle("Mediator Canary Mockup")
public class TOSCanaryMockupView extends VerticalLayout implements CananyMockupWebService.WebServiceListener {

    protected static final Logger LOG = LoggerFactory.getLogger(TOSCanaryMockupView.class.getName());

    private final UI ui;
    private final TextArea requestDisplay;
    private final TextArea responseEditor;
    private final Checkbox freezeResponseSwitch;
    private final Checkbox useModifiedResponseSwitch;
    private final Checkbox blockResponseSendSwitch;

    public TOSCanaryMockupView() {
        LOG.info("Construct....");
        this.ui = UI.getCurrent();

        setPadding(true);

        DataStore dataStore = DataStore.getInstance();

        requestDisplay = createRequestDisplay();
        requestDisplay.setValue(dataStore.getRequestDisplayValue());

        responseEditor = createResponseEditor();
        responseEditor.setValue(dataStore.getResponseEditorValue());
        responseEditor.addValueChangeListener(event -> {
            String newValue = event.getValue();
            dataStore.setResponseEditorValue(newValue);
            LOG.info("Response editor updated: {}", newValue);
        });

        freezeResponseSwitch = createFreezeResponseSwitch();
        freezeResponseSwitch.setValue(dataStore.isFreezeResponse());
        freezeResponseSwitch.addValueChangeListener(event -> {
            boolean newValue = event.getValue();
            dataStore.setFreezeResponse(newValue);
            LOG.info("Freeze response switch updated: {}", newValue);
        });

        useModifiedResponseSwitch = createUseModifiedResponseSwitch();
        useModifiedResponseSwitch.setValue(dataStore.isUseModifiedResponse());
        useModifiedResponseSwitch.addValueChangeListener(event -> {
            boolean newValue = event.getValue();
            dataStore.setUseModifiedResponse(newValue);
            LOG.info("Use modified response switch updated: {}", newValue);
        });

        blockResponseSendSwitch = createBlockResponseSendSwitch();
        blockResponseSendSwitch.setValue(dataStore.isBlockResponseSend());
        blockResponseSendSwitch.addValueChangeListener(event -> {
            boolean newValue = event.getValue();
            dataStore.setBlockResponseSend(newValue);
            LOG.info("Block response switch updated: {}", newValue);
        });
        add(requestDisplay, responseEditor, freezeResponseSwitch, useModifiedResponseSwitch, blockResponseSendSwitch);

        CananyMockupWebService.registerListener(this);
    }

    private TextArea createRequestDisplay() {
        TextArea textArea = new TextArea("Received Request");
        textArea.setWidthFull();
        textArea.setReadOnly(true);
        return textArea;
    }

    private TextArea createResponseEditor() {
        TextArea textArea = new TextArea("Response to Send");
        textArea.setWidthFull();
        return textArea;
    }

    private Checkbox createFreezeResponseSwitch() {
        Checkbox checkbox = new Checkbox("Freeze response values");
        checkbox.setValue(false);
        return checkbox;
    }

    private Checkbox createUseModifiedResponseSwitch() {
        Checkbox checkbox = new Checkbox("Use modified response values");
        checkbox.setValue(false);
        return checkbox;
    }

    private Checkbox createBlockResponseSendSwitch() {
        Checkbox checkbox = new Checkbox("Block Response Send");
        checkbox.setValue(false);
        checkbox.addValueChangeListener(event -> {
            boolean block = event.getValue();
            CananyMockupWebService.setBlockResponseSend(block);
            if (block) {
                LOG.info("Response sending has been blocked.");
            } else {
                LOG.info("Response sending has been unblocked.");
            }
        });
        return checkbox;
    }

    @Override
    public void onRequestReceived(AiconTosCanaryRequest request) {
        if (ui != null && ui.isAttached()) {
            ui.access(() -> {
                DataStore dataStore = DataStore.getInstance();

                String displayValue = "Id: " + request.getRequestId() + ", cdcTables:" + request.getCdcTables();
                requestDisplay.setValue(displayValue);
                dataStore.setRequestDisplayValue(displayValue);

                AiconTosCanaryResponse response = CananyMockupWebService.createResponse(request);

                if (freezeResponseSwitch.getValue().equals(Boolean.FALSE)) {
                    responseEditor.setValue(response.getLatestCdcCreations());
                    dataStore.setResponseEditorValue(response.getLatestCdcCreations());
                } else {
                    response.setLatestCdcCreations(responseEditor.getValue());
                    CananyMockupWebService.setModifiedResponse(response);
                }

                if (Boolean.FALSE.equals(useModifiedResponseSwitch.getValue())) {
                    CananyMockupWebService.setModifiedResponse(response);
                }

                dataStore.setFreezeResponse(freezeResponseSwitch.getValue());
                dataStore.setUseModifiedResponse(useModifiedResponseSwitch.getValue());
                dataStore.setBlockResponseSend(blockResponseSendSwitch.getValue());
            });
        } else {
            LOG.warn("UI is not attached or available. Skipping update.");
        }
    }
}
