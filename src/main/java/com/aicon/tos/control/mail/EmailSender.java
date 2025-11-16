package com.aicon.tos.control.mail;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.aicon.tos.shared.exceptions.EmailSendingException;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.UserOperatingMode;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aicon.tos.shared.config.ConfigSettings.CFG_EMAIL_HOSTNAME;
import static com.aicon.tos.shared.config.ConfigSettings.CFG_EMAIL_PASSWORD;
import static com.aicon.tos.shared.config.ConfigSettings.CFG_EMAIL_SENDER;
import static com.aicon.tos.shared.config.ConfigSettings.CFG_EMAIL_SMTPPORT;
import static com.aicon.tos.shared.config.ConfigSettings.CFG_EMAIL_USERNAME;

/**
 * The EmailSender class is responsible for sending emails to configured employees
 * based on their interest level and active account status, using the OperatingMode.
 */
public class EmailSender {

    private static final String EMAIL_SENDING_ERROR_MESSAGE = "Email sending failed";

    private static final Logger LOG = LoggerFactory.getLogger(EmailSender.class);
    private static final Map<String, OperatingMode> sendMails = new HashMap<>();


    public static void sendEmailToConfiguredEmployees(EmailInfoService service,
                                                      OperatingMode operatingMode, OperatingMode newOperatingMode,
                                                      UserOperatingMode userOperatingMode, UserOperatingMode newUserOperatingMode) {
        String reason = String.format("Reason for change is the changed user operating mode, went from %s to %s",
                                            userOperatingMode.name(),newUserOperatingMode.name());
        try {
            sendEmailToConfiguredEmployees(service, newOperatingMode, reason);
        } catch (EmailException e) {
            handleEmailException(newOperatingMode, e);
        }
    }

    public static void sendEmailToConfiguredEmployees(EmailInfoService emailInfoService,
                                                      OperatingMode operatingMode, OperatingMode newOperatingMode,
                                                      ConnectionStatus connectionStatus, ConnectionStatus newConnectionStatus,
                                                      Boolean cdcOk, Boolean newCdcOk) {
        String reason = "Reason for change is the changed ";
        String connection = "";
        String cdc = "";

        if (!connectionStatus.equals(newConnectionStatus)) {
            connection = String.format("connection status, went from %s to %s", connectionStatus, newConnectionStatus);
        }
        if (!cdcOk.equals(newCdcOk)) {
            cdc = String.format("cdc status, went from %s to %s", cdcOk, newCdcOk);
        }

        if (!connection.isEmpty()) {
            reason += connection;
        }
        if (!connection.isEmpty() && !cdc.isEmpty()) {
            reason += " and ";
        }
        if (!cdc.isEmpty()) {
            reason += cdc;
        }

        try {
            sendEmailToConfiguredEmployees(emailInfoService, newOperatingMode, reason);
        } catch (
                EmailException e) {
            handleEmailException(newOperatingMode, e);
        }
    }

    /**
     * Sends an email to employees whose accounts are active and match the provided OperatingMode.
     *
     * @param emailInfoService the service used to filter email recipients based on OperatingMode and account status
     * @param operatingMode    the current operating mode used to filter recipients
     * @throws EmailException if an error occurs while sending the email
     */
    public static void sendEmailToConfiguredEmployees(EmailInfoService emailInfoService, OperatingMode operatingMode, String reason)
            throws EmailException {
        // Get the list of email accounts that match the OperatingMode and are active
        List<EmailInfo> selectedEmailAccounts = emailInfoService.filterEmailByOperatingModeAndActiveAccount(
                operatingMode);

        for (EmailInfo selectedEmailAccount : selectedEmailAccounts) {
            String emailAddress = selectedEmailAccount.getEmailAddress();

            if (!sameModeMailAlreadySent(emailAddress, operatingMode)) {
                Email email = createOperatingModeChangeEmail(operatingMode,
                        reason,
                        emailAddress,
                        selectedEmailAccount.getName());
                if (email != null) {
                    email.send(); // Send the email
                    LOG.info("Email sent to {}", emailAddress);
                } else {
                    LOG.error("Error while sending email to {}", emailAddress);
                }

                // Sla de verzonden e-mail op
                storeLastSentMailToAddress(emailAddress, operatingMode);
            } else {
                LOG.info("Skipping email to {}, already sent for mode: {}", emailAddress, operatingMode);
            }
        }
    }

