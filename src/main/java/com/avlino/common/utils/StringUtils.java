package com.avlino.common.utils;

import com.aicon.tos.shared.ResultLevel;
import com.avlino.common.Constants;
import io.netty.util.internal.logging.MessageFormatter;

/**
 * Provides convenience methods for Strings.
 */
public class StringUtils implements Constants {

    /**
     * @param str the String to check for
     * @return true if not null && ! isEmpty()
     */
    public static boolean hasContent(String str) {
        return str != null && !str.isEmpty();
    }


    /**
     * @param value     the value to check and return if != null
     * @param nullValue the value to return when value is null
     * @return When <code>value</code> is null, then <code>nullValue</code>Value is returned else <code>value</code>.
     */
    public static String ifNull(Object value, String nullValue) {
        return value == null ? nullValue : String.valueOf(value);
    }


    /**
     * Concats a addValue to oldValue and seperate them with given sep. If prevValue == null then newValue will be returned.
     * @param oldValue the old value to add to
     * @param addValue the value to add
     * @param sep the separator
     * @return oldValue'sep'newValue or oldValue when newValue == null or newValue when oldValue == null
     */
    public static String concat(String oldValue, String addValue, String sep) {
        return oldValue == null ? addValue : addValue == null ? oldValue : oldValue + sep + addValue;
    }


    /**
     * Concats a addValue to oldValue and seperates them with a comma.
     * @param oldValue the old value to add to
     * @param addValue the value to add
     * @return oldValue,newValue or oldValue when newValue == null or newValue when oldValue == null
     */
    public static String concatCsv(String oldValue, String addValue) {
        return concat(oldValue, addValue, COMMA);
    }


    /**
     * Formats a text string having {} as placeholders for given parameters.
     * @param curlyFormat text using {} as placeholders (filled in order of appearance).
     * @param params the parameters to fill in.
     * @return the formatted text.
     */
    public static String format(String curlyFormat, Object... params) {
        if (hasContent(curlyFormat) && params != null && params.length > 0) {
            curlyFormat = MessageFormatter.arrayFormat(curlyFormat, params).getMessage();
        }
        return curlyFormat;
    }
}
