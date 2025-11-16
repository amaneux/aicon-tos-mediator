package com.aicon.tos.shared.config;

import org.jdom2.Element;

public class ConfigItem {

    public static final String ELEMENT_NAME = "ConfigItem";
    public static final String ATT_KEY = "key";
    public static final String ATT_VALUE = "value";
    public static final String ATT_COMMENT = "comment";
    private String key;
    private String value;
    private String comment;

    public ConfigItem(String key, String value) {
        setKey(key);
        setValue(value);
    }

    public ConfigItem(String key, String value, String comment) {
        this(key, value);
        setComment(comment);
    }

    public ConfigItem(String key, long value) {
        setKey(key);
        setValue(String.valueOf(value));
    }

    public ConfigItem(String key, long value, String comment) {
        this(key, value);
        setComment(comment);
    }

    public ConfigItem(Element elm) {
        this(elm.getAttributeValue(ATT_KEY), elm.getAttributeValue(ATT_VALUE), elm.getAttributeValue(ATT_COMMENT));
    }

    public ConfigItem(ConfigItem org) {
        this(org.key(), org.value(), org.comment());
    }

    public String toString() {
        return String.format("ConfigItem(key=%s, value=%s)", key, value);
    }

    public String key() {
        return key;
    }

    public ConfigItem setKey(String key) {
        this.key = key;
        return this;
    }

    public String value() {
        return value;
    }

    public ConfigItem setValue(String value) {
        this.value = value;
        return this;
    }

    public String comment() {
        return comment;
    }

    public ConfigItem setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Element toXml(Element parent) {
        Element elm = new Element(ELEMENT_NAME);
        parent.addContent(elm);
        elm.setAttribute(ATT_KEY, key);
        if (value != null) {
            elm.setAttribute(ATT_VALUE, value);
        }
        if (comment != null) {
            elm.setAttribute(ATT_COMMENT, comment);
        }
        return elm;
    }
}
