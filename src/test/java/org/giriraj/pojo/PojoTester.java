package org.giriraj.pojo;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
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
            } // Add more test cases as needed
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

    // Nested classes to represent test cases (Setters, Getters, ToString, etc.)
    public static class Setters { }
    public static class Getters { }
    public static class ToString { }
}