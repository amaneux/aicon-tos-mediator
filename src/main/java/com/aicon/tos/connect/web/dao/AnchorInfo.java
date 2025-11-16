package com.aicon.tos.connect.web.dao;


/**
 * Combines a text with its href/url to provide the data for an anchor/link.
 */
public class AnchorInfo {
    private String href;
    private String text;

    public AnchorInfo() {
    }

    public AnchorInfo(String href, String text) {
        this();
        this.href = href;
        this.text = text;
    }

    public String getHref() {
        return href;
    }

    public AnchorInfo setHref(String href) {
        this.href = href;
        return this;
    }

    public String getText() {
        return text;
    }

    public AnchorInfo setText(String text) {
        this.text = text;
        return this;
    }

    public String toString() {
        return text;
    }
}
