package com.aicon.tos.connect.web.pages;

import com.aicon.tos.connect.web.dao.AnchorInfo;
import com.aicon.tos.shared.ResultLevel;
import com.avlino.common.KeyValue;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Helper class for basic setup of UI components.
 */
public class ViewHelper {

    public static final String SPACER_HEIGHT = "1em";
    public static final String DEFAULT_WIDTH = "5.66em";
    public static final String DOUBLE_FIELD_WIDTH = "24.66em";
    public static final String QUAD_FIELD_WIDTH = "48.6em";
    public static final String DEFAULT_BUTTON_WIDTH = DEFAULT_WIDTH;
    public static final String HALF_BUTTON_WIDTH = "2.83em";

    public static Div createDiv(String label, String height) {
        Div div = new Div(label);
        div.setHeight(height);
        return div;
    }

    public static Div createSpacer() {
        return createDiv(null, SPACER_HEIGHT);
    }

    public static HorizontalLayout createHorizontalLayout(FlexComponent.Alignment vertAlign) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(false);
        // layout.setSpacing(false);
        layout.setWidthFull();
        if (vertAlign != null) {
            layout.setDefaultVerticalComponentAlignment(vertAlign);
        }
        return layout;
    }

    public static HorizontalLayout createHorizontalLayout() {
        return createHorizontalLayout(FlexComponent.Alignment.END);
    }

    public static VerticalLayout createVerticalLayout(FlexComponent.Alignment horAlign) {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        if (horAlign != null) {
            layout.setDefaultHorizontalComponentAlignment(horAlign);
        }
        return layout;
    }

    public static VerticalLayout createVerticalLayout() {
        return createVerticalLayout(FlexComponent.Alignment.START);
    }

    public static Button createButton(
            String label,
            Icon icon,
            Key shortCutKey,
            ButtonVariant variant,
            String width
    ) {
        Button button = new Button();
        if (label != null       ) {button.setText(label);}
        if (icon != null        ) {button.setIcon(icon);}
        if (shortCutKey != null ) {button.addClickShortcut(shortCutKey);}
        if (variant != null     ) {button.addThemeVariants(variant);}
        button.setWidth(width != null ? width : DEFAULT_BUTTON_WIDTH);
        return button;
    }

    public static Button createButton(String label) {
        return createButton(label, null, null, null, null);
    }

    public static ComboBox createComboBox(
            String label,
            boolean allowNew
    ) {
        ComboBox combo = new ComboBox(label);
        combo.setAutoOpen(true);
        if (allowNew) {
            combo.setAllowCustomValue(true);
        }
        return combo;
    }


    public static TextArea createTextArea(
        String label,
        String value,
        String tooltip,
        Icon icon
    ) {
        TextArea area = new TextArea();
        if (label != null  ) {area.setLabel(label);}
        if (value != null  ) {area.setValue(value);}
        if (tooltip != null) {area.setTooltipText(tooltip);}
        if (icon != null   ) {area.setPrefixComponent(icon);}
        return area;
    }


    public static TextField createTextField(
            String label,
            String value
    ) {
        return createTextField(label, value, null, null);
    }


    public static Checkbox createCheckBox(
            String label,
            Boolean value
    ) {
        Checkbox box = new Checkbox();
        if (label != null  ) {box.setLabel(label);}
        if (value != null  ) {box.setValue(value);}
        return box;
    }


    public static CheckboxGroup<String> createCheckboxGroup(
            String label,
            String[] items,
            String[] selected,
            CheckboxGroupVariant variant
    ) {
    CheckboxGroup<String> box = new CheckboxGroup();
        if (label    != null) {box.setLabel(label);}
        if (items    != null) {box.setItems(items);}
        if (selected != null) {box.select(selected);}
        if (variant  != null) {box.addThemeVariants(variant);}
        return box;
    }


    public static Paragraph createParagraph(String text, boolean preserveLF) {
        Paragraph paragraph = new Paragraph(text);
        if (preserveLF) {
            preserveLineFeed(paragraph);
        }
        return paragraph;
    }


    public static void preserveLineFeed(HasStyle styleComponent) {
        styleComponent.getStyle().set("white-space", "pre-wrap");
    }


    public static RadioButtonGroup<String> createRadioButton(
            String label,
            String[] items,
            String value,
            RadioGroupVariant variant
    ) {
        RadioButtonGroup<String> radio = new RadioButtonGroup();
        if (label   != null) {radio.setLabel(label);}
        if (items   != null) {radio.setItems(items);}
        if (value   != null) {radio.setValue(value);}
        if (variant != null) {radio.addThemeVariants(variant);}
        return radio;
    }


    public static TextField createTextField(
            String label,
            String value,
            String tooltip,
            Icon icon
    ) {
        TextField fld = new TextField();
        if (label != null  ) {fld.setLabel(label);}
        if (value != null  ) {fld.setValue(value);}
        if (tooltip != null) {fld.setTooltipText(tooltip);}
        if (icon != null   ) {fld.setPrefixComponent(icon);}
        return fld;
    }


    public static Grid<String[]> createStringArrayGrid(String[] headers, boolean maxSize, boolean columnReordering, boolean columnsResizable) {
        Grid<String[]> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                final int idx = i;
                grid.addColumn(str -> str[idx])
                        .setKey(headers[i])
                        .setHeader(headers[i])
                        .setResizable(columnsResizable);
            }
        }
        grid.setColumnReorderingAllowed(columnReordering);
        if (maxSize) {
            grid.setSizeFull();
        }
        grid.getStyle().set("flex-grow", "0");
        return grid;
    }


    public static Grid<KeyValue> createKeyValueGrid(String[] headers, String[] width, boolean autoWidth, boolean columnsResizable) {
        Grid<KeyValue> grid = new Grid<>(KeyValue.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        if (headers != null && headers.length >= 2) {
            grid.addColumn(KeyValue::key)  .setKey(headers[0]).setHeader(headers[0]).setAutoWidth(autoWidth).setResizable(columnsResizable);
            grid.addColumn(KeyValue::value).setKey(headers[1]).setHeader(headers[1]).setAutoWidth(autoWidth).setResizable(columnsResizable);
            if (width != null && width.length == headers.length) {
                int idx = 0;
                for (Grid.Column<KeyValue> col: grid.getColumns()) {
                    col.setWidth(width[idx++]);
                }
            }
        }
        return grid;
    }


    public static Anchor createLink(AnchorInfo anchorText, AnchorTarget target) {
        return createLink(anchorText.getHref(), anchorText.getText(), target);
    }


    public static Anchor createLink(String href, String text, AnchorTarget target) {
        if (target == null) {
            target = AnchorTarget.BLANK;
        }
        Anchor link = new Anchor(href, text, target);
        link.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("text-decoration", "none");
        return link;
    }


    public static Span createLinkSpan(String label) {
        Span span = new Span(String.valueOf(label));
        span.getStyle().set("cursor", "pointer").set("color", "blue").set("text-decoration", "underline");
        return span;
    }


    public static String notify(ResultLevel level, String text) {
        if (level != null) {
            switch (level) {
                case OK     -> notifySuccess(text);
                case WARN   -> notifyWarning(text);
                case ERROR  -> notifyError(text);
            }
        }
        return text;
    }


    public static String notifyInfo(String infoText) {
        Notification.show(infoText, 5000, Notification.Position.BOTTOM_START);
        return infoText;
    }


    public static String notifySuccess(String infoText) {
        Notification.show(infoText, 5000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        return infoText;
    }


    public static String notifyWarning(String warnText) {
        Notification.show(warnText, 5000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_WARNING);
        return warnText;
    }


    public static String notifyError(String errText) {
        Notification.show(errText, 10000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_ERROR);
        return errText;
    }
}
