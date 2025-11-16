package com.aicon.tos.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * A utility class for retrieving configuration values from a configuration group.
 * Provides methods to fetch configuration items with a specified type and default value.
 */
public class ConfigUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigUtil.class);

    private ConfigUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves a configuration item from the specified configuration group.
     * The value of the configuration item is converted to the specified type.
     * If the configuration item is not found or its value cannot be converted to the expected type,
     * the default value is returned.
     *
     * @param configGroup  the configuration group containing the configuration items
     * @param itemName     the name of the configuration item to retrieve
     * @param type         the expected type of the configuration item's value
     * @param defaultValue the default value to return if the configuration item is not found
     *                     or its value cannot be converted to the expected type
     * @param <T>          the type of the returned value
     * @return the value of the configuration item converted to the specified type, or the default value if the item
     * cannot be found or converted to the specified type
     */
    public static <T> T getConfigItem(ConfigGroup configGroup, String itemName, Class<T> type, T defaultValue) {
        ConfigItem configItem = configGroup.findItem(itemName);

        if (configItem != null) {
            String value = configItem.value();
            try {
                if (type == Integer.class) {
                    return type.cast(Integer.valueOf(value));
                } else if (type == Boolean.class) {
                    return type.cast(Boolean.valueOf(value));
                } else if (type == Double.class) {
                    return type.cast(Double.valueOf(value));
                } else if (type == Long.class) {
                    return type.cast(Long.valueOf(value));
                } else if (type == String.class) {
                    return type.cast(value);
                } else {
                    LOG.error("Unsupported type {} for ConfigItem {}", type.getSimpleName(), itemName);
                }
            } catch (NumberFormatException e) {
                LOG.error("Invalid format for ConfigItem {}: expected {}, but found '{}'", itemName, type.getSimpleName(), value, e);
            }
        } else {
            LOG.error("ConfigItem {} undefined!", itemName);
        }

        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createObject(String className, Class<T> interfaceType) throws Exception {
        if (className == null) {
            throw new NullPointerException("createObject called with null className");
        }
        // Load the class
        Class<?> clazz = Class.forName(className);

        // Check if the class implements the interface
        if (!interfaceType.isAssignableFrom(clazz)) {
            throw new ClassCastException("Class " + className + " does not implement " + interfaceType.getName()
            );
        }

        // Get the default constructor
        Constructor<?> constructor = clazz.getConstructor();

        // Create and return a new instance
        return (T) constructor.newInstance();
    }
}