    private static boolean sameModeMailAlreadySent(String emailAddress, OperatingMode operatingMode) {
        return sendMails.containsKey(emailAddress) && sendMails.get(emailAddress).equals(operatingMode);
    }

    private static void storeLastSentMailToAddress(String emailAddress, OperatingMode operatingMode) {
        sendMails.put(emailAddress, operatingMode);
    }

    /**
     * Creates an email to notify the recipient of a change in the operating mode.
     *
     * @param newOperatingMode      the new operating mode
     * @param recipientEmailAddress the recipient's email address
     * @param recipientName         the recipient's name
     * @return a configured Email object ready to be sent
     */
    public static Email createOperatingModeChangeEmail(OperatingMode newOperatingMode,
                                                       String reason,
                                                       String recipientEmailAddress,
                                                       String recipientName) {
        ConfigSettings config = ConfigSettings.getInstance();
        ConfigGroup general = config.getMainGroup(ConfigType.General).getChildGroup(ConfigType.GeneralItems);
        String site = String.format("%s-%s", general.findItem(ConfigDomain.CFG_TERMINAL_NAME).value(),
                general.findItem(ConfigDomain.CFG_ENVIRONMENT_NAME).value());
        String messageBody;
        Email email = new SimpleEmail();

        try {
            // Set up the email server configuration
            email.setHostName(general.findItem(CFG_EMAIL_HOSTNAME).value());
            email.setSmtpPort(Integer.parseInt(general.findItem(CFG_EMAIL_SMTPPORT).value()));
            email.setAuthentication(
                    general.findItem(CFG_EMAIL_USERNAME).value(),
                    general.findItem(CFG_EMAIL_PASSWORD).value());
            email.setStartTLSEnabled(true);

            // Set the email subject and content
            email.setFrom(general.findItem(CFG_EMAIL_SENDER).value());
            email.setSubject(createMailSubject(site));
            messageBody = createMailBody(recipientName, newOperatingMode, reason, site);
            email.setMsg(messageBody);
            email.setContent(messageBody, "text/html; charset=utf-8");
            email.addTo(recipientEmailAddress);

        } catch (EmailException e) {
            LOG.error("Error while creating email", e);
            return null;
        }

        return email;
    }

    /**
     * Generates the email subject for the operating mode change notification.
     *
     * @param site the site where the operating mode change occurred
     * @return the formatted subject
     */
    private static String createMailSubject(String site) {
        return String.format("Update on AiCon and TOS Operating Mode in %s", site);
    }

    /**
     * Generates the email body for the operating mode change notification.
     *
     * @param recipientName the name of the email recipient
     * @param operatingMode the new operating mode
     * @param site          the site where the change occurred
     * @return the formatted HTML email body
     */
    private static String createMailBody(String recipientName, OperatingMode operatingMode, String reason, String site) {
        String senderName = "AiCon-TOS-Control";

        return String.format(
                "<html>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<p>Dear %s,</p>" +
                        "<p>I would like to inform you that the operating mode for <strong>AiCon</strong> and " +
                        "<strong>TOS</strong> in <strong>%s</strong> has been changed to <strong>%s</strong>.</p>" +
                        "<p>%s</p>" +
                        "<p>Kind regards,<br>%s</p>" +
                        "</body>" +
                        "</html>", recipientName, site, operatingMode.name(), reason, senderName);
    }

    private static void handleEmailException(OperatingMode operatingMode, EmailException e) {
        LOG.error("Failed to send emails. OperatingMode: {}. Error: {}", operatingMode, e.getMessage());
        throw new EmailSendingException(EMAIL_SENDING_ERROR_MESSAGE, e);
    }
}

