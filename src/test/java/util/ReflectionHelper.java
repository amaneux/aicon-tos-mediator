package util;

import java.lang.reflect.Field;

public class ReflectionHelper {

    // Method to set the value of a private field using reflection
    public static void setPrivateField(Object object, String fieldName, Object value) {

        // Get the declared field from the class
        Field field = null;
        try {
            field = object.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // Make the field accessible to bypass the private modifier
        field.setAccessible(true);

        // Set the value of the field
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
