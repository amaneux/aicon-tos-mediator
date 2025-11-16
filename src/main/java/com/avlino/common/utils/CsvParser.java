package com.avlino.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NameNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.avlino.common.Constants.COMMA;

/**
 * Reads a CSV from a csv file and copies it into a String[] or list of your own external PoJo objects (so no inner class).
 * The PoJo fields need to match the header names exactly but the PoJo fields determines which csv headers will be read
 * from the dataset.
 */
public class CsvParser {
    static private Logger LOG = LoggerFactory.getLogger(CsvParser.class);

    static public final String DELIMITER = COMMA;

    static private char delimiter = DELIMITER.toCharArray()[0];

    public static void main(String[] args) {
        String csvFile = "C:\\Data\\customers\\RWG\\test-prod\\rehandles_inv_move_M018_1244014183_20250714_115739.csv";

        List<String[]> csvData = parseToList(csvFile, true, String[].class);

        // Print the parsed data
//        for (String[] row : csvData) {
//            for (String cell : row) {
//                System.out.print(cell + " ");
//            }
//            System.out.println();
//        }

//        parse(csvFile, true, CsvRehandlesInvMove.class);
//
//        // Print the parsed data
//        for (CsvRehandlesInvMove move : moves) {
//            System.out.println(move);
//        }
    }

    public static void setDelimiter(char delimiterChar) {
        delimiter = delimiterChar;
    }

    /**
     * Parses the file directly to objects without returning them.
     * When a PojoClass is used, only String and primitive fields may be defined (like long). The mapping is directly
     * to the fields, getters and setters are not used to populate the PoJo.
     *
     * @param filePath the full file path to read from.
     * @param hasHeader when true the first row is expected to be a header row.
     * @param clazz String[].class or PoJo.class.
     * @return the read rows as List<Clazz>; when a Pojo is used, the header row is skipped, else the first row contains the header columns.
     */
    public static <T> void parse(String filePath, boolean hasHeader, Class<T> clazz) {
        parse(filePath, hasHeader, clazz, false);
    }

    /**
     * Parses the file to a list of objects.
     * When a PojoClass is used, only String and primitive fields may be defined (like long). The mapping is directly
     * to the fields, getters and setters are not used to populate the PoJo.
     *
     * @param filePath the full file path to read from.
     * @param hasHeader when true the first row is expected to be a header row.
     * @param clazz String[].class or PoJo.class.
     * @return the read rows as List<Clazz>; when a Pojo is used, the header row is skipped, else the first row contains the header columns.
     */
    public static <T> List<T> parseToList(String filePath, boolean hasHeader, Class<T> clazz) {
        return parse(filePath, hasHeader, clazz, true);
    }

    private static <T> List<T> parse(String filePath, boolean hasHeader, Class<T> clazz, boolean toList) {
        List<T> objects = null;
        String[] fieldValues = null;
        if (toList) {
            objects = new ArrayList<>();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            if (clazz == String[].class) {                        // Handle String[] case
                while ((line = br.readLine()) != null) {
                    @SuppressWarnings("unchecked")
                    T stringArray = (T) parseLine(line);
                    if (toList) {objects.add(stringArray);}
                }
            } else {                                              // Handle custom class case
                List<String> headers = null;
                Field[] fields = clazz.getDeclaredFields();
                while ((line = br.readLine()) != null) {
                    if (headers == null && hasHeader) {
                        headers = Arrays.stream(parseLine(line)).toList();
                        continue;                                 // first line should have the headers
                    }

                    fieldValues = parseLine(line);
                    T object = createObject(clazz, fields, headers, fieldValues);
                    if (toList) {objects.add(object);}
                }
            }
        } catch (Exception e) {
            LOG.error("Error parsing CSV file {} reason: {}", filePath, e.getMessage());
        }

        return objects;
    }

    private static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                fields.add(field.toString().trim());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());

        return fields.toArray(new String[0]);
    }

    private static <T> T createObject(Class<T> clazz, Field[] fields, List<String> headers, String[] fieldValues) {
        try {
            T object = clazz.getDeclaredConstructor().newInstance();
            if (object instanceof CsvRecord) {
                ((CsvRecord) object).beforeParsing(headers, fieldValues);
            }
            int idx = -1;
            String value = null;
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                field.setAccessible(true);
                if (headers != null) {
                    idx = headers.indexOf(field.getName());
                    if (idx >= 0 && idx < fieldValues.length) {
                        value = fieldValues[idx];
                    } else {
                        throw new NameNotFoundException(String.format("Object field %s not found in the CVS headers.", field.getName()));
                    }
                } else if (i >= 0 && i < fieldValues.length) {
                    value = fieldValues[i];
                }

                setFieldValue(object, field, value);
            }
            if (object instanceof CsvRecord) {
                ((CsvRecord) object).afterParsing();
            }
            return object;
        } catch (Exception e) {
            LOG.error("Error converting row to object, reason: {}", e.getMessage());
        }
        return null;
    }

    private static <T> void setFieldValue(T object, Field field, String value) throws Exception {
        Class<?> type = field.getType();
        try {
            if (type == String.class) {
                field.set(object, value);
            } else if (type == int.class) {
                field.setInt(object, Integer.parseInt(value));
            } else if (type == long.class) {
                field.setLong(object, Long.parseLong(value));
            } else if (type == float.class) {
                field.setFloat(object, Float.parseFloat(value));
            } else if (type == double.class) {
                field.setDouble(object, Double.parseDouble(value));
            } else if (type == char.class) {
                field.setChar(object, value.toCharArray()[0]);
            } else if (type == byte.class) {
                field.setByte(object, Byte.parseByte(value));
            } else if (type == short.class) {
                field.setDouble(object, Short.parseShort(value));
            } else if (type == boolean.class) {
                field.setBoolean(object, Boolean.parseBoolean(value));
            }
        } catch (Exception e) {
            LOG.error("Error converting field {} to object, reason: {}", field.getName(), e.getMessage());
        }
    }
}
