package com.avlino.common.datasources;

import com.avlino.common.MetaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static java.sql.Types.LONGVARCHAR;

/**
 * Sql wrapper around the MetaField for conversion from Sql types to java classes and its values.
 */
public class SqlMetaField extends MetaField {

    private static final Logger LOG = LoggerFactory.getLogger(SqlMetaField.class);

    private int colIndex;
    private int sqlType;
    // All digits (including decimals) of the numeric
    private int precision;
    // The number of decimals of the numeric
    private int scale;

    SqlMetaField(int colIndex, ResultSetMetaData metaData) throws SQLException {
        super(metaData.getColumnName(colIndex).toLowerCase(),
                sqlTypeToJavaClass(
                        metaData.getColumnType(colIndex),
                        metaData.getPrecision(colIndex),
                        metaData.getScale(colIndex))
        );
        this.sqlType = metaData.getColumnType(colIndex);
        this.precision = metaData.getPrecision(colIndex);
        this.scale = metaData.getScale(colIndex);
        this.colIndex = colIndex;
    }

    /**
     * Converts Sql type id's for both Oracle and SqlServer to the same Java classes.
     * Keep this one in sync with {@link #getValue(ResultSet)}
     *
     * @param sqlType   the sql type id as found in {@link Types}
     * @param precision the total amount of digits (incl. decimals)
     * @param scale     the amount of decimals
     * @return the java class
     */
    static Class<?> sqlTypeToJavaClass(int sqlType, int precision, int scale) {
        switch (sqlType) {
            case Types.INTEGER:
                return Integer.class;
            case Types.BIGINT, Types.NUMERIC:
                if (scale == 0) {
                    if (precision == 1) return Boolean.class;
                    if (precision <= 10) return Integer.class;
                    return Long.class;
                }
                return Double.class;
            case Types.DOUBLE:
                return Double.class;
            case Types.FLOAT:
                return Float.class;
            case Types.NVARCHAR, Types.VARCHAR, LONGVARCHAR:
                return String.class;
            case Types.BOOLEAN, Types.BIT:
                return Boolean.class;
            case Types.DATE:
                return java.sql.Date.class;
            case Types.TIMESTAMP:
                return java.sql.Timestamp.class;
            default:
                LOG.error("Unsupported SqlType is: {}", sqlType);
                return Object.class; // Fallback for unsupported types
        }
    }


    /**
     * Returns the value of a column in the current record of the resultSet into its java Class.
     *
     * @param rs the resultSet pointing to a record
     * @return the Value in its appropiate java class
     * @throws SQLException something wrong while reading from the db
     */
    public Object getValue(
            ResultSet rs
    ) throws SQLException {
        return switch (sqlType) {
            case Types.INTEGER -> rs.getInt(colIndex);
            case Types.BIGINT, Types.NUMERIC -> {
                if (scale == 0) {
                    if (precision == 1) yield rs.getBoolean(colIndex);
                    if (precision <= 10) yield rs.getInt(colIndex);
                    yield rs.getLong(colIndex);
                }
                yield rs.getDouble(colIndex);
            }
            case Types.DOUBLE -> rs.getDouble(colIndex);
            case Types.FLOAT -> rs.getFloat(colIndex);
            case Types.NVARCHAR, Types.VARCHAR, Types.LONGVARCHAR -> rs.getString(colIndex);
            case Types.BOOLEAN, Types.BIT -> rs.getBoolean(colIndex);
            case Types.DATE -> rs.getDate(colIndex);
            case Types.TIMESTAMP -> rs.getTimestamp(colIndex);
            default -> rs.getObject(colIndex); // Fallback for unsupported types
        };
    }

    @Override
    public String toString() {
        return String.format("SqlField #%s (%s/%s)", colIndex, id(), type().getSimpleName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlMetaField that)) return false;
        if (!super.equals(o)) return false; // ensure superclass fields are compared

        return colIndex == that.colIndex &&
                sqlType == that.sqlType &&
                precision == that.precision &&
                scale == that.scale;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode(); // include superclass fields for hashcode computation
        result = 31 * result + colIndex;
        result = 31 * result + sqlType;
        result = 31 * result + precision;
        result = 31 * result + scale;
        return result;
    }

}
