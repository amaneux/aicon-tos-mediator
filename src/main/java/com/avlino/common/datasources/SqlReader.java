package com.avlino.common.datasources;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigItem;
import com.aicon.tos.shared.config.ConfigType;
import com.avlino.common.ValueObject;
import com.avlino.common.utils.CsvBuilder;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import oracle.jdbc.OracleDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqlReader {
    private static final Logger LOG = LoggerFactory.getLogger(SqlReader.class);

    public static final int DEFAULT_MAXROWS = 200;
    public static final int DEFAULT_MAXTIME_S = 10;
    public static final String STMT_SCHEMA_S = "ALTER SESSION SET CURRENT_SCHEMA = %s";

    public enum SqlProduct {
        ORACLE   ("jdbc:oracle:thin:@//%s:%s/%s", 1521, OracleDriver.class),
        SQLSERVER("jdbc:sqlserver://;serverName=%s;port=%s;databaseName=%s;encrypt=false", 1433, SQLServerDriver.class),
        MYSQL    ("jdbc:mysql://%s:%s/%s", 3306, null);

        private String url;
        private int defaultPort;
        private Class driverClass;


        SqlProduct (String url, int port, Class driverClass) {
            this.url = url;
            this.defaultPort = port;
            this.driverClass = driverClass;
        }

        String getJdbcUrl(String host, Integer port, String database) {
            try {
                if (driverClass != null) {
                    // This needs to be done when running in Tomcat, but why?
                    Class.forName(driverClass.getName());
                }
            } catch (ClassNotFoundException e) {
                LOG.error("Can't find driver class {}", driverClass.getName());
            }
            return String.format(url, host, port != null ? port : defaultPort, database);
        }
    };

    public static final String CFG_SQL_PRODUCT  = "product";
    public static final String CFG_SQL_HOST     = "host";
    public static final String CFG_SQL_PORT     = "port";
    public static final String CFG_SQL_DATABASE = "database";
    public static final String CFG_SQL_USERNAME = "username";
    public static final String CFG_SQL_PASSWORD = "password";
    public static final String CFG_SQL_SCHEMA = "schema";

    private ConfigGroup configSql = null;
    private SqlProduct prod = null;
    private Connection connection = null;
    private Statement stmt = null;
    private ResultSet resultSet = null;
    private long connectTimeMs = 0;
    private long queryTimeMs = 0;
    private long startTimeMs = 0;
    private int recCount = 0;
    private boolean logQuery = false;
    private List<SqlMetaField> metaFields = null;
    private int maxRows = DEFAULT_MAXROWS;
    private int maxTimeS = DEFAULT_MAXTIME_S;

    public static ConfigGroup getConfigSql() {
        ConfigGroup vitDev = new ConfigGroup(ConfigType.SqlDb)
                .ensureItem(new ConfigItem(CFG_SQL_PRODUCT, SqlProduct.ORACLE.name()))
                .ensureItem(new ConfigItem(CFG_SQL_HOST, "10.111.212.40"))
                .ensureItem(new ConfigItem(CFG_SQL_PORT, "1521"))
                .ensureItem(new ConfigItem(CFG_SQL_DATABASE, "TN4DB"))
                .ensureItem(new ConfigItem(CFG_SQL_USERNAME, "TN4USER"))
                .ensureItem(new ConfigItem(CFG_SQL_PASSWORD, "Avl1n0"));

        ConfigGroup dctDev = new ConfigGroup(ConfigType.SqlDb)
                .ensureItem(new ConfigItem(CFG_SQL_PRODUCT, SqlProduct.SQLSERVER.name()))
                .ensureItem(new ConfigItem(CFG_SQL_HOST, "10.52.80.7"))
                .ensureItem(new ConfigItem(CFG_SQL_PORT, "1433"))
                .ensureItem(new ConfigItem(CFG_SQL_DATABASE, "apex"))
                .ensureItem(new ConfigItem(CFG_SQL_USERNAME, "rdeWaard_avlino"))
                .ensureItem(new ConfigItem(CFG_SQL_PASSWORD, "Z51ho1Vj?"));

        return dctDev; //select correct config
    }

    public static void main(String[] args) {
        ConfigGroup configSql = getConfigSql();

        SqlReader sqlReader = new SqlReader();

        String query = "SELECT * from inv_unit where id = 'TCNU9288491'";
        try {
            sqlReader.connect(configSql);
            sqlReader.executeQuery(query);
            List<SqlMetaField> fields = sqlReader.getMetaFields();
            while (fields != null && sqlReader.nextRecord()) {
                for (int i = 0; i < fields.size(); i++) {
                    SqlMetaField field = fields.get(i);
                    System.out.println(String.format("%s = %s", field, sqlReader.getRecordValue(field)));
                }
            }
            System.out.println(String.format("\n----------- Query resulted in %s record(s) ---------------", sqlReader.getReadCount()));
        } catch (SQLException e) {
            System.out.println("\nQuery execution failed, reason: " + e);
        } finally {
            sqlReader.closeQuery();
            sqlReader.closeConnection();
        }
    }


    /**
     * Creates a physical connection to the database server using the given parameter set.
     * @param configSql the parameter set
     * @return the connection when succeeded
     * @throws SQLException when something went wrong.
     */
    public Connection connect(
            ConfigGroup configSql
    ) throws SQLException {
        String driverClass = "";
        long start = System.currentTimeMillis();
        try {
            this.configSql = configSql;
            prod = SqlProduct.valueOf(configSql.getItemValue(CFG_SQL_PRODUCT));

            String port = configSql.getItemValue(CFG_SQL_PORT);
            String url = prod.getJdbcUrl(
                    configSql.getItemValue(CFG_SQL_HOST),
                    port == null ? null : Integer.valueOf(port),
                    configSql.getItemValue(CFG_SQL_DATABASE)
            );
            start = System.currentTimeMillis();
            connection = DriverManager.getConnection(
                    url,
                    configSql.getItemValue(CFG_SQL_USERNAME), configSql.getItemValue(CFG_SQL_PASSWORD)
            );
            String schema = configSql.getItemValue(CFG_SQL_SCHEMA);
            if (schema != null && !schema.isEmpty() && !schema.contains(" ")) {
                LOG.info("Set schema to: {}", schema);
                Statement stmt = connection.createStatement();
                stmt.execute(String.format(STMT_SCHEMA_S, schema));
            }
            return connection;
        } catch (NumberFormatException nfe) {
            throw new SQLException("Port number not a valid value");
        } catch (IllegalArgumentException iae) {
            throw new SQLException("Not a valid product value");
        } finally {
            connectTimeMs = System.currentTimeMillis() - start;
        }
    }


    /**
     * Use the connection of a previous query execution using the same parameters.
     * @param connection the previous connection to re-use
     * @return the connection
     */
    public Connection connect(
            Connection connection) {
        this.connection = connection;
        return connection;
    }


    /**
     * @return true when a connection object has been found
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }


    public void logQueries(boolean log) {
        logQuery = log;
    }


    /**
     * Performs the actual execution of the query and returns the full result into the <code>CsvBuilder</code>.
     * @param query the query to execute.
     * @param csvBuilder the CsvBuilder to use (which has presets for delimiters, etc).
     * @return the number of records read/processed.
     * @throws SQLException when something went wrong
     */
    public int executeQuery(
            String query,
            CsvBuilder csvBuilder,
            boolean withHeaders
    ) throws SQLException {
        executeQuery(query);
        if (csvBuilder != null) {
            List<SqlMetaField> columns = getMetaFields();
            if (withHeaders) {
                csvBuilder.addColumnRow(columns);
            }

            while (nextRecord()) {
                csvBuilder.addDataRow(getRecordValues());
            }
        }
        return recCount;
    }


    /**
     * Performs the actual execution of the query and sets pointer to the 1st record.
     * Call {@link #nextRecord()}, {@link #getRecordValue(SqlMetaField)} / {@link #getRecordValues()} repeatedly to
     * retrieve all records.
     *
     * @param query the query to execute.
     * @throws SQLException when something went wrong
     */
    public void executeQuery(
            String query
    ) throws SQLException {
        if (connection == null) {
            throw new SQLException("Connection not set, please connect first.");
        }
        recCount = 0;
        metaFields = null;
        startTimeMs = System.currentTimeMillis();

        stmt = connection.createStatement();
        stmt.setMaxRows(maxRows);
        stmt.setQueryTimeout(maxTimeS);
        if (logQuery) {
            LOG.info("Executing query: {}", query);
        }
        try {
            resultSet = stmt.executeQuery(query);
            queryTimeMs = System.currentTimeMillis() - startTimeMs;
        } catch (SQLException e) {
            queryTimeMs = System.currentTimeMillis() - startTimeMs;
            LOG.error("Query failed in {} ms, reason {}", queryTimeMs, e.getMessage());
            throw e;
        } finally {
            LOG.info("First query results in {} ms.", queryTimeMs);
        }
    }


    /**
     * Returns the metadata after the query has been executed
     * @return the ResultSetMetaData containing column info, etc.
     */
    private ResultSetMetaData getMetaData() {
        try {
            return resultSet == null ? null : resultSet.getMetaData();
        } catch (SQLException e) {
            LOG.error("Can't read metadata, reason: {}", e.getMessage());
            return null;
        }
    }


    /**
     * Returns a list of all column names after a query has been executed. Will fetch metadata only once to avoid multiple
     * warnings about retrieved types, etc. Gets reset before every query execution.
     * @return the list of column names, or null when no query has been executed yet.
     */
    public List<SqlMetaField> getMetaFields() {
        if (metaFields != null) {
            return metaFields;
        }
        ResultSetMetaData metaData = getMetaData();
        if (metaData == null) {
            return null;
        }
        metaFields = new ArrayList<>();
        try {
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) { // Columns are 1-based
                SqlMetaField field = new SqlMetaField(i, metaData);
                metaFields.add(field);
            }
        } catch (SQLException e) {
            LOG.error("Reading column names failed, reason: {}", e.getMessage());
        }
        return metaFields;
    }


    /**
     * Returns a list of all column names from the List of SqlMetaField
     * @return the list of column names, or empty when no fields are found.
     */
    public List<String> getColumnNames(List<SqlMetaField> fields) {
        List<String> names = new ArrayList<>();
        if (fields != null) {
            for (SqlMetaField field : fields) {
                names.add(field.id());
            }
        }
        return names;
    }


    /**
     * @return true when next record is available, else false (end-of-list)
     */
    public boolean nextRecord() {
        if (resultSet == null) {
            return false;
        }
        try {
            boolean hasNext = resultSet.next();
            queryTimeMs = System.currentTimeMillis() - startTimeMs;
            if (hasNext) {
                recCount++;
            } else {
                LOG.info("Last record # {} of query fetched after {} ms.", recCount, queryTimeMs);
            }
            return hasNext;
        } catch (SQLException e) {
            LOG.error("Going to next record {} failed, reason: {}", recCount + 1, e.getMessage());
            return false;
        }
    }


    /**
     * Returns the String value of the current record for given column index (matching the one from #getColumnNames).
     * @param field the SqlMetaField to get a value for.
     * @return a new ValueObject (with null value when something failed or the value is just null).
     */
    public ValueObject getRecordValue(SqlMetaField field) {
        ValueObject vo = new ValueObject(field);
        try {
            return vo.set(field.getValue(resultSet));
        } catch (Exception e) {
            LOG.error("Reading value of {} failed, reason: {}", field, e.getMessage());
            return vo;
        }
    }


    /**
     * Returns all values of the current record.
     * @return the list of ValueObject's.
     */
    public List<ValueObject> getRecordValues() {
        List<ValueObject> values = new ArrayList<>(getMetaFields().size());
        for (SqlMetaField field: getMetaFields()) {
            values.add(getRecordValue(field));
        }
        return values;
    }


    /**
     * Closes the query silently, call this after the records has been read and you want to stop or to execute another query.
     * @return the time in ms it took to execute the query and read all data
     */
    public long closeQuery() {
        try {
            if (resultSet != null) {resultSet.close();}
            if (stmt != null) {stmt.close();}
        } catch (SQLException e) {
            LOG.warn("Couldn't close query (resultSet/Statement), reason: {}", e.getMessage());
        } finally {
            resultSet = null;
            stmt = null;
        }
        return System.currentTimeMillis() - startTimeMs;
    }


    /**
     * Closes the physical connection to the database server silently.
     */
    public void closeConnection() {
        try {
            if (connection != null) {connection.close();}
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connection = null;        }
    }


    /**
     * @return the time in ms before connection got established
     */
    public long getConnectTimeMs() {
        return connectTimeMs;
    }


    /**
     * @return the time in ms it took to run a query.
     */
    public long getQueryTimeMs() {
        return queryTimeMs;
    }


    /**
     * @return the number of records read.
     */
    public int getReadCount() {
        return recCount;
    }


    /**
     * Limits the number of rows to fetch.
     * @param maxRows 0 is all, or else another positive number
     */
    public void setMaxRows(int maxRows) {
        if (maxRows >= 0) {
            this.maxRows = maxRows;
        }
    }


    /**
     * Limits the execution time.
     * @param maxTimeS 0 is all, or else another positive number (in seconds)
     */
    public void setMaxTimeS(int maxTimeS) {
        if (maxTimeS >= 0) {
            this.maxTimeS = maxTimeS;
        }
    }
}
