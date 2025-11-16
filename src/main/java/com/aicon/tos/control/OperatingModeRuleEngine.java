package com.aicon.tos.control;

import com.aicon.tos.shared.kafka.AiconTosControlRuleTableConsumer;
import com.aicon.tos.shared.kafka.AiconTosControlRuleTableMessageListener;
import com.aicon.tos.shared.kafka.AiconTosControlRuleTableProducer;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.OperatingModeRule;
import com.aicon.tos.shared.schema.UserOperatingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The OperatingModeRuleEngine class defines the rules for determining the operating mode
 * based on the current and new states of connection status, CDC status, and user operating mode.
 */
public class OperatingModeRuleEngine implements AiconTosControlRuleTableMessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(OperatingModeRuleEngine.class);
    private static OperatingModeRuleEngine instance; // Singleton instance

    private List<OperatingModeRule> rules;
    private final AiconTosControlRuleTableConsumer ruleTableConsumer;
    private final AiconTosControlRuleTableProducer ruleTableProducer;

    // Private constructor to prevent instantiation
    private OperatingModeRuleEngine(Boolean activateListener) {
        rules = new ArrayList<>();
        this.ruleTableConsumer = new AiconTosControlRuleTableConsumer();
        this.ruleTableProducer = new AiconTosControlRuleTableProducer();
        if (activateListener) {
            this.ruleTableConsumer.addMessageListener(this);
        }
        initializeRules();
        ruleTableConsumer.initializeConsumer(KafkaConfig.getConsumerProps());
    }

    /**
     * Provides the single instance of OperatingModeRuleEngine.
     *
     * @return The singleton instance of OperatingModeRuleEngine.
     */
    public static synchronized OperatingModeRuleEngine getInstance(Boolean activateListener) {
        if (instance == null) {
            instance = new OperatingModeRuleEngine(activateListener);
        }
        return instance;
    }

    @Override
    public void onMessageReceived(List<OperatingModeRule> ruleTable) {
        LOG.debug("Received new ruleTable = {}", ruleTable);
        this.rules = ruleTable;
    }


    /**
     * Initializes the rule set with predefined conditions for determining the operating mode.
     */
    public void initializeRules() {
        // @formatter:off
        add(ConnectionStatus.OK, 	ConnectionStatus.OK, 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.ON);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.ON);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.ON);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.AUTO  , 	OperatingMode.ON   );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.ON    , 	OperatingMode.ON   );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.AUTO  , 	OperatingMode.ON   );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.ON    , 	OperatingMode.ON   );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.ON    , 	OperatingMode.ON   );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.AUTO  , 	OperatingMode.ON   );

        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.OFF  );

        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.OFF);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.OFF);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.OFF);

        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.OK , 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW  );

        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.ON);
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.ON);
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	false, 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.ON   );
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.ON   );
        add(ConnectionStatus.NOK, 	ConnectionStatus.OK , 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.AUTO  , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.SHADOW, 	OperatingMode.OFF  );

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.ON    , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.AUTO  , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.SHADOW, 	OperatingMode.OFF  );

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.ON    , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.SHADOW, 	OperatingMode.OFF  );

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.ON    , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.AUTO  , 	OperatingMode.OFF  );

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	false, 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.ON    , 	UserOperatingMode.ON    , 	OperatingMode.OFF);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.OFF   , 	UserOperatingMode.OFF   , 	OperatingMode.OFF);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.AUTO  , 	UserOperatingMode.AUTO  , 	OperatingMode.OFF);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	false, 	UserOperatingMode.SHADOW, 	UserOperatingMode.SHADOW, 	OperatingMode.OFF);

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.OFF   , 	OperatingMode.OFF  );
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.ON    , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.OFF   , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.OFF   , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.AUTO  , 	UserOperatingMode.SHADOW, 	OperatingMode.SHADOW);

        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.ON    , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.OFF   , 	OperatingMode.SHADOW);
        add(ConnectionStatus.NOK, 	ConnectionStatus.NOK, 	true , 	true , 	UserOperatingMode.SHADOW, 	UserOperatingMode.AUTO  , 	OperatingMode.SHADOW);
        // @formatter:on
    }

    /**
     * Returns the rule set with predefined conditions for determining the operating mode.
     */
    public List<OperatingModeRule> getRules() {
        return rules;
    }

    /**
     * Adds a new rule to the rule set.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param newConnectionStatus      The new connection status.
     * @param currentCdcOk             The current CDC status.
     * @param newCdcOk                 The new CDC status.
     * @param currentUserOperatingMode The current user operating mode.
     * @param newUserOperatingMode     The new user operating mode.
     * @param newOperatingMode         The resulting operating mode for this rule.
     */
    private void add(ConnectionStatus currentConnectionStatus, ConnectionStatus newConnectionStatus,
                     Boolean currentCdcOk, Boolean newCdcOk,
                     UserOperatingMode currentUserOperatingMode, UserOperatingMode newUserOperatingMode,
                     OperatingMode newOperatingMode) {
        rules.add(new OperatingModeRule(currentConnectionStatus, newConnectionStatus,
                                        currentCdcOk, newCdcOk,
                                        currentUserOperatingMode, newUserOperatingMode,
                                        newOperatingMode));
    }

    /**
     * Determines the new operating mode based on the current and new states provided.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param newConnectionStatus      The new connection status.
     * @param currentCdcOk             The current CDC status.
     * @param newCdcOk                 The new CDC status.
     * @param currentUserOperatingMode The current user operating mode.
     * @param newUserOperatingMode     The new user operating mode.
     * @return The determined operating mode based on the matching rule. If no rule matches, returns {@code OperatingMode.OFF}.
     */
    public OperatingMode newOperatingMode(ConnectionStatus currentConnectionStatus,
                                          ConnectionStatus newConnectionStatus,
                                          Boolean currentCdcOk,
                                          Boolean newCdcOk,
                                          UserOperatingMode currentUserOperatingMode,
                                          UserOperatingMode newUserOperatingMode) {
        LOG.info("===> Conn {}=>{}, CDC {}=>{}, User {}=>{}",
                 currentConnectionStatus, newConnectionStatus,
                 currentCdcOk, newCdcOk,

                 currentUserOperatingMode, newUserOperatingMode);
        return rules.stream()
                .filter(rule -> matches(rule, currentConnectionStatus, newConnectionStatus,
                                        currentCdcOk, newCdcOk,
                                        currentUserOperatingMode, newUserOperatingMode))
                .findFirst()
                .map(OperatingModeRule::getNewOperatingMode)
                .orElseGet(() -> {
                    LOG.error("No matching rule found for the given inputs: Conn {}=>{}, CDC {}=>{}, User {}=>{}",
                              currentConnectionStatus, newConnectionStatus,
                              currentCdcOk, newCdcOk,
                              currentUserOperatingMode, newUserOperatingMode);
                    return OperatingMode.OFF;
                });
    }

    /**
     * Determines if this rule matches the given parameters.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param newConnectionStatus      The new connection status.
     * @param currentCdcOk             The current CDC status (true if OK).
     * @param newCdcOk                 The new CDC status (true if OK).
     * @param currentUserOperatingMode The current user operating mode.
     * @param newUserOperatingMode     The new user operating mode.
     * @return True if all parameters match this rule, otherwise false.
     */
    public Boolean matches(OperatingModeRule rule,
                           ConnectionStatus currentConnectionStatus, ConnectionStatus newConnectionStatus,
                           Boolean currentCdcOk, Boolean newCdcOk, UserOperatingMode currentUserOperatingMode,
                           UserOperatingMode newUserOperatingMode) {
        return rule.getCurrentConnectionStatus() == currentConnectionStatus &&
                rule.getNewConnectionStatus() == newConnectionStatus &&
                rule.getCurrentCdcOk().equals(currentCdcOk) &&
                rule.getNewCdcOk().equals(newCdcOk) &&
                rule.getCurrentUserOperatingMode() == currentUserOperatingMode &&
                rule.getNewUserOperatingMode() == newUserOperatingMode;
    }

    public void saveRules(List<OperatingModeRule> ruleTable) {
        ruleTableProducer.sendRuleTableMessage(ruleTable);
    }
}