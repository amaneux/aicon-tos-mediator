package com.avlino.common.utils;

import java.util.List;

/**
 * Marks a PoJo as a Csv record to be read with the {@link CsvParser}. It's use is optional.
 */
public interface CsvRecord {

    /**
     * Wil be called before all fields of a row has been parsed, so you will receive the read String array as is.
     * @param headers optional lsit of column names (may be null)
     * @param values the list of values, headers[idx] = values[idx]
     */
    public default void beforeParsing(List<String> headers, String[] values) {};

    /**
     * Wil be called when all fields of a row has been read. Can be used to do processing based on the full read row.
     */
    public void afterParsing();
}
