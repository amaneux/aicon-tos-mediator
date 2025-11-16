package com.aicon.tos.control;

import com.aicon.tos.connect.web.pages.DataStore;
import com.aicon.tos.control.mail.EmailInfoService;
import com.aicon.tos.control.mail.EmailSender;
import com.aicon.tos.shared.AiconTosMediatorConfig;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.kafka.AiconTosConnectionStatusConsumer;
import com.aicon.tos.shared.kafka.AiconTosControlProducer;
import com.aicon.tos.shared.kafka.AiconUserControlConsumer;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.aicon.tos.shared.schema.AiconTosConnectionStatusMessage;
import com.aicon.tos.shared.schema.AiconUserControlMessage;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.UserOperatingMode;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AiconTosControl class serves as the central service for managing the operating mode
 * of the AICON TOS system. It integrates with Kafka to consume messages from various topics
 * and determines the appropriate operating mode based on the received connection statuses
 * and user control inputs.
 * <p>
 * Responsibilities of the class include:
 * - Monitoring the connection status and user control messages.
 * - Updating the current operating mode based on predefined rules supplied by the
 * OperatingModeRuleEngine.
 * - Sending control messages to the AICON TOS system to reflect changes in the operating mode.
 * - Managing the lifecycle of Kafka consumers and producers.
 */
public class AiconTosControl {

    private final Logger LOG = LoggerFactory.getLogger(AiconTosControl.class);

    private static final String UNCHANGED_INPUTS_TXT = "Unchanged inputs";

    private AiconTosConnectionStatusConsumer connectionStatusConsumer;
    private AiconUserControlConsumer userControlConsumer;
    private AiconTosControlProducer aiconTosControlProducer;
    private ConnectionStatus currentConnectionStatus = ConnectionStatus.NOK;
    private UserOperatingMode currentUserOperatingMode = UserOperatingMode.OFF;
    private Boolean currentCdcOk = Boolean.FALSE;
    private Boolean ignore = Boolean.TRUE;
    private OperatingMode newOperatingMode = OperatingMode.OFF;
    private volatile boolean running = true;
    private DataStore dataStore = null;
    private EmailInfoService emailInfoService = null;
    private OperatingModeRuleEngine ruleEngine;
    private OperatingMode currentOperatingMode = OperatingMode.OFF;
    ConfigSettings config = ConfigSettings.getInstance();

    public AiconTosControl() {
        config = ConfigSettings.getInstance();
        if (!config.hasStorageError()) {
            dataStore = DataStore.getInstance();

            connectionStatusConsumer = new AiconTosConnectionStatusConsumer();
            userControlConsumer = new AiconUserControlConsumer();
            aiconTosControlProducer = new AiconTosControlProducer();

            // Initialize consumers with configurations
            connectionStatusConsumer.initializeConsumer(KafkaConfig.getConsumerProps());
            userControlConsumer.initializeConsumer(KafkaConfig.getConsumerProps());
            this.emailInfoService = new EmailInfoService();

            ruleEngine = OperatingModeRuleEngine.getInstance(true);
        } else {
            LOG.error("Configuration probleem, AiconTosControl not initialized.");
        }
    }

    public AiconTosControl(
            DataStore dataStore,
            AiconTosControlProducer tosControlProducer,
            AiconTosConnectionStatusConsumer connectionStatusConsumer,
            AiconUserControlConsumer userControlConsumer) {
        this();

        this.dataStore = dataStore;
        this.aiconTosControlProducer = tosControlProducer;
        this.connectionStatusConsumer = connectionStatusConsumer;
        this.userControlConsumer = userControlConsumer;
        this.emailInfoService = new EmailInfoService();
    }

    /**
     * Starts the AICON TOS Control service, initializes logging and consumers, and continuously processes messages.
     */
   public void start() {
       Thread.currentThread().setName("AiconTosControl");

       // Initial operating mode
       sendAiconTosControlMessage(
           AiconTosMediatorConfig.REQUEST_ID, currentOperatingMode, ignore, "Starting up"
       );

       // Continuously process incoming messages
       while (running) {
           processConnectionStatusAndCDCMessages();
           processUserControlMessages();
       }
   }


    /**
     * Stops the service and gracefully shuts down Kafka consumers.
     */
    public void stop() {
        running = false;
        connectionStatusConsumer.stop();
        userControlConsumer.stop();
    }

