package com.aicon.tos.control.mockup.views;

import com.aicon.tos.control.OperatingModeRuleEngine;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.OperatingModeRule;
import com.aicon.tos.shared.schema.UserOperatingMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The RuleEditorForm class provides a UI component to edit and view OperatingModeRule objects.
 */
public class RuleEditorForm {

    private Grid<OperatingModeRule> grid;
    private Binder<OperatingModeRule> binder;
    private Editor<OperatingModeRule> editor;
    private OperatingModeRuleEngine ruleEngine;

    private Button saveButton;
    private Button cancelButton;

    private OperatingModeRule selectedRule;

    // Filter fields
    ComboBox<ConnectionStatus> currentConnStatusFilter;
    ComboBox<ConnectionStatus> newConnStatusFilter;
    ComboBox<Boolean> currentCdcOkFilter;
    ComboBox<Boolean> newCdcOkFilter;
    ComboBox<UserOperatingMode> currentUserOpModeFilter;
    ComboBox<UserOperatingMode> newUserOpModeFilter;
    ComboBox<OperatingMode> newOperatingModeFilter;


    private List<OperatingModeRule> ruleTable; // All rules before filtering
    private boolean isNewRule;

    /**
     * Constructor to set up the Rule Editor tab, displaying and editing operating mode rules.
     */
    public RuleEditorForm() {
        ruleEngine = OperatingModeRuleEngine.getInstance(false);
//        if topic contains a message used it otherwise start from scratch
        ruleTable = ruleEngine.getRules();
    }

