package com.aicon.tos.control.mail;

import com.aicon.tos.shared.schema.OperatingMode;

/**
 * Represents an email contact's information, including the contact's name, email address,
 * interest level in receiving updates about the system's operating mode, and whether their account is active.
 */
public class EmailInfo {

    // The name of the email contact
    private String name;

    // The email address of the contact
    private String emailAddress;

    // The interest level of the contact, determining which OperatingMode updates they wish to receive
    private OperatingMode interestLevel;

    // A boolean value indicating whether the account is active
    private Boolean isActiveAccount = false;

    /**
     * Constructs an EmailInfo object with the provided name, email address, interest level, and active account status.
     *
     * @param name            the name of the contact
     * @param emailAddress    the email address of the contact
     * @param interestLevel   the operating mode interest level of the contact
     * @param isActiveAccount whether the contact's account is active
     */
    public EmailInfo(String name, String emailAddress, OperatingMode interestLevel, Boolean isActiveAccount) {
        this.name = name;
        this.emailAddress = emailAddress;
        this.interestLevel = interestLevel;
        this.isActiveAccount = isActiveAccount;
    }

    /**
     * Returns the name of the contact.
     *
     * @return the name of the contact
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the contact.
     *
     * @param name the new name of the contact
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the email address of the contact.
     *
     * @return the email address of the contact
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Sets the email address of the contact.
     *
     * @param emailAddress the new email address of the contact
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Returns the interest level of the contact for receiving operating mode updates.
     *
     * @return the interest level of the contact
     */
    public OperatingMode getInterestLevel() {
        return interestLevel;
    }

    /**
     * Sets the interest level of the contact for receiving operating mode updates.
     *
     * @param interestLevel the new interest level of the contact
     */
    public void setInterestLevel(OperatingMode interestLevel) {
        this.interestLevel = interestLevel;
    }

    /**
     * Returns whether the contact's account is active.
     *
     * @return true if the account is active, false otherwise
     */
    public Boolean isActiveAccount() {
        return isActiveAccount;
    }

    /**
     * Sets whether the contact's account is active.
     *
     * @param isActiveAccount true if the account should be active, false otherwise
     */
    public void setIsActiveAccount(Boolean isActiveAccount) {
        this.isActiveAccount = isActiveAccount;
    }
}
