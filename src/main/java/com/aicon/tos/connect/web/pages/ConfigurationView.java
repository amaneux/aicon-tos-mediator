package com.aicon.tos.connect.web.pages;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigItem;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aicon.tos.connect.web.pages.ViewHelper.createButton;
import static com.aicon.tos.connect.web.pages.ViewHelper.createComboBox;
import static com.aicon.tos.connect.web.pages.ViewHelper.createHorizontalLayout;
import static com.aicon.tos.connect.web.pages.ViewHelper.notifyError;
import static com.aicon.tos.connect.web.pages.ViewHelper.notifyInfo;
import static com.aicon.tos.connect.web.pages.ViewHelper.notifyWarning;
import static com.aicon.tos.shared.config.ConfigType.Connections;
import static com.aicon.tos.shared.config.ConfigType.Flows;

@PageTitle("Mediator Configuration")
@Route(value = "ConfigControl", layout = MainLayout.class)
//@RouteAlias(value = "", layout = MainLayout.class)
@Uses(Icon.class)
public class ConfigurationView extends VerticalLayout {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationView.class);

    private static final String COL_KEY = "Key";
    private static final String COL_VALUE = "Value";
    private static final String COL_COMMENT = "Comments";

    ConfigSettings config;
    ConfigGroup selected;
    private ConfigType connType = null;

    private ComboBox comboComponent;
    private ComboBox comboProtocols;
    private ComboBox comboConns;
    private ComboBox comboFlows;
    private ComboBox comboFlowConn;

    private HorizontalLayout hlConnections = createHorizontalLayout();
    private HorizontalLayout hlFlows = createHorizontalLayout();

    private Grid<ConfigItem> grid;

    public ConfigurationView() {
        setSizeFull();
        setMargin(false);
        setSpacing(false);

        config = ConfigSettings.getInstance();
        if (config.getStorageError() != null) {
            ViewHelper.notifyError("Read of environment properties failed, reason: " + config.getStorageError());
        }

        comboComponent = ViewHelper.createComboBox("Components", false);
        comboComponent.setItems(ConfigType.COMPONENTS);

        comboComponent.addValueChangeListener(event -> {
            switchComponent();
            comboConns.setValue(null);
            comboFlows.setValue(null);
        });

        // Setup ComboBoxes

        comboProtocols = createComboBox("Protocol", false);
        comboProtocols.setItems((Object[])ConfigType.CONNECTION_TYPES);
        comboConns = createComboBox("Connection", true);
        // list of connections depends on the protocol

        comboFlows = createComboBox("Flow", true);
        comboFlows.setItems(config.getMainGroup(Flows).getChildrensNamesOrTypes());
        comboFlowConn = createComboBox("TOS Connection", false);

        comboProtocols.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                connType = (ConfigType) event.getValue();
                if (ConfigType.Kafka == connType) {
                    selectGridGroup(config.getMainGroup(Connections).ensure(connType, null, true));
                    comboConns.setVisible(false);
                } else {
                    comboConns.setItems(config.getMainGroup(Connections).getChildrenWithNames(connType));
                    comboConns.setVisible(true);
                }
            }
        });

        comboConns.addCustomValueSetListener(event -> {
            String newValue = ((ComboBoxBase.CustomValueSetEvent) event).getDetail();
            if (newValue != null && !newValue.trim().isEmpty()) {
                ConfigGroup http = config.getMainGroup(Connections).ensure(connType, newValue, false);
                selectGridGroup(http);
                comboConns.setItems(config.getMainGroup(Connections).getChildrenWithNames(connType));
                comboConns.setValue(newValue);
                Notification.show("Added new connection: " + newValue);
            } else {
                Notification.show("Invalid or duplicate connection.");
            }
        });
        comboConns.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                selectGridGroup(config.getMainGroup(Connections).ensure(connType, comboConns.getValue(), true));
                comboConns.setValue(event.getValue());
            }
        });

        comboFlows.addCustomValueSetListener(event -> {
            String newValue = ((ComboBoxBase.CustomValueSetEvent) event).getDetail();
            if (newValue != null && !newValue.trim().isEmpty()) {
                ConfigGroup flow = config.getMainGroup(Flows).ensure(ConfigType.Flow, newValue, false);
                selectGridGroup(flow);
                comboFlows.setItems(config.getMainGroup(Flows).getChildrensNamesOrTypes());
                comboFlows.setValue(newValue);
                Notification.show("Added new connection: " + newValue);
            } else {
                Notification.show("Invalid or duplicate connection.");
            }
        });
        comboFlows.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                ConfigGroup flow = config.getMainGroup(Flows).ensure(ConfigType.Flow, comboFlows.getValue(), false);
                selectGridGroup(flow);
                comboFlowConn.setItems(config.getMainGroup(Connections).getChildrenWithNames(connType));
                ConfigGroup connConfig = flow.getReferenceGroup(ConfigType.CONNECTION_TYPES);
                if (connConfig != null) {
                    comboFlowConn.setValue(connConfig.getRef());
                }
            }
        });

        comboFlowConn.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                String value = (String) event.getValue();
                ConfigGroup connRef = new ConfigGroup(ConfigType.Http).setRef(value);
                selected.replaceChildGroup(connRef);
            }
        });

        // Setup editable grid

        grid = new Grid<>(ConfigItem.class);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);

        Binder<ConfigItem> binder = new Binder<>(ConfigItem.class);
        Editor<ConfigItem> editor = grid.getEditor();
        editor.setBinder(binder);
        editor.setBuffered(true);

        grid.getElement().addEventListener("keydown", e -> editor.cancel())
                .setFilter("event.key === 'Escape' || event.key === 'Esc'");
        grid.addItemDoubleClickListener(event -> editor.editItem(event.getItem()));

        binder.forField(addEditableGridColumn(ConfigItem::key, COL_KEY, "15%")).asRequired("Key must not be empty")
                .bind(ConfigItem::key, ConfigItem::setKey);
        binder.forField(addEditableGridColumn(ConfigItem::value, COL_VALUE, "40%"))
                .bind(ConfigItem::value, ConfigItem::setValue);
        binder.forField(addEditableGridColumn(ConfigItem::comment, COL_COMMENT, "45%"))
                .bind(ConfigItem::comment, ConfigItem::setComment);

        // Create the buttons

        Button butRead = createButton("Read", new Icon(VaadinIcon.UPLOAD), null, ButtonVariant.LUMO_CONTRAST, "8em");
        butRead.setTooltipText("Read configuration from file again (refresh).");
        Button butSave = createButton("Save", new Icon(VaadinIcon.DOWNLOAD), null, ButtonVariant.LUMO_CONTRAST, "8em");

        butRead.addClickListener(e -> {
            String filePath = config.getFullFilename();
            try {
                config.read();
                comboConns.setItems(config.getMainGroup(Connections).getChildrenWithNames(connType));
                comboFlows.setItems(config.getMainGroup(Flows).getChildrensNamesOrTypes());
                String errors = config.getMainGroup(Flows).verifyReferences(config.getMainGroup(Connections));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Read: \n", config.getRoot().toString());
                }
                if (errors == null) {
                    notifyInfo(String.format("Refreshed from %s", filePath));
                } else {
                    notifyWarning(String.format("Refreshed from %s%s", filePath, errors == null ? "" : ", detected the following errors: " + errors));
                }
            } catch (Exception exc) {
                notifyError(String.format("Save to %s failed, reason: %s", filePath, exc));
            }
        });

        butSave.addClickListener(e -> {
            String filePath = config.getFullFilename();
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Saving: \n" + config.getRoot().toString());
                }
                config.save();
                notifyInfo("Saved to " + filePath);
            } catch (Exception exc) {
                notifyError(String.format("Save to %s failed, reason: %s", filePath, exc));
            }
        });

        // Create the layout and all components within

        HorizontalLayout hlTop = createHorizontalLayout();
        hlConnections.add(comboProtocols, comboConns);
        hlConnections.setVerticalComponentAlignment(Alignment.END, comboProtocols, comboConns);

        hlFlows.add(comboFlows, comboFlowConn);
        hlFlows.setVerticalComponentAlignment(Alignment.END, comboFlows, comboFlowConn);

        hlTop.add(butRead, butSave, comboComponent, hlConnections, hlFlows);
        switchComponent();

        add(hlTop, grid);
    }

    private TextField addEditableGridColumn(
            ValueProvider<ConfigItem, ?> valueProvider,
            String header,
            String columnWidth
    ) {
        Grid.Column<ConfigItem> column = grid.addColumn(valueProvider).setHeader(header);
        TextField textField = new TextField();
        textField.setWidthFull();
        textField.addBlurListener(event -> {
            grid.getEditor().save();
        });
        column.setEditorComponent(textField);
        column.setWidth(columnWidth)
                .setResizable(true)
                .setSortable(true);
        return textField;
    }


    private void selectGridGroup(ConfigGroup group) {
        selected = group;
        if (group != null) {
            grid.setItems(group.getItems());
        }
        grid.getDataProvider().refreshAll();
    }


    private void switchComponent() {
//        boolean presetsActive = (Connections == ConfigType.valueOf((String)comboComponent.getValue()));
//        selectGridGroup(presetsActive ? config.getMainGroup(Connections) : config.getMainGroup(Flows));
        if (comboComponent.getValue() != null) {
            selectGridGroup(config.getMainGroup(ConfigType.valueOf((String) comboComponent.getValue())));
        }

//        hlConnections.setVisible(presetsActive);
//        hlFlows.setVisible(!presetsActive);
    }
}