    /**
     * Creates and returns the form content as a Div.
     *
     * @return A Div containing the form layout with a grid and control buttons.
     */
    public Div getForm() {
        VerticalLayout layout = new VerticalLayout();

        // Create the grid and binder for displaying the rules
        grid = new Grid<>(OperatingModeRule.class, false);
        binder = new Binder<>(OperatingModeRule.class);
        editor = grid.getEditor();
        editor.setBinder(binder);

        // Configure columns with both filter and editor functionality
        currentConnStatusFilter = configureEditableColumn(
                grid, ConnectionStatus.class, OperatingModeRule::getCurrentConnectionStatus,
                OperatingModeRule::setCurrentConnectionStatus, "Current Connection Status", "150px");
        newConnStatusFilter = configureEditableColumn(
                grid, ConnectionStatus.class, OperatingModeRule::getNewConnectionStatus,
                OperatingModeRule::setNewConnectionStatus, "New Connection Status", "150px");
        currentCdcOkFilter = configureEditableColumn(
                grid, Boolean.class, OperatingModeRule::getCurrentCdcOk,
                OperatingModeRule::setCurrentCdcOk, "Current CDC OK", "150px");
        newCdcOkFilter = configureEditableColumn(
                grid, Boolean.class, OperatingModeRule::getNewCdcOk,
                OperatingModeRule::setNewCdcOk, "New CDC OK", "150px");
        currentUserOpModeFilter = configureEditableColumn(
                grid, UserOperatingMode.class, OperatingModeRule::getCurrentUserOperatingMode,
                OperatingModeRule::setCurrentUserOperatingMode, "Current User Operating Mode", "150px");
        newUserOpModeFilter = configureEditableColumn(
                grid, UserOperatingMode.class, OperatingModeRule::getNewUserOperatingMode,
                OperatingModeRule::setNewUserOperatingMode, "New User Operating Mode", "150px");
        newOperatingModeFilter = configureEditableColumn(
                grid, OperatingMode.class, OperatingModeRule::getNewOperatingMode,
                OperatingModeRule::setNewOperatingMode, "New Operating Mode", "150px");

        grid.addSelectionListener(event -> {
            selectedRule = event.getFirstSelectedItem().orElse(null);
            grid.getDataProvider().refreshAll(); // Refresh the grid to show the delete button only for the selected row
        });

        Button newButton = new Button("New", event -> {
            // Maak een nieuwe OperatingModeRule aan met de waarden uit de filters
            OperatingModeRule newRule = new OperatingModeRule(
                    currentConnStatusFilter.getValue(),
                    newConnStatusFilter.getValue(),
                    currentCdcOkFilter.getValue(),
                    newCdcOkFilter.getValue(),
                    currentUserOpModeFilter.getValue(),
                    newUserOpModeFilter.getValue(),
                    newOperatingModeFilter.getValue()
            );

            // Voeg de nieuwe regel toe aan de regels en refresh de grid
            ruleTable.add(0, newRule);
            // Toon de nieuwe regel, zelfs als deze niet voldoet aan de filters
            List<OperatingModeRule> filteredAndNewRules = ruleTable.stream()
                    .filter(rule -> rule == newRule ||
                            matchesFilters(rule)) // Voegt de nieuwe regel toe, zelfs als hij niet matcht
                    .collect(Collectors.toList());
            grid.setItems(filteredAndNewRules);

            // Zet de nieuwe regel direct in edit-modus
            grid.getEditor().editItem(newRule);
            setButtonsVisible(true); // Toon de Save en Cancel buttons
            isNewRule = true;
        });

        // Configure header
        Span header = new Span("Actions");
        header.getStyle().set("white-space", "pre-wrap");
        header.getStyle().set("text-align", "center");
        header.getStyle().set("height", "30px");

        // FlexLayout to organize header and filter field
        FlexLayout headerLayout = new FlexLayout(header, newButton);
        headerLayout.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
        headerLayout.setAlignItems(FlexLayout.Alignment.CENTER);
        headerLayout.getStyle().set("padding", "0");
        headerLayout.getStyle().set("margin", "0");
        headerLayout.getStyle().set("gap", "10px");

        grid.addComponentColumn(rule -> {
                    Button deleteButton = new Button("Delete");
                    deleteButton.addClickListener(e -> showDeleteConfirmation(rule));

                    // Only show the delete button for the selected row
                    deleteButton.setVisible(selectedRule == rule);

                    return deleteButton;
                })
                .setTextAlign(ColumnTextAlign.CENTER)
                .setHeader(headerLayout)
                .setWidth("100px");

        // Pas de 'Cancel'-knop aan om de nieuwe regel te verwijderen als deze nieuw is
        cancelButton = new Button("Cancel", e -> {
            if (isNewRule) {
                ruleTable.remove(0); // Verwijder de nieuw toegevoegde regel
                grid.getDataProvider().refreshAll(); // Refresh grid om de regel te verwijderen
                isNewRule = false; // Reset de nieuwe regelstatus
            }
            editor.cancel(); // Cancel editing
            setButtonsVisible(false); // Verberg de Save en Cancel buttons
            applyFilters();
        });

        // Pas de 'Save'-knop aan om de nieuwe regel als 'niet nieuw' te markeren na het opslaan
        saveButton = new Button("Save", e -> {
            editor.save(); // Save changes
            setButtonsVisible(false); // Verberg de Save en Cancel buttons
            isNewRule = false; // Reset de nieuwe regelstatus na het opslaan
            applyFilters();
            ruleEngine.saveRules(ruleTable);
        });
        setButtonsVisible(false); // Initially hide buttons

        // Set up the editor to start editing on double-click or Enter key
        grid.addItemDoubleClickListener(event -> {
            if (editor.isOpen()) {
                editor.save(); // Save current row if another row is being edited
            }
            editor.editItem(event.getItem()); // Open editor for the clicked row
            setButtonsVisible(true); // Show Save and Cancel buttons
        });

        // Save and cancel buttons when exiting editor
        editor.addSaveListener(event -> {
            Notification.show("Changes saved.", 2000, Notification.Position.MIDDLE);
        });
        editor.addCancelListener(event -> {
            Notification.show("Edit cancelled.", 2000, Notification.Position.MIDDLE);
        });

        // Layout for Save and Cancel buttons
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);

        // Set grid to full width and height
        grid.setWidthFull();
        grid.setHeight("600px");
        grid.setWidth("1200px");

        // Load the table data from the service
        grid.setItems(ruleTable);

        // Add the grid and buttons to the layout
        layout.add(grid, buttons);
        layout.setWidth("600px");

        // Wrap the form in a Div and return it
        Div content = new Div();
        content.add(layout);
        content.setWidthFull();

