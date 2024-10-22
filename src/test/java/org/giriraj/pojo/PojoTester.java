package org.giriraj.pojo;

import java.lang.reflect.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;

public class PojoTester<T> {

    private final Class<T> clazz;
    private Object[] constructorArgs;

    public PojoTester(Class<T> clazz) {
        this.clazz = clazz;
    }

    // Method to set constructor arguments if constructor-based instantiation is needed
    public PojoTester<T> withConstructorArgs(Object... args) {
        this.constructorArgs = args;
        return this;
    }

    // Main method to handle various test cases based on what the user wants to test
    public PojoTester<T> test(Class<?>... tests) throws Exception {
        T instance = createInstance(); // Create instance using builder or constructor

        for (Class<?> test : tests) {
            if (test == Setters.class) {
                testSetters(instance);
            } else if (test == Getters.class) {
                testGetters(instance);
            } else if (test == ToString.class) {
                testToString(instance);
            } else if (test == HashCode.class) {
                testHashCode(instance);
            } else if (test == Equals.class) {
                testEquals(instance);
            }
        }
        return this;
    }

    // Finalize the testing process
    public void build() {
        // This can be used for additional validations after all tests
    }

    // Method to create an instance using the builder if available or constructor
    private T createInstance() throws Exception {
        Method builderMethod = findBuilderMethod();

        if (builderMethod != null) {
            Object builder = builderMethod.invoke(null); // Call static builder() method
            Method buildMethod = builder.getClass().getMethod("build");
            return (T) buildMethod.invoke(builder); // Call build() method
        } else {
            return createInstanceUsingConstructor();
        }
    }

    // Detect if the class has a builder method
    private Method findBuilderMethod() {
        try {
            return clazz.getMethod("builder");
        } catch (NoSuchMethodException e) {
            return null; // No builder method available
        }
    }

    // Create an instance using the constructor if no builder is available
    private T createInstanceUsingConstructor() throws Exception {
        Constructor<T> constructor = findBestConstructor();
        return constructor.newInstance(constructorArgs != null ? constructorArgs : new Object[0]);
    }

