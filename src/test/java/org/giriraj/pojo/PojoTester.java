package org.giriraj.pojo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PojoTester {

    public static void testPojo(Class<?> clazz) {
        try {
            // Test constructor
            testConstructors(clazz);

            // Test getter and setter methods
            testGettersAndSetters(clazz);

            // Test Builder if present
            testBuilder(clazz);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testConstructors(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            System.out.println("Constructor tested: " + constructor);
        }
    }

    private static void testGettersAndSetters(Class<?> clazz) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            // Find setter method
            String setterName = "set" + capitalize(fieldName);
            Method setter = findMethod(clazz, setterName, fieldType);
            if (setter != null) {
                Object testValue = generateTestValue(fieldType);
                setter.invoke(instance, testValue);
                System.out.println("Setter tested: " + setter);
            }

            // Find getter method
            String getterName = fieldType.equals(boolean.class) ? "is" + capitalize(fieldName) : "get" + capitalize(fieldName);
            Method getter = findMethod(clazz, getterName);
            if (getter != null) {
                Object returnValue = getter.invoke(instance);
                System.out.println("Getter tested: " + getter);
                if (returnValue == null) {
                    throw new AssertionError("Getter returned null for field: " + fieldName);
                }
            }
        }
    }

    private static void testBuilder(Class<?> clazz) throws Exception {
        // Assuming builder method name is 'builder'
        Method builderMethod = findMethod(clazz, "builder");
        if (builderMethod != null) {
            Object builderInstance = builderMethod.invoke(null);
            System.out.println("Builder found and tested for: " + clazz.getSimpleName());

            Method buildMethod = findMethod(builderInstance.getClass(), "build");
            if (buildMethod != null) {
                Object pojoInstance = buildMethod.invoke(builderInstance);
                System.out.println("Builder build method invoked: " + pojoInstance);
            }
        }
    }

    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            System.out.println("Method not found: " + methodName);
            return null;
        }
    }

    private static Object generateTestValue(Class<?> fieldType) {
        if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
            return 42;
        } else if (fieldType.equals(String.class)) {
            return "test";
        } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            return true;
        } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return 42.0;
        }
        // Add more cases for other types
        return null;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
