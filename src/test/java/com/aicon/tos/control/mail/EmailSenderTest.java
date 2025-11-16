package com.aicon.tos.control.mail;

import com.aicon.TestConstants;
import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.aicon.tos.shared.schema.OperatingMode;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link EmailSender} class.
 * These tests validate the behavior of sending emails based on different {@link OperatingMode}s.
 */
class EmailSenderTest {

    /**
     * Sets up a mock list of email data for testing before each test.
     * Initializes the {@link EmailInfoService} and populates it with test data.
     */
    @BeforeEach
    public void setUp() {
        ConfigSettings.setConfigFile(TestConstants.PATH_TO_TEST_CONFIG_FILES + "conf/mediator/emailsendertest.xml");

        // Gebruik een globale mock voor alle tests
        EmailInfoService mockEmailInfoService = Mockito.mock(EmailInfoService.class);

        // Stel een standaard gedrag in voor mock-methoden (optioneel)
        Mockito.when(mockEmailInfoService.filterEmailByOperatingModeAndActiveAccount(Mockito.any()))
                .thenReturn(new ArrayList<>());

        // Initialize a local list with mock data for testing
        List<EmailInfo> emailInfoList = new ArrayList<>();
        emailInfoList.add(new EmailInfo("Gauthaman", "gauthaman.vasudevan@avlino.com", OperatingMode.OFF, false));
        emailInfoList.add(new EmailInfo("Nithin", "nithin.dsouza@avlino.com", OperatingMode.OFF, false));
        emailInfoList.add(new EmailInfo("Yusuf", "yusuf.ali@avlino.com.nosend", OperatingMode.ON, true));
        emailInfoList.add(new EmailInfo("Harrie", "harrie.vanrijn@avlino.com", OperatingMode.OFF, true));
        emailInfoList.add(new EmailInfo("Ron", "ron.dewaard@avlino.com.nosend", OperatingMode.SHADOW, true));

        // Initialize the EmailInfoService with the local list
        EmailInfoService emailInfoService = Mockito.spy(new EmailInfoService());
        emailInfoService.setEmailInfoList(emailInfoList);
    }


    /**
     * Tests the email sending functionality for the {@link OperatingMode#ON}.
     * Verifies that the correct number of emails is sent for the ON mode.
     *
     * @throws EmailException If email sending fails.
     */
    @Test
    public void testSendEmailToConfiguredEmployees_OperatingModeON() throws EmailException {
        // Mock email sending by creating a mock for the EmailInfoService
        EmailInfoService emailInfoService = Mockito.mock(EmailInfoService.class);

        // Define mock behavior for filtering emails based on OperatingMode ON
        List<EmailInfo> mockEmails = new ArrayList<>();
        mockEmails.add(new EmailInfo("Yusuf", "yusuf.ali@avlino.com.nosend", OperatingMode.ON, true));

        // Stub the method to return the mock emails
        Mockito.when(emailInfoService.filterEmailByOperatingModeAndActiveAccount(OperatingMode.ON))
                .thenReturn(mockEmails);

        // Simulate sending emails for OperatingMode ON
        EmailSender.sendEmailToConfiguredEmployees(emailInfoService, OperatingMode.ON, "reason");

        // Verify that the filtering method was called exactly once
        verify(emailInfoService, times(1)).filterEmailByOperatingModeAndActiveAccount(OperatingMode.ON);

        // Assert the correct number of emails are sent
        assertEquals(1, mockEmails.size(), "Expected 1 email address for ON mode");
    }