    /**
     * Processes connection status messages from Kafka topics and updates operational
     * mode based on message content and predefined rules.
     */
    public void processConnectionStatusAndCDCMessages() {

        ConsumerRecords<String, AiconTosConnectionStatusMessage> records;

        if (running) {
            records = connectionStatusConsumer.pollMessages();

            records.forEach(consumerRecord -> {

                LOG.info("-----------------Received connection status message --------------------");

                AiconTosConnectionStatusMessage connectionStatusMessage = consumerRecord.value();
                if (connectionStatusMessage != null && connectionStatusMessage.getCdcOk() != null) {
                    LOG.info("Connection status message: {}", connectionStatusMessage.toString());
                    String comment;
                    if (this.currentConnectionStatus != connectionStatusMessage.getConnectionStatus() ||
                            !this.currentCdcOk.equals(connectionStatusMessage.getCdcOk())) {
                        this.newOperatingMode = ruleEngine.newOperatingMode(
                                this.currentConnectionStatus, connectionStatusMessage.getConnectionStatus(),
                                this.currentCdcOk, connectionStatusMessage.getCdcOk(),
                                this.currentUserOperatingMode, this.currentUserOperatingMode);
                        comment = createComment(this.currentConnectionStatus, connectionStatusMessage.getConnectionStatus(),
                                this.currentCdcOk, connectionStatusMessage.getCdcOk(),
                                this.currentUserOperatingMode, this.currentUserOperatingMode);

                        EmailSender.sendEmailToConfiguredEmployees(
                                emailInfoService,
                                currentOperatingMode,
                                newOperatingMode,
                                currentConnectionStatus,
                                connectionStatusMessage.getConnectionStatus(),
                                currentCdcOk,
                                connectionStatusMessage.getCdcOk()
                        );

                        //Switch to new values
                        this.currentConnectionStatus = connectionStatusMessage.getConnectionStatus();
                        this.currentCdcOk = connectionStatusMessage.getCdcOk();
                        this.currentOperatingMode = newOperatingMode;
                        LOG.info("New operating mode: {}", newOperatingMode.name());

                        // Send the TOS Control message
                        sendAiconTosControlMessage(String.valueOf(connectionStatusMessage.getRequestId()),
                                this.currentOperatingMode, ignore, comment);
                    } else {
                        LOG.info(UNCHANGED_INPUTS_TXT);
                        comment = UNCHANGED_INPUTS_TXT;
                    }
                } else {
                    LOG.error("FOUND ANOTHER CONNECTION STATUS MESSAGE WITHOUT CDC OK VALUE!");
                }
            });
        }
    }

    /**
     * Processes user control messages from Kafka topics and updates operational
     * mode based on user input and predefined rules.
     */
    public void processUserControlMessages() {
        ConsumerRecords<String, AiconUserControlMessage> records;

        if (running) {
            records = userControlConsumer.pollMessages();

            records.forEach(consumerRecord -> {
                AiconUserControlMessage userControlMessage = consumerRecord.value();
                LOG.info("-----------------Received user control message: {}", userControlMessage);

                if (this.currentUserOperatingMode == userControlMessage.getUserOperatingMode() &&
                        this.ignore.equals(userControlMessage.getIgnore())) {
                    LOG.info(UNCHANGED_INPUTS_TXT);
                } else {
                    // Set TOS control logic based on the user control message
                    var newUserOperatingMode = userControlMessage.getUserOperatingMode();
                    var tosControlId = String.valueOf(userControlMessage.getId());
                    newOperatingMode = ruleEngine.newOperatingMode(
                            this.currentConnectionStatus, this.currentConnectionStatus,
                            this.currentCdcOk, this.currentCdcOk,
                            this.currentUserOperatingMode, newUserOperatingMode);
                    String comment = createComment(
                            this.currentConnectionStatus, this.currentConnectionStatus,
                            this.currentCdcOk, this.currentCdcOk,
                            this.currentUserOperatingMode, newUserOperatingMode);
                    // Send email notifications based on the new operating mode
                    EmailSender.sendEmailToConfiguredEmployees(
                            emailInfoService,
                            currentOperatingMode,
                            newOperatingMode,
                            currentUserOperatingMode,
                            newUserOperatingMode);

                    this.ignore = userControlMessage.getIgnore();
                    this.currentUserOperatingMode = userControlMessage.getUserOperatingMode();
                    this.currentOperatingMode = newOperatingMode;

                    LOG.info("New operating mode: {}", newOperatingMode.name());

                    // Send the TOS Control message
                    sendAiconTosControlMessage(tosControlId, currentOperatingMode, ignore, comment);
                }
            });
        }
    }

    /**
     * Creates a string comment summarizing changes in connection status,
     * CDC state, and user operating mode.
     *
     * @param currentConnStat     Current connection status.
     * @param newConnStat         New connection status.
     * @param currentCdcOk        Current CDC status.
     * @param newCdcOk            New CDC status.
     * @param currentUserOperMode Current user operating mode.
     * @param newUserOperMode     New user operating mode.
     * @return A string summarizing the changes for logging and messaging.
     */
    private String createComment(ConnectionStatus currentConnStat, ConnectionStatus newConnStat,
                                 Boolean currentCdcOk, Boolean newCdcOk,
                                 UserOperatingMode currentUserOperMode, UserOperatingMode newUserOperMode) {
        return String.format("Conn %s => %s, cdc %s => %s, user %s => %s",
                currentConnStat, newConnStat, currentCdcOk, newCdcOk, currentUserOperMode, newUserOperMode);
    }

    /**
     * Sends a control message with id, operating mode, and comment to the TOS system.
     *
     * @param id            The identifier for the control message.
     * @param operatingMode The operating mode to be set.
     * @param ignore Saying ignore instruction from AICON
     * @param comment       Additional text describing operational mode updates.
     */
    public void sendAiconTosControlMessage(String id, OperatingMode operatingMode, Boolean ignore, String comment) {
        LOG.info("Sending AICON_TOS_Control message with id: {} and operating mode: {} and timeSync: {}",
                id, operatingMode, dataStore.getTimeSync());
        aiconTosControlProducer.sendAiconTosControlMessage(id, operatingMode.name(), ignore,
                dataStore.getTimeSync(), comment);
    }

    /**
     * @return The current connection status.
     */
    public ConnectionStatus getCurrentConnectionStatus() {
        return this.currentConnectionStatus;
    }

    /**
     * @return The current user-operating mode.
     */
    public UserOperatingMode getCurrentUserOperatingMode() {
        return this.currentUserOperatingMode;
    }

    /**
     * @return The current CDC OK state.
     */
    public Boolean getCurrentCdcOk() {
        return this.currentCdcOk;
    }


    public ConfigSettings getConfig() {
        return config;
    }

}