    // Find the best constructor for instantiation
    private Constructor<T> findBestConstructor() throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount)); // Sort by least args first
        Constructor<?> bestConstructor = constructors[0];
        bestConstructor.setAccessible(true);
        return (Constructor<T>) bestConstructor;
    }

    // Test setters to ensure that values are properly set
    private void testSetters(T instance) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            if (isSetter(method)) {
                String fieldName = method.getName().substring(3);
                Field field = clazz.getDeclaredField(decapitalize(fieldName));
                field.setAccessible(true);

                Object value = generateDummyValue(field.getType()); // Generate a test value
                method.invoke(instance, value); // Set value via setter

                assertEquals(value, field.get(instance), "Setter test failed for field: " + field.getName());
            }
        }
    }

    // Test getters to ensure that they return the correct values
    private void testGetters(T instance) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            if (isGetter(method, clazz)) {
                String fieldName = method.getName().startsWith("get")
                        ? method.getName().substring(3)
                        : method.getName().substring(2);

                Field field = clazz.getDeclaredField(decapitalize(fieldName));
                field.setAccessible(true);

                Object expectedValue = generateDummyValue(field.getType()); // Generate a test value
                field.set(instance, expectedValue); // Set value directly via reflection

                Object actualValue = method.invoke(instance); // Get value via getter
                assertEquals(expectedValue, actualValue, "Getter test failed for field: " + field.getName());
            }
        }
    }

    // Test toString method to ensure non-null and correct format
    private void testToString(T instance) throws Exception {
        String toStringResult = instance.toString();
        assertNotNull(toStringResult, "toString() method should not return null");
        assertTrue(toStringResult.contains(clazz.getSimpleName()), "toString() should contain class name");
    }

    // Test hashCode method to ensure it's consistent and follows the contract
    private void testHashCode(T instance) throws Exception {
        // Generate the hashCode for the first time
        int hashCode1 = instance.hashCode();

        // Generate the hashCode for the second time and assert consistency
        int hashCode2 = instance.hashCode();
        assertEquals(hashCode1, hashCode2, "hashCode() should return consistent values");

        // Test non-null fields scenario
        T instanceWithNonNullFields = createInstanceWithNonNullFields(); // Ensure fields are not null
        int hashCodeWithNonNullFields1 = instanceWithNonNullFields.hashCode();
        int hashCodeWithNonNullFields2 = instanceWithNonNullFields.hashCode();
        assertEquals(hashCodeWithNonNullFields1, hashCodeWithNonNullFields2, "hashCode() with non-null fields should return consistent values");

        // Ensure that two distinct instances with different field values do not have the same hashCode
        T differentInstance = createInstanceWithNonNullFields(); // Modify some fields to ensure difference
        assertNotEquals(instance.hashCode(), differentInstance.hashCode(), "Different instances should not have the same hashCode");
    }
    // Test equals method to ensure it follows contract
    private void testEquals(T instance) throws Exception {
        Class<?> clazz = instance.getClass();

        // Reflexive: x.equals(x) must be true
        assertTrue(instance.equals(instance), "equals() failed reflexive test");

        // Symmetric: x.equals(y) == y.equals(x)
        T otherInstance = createInstanceWithNonNullFields(); // Create another instance with non-null fields
        assertEquals(instance.equals(otherInstance), otherInstance.equals(instance), "equals() failed symmetric test");

        // Transitive: if x.equals(y) and y.equals(z), then x.equals(z)
        T thirdInstance = createInstance();
        if (instance.equals(otherInstance) && otherInstance.equals(thirdInstance)) {
            assertTrue(instance.equals(thirdInstance), "equals() failed transitive test");
        }

        // Consistency: Multiple calls must consistently return the same value
        assertEquals(instance.equals(otherInstance), instance.equals(otherInstance), "equals() failed consistency test");

        // Null comparison: x.equals(null) must be false
        assertFalse(instance.equals(null), "equals() failed null comparison test");

        // Additional test: x.equals(object of a different class) must be false
        Object nonSameClassObject = new Object();
        assertFalse(instance.equals(nonSameClassObject), "equals() failed different class comparison");

        // Test unequal field values: Iterate over all fields of the class
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            // Create another instance for field comparison
            T fieldModifiedInstance = createInstance();

            // Modify the current field to a different value
            Object originalValue = field.get(fieldModifiedInstance);
            Object differentValue = getDifferentValue(field.getType(), originalValue);

            // Skip modification if different value couldn't be generated (e.g., no alternative value available)
            if (differentValue != null) {
                field.set(fieldModifiedInstance, differentValue);
                assertFalse(instance.equals(fieldModifiedInstance), "equals() failed when field " + field.getName() + " was different");
            }

            // Reset the field to its original value
            field.set(fieldModifiedInstance, originalValue);
        }

        // --------------- Testing with Non-Null Fields ---------------

        // Populate fields with non-null values
        T nonNullInstance = createInstanceWithNonNullFields();

        // Reflexive: x.equals(x) must still be true with non-null values
        assertTrue(nonNullInstance.equals(nonNullInstance), "equals() failed reflexive test with non-null fields");

        // Symmetric: Test symmetric property with non-null values
        assertEquals(nonNullInstance.equals(instance), instance.equals(nonNullInstance), "equals() failed symmetric test with non-null fields");

        // Consistency test with non-null fields
        assertEquals(nonNullInstance.equals(instance), nonNullInstance.equals(instance), "equals() failed consistency test with non-null fields");

        // Testing inequality after modifying non-null fields
        for (Field field : fields) {
            field.setAccessible(true);

            // Modify the non-null instance for field comparison
            T modifiedNonNullInstance = createInstanceWithNonNullFields();
            Object originalValue = field.get(modifiedNonNullInstance);
            Object differentValue = getDifferentValue(field.getType(), originalValue);

            if (differentValue != null) {
                field.set(modifiedNonNullInstance, differentValue);
                assertFalse(nonNullInstance.equals(modifiedNonNullInstance), "equals() failed with modified non-null field " + field.getName());
            }
        }
    }
    // Method to create an instance using the builder if available or constructor
    private T createInstanceWithNonNullFields() throws Exception {
        T instance = createInstance(); // Create the instance using your existing logic

        // Populate fields with non-null values
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(instance);

            if (value == null) {
                Object nonNullValue = getNonNullValueForField(field.getType());
                field.set(instance, nonNullValue); // Set a non-null value for the field
            }
        }

        return instance;
    }

    // Helper method to return a non-null value based on the field type
    private Object getNonNullValueForField(Class<?> fieldType) {
        if (fieldType.equals(String.class)) {
            return "defaultString"; // Set a default string value
        }
        if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
            return 1; // Set a default integer value
        }
        if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            return true; // Set a default boolean value
        }
        if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return 1.0; // Set a default double value
        }
        if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
            return 1L; // Set a default long value
        }
        if (fieldType.equals(Date.class)) {
            return new Date(System.currentTimeMillis()); // Set a default Date value
        }
        if (fieldType.equals(Timestamp.class)) {
            return new Timestamp(System.currentTimeMillis()); // Set a default Timestamp value
        }

        // Add more types as needed
        return null; // Return null for unsupported types
    }
    /**
     * Create an instance with non-null field values.
     */
    private T createNonNullInstance(Class<?> clazz) throws Exception {
        T instance = (T) clazz.getDeclaredConstructor().newInstance();

        // Set non-null values for all fields
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object nonNullValue = getNonNullValue(field.getType());
            field.set(instance, nonNullValue);
        }

        return instance;
    }

    /**
     * Generate non-null value for fields based on their type.
     */
    private Object getNonNullValue(Class<?> fieldType) {
        if (fieldType.equals(String.class)) {
            return "nonNullString";
        }
        if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
            return 1;
        }
        if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            return true;
        }
        if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return 1.0;
        }
        if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
            return 1L;
        }
        if (fieldType.equals(Date.class)) {
            return new Date(System.currentTimeMillis());
        }
        if (fieldType.equals(Timestamp.class)) {
            return new Timestamp(System.currentTimeMillis());
        }

        // Handle other types if needed
        return null;
    }

    /**
     * Generate a different value for the field based on its type.
     * This method can be enhanced to handle more types if necessary.
     */
    private Object getDifferentValue(Class<?> fieldType, Object originalValue) {
        if (fieldType.equals(String.class)) {
            return "differentString"; // Return a different string value
        }
        if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
            return (originalValue != null && originalValue.equals(1)) ? 2 : 1; // Return a different integer value
        }
        if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            return !(Boolean) originalValue; // Flip the boolean value
        }
        if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return (originalValue != null && originalValue.equals(1.0)) ? 2.0 : 1.0; // Return a different double value
        }
        if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
            return (originalValue != null && originalValue.equals(1L)) ? 2L : 1L; // Return a different long value
        }
        if (fieldType.equals(Date.class)) {
            return (originalValue != null) ? new Date(((Date) originalValue).getTime() + 1000) : new Date(System.currentTimeMillis()); // Return a new java.sql.Date if originalValue is null
        }
        if (fieldType.equals(Timestamp.class)) {
            return (originalValue != null) ? new Timestamp(((Timestamp) originalValue).getTime() + 1000) : new Timestamp(System.currentTimeMillis()); // Return a new Timestamp if originalValue is null
        }

        // Handle other types (float, char, etc.) similarly
        // Return null for types we don't handle or can't modify
        return null;
    }

    // Utility methods to check if the method is a setter or getter
    private boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterCount() == 1;
    }

    private boolean isGetter(Method method, Class<?> clazz) {
        // Check if the method is static
        if (Modifier.isStatic(method.getModifiers())) {
            return false; // Ignore static methods
        }

        // Check if the method is a valid getter
        boolean isValidGetter = (method.getName().startsWith("get") && method.getParameterCount() == 0)
                || (method.getName().startsWith("is") && method.getParameterCount() == 0);

        // If it's a valid getter, ensure it corresponds to an instance field
        if (isValidGetter) {
            String fieldName = method.getName().startsWith("get") ?
                    method.getName().substring(3) : method.getName().substring(2);
            fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1); // convert to camelCase

            // Check if the field exists in the class
            try {
                clazz.getDeclaredField(fieldName);
                return true; // The method is a valid getter for an instance field
            } catch (NoSuchFieldException e) {
                return false; // No corresponding field found
            }
        }

        return false; // Not a valid getter
    }

    // Dummy value generator for test data
    private Object generateDummyValue(Class<?> type) {
        if (type == String.class) return "testValue";
        if (type == int.class || type == Integer.class) return 42;
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == double.class || type == Double.class) return 3.14;
        if (type == long.class || type == Long.class) return 100L;
        // Add more dummy types as needed
        return null;
    }

    // Utility to decapitalize field name
    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    // Nested classes to represent test cases (Setters, Getters, ToString, HashCode, Equals, etc.)
    public static class Setters { }
    public static class Getters { }
    public static class ToString { }
    public static class HashCode { }
    public static class Equals { }
}