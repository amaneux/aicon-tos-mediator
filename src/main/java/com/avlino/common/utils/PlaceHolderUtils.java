package com.avlino.common.utils;

import com.avlino.common.Constants;
import com.avlino.common.KeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides facilities to find and replace ${} placeholders with its values.
 */
public class PlaceHolderUtils {

    public static final String REGEX_PLACEHOLDER_FINDER = "\\$\\{([^}]+)\\}";      // finds all ${*}
    public static final String PLACEHOLDER_S            = "${%s}";

    private static final Pattern pattern = Pattern.compile(REGEX_PLACEHOLDER_FINDER);

    /**
     * Finds any placeholders in given text (skipped when null) and returns a unique sorted list of them.
     * @param text the text which may contain placeholders
     * @return a list of unique and sorted placeholder names, empty when nothing found.
     */
    static public List<KeyValue> findPlaceHolders(String text) {
        List<KeyValue> placeHolders = new ArrayList<>();
        if (text != null) {
            // Create a pattern to match all placeholders
            Matcher matcher = pattern.matcher(text);
            TreeSet<String> keys = new TreeSet<>();
            while(matcher.find()) {
                keys.add(matcher.group(1));
            }
            for(String key : keys) {
                placeHolders.add(new KeyValue(key));
            }
        }
        return placeHolders;
    }

    /**
     * Replaces all placeholders in the <code>params</code> list with their values.
     * @param text the text to replace placeholders
     * @param params the list of key,value pairs (when null <code>text</code> is returned as is).
     * @return the text with all recognized placeholders replaced by their values (or as is when nothing has been replaced).
     */
    static public String replacePlaceHolders(String text, List<KeyValue> params) {
        if (text != null && params != null) {
            for (KeyValue item : params) {
                text = text.replace(String.format(PLACEHOLDER_S, item.key()), item.valueAsString(Constants.EMPTY));
            }
        }
        return text;
    }

}