        return content;
    }


    /**
     * Configures a column for the grid with a header, filter, and editor.
     *
     * @param grid          The grid to which the column will be added.
     * @param typeClass     The class of the values in the column.
     * @param valueProvider The value provider to get the value for the column.
     * @param setter        The setter for editing values.
     * @param headerText    The text for the column header.
     * @param width         The width of the column.
     * @param <T>           The type of the values in the column.
     * @return The filter component (ComboBox) for the column.
     */
    private <T> ComboBox<T> configureEditableColumn(Grid<OperatingModeRule> grid, Class<T> typeClass,
                                                    ValueProvider<OperatingModeRule, T> valueProvider,
                                                    Setter<OperatingModeRule, T> setter,
                                                    String headerText, String width) {
        ComboBox<T> filterField = new ComboBox<>();
        ComboBox<T> editorField = new ComboBox<>();

        // Set filter and editor items based on type
        if (typeClass.isEnum()) {
            filterField.setItems(typeClass.getEnumConstants());
            editorField.setItems(typeClass.getEnumConstants());
        } else {
            if (Boolean.class.isAssignableFrom(typeClass)) {
                filterField.setItems((T) Boolean.TRUE, (T) Boolean.FALSE);
                editorField.setItems((T) Boolean.TRUE, (T) Boolean.FALSE);
            }
        }

        // Configure header
        Span header = new Span(headerText);
        header.getStyle().set("white-space", "pre-wrap");
        header.getStyle().set("text-align", "center");
        header.getStyle().set("width", width);
        header.getStyle().set("height", "30px");

        // FlexLayout to organize header and filter field
        FlexLayout headerLayout = new FlexLayout(header, filterField);
        headerLayout.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
        headerLayout.setAlignItems(FlexLayout.Alignment.CENTER);
        headerLayout.getStyle().set("padding", "0");
        headerLayout.getStyle().set("margin", "0");
        headerLayout.getStyle().set("gap", "10px");

        // Add column to grid with editor component
        grid.addColumn(valueProvider)
                .setHeader(headerLayout)
                .setWidth(width)
                .setFlexGrow(0)
                .setAutoWidth(false)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setEditorComponent(editorField);

        // Bind editor for editing functionality
        binder.bind(editorField, valueProvider, setter);

        filterField.setPlaceholder("Filter");
        filterField.setWidth("90%"); // Keep filter field slightly narrower than column

        // Filtering logic when filter value changes
        filterField.addValueChangeListener(event -> applyFilters());

        return filterField;
    }

    /**
     * Applies the filters from the ComboBox fields to the grid.
     */
    private void applyFilters() {
        List<OperatingModeRule> filteredRules = ruleTable.stream()
                .filter(this::matchesFilters)
                .collect(Collectors.toList());

        grid.setItems(filteredRules);
    }

    private boolean matchesFilters(OperatingModeRule rule) {
        return filterMatches(rule.getCurrentConnectionStatus(), currentConnStatusFilter.getValue()) &&
                filterMatches(rule.getNewConnectionStatus(), newConnStatusFilter.getValue()) &&
                filterMatches(rule.getCurrentCdcOk(), currentCdcOkFilter.getValue()) &&
                filterMatches(rule.getNewCdcOk(), newCdcOkFilter.getValue()) &&
                filterMatches(rule.getCurrentUserOperatingMode(), currentUserOpModeFilter.getValue()) &&
                filterMatches(rule.getNewUserOperatingMode(), newUserOpModeFilter.getValue()) &&
                filterMatches(rule.getNewOperatingMode(), newOperatingModeFilter.getValue());
    }

    /**
     * Checks if a filter matches the value. If the filter is null, it returns true (no filtering).
     */
    private <T> boolean filterMatches(T value, T filterValue) {
        return filterValue == null || filterValue.equals(value);
    }

    /**
     * Shows a confirmation dialog before deleting a rule.
     */
    private void showDeleteConfirmation(OperatingModeRule rule) {
        Dialog dialog = new Dialog();
        dialog.add("Are you sure you want to delete this rule?");

        Button confirmButton = new Button("OK", e -> {
            ruleTable.remove(rule);
            grid.setItems(ruleTable); // Update grid data
            selectedRule = null; // Clear selection
            grid.getDataProvider().refreshAll(); // Refresh grid to hide delete button
            dialog.close();
            Notification.show("Rule deleted.");
            applyFilters();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        dialog.add(buttons);
        dialog.open();
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
