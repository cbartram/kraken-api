package com.kraken.api.service.util.reflect;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.service.util.reflect.hooks.model.FieldHook;
import com.kraken.api.service.util.reflect.hooks.model.MethodHook;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling reflection operations, including field access and method invocation.
 * <p>
 * This service maintains a cache of reflected fields and methods to improve performance
 * for repeated accesses. It supports accessing obfuscated members via {@link FieldHook}
 * and {@link MethodHook} definitions.
 */
@Slf4j
@Singleton
public class ReflectionService {

    private final ClassLoader classLoader;
    private final Map<FieldHook, Field> fieldCache = new ConcurrentHashMap<>();
    private final Map<MethodHook, Method> methodCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new ReflectionService.
     *
     * @param client The RuneLite client instance, used to obtain the class loader.
     */
    @Inject
    public ReflectionService(Client client) {
        this.classLoader = client.getClass().getClassLoader();
    }

    /**
     * Retrieves the value of a field specified by the given hook.
     *
     * @param hook     The {@link FieldHook} defining the class and field name.
     * @param instance The object instance to retrieve the field value from.
     * @param <T>      The expected type of the field value.
     * @return The value of the field, or null if an error occurs.
     */
    public <T> T getFieldValue(FieldHook hook, Object instance) {
        try {
            Field field = getField(hook);
            return (T) field.get(instance);
        } catch (Exception e) {
            log.error("Failed to get field {}.{}", hook.getClassName(), hook.getFieldName(), e);
            return null;
        }
    }

    /**
     * Sets the value of a field specified by the given hook.
     *
     * @param hook     The {@link FieldHook} defining the class and field name.
     * @param instance The object instance to set the field value on.
     * @param value    The new value to set.
     */
    public void setFieldValue(FieldHook hook, Object instance, Object value) {
        try {
            Field field = getField(hook);
            field.set(instance, value);
        } catch (Exception e) {
            log.error("Failed to set field {}.{}", hook.getClassName(), hook.getFieldName(), e);
        }
    }

    /**
     * Invokes a method specified by the given hook.
     * <p>
     * This method automatically handles garbage values if the hook specifies one,
     * appending it to the arguments list.
     *
     * @param hook     The {@link MethodHook} defining the class and method name.
     * @param instance The object instance to invoke the method on.
     * @param args     The arguments to pass to the method.
     * @return The result of the method invocation, or null if an error occurs.
     */
    public Object invoke(MethodHook hook, Object instance, Object... args) {
        try {
            Method method = getMethod(hook, args);
            Object[] finalArgs = prepareArgs(hook, args);
            return method.invoke(instance, finalArgs);
        } catch (Exception e) {
            log.error("Failed to invoke {}.{}", hook.getClassName(), hook.getMethodName(), e);
            return null;
        }
    }

    private Field getField(FieldHook hook) throws Exception {
        return fieldCache.computeIfAbsent(hook, h -> {
            try {
                Class<?> clazz = classLoader.loadClass(h.getClassName());
                Field field = clazz.getDeclaredField(h.getFieldName());
                field.setAccessible(true);
                return field;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load field: " + h, e);
            }
        });
    }

    private Method getMethod(MethodHook hook, Object[] args) throws Exception {
        return methodCache.computeIfAbsent(hook, h -> {
            try {
                Class<?> clazz = classLoader.loadClass(h.getClassName());
                int expectedParams = args.length + (h.getGarbageValue() != null ? 1 : 0);

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(h.getMethodName()) &&
                            method.getParameterCount() == expectedParams) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                throw new NoSuchMethodException(
                        String.format("Method %s with %d params not found in %s",
                                h.getMethodName(), expectedParams, h.getClassName())
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to load method: " + h, e);
            }
        });
    }

    private Object[] prepareArgs(MethodHook hook, Object[] args) {
        if (hook.getGarbageValue() == null) {
            return args;
        }

        Object[] newArgs = Arrays.copyOf(args, args.length + 1);
        int gVal = hook.getGarbageValue();

        // Determine primitive type based on value range
        if (gVal >= Byte.MIN_VALUE && gVal <= Byte.MAX_VALUE) {
            newArgs[args.length] = (byte) gVal;
        } else if (gVal >= Short.MIN_VALUE && gVal <= Short.MAX_VALUE) {
            newArgs[args.length] = (short) gVal;
        } else {
            newArgs[args.length] = gVal;
        }

        return newArgs;
    }
}