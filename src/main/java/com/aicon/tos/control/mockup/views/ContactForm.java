package com.aicon.tos.control.mockup.views;

import com.aicon.tos.control.mail.EmailInfo;
import com.aicon.tos.control.mail.EmailInfoService;
import com.aicon.tos.shared.schema.OperatingMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.VaadinServlet;
import jakarta.servlet.ServletContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The `ContactForm` class provides a grid-based user interface for managing
 * employee contact information. This form allows the user to view, edit, save,
 * and update contact details such as name, email address, and interest level using Vaadin components.
 */
public class ContactForm {

    // List to hold email contact information
    private List<EmailInfo> emailInfoList = new ArrayList<>();

    private Grid<EmailInfo> grid; // Grid for displaying and editing contact information
    private EmailInfoService emailInfoService; // Service class for email-related operations
    private Button saveButton; // Button for saving edits
    private Button cancelButton; // Button for canceling edits
    private Button exportButton; // Button for exporting data to JSON
    private Button importButton; // Button for importing data from JSON

    // JSON file location, relative to the webapp folder
    private static String getJsonFilePath() {
        ServletContext context = VaadinServlet.getCurrent().getServletContext();
        String relativePath = "/WEB-INF/email_info.json";  // Customize the relative path as needed
        return context.getRealPath(relativePath);
    }
    /**
     * Private constructor to prevent direct instantiation without using the service.
     * This ensures that the `EmailInfoService` is always provided.
     */
    private ContactForm() {
    }

    /**
     * Constructs the `ContactForm` with a reference to the `EmailInfoService`.
     * The `EmailInfoService` provides the necessary data for the form.
     *
     * @param emailInfoService Service for handling email contact data.
     */
    public ContactForm(EmailInfoService emailInfoService) {
        this.emailInfoService = emailInfoService;
    }

    /**
     * Creates and returns the form content including a grid for displaying contact information.
     * This form provides functionality to edit, save, and load contact details.
     *
     * @return A Div containing the form layout with a grid and control buttons.
     */
    public Div getForm() {
        VerticalLayout layout = new VerticalLayout();

        // Create a grid with manually defined columns
        grid = new Grid<>(EmailInfo.class, false); // Disable automatic column generation

        // Binder for connecting fields with emailInfo properties
        Binder<EmailInfo> binder = new Binder<>(EmailInfo.class);
        Editor<EmailInfo> editor = grid.getEditor();
        editor.setBinder(binder);
        editor.setBuffered(true); // Enables buffer mode for editing

        // Name column with TextField editor
        Grid.Column<EmailInfo> nameColumn = grid.addColumn(EmailInfo::getName)
                .setHeader("Name").setKey("name").setFlexGrow(0).setWidth("150px");
        TextField nameField = new TextField();
        binder.bind(nameField, EmailInfo::getName, EmailInfo::setName);
        nameColumn.setEditorComponent(nameField);

        // Email address column with TextField editor
        Grid.Column<EmailInfo> emailAddressColumn = grid.addColumn(EmailInfo::getEmailAddress)
                .setHeader("EmailAddress").setKey("emailAddress").setFlexGrow(0).setWidth("300px");
        TextField emailAddressField = new TextField();
        emailAddressField.setWidth("280px");
        binder.bind(emailAddressField, EmailInfo::getEmailAddress, EmailInfo::setEmailAddress);
        emailAddressColumn.setEditorComponent(emailAddressField);

        // Interest Level column with ComboBox editor for selecting OperatingMode
        Grid.Column<EmailInfo> interestLevelOperatingModeColumn = grid.addColumn(EmailInfo::getInterestLevel)
                .setHeader("InterestLevel").setKey("interestLevel").setFlexGrow(1);
        ComboBox<OperatingMode> interestLevelField = new ComboBox<>();
        interestLevelField.setItems(OperatingMode.values()); // Populate ComboBox with enum values
        binder.bind(interestLevelField, EmailInfo::getInterestLevel, EmailInfo::setInterestLevel);
        interestLevelOperatingModeColumn.setEditorComponent(interestLevelField);

        // Active account column with RadioButtonGroup editor for boolean values (Yes/No)
        Grid.Column<EmailInfo> sendMailColumn = grid.addColumn(EmailInfo::isActiveAccount)
                .setHeader("Active").setKey("active").setFlexGrow(1);
        RadioButtonGroup<Boolean> sendMailField = new RadioButtonGroup<>();
        sendMailField.setItems(true, false); // Define "Yes" or "No" options
        sendMailField.setItemLabelGenerator(item -> item ? "Yes" : "No"); // Label options as "Yes" or "No"
        binder.bind(sendMailField, EmailInfo::isActiveAccount, EmailInfo::setIsActiveAccount);
        sendMailColumn.setEditorComponent(sendMailField);

        // Create Save and Cancel buttons for the editor
        saveButton = new Button("Save", e -> {
            editor.save(); // Save changes
            setButtonsVisible(false); // Hide buttons after save
        });
        cancelButton = new Button("Cancel", e -> {
            editor.cancel(); // Cancel editing
            setButtonsVisible(false); // Hide buttons after cancel
        });
        exportButton = new Button("Export to JSON", e -> exportToJson());
        importButton = new Button("Import from JSON", e -> importFromJson());

        setButtonsVisible(false); // Initially hide Save and Cancel buttons

        // Set up the editor to start editing on double-click or Enter key
        grid.addItemDoubleClickListener(event -> {
            if (editor.isOpen()) {
                editor.save(); // Save current row if another row is being edited
            }
            editor.editItem(event.getItem()); // Open editor for the clicked row
            nameField.focus(); // Focus on the name field
            setButtonsVisible(true); // Show Save and Cancel buttons
        });

        // Layout for Save and Cancel buttons
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);

        // Add the grid and buttons to the layout
        layout.add(grid, buttons);

        // Set grid to full width and height
        grid.setHeight("400px");
        grid.setWidth("800px");

        // Load the table data from the service
        grid.setItems(emailInfoService.getEmailInfoList());

        layout.setWidth("600px");

        // Wrap the form in a Div and return it
        Div content = new Div();
        content.add(layout);

        return content;
    }

    /**
     * Exports the current email contact list to a JSON file.
     */
    private void exportToJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(new File(getJsonFilePath()), emailInfoList);
            Notification.show("Data exported to JSON.");
        } catch (IOException e) {
            Notification.show("Failed to export data.");
        }
    }

    /**
     * Imports email contact list data from a JSON file.
     */
    private void importFromJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            emailInfoList = objectMapper.readValue(new File(getJsonFilePath()), objectMapper.getTypeFactory().constructCollectionType(List.class, EmailInfo.class));
            grid.setItems(emailInfoList);
            Notification.show("Data imported from JSON.");
        } catch (IOException e) {
            Notification.show("Failed to import data.");
        }
    }

    /**
     * Sets the visibility of the Save and Cancel buttons.
     *
     * @param visible Boolean indicating whether the buttons should be visible.
     */
    private void setButtonsVisible(boolean visible) {
        saveButton.setVisible(visible);
        cancelButton.setVisible(visible);
    }
}