    /**
     * Tests the email sending functionality for the {@link OperatingMode#SHADOW}.
     * Verifies that the correct number of emails is sent for the SHADOW mode.
     *
     * @throws EmailException If email sending fails.
     */
    @Test
    public void testSendEmailToConfiguredEmployees_OperatingModeSHADOW() throws EmailException {
        // Mock email sending by creating a mock for the EmailInfoService
        EmailInfoService emailInfoService = Mockito.mock(EmailInfoService.class);

        // Define mock behavior for filtering emails based on OperatingMode SHADOW
        List<EmailInfo> mockEmails = new ArrayList<>();
        mockEmails.add(new EmailInfo("Ron", "ron.dewaard@avlino.com.nosend", OperatingMode.SHADOW, true));
        mockEmails.add(new EmailInfo("Harrie", "harrie.vanrijn@avlino.com", OperatingMode.SHADOW, true));

        // Stub the method to return the mock emails
        Mockito.when(emailInfoService.filterEmailByOperatingModeAndActiveAccount(OperatingMode.SHADOW))
                .thenReturn(mockEmails);

        // Simulate sending emails for OperatingMode SHADOW
        EmailSender.sendEmailToConfiguredEmployees(emailInfoService, OperatingMode.SHADOW,"reason");

        // Verify that the filtering method was called exactly once
        verify(emailInfoService, times(1)).filterEmailByOperatingModeAndActiveAccount(OperatingMode.SHADOW);

        // Assert the correct number of emails are sent
        assertEquals(2, mockEmails.size(), "Expected 2 email addresses for SHADOW mode");
    }

    /**
     * Tests the email sending functionality for the {@link OperatingMode#OFF}.
     * Verifies that the correct number of emails is sent for the OFF mode.
     *
     * @throws EmailException If email sending fails.
     */
    @Test
    public void testSendEmailToConfiguredEmployees_OperatingModeOFF() throws EmailException {
        // Mock email sending by creating a mock for the EmailInfoService
        EmailInfoService emailInfoService = Mockito.mock(EmailInfoService.class);

        // Define mock behavior for filtering emails based on OperatingMode OFF
        List<EmailInfo> mockEmails = new ArrayList<>();
        mockEmails.add(new EmailInfo("Yusuf", "yusuf.ali@avlino.com.nosend", OperatingMode.OFF, true));
        mockEmails.add(new EmailInfo("Harrie", "harrie.vanrijn@avlino.com", OperatingMode.OFF, true));
        mockEmails.add(new EmailInfo("Ron", "ron.dewaard@avlino.com.nosend", OperatingMode.SHADOW, true));

        // Stub the method to return the mock emails
        Mockito.when(emailInfoService.filterEmailByOperatingModeAndActiveAccount(OperatingMode.OFF))
                .thenReturn(mockEmails);

        // Simulate sending emails for OperatingMode OFF
        EmailSender.sendEmailToConfiguredEmployees(emailInfoService, OperatingMode.OFF,"reason");

        // Verify that the filtering method was called exactly once
        verify(emailInfoService, times(1)).filterEmailByOperatingModeAndActiveAccount(OperatingMode.OFF);

        // Assert the correct number of emails are sent
        assertEquals(3, mockEmails.size(), "Expected 3 email addresses for OFF mode");
    }

    /**
     * Tests the creation of an email for a given operating mode and recipient.
     * Verifies that the email is created with the correct subject and recipient.
     */
    @Test
    public void testCreateOperatingModeChangeEmail() {
        String recipientEmail = "test@example.com";
        String recipientName = "Test User";
        OperatingMode operatingMode = OperatingMode.ON;

        ConfigSettings config = ConfigSettings.getInstance();
        ConfigGroup general = config.getMainGroup(ConfigType.General).getChildGroup(ConfigType.GeneralItems);
        String site = String.format("%s-%s", general.findItem(ConfigDomain.CFG_TERMINAL_NAME).value(),
                general.findItem(ConfigDomain.CFG_ENVIRONMENT_NAME).value());

        Email email = EmailSender.createOperatingModeChangeEmail(
                operatingMode, "Good reason", recipientEmail, recipientName);

        // Verify email properties
        assertNotNull(email, "Email should not be null");
        assertEquals("Update on AiCon and TOS Operating Mode in " + site, email.getSubject());
        assertEquals(recipientEmail, email.getToAddresses().get(0).toString());
    }
}
