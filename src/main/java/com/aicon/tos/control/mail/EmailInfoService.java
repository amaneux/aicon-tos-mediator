package com.aicon.tos.control.mail;

import com.aicon.tos.shared.schema.OperatingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The EmailInfoService class is responsible for managing email information and filtering contacts
 * based on their operating mode interest level and active status.
 * It logs relevant details about users and their notification preferences.
 */
public class EmailInfoService {

    // Logger for logging actions within this class
    private Logger LOG = LoggerFactory.getLogger(EmailInfoService.class);

    // List to hold all the email information
    private List<EmailInfo> emailInfoList;

    /**
     * Default constructor that initializes a list of EmailInfo objects with predefined data.
     */
    public EmailInfoService() {
        // Initialize the list with sample data
        emailInfoList = new ArrayList<>();
        emailInfoList.add(new EmailInfo("Gauthaman", "gauthaman.vasudevan@avlino.com", OperatingMode.OFF, false));
        emailInfoList.add(new EmailInfo("Nithin", "nithin.dsouza@avlino.com", OperatingMode.OFF, false));
        emailInfoList.add(new EmailInfo("Yusuf", "yusuf.ali@avlino.com.nosend", OperatingMode.ON, false));
        emailInfoList.add(new EmailInfo("Harrie", "harrie.vanrijn@avlino.com", OperatingMode.OFF, true));
        emailInfoList.add(new EmailInfo("Ron", "ron.dewaard@avlino.com.nosend", OperatingMode.SHADOW, false));
    }

    /**
     * Returns the full list of EmailInfo objects.
     *
     * @return the list of EmailInfo objects
     */
    public List<EmailInfo> getEmailInfoList() {
        return emailInfoList;
    }

    /**
     * Sets the list of EmailInfo objects.
     *
     * @param emailInfoList the list of EmailInfo objects to be set
     */
    public void setEmailInfoList(List<EmailInfo> emailInfoList) {
        this.emailInfoList = emailInfoList;
    }

    /**
     * Filters the email list based on the selected operating mode and active accounts.
     * Logs the details of active users and determines whether they should receive emails
     * based on their interest level and the selected operating mode.
     *
     * @param newOperatingMode The operating mode selected by the user (ON, SHADOW, OFF)
     * @return A filtered list of active EmailInfo objects
     */
    public List<EmailInfo> filterEmailByOperatingModeAndActiveAccount(OperatingMode newOperatingMode) {
        List<EmailInfo> filteredEmails = new ArrayList<>();

        for (EmailInfo email : emailInfoList) {
            if (email.isActiveAccount()) {
                if (shouldSendEmail(email.getInterestLevel(), newOperatingMode)) {
                    LOG.info("User {} with interest level {} is going to receive mail for operating mode {}",
                             email.getName(), email.getInterestLevel(), newOperatingMode.name());
                    filteredEmails.add(email);
                } else {
                    LOG.info("User {} with interest level {} is NOT going to receive mail for operating mode {}",
                             email.getName(), email.getInterestLevel(), newOperatingMode.name());
                }
            }
        }

        return filteredEmails;
    }

    /**
     * Determines whether an email should be sent to the user based on their interest level
     * and the selected operating mode.
     *
     * @param emailMode        The operating mode in the user's email configuration
     * @param newOperatingMode The operating mode selected by the user
     * @return true if the email should be sent, false otherwise
     */
    private boolean shouldSendEmail(OperatingMode emailMode, OperatingMode newOperatingMode) {
        switch (emailMode) {
            case OFF:
                // The email is sent only if the selected mode is OFF
                return newOperatingMode == OperatingMode.OFF;
            case SHADOW:
                // The email is sent for SHADOW or OFF modes
                return newOperatingMode == OperatingMode.SHADOW || newOperatingMode == OperatingMode.OFF;
            case ON:
                // The email is sent for any mode (ON, SHADOW, OFF)
                return newOperatingMode == OperatingMode.ON || newOperatingMode == OperatingMode.SHADOW
                        || newOperatingMode == OperatingMode.OFF;
            default:
                return false;
        }
    }
}
