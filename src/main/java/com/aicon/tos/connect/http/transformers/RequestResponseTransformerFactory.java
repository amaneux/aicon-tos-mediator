package com.aicon.tos.connect.http.transformers;

import com.aicon.tos.shared.exceptions.TransformerCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Factory for creating instances of RequestResponseTransformer implementations.
 * It dynamically loads a class by its fully qualified name and ensures that
 * the class implements the RequestResponseTransformer interface.
 */
public class RequestResponseTransformerFactory {

    protected static final Logger LOG = LoggerFactory.getLogger(RequestResponseTransformerFactory.class);

    /**
     * Creates an instance of the specified class that implements the RequestResponseTransformer interface.
     *
     * @param className The fully qualified name of the class.
     * @return A new instance of the specified class.
     * @throws TransformerCreationException If the class cannot be instantiated or is invalid.
     */
    public static RequestResponseTransformer getTransformer(String className) throws TransformerCreationException {
        Class<?> clazz;

        // Load the class dynamically.
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOG.error("Class '{}' not found.", className, e);
            throw new TransformerCreationException("Class not found: " + className, e);
        }

        // Ensure it implements the required interface.
        if (!RequestResponseTransformer.class.isAssignableFrom(clazz)) {
            throw new TransformerCreationException(
                "Class " + className + " does not implement RequestResponseTransformer.");
        }

        // Instantiate the object using a no-argument constructor.
        try {
            return (RequestResponseTransformer) clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new TransformerCreationException(
                "Class " + className + " requires a no-argument constructor.", e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new TransformerCreationException(
                "Failed to instantiate transformer class: " + className, e);
        }
    }
}